import { Injectable } from '@angular/core'; // @angular/core v16.x
import { 
  HttpInterceptor, 
  HttpRequest, 
  HttpHandler, 
  HttpEvent, 
  HttpErrorResponse 
} from '@angular/common/http'; // @angular/common/http v16.x
import { Observable, BehaviorSubject, throwError } from 'rxjs'; // rxjs v7.8.0
import { 
  catchError, 
  switchMap, 
  filter, 
  take, 
  finalize, 
  retry, 
  timeout 
} from 'rxjs/operators'; // rxjs/operators v7.8.0

import { JwtService } from './jwt.service';
import { AuthService } from './auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private refreshTokenSubject: BehaviorSubject<string | null> = new BehaviorSubject<string | null>(null);
  private readonly MAX_RETRY_ATTEMPTS = 3;
  private readonly REQUEST_TIMEOUT = 30000; // 30 seconds

  constructor(
    private jwtService: JwtService,
    private authService: AuthService
  ) {}

  /**
   * Intercepts HTTP requests to add JWT token and handle token refresh
   * @param request The outgoing request
   * @param next The next handler in the chain
   * @returns Observable of the HTTP event
   */
  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Skip token injection for public endpoints
    if (this.isPublicEndpoint(request.url)) {
      return next.handle(request).pipe(
        timeout(this.REQUEST_TIMEOUT),
        retry(this.MAX_RETRY_ATTEMPTS)
      );
    }

    // Add token if available and valid
    const token = this.jwtService.getToken();
    if (token && this.jwtService.isTokenValid()) {
      request = this.addToken(request, token);
    }

    return next.handle(request).pipe(
      timeout(this.REQUEST_TIMEOUT),
      retry(this.MAX_RETRY_ATTEMPTS),
      catchError(error => {
        return this.handleError(request, error, next);
      })
    );
  }

  /**
   * Adds JWT token and security headers to the request
   * @param request The original request
   * @param token The JWT token to add
   * @returns Cloned request with token
   */
  private addToken(request: HttpRequest<any>, token: string): HttpRequest<any> {
    return request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
        'X-Request-ID': `req_${Date.now()}`,
        'Cache-Control': 'no-cache',
        Pragma: 'no-cache'
      }
    });
  }

  /**
   * Handles authentication errors and token refresh
   * @param request The failed request
   * @param error The error response
   * @param next The next handler
   * @returns Observable of the HTTP event
   */
  private handleError(
    request: HttpRequest<any>, 
    error: HttpErrorResponse, 
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    // Handle 401 Unauthorized errors
    if (error instanceof HttpErrorResponse && error.status === 401) {
      // Check if we're not already refreshing
      if (!this.isRefreshing) {
        this.isRefreshing = true;
        this.refreshTokenSubject.next(null);

        // Attempt to refresh token
        return this.authService.refreshToken().pipe(
          switchMap(() => {
            this.isRefreshing = false;
            const newToken = this.jwtService.getToken();
            if (!newToken) {
              throw new Error('Token refresh failed');
            }
            this.refreshTokenSubject.next(newToken);
            return next.handle(this.addToken(request, newToken));
          }),
          catchError(refreshError => {
            this.isRefreshing = false;
            this.authService.logout();
            return throwError(() => refreshError);
          }),
          finalize(() => {
            this.isRefreshing = false;
          })
        );
      }

      // Wait for token refresh to complete
      return this.refreshTokenSubject.pipe(
        filter(token => token !== null),
        take(1),
        switchMap(token => {
          if (!token) {
            throw new Error('Token not available');
          }
          return next.handle(this.addToken(request, token));
        })
      );
    }

    // Propagate other errors
    return throwError(() => error);
  }

  /**
   * Checks if the endpoint is public (no authentication required)
   * @param url The request URL
   * @returns boolean indicating if endpoint is public
   */
  private isPublicEndpoint(url: string): boolean {
    const publicPaths = [
      '/auth/login',
      '/auth/refresh',
      '/auth/azure',
      '/public/',
      '/assets/'
    ];
    return publicPaths.some(path => url.includes(path));
  }
}