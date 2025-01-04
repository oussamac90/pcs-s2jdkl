import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { MatSnackBar, MatSnackBarConfig } from '@angular/material/snack-bar';
import { Observable, throwError } from 'rxjs';
import { retry } from 'rxjs/operators';
import { ApiErrorResponse } from '../../shared/models/api-response.model';

/**
 * Severity levels for error messages
 */
export type ErrorSeverity = 'error' | 'warning' | 'info';

/**
 * Interface for additional error metadata
 */
interface ErrorMetadata {
  component?: string;
  action?: string;
  userId?: string;
  timestamp?: string;
  context?: Record<string, unknown>;
}

/**
 * Service that provides centralized error handling functionality for the Vessel Call Management System.
 * Handles API errors, displays error notifications, and manages error logging with monitoring integration.
 * @version 1.0.0
 */
@Injectable({
  providedIn: 'root'
})
export class ErrorService {
  private readonly DEFAULT_ERROR_MESSAGE = 'An unexpected error occurred. Please try again later.';
  private readonly DEFAULT_SNACKBAR_DURATION = 5000;

  private readonly ERROR_SEVERITY_CONFIG: Record<ErrorSeverity, MatSnackBarConfig> = {
    error: { panelClass: ['error-snackbar'], duration: this.DEFAULT_SNACKBAR_DURATION },
    warning: { panelClass: ['warning-snackbar'], duration: this.DEFAULT_SNACKBAR_DURATION },
    info: { panelClass: ['info-snackbar'], duration: this.DEFAULT_SNACKBAR_DURATION }
  };

  private readonly HTTP_STATUS_MESSAGES: Record<number, string> = {
    400: 'Bad Request: The server could not process the request',
    401: 'Unauthorized: Please log in to continue',
    403: 'Forbidden: You do not have permission to access this resource',
    404: 'Not Found: The requested resource was not found',
    408: 'Request Timeout: The server request timed out',
    500: 'Internal Server Error: Something went wrong on our end',
    502: 'Bad Gateway: The server received an invalid response',
    503: 'Service Unavailable: The server is temporarily unavailable',
    504: 'Gateway Timeout: The server request timed out'
  };

  constructor(private snackBar: MatSnackBar) {}

  /**
   * Handles API errors with comprehensive error transformation and retry logic
   * @param error The HTTP or API error response
   * @param enableRetry Enable automatic retry for failed requests
   * @param maxRetries Maximum number of retry attempts
   * @returns Observable that errors with transformed error
   */
  handleApiError(
    error: HttpErrorResponse | ApiErrorResponse,
    enableRetry = false,
    maxRetries = 3
  ): Observable<never> {
    let transformedError: ApiErrorResponse;

    if (error instanceof HttpErrorResponse) {
      transformedError = {
        code: error.status.toString(),
        message: this.HTTP_STATUS_MESSAGES[error.status] || error.message || this.DEFAULT_ERROR_MESSAGE,
        status: error.status,
        timestamp: new Date().toISOString(),
        details: error.error?.details || [error.message],
        requestId: error.headers?.get('x-request-id') || 'unknown',
        path: error.url || 'unknown'
      };
    } else {
      transformedError = error;
    }

    this.showErrorMessage(
      transformedError.message,
      'Close',
      'error',
      this.DEFAULT_SNACKBAR_DURATION
    );

    this.logError(transformedError, {
      timestamp: new Date().toISOString(),
      context: {
        url: transformedError.path,
        status: transformedError.status
      }
    });

    return enableRetry
      ? throwError(() => transformedError).pipe(retry({ count: maxRetries, delay: 1000 }))
      : throwError(() => transformedError);
  }

  /**
   * Displays error message using Material snackbar with configurable styling
   * @param message Error message to display
   * @param action Action button text
   * @param severity Message severity level
   * @param duration Display duration in milliseconds
   */
  showErrorMessage(
    message: string,
    action = 'Close',
    severity: ErrorSeverity = 'error',
    duration?: number
  ): void {
    const config: MatSnackBarConfig = {
      ...this.ERROR_SEVERITY_CONFIG[severity],
      duration: duration || this.DEFAULT_SNACKBAR_DURATION,
      horizontalPosition: 'center',
      verticalPosition: 'bottom'
    };

    const sanitizedMessage = this.sanitizeMessage(message);
    
    this.snackBar
      .open(sanitizedMessage, action, config)
      .afterDismissed()
      .subscribe(() => {
        // Track user interaction with error message
        this.trackErrorInteraction(sanitizedMessage, severity);
      });
  }

  /**
   * Enhanced error logging with monitoring integration support
   * @param error API error response
   * @param metadata Additional error context metadata
   */
  private logError(error: ApiErrorResponse, metadata?: ErrorMetadata): void {
    const errorLog = {
      timestamp: metadata?.timestamp || new Date().toISOString(),
      level: 'ERROR',
      code: error.code,
      message: error.message,
      details: error.details,
      status: error.status,
      path: error.path,
      requestId: error.requestId,
      metadata: {
        ...metadata,
        environment: process.env['NODE_ENV'],
        userAgent: navigator.userAgent
      }
    };

    // Console logging for development
    if (process.env['NODE_ENV'] !== 'production') {
      console.error('[ErrorService]', errorLog);
    }

    // TODO: Integrate with monitoring service (e.g., Datadog, New Relic)
    this.sendToMonitoringService(errorLog);
  }

  /**
   * Sanitizes error message content
   * @param message Raw error message
   * @returns Sanitized message
   */
  private sanitizeMessage(message: string): string {
    // Basic XSS prevention
    return message
      .replace(/[<>]/g, '')
      .trim() || this.DEFAULT_ERROR_MESSAGE;
  }

  /**
   * Tracks user interaction with error messages
   * @param message Error message
   * @param severity Error severity
   */
  private trackErrorInteraction(message: string, severity: ErrorSeverity): void {
    // TODO: Implement analytics tracking
    console.debug('[ErrorService] Error interaction:', { message, severity });
  }

  /**
   * Sends error data to monitoring service
   * @param errorLog Error log data
   */
  private sendToMonitoringService(errorLog: Record<string, unknown>): void {
    // TODO: Implement monitoring service integration
    console.debug('[ErrorService] Sending to monitoring:', errorLog);
  }
}