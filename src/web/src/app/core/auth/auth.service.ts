import { Injectable } from '@angular/core'; // @angular/core v16.x
import { Router } from '@angular/router'; // @angular/router v16.x
import { BehaviorSubject, Observable, of, throwError, timer } from 'rxjs'; // rxjs v7.8.0
import { map, catchError, tap, retry, debounceTime, switchMap } from 'rxjs/operators'; // rxjs/operators v7.8.0

import { JwtService } from './jwt.service';
import { User, UserRole } from './user.model';
import { ApiService } from '../http/api.service';
import { ApiResponse, ApiErrorResponse } from '../../shared/models/api-response.model';

/**
 * Interface for authentication response from the backend
 */
interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

/**
 * Core authentication service implementing OAuth2/JWT with Azure AD integration.
 * Provides secure user authentication, authorization, and session management.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly AUTH_API_PATH = '/auth';
  private readonly TOKEN_REFRESH_INTERVAL = 300000; // 5 minutes
  private readonly MAX_LOGIN_ATTEMPTS = 3;

  private currentUserSubject: BehaviorSubject<User | null>;
  public currentUser$: Observable<User | null>;
  private loginAttemptsSubject: BehaviorSubject<number>;
  private refreshTokenTimeout: any;

  constructor(
    private apiService: ApiService,
    private jwtService: JwtService,
    private router: Router
  ) {
    this.currentUserSubject = new BehaviorSubject<User | null>(null);
    this.currentUser$ = this.currentUserSubject.asObservable();
    this.loginAttemptsSubject = new BehaviorSubject<number>(0);

    // Initialize authentication state
    this.checkInitialAuth();
  }

  /**
   * Authenticates user with credentials and implements rate limiting
   */
  public login(username: string, password: string): Observable<User> {
    if (this.loginAttemptsSubject.value >= this.MAX_LOGIN_ATTEMPTS) {
      return throwError(() => new Error('Maximum login attempts exceeded. Please try again later.'));
    }

    return this.apiService.post<AuthResponse>(`${this.AUTH_API_PATH}/login`, {
      username,
      password
    }).pipe(
      tap(response => {
        this.handleAuthSuccess(response.data);
        this.loginAttemptsSubject.next(0);
      }),
      map(response => response.data.user),
      catchError((error: ApiErrorResponse) => {
        this.loginAttemptsSubject.next(this.loginAttemptsSubject.value + 1);
        return throwError(() => error);
      })
    );
  }

  /**
   * Initiates Azure AD OAuth2 authentication flow
   */
  public loginWithAzureAD(): Observable<User> {
    return this.apiService.get<AuthResponse>(`${this.AUTH_API_PATH}/azure`).pipe(
      tap(response => this.handleAuthSuccess(response.data)),
      map(response => response.data.user),
      catchError((error: ApiErrorResponse) => throwError(() => error))
    );
  }

  /**
   * Securely logs out user and cleans up session
   */
  public logout(): void {
    // Attempt to notify backend of logout
    this.apiService.post(`${this.AUTH_API_PATH}/logout`, {}).pipe(
      catchError(() => of(null)) // Proceed with local logout even if backend call fails
    ).subscribe(() => {
      this.cleanupAuthSession();
    });
  }

  /**
   * Implements automatic token refresh mechanism
   */
  public refreshToken(): Observable<boolean> {
    const refreshToken = this.jwtService.getRefreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }

    return this.apiService.post<AuthResponse>(`${this.AUTH_API_PATH}/refresh`, {
      refreshToken
    }).pipe(
      tap(response => {
        this.handleAuthSuccess(response.data);
      }),
      map(() => true),
      catchError((error: ApiErrorResponse) => {
        this.cleanupAuthSession();
        return throwError(() => error);
      })
    );
  }

  /**
   * Type-safe role verification with strict checking
   */
  public hasRole(role: UserRole): boolean {
    const currentUser = this.currentUserSubject.value;
    if (!currentUser) {
      return false;
    }

    if (currentUser.role === UserRole.SYSTEM_ADMIN) {
      return true; // System admin has all roles
    }

    return currentUser.role === role;
  }

  /**
   * Checks if user has any of the specified roles
   */
  public hasAnyRole(roles: UserRole[]): boolean {
    return roles.some(role => this.hasRole(role));
  }

  /**
   * Returns current authenticated user
   */
  public getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  /**
   * Initializes authentication state from stored tokens
   */
  private checkInitialAuth(): void {
    if (this.jwtService.isTokenValid()) {
      this.refreshToken().subscribe();
    } else {
      this.cleanupAuthSession();
    }
  }

  /**
   * Handles successful authentication response
   */
  private handleAuthSuccess(authResponse: AuthResponse): void {
    this.jwtService.saveToken(authResponse.accessToken);
    this.jwtService.saveRefreshToken(authResponse.refreshToken);
    this.currentUserSubject.next(authResponse.user);
    this.setupTokenRefresh();
  }

  /**
   * Sets up automatic token refresh
   */
  private setupTokenRefresh(): void {
    if (this.refreshTokenTimeout) {
      clearTimeout(this.refreshTokenTimeout);
    }

    this.refreshTokenTimeout = timer(this.TOKEN_REFRESH_INTERVAL)
      .pipe(
        switchMap(() => this.refreshToken()),
        retry(3)
      )
      .subscribe({
        error: () => this.cleanupAuthSession()
      });
  }

  /**
   * Cleans up authentication session
   */
  private cleanupAuthSession(): void {
    this.jwtService.removeToken();
    this.jwtService.removeRefreshToken();
    this.currentUserSubject.next(null);
    this.loginAttemptsSubject.next(0);
    if (this.refreshTokenTimeout) {
      clearTimeout(this.refreshTokenTimeout);
    }
    this.router.navigate(['/login']);
  }
}