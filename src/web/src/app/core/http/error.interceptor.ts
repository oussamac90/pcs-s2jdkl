import { Injectable } from '@angular/core';
import { 
  HttpInterceptor, 
  HttpRequest, 
  HttpHandler, 
  HttpEvent, 
  HttpErrorResponse 
} from '@angular/common/http';
import { Observable, throwError, timer } from 'rxjs';
import { catchError, retryWhen, delayWhen } from 'rxjs/operators';
import { ErrorService } from '../services/error.service';
import { ApiErrorResponse } from '../../shared/models/api-response.model';

/**
 * Enhanced HTTP interceptor that provides centralized error handling for all HTTP requests
 * in the Vessel Call Management System. Implements retry mechanisms, security error handling,
 * and error tracking.
 * @version 1.0.0
 */
@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  private readonly maxRetries = 3;
  private readonly baseRetryDelay = 1000; // milliseconds
  private readonly retryableStatusCodes: Set<number> = new Set([408, 429, 500, 502, 503, 504]);

  constructor(private errorService: ErrorService) {}

  /**
   * Intercepts HTTP requests and handles any errors with retry mechanisms and tracking
   * @param request The outgoing HTTP request
   * @param next The HTTP handler for the request
   * @returns Observable of the HTTP event stream
   */
  intercept(
    request: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    return next.handle(request).pipe(
      retryWhen(errors => 
        errors.pipe(
          delayWhen((error, index) => {
            if (!this.shouldRetry(error)) {
              return throwError(() => error);
            }
            // Implement exponential backoff
            const retryAttempt = index + 1;
            const delay = this.baseRetryDelay * Math.pow(2, retryAttempt);
            return timer(delay);
          })
        )
      ),
      catchError((error: HttpErrorResponse) => {
        let enhancedError: ApiErrorResponse;

        if (error instanceof HttpErrorResponse) {
          enhancedError = {
            code: error.status.toString(),
            message: error.error?.message || error.message,
            status: error.status,
            timestamp: new Date().toISOString(),
            details: error.error?.details || [error.message],
            requestId: error.headers?.get('x-request-id') || 'unknown',
            path: error.url || request.url
          };

          // Handle specific security-related errors
          switch (error.status) {
            case 401: // Unauthorized
              // Clear any stored authentication tokens
              this.handleAuthenticationError(enhancedError);
              break;
            case 403: // Forbidden
              this.handleAuthorizationError(enhancedError);
              break;
            case 429: // Too Many Requests
              this.handleRateLimitError(enhancedError);
              break;
          }

          // Track security-related errors
          if (this.isSecurityError(error.status)) {
            this.trackSecurityError(enhancedError);
          }
        }

        // Pass the enhanced error to the error service for handling
        return this.errorService.handleApiError(
          enhancedError || error,
          this.shouldRetry(error),
          this.maxRetries
        );
      })
    );
  }

  /**
   * Determines if a request should be retried based on error type and status
   * @param error The HTTP error response
   * @returns boolean indicating if retry should be attempted
   */
  private shouldRetry(error: HttpErrorResponse): boolean {
    if (!(error instanceof HttpErrorResponse)) {
      return false;
    }

    // Don't retry client errors except specific cases
    if (error.status >= 400 && error.status < 500) {
      return error.status === 408 || error.status === 429;
    }

    // Retry server errors that are in the retryable set
    return this.retryableStatusCodes.has(error.status);
  }

  /**
   * Handles authentication errors (401)
   * @param error The enhanced API error response
   */
  private handleAuthenticationError(error: ApiErrorResponse): void {
    // TODO: Implement authentication error handling
    // e.g., redirect to login, clear tokens, etc.
    console.debug('[ErrorInterceptor] Authentication error:', error);
  }

  /**
   * Handles authorization errors (403)
   * @param error The enhanced API error response
   */
  private handleAuthorizationError(error: ApiErrorResponse): void {
    // TODO: Implement authorization error handling
    // e.g., show access denied message, redirect to home, etc.
    console.debug('[ErrorInterceptor] Authorization error:', error);
  }

  /**
   * Handles rate limit errors (429)
   * @param error The enhanced API error response
   */
  private handleRateLimitError(error: ApiErrorResponse): void {
    // TODO: Implement rate limit error handling
    // e.g., show retry after message, implement backoff, etc.
    console.debug('[ErrorInterceptor] Rate limit error:', error);
  }

  /**
   * Determines if the error is security-related
   * @param status HTTP status code
   * @returns boolean indicating if it's a security error
   */
  private isSecurityError(status: number): boolean {
    return [401, 403, 407, 429].includes(status);
  }

  /**
   * Tracks security-related errors for monitoring
   * @param error The enhanced API error response
   */
  private trackSecurityError(error: ApiErrorResponse): void {
    // TODO: Implement security error tracking
    // e.g., send to security monitoring system
    console.debug('[ErrorInterceptor] Security error tracked:', error);
  }
}