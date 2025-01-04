import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { catchError, map, retry, timeout, finalize } from 'rxjs/operators';
import { 
  ApiResponse, 
  ApiErrorResponse, 
  ApiPaginatedResponse 
} from '../../shared/models/api-response.model';

/**
 * Configuration interface for HTTP requests
 */
interface RequestOptions {
  headers?: HttpHeaders;
  params?: HttpParams | { [param: string]: string | string[] };
  responseType?: 'json';
  withCredentials?: boolean;
  timeout?: number;
  retries?: number;
}

/**
 * Core service for handling all HTTP communications with the backend API.
 * Implements comprehensive error handling, request tracking, and type-safe API communications.
 */
@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly apiUrl: string = '/api/v1'; // Base API URL
  private defaultHeaders: HttpHeaders;
  private readonly requestTimeout: number = 30000; // 30 seconds
  private readonly maxRetries: number = 3;
  private activeRequests: Map<string, AbortController>;

  constructor(private http: HttpClient) {
    this.defaultHeaders = new HttpHeaders({
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      'X-Request-With': 'XMLHttpRequest'
    });
    this.activeRequests = new Map<string, AbortController>();
  }

  /**
   * Performs a type-safe HTTP GET request
   * @param endpoint - API endpoint path
   * @param params - Optional query parameters
   * @param options - Optional request configuration
   * @returns Observable of typed API response
   */
  public get<T>(
    endpoint: string,
    params?: HttpParams | { [param: string]: string | string[] },
    options?: RequestOptions
  ): Observable<ApiResponse<T>> {
    const requestId = this.generateRequestId();
    const controller = new AbortController();
    this.activeRequests.set(requestId, controller);

    const requestOptions = this.buildRequestOptions(options, params, controller.signal);

    return this.http.get<ApiResponse<T>>(`${this.apiUrl}${endpoint}`, requestOptions).pipe(
      timeout(options?.timeout || this.requestTimeout),
      retry({ count: options?.retries || this.maxRetries, delay: 1000 }),
      map(response => this.transformResponse<T>(response)),
      catchError(error => this.handleError(error)),
      finalize(() => this.cleanupRequest(requestId))
    );
  }

  /**
   * Performs a type-safe HTTP POST request
   * @param endpoint - API endpoint path
   * @param body - Request payload
   * @param options - Optional request configuration
   * @returns Observable of typed API response
   */
  public post<T>(
    endpoint: string,
    body: any,
    options?: RequestOptions
  ): Observable<ApiResponse<T>> {
    const requestId = this.generateRequestId();
    const controller = new AbortController();
    this.activeRequests.set(requestId, controller);

    const requestOptions = this.buildRequestOptions(options, undefined, controller.signal);

    return this.http.post<ApiResponse<T>>(`${this.apiUrl}${endpoint}`, body, requestOptions).pipe(
      timeout(options?.timeout || this.requestTimeout),
      retry({ count: options?.retries || this.maxRetries, delay: 1000 }),
      map(response => this.transformResponse<T>(response)),
      catchError(error => this.handleError(error)),
      finalize(() => this.cleanupRequest(requestId))
    );
  }

  /**
   * Performs a type-safe HTTP PUT request
   * @param endpoint - API endpoint path
   * @param body - Request payload
   * @param options - Optional request configuration
   * @returns Observable of typed API response
   */
  public put<T>(
    endpoint: string,
    body: any,
    options?: RequestOptions
  ): Observable<ApiResponse<T>> {
    const requestId = this.generateRequestId();
    const controller = new AbortController();
    this.activeRequests.set(requestId, controller);

    const requestOptions = this.buildRequestOptions(options, undefined, controller.signal);

    return this.http.put<ApiResponse<T>>(`${this.apiUrl}${endpoint}`, body, requestOptions).pipe(
      timeout(options?.timeout || this.requestTimeout),
      retry({ count: options?.retries || this.maxRetries, delay: 1000 }),
      map(response => this.transformResponse<T>(response)),
      catchError(error => this.handleError(error)),
      finalize(() => this.cleanupRequest(requestId))
    );
  }

  /**
   * Performs a type-safe HTTP DELETE request
   * @param endpoint - API endpoint path
   * @param options - Optional request configuration
   * @returns Observable of typed API response
   */
  public delete<T>(
    endpoint: string,
    options?: RequestOptions
  ): Observable<ApiResponse<T>> {
    const requestId = this.generateRequestId();
    const controller = new AbortController();
    this.activeRequests.set(requestId, controller);

    const requestOptions = this.buildRequestOptions(options, undefined, controller.signal);

    return this.http.delete<ApiResponse<T>>(`${this.apiUrl}${endpoint}`, requestOptions).pipe(
      timeout(options?.timeout || this.requestTimeout),
      retry({ count: options?.retries || this.maxRetries, delay: 1000 }),
      map(response => this.transformResponse<T>(response)),
      catchError(error => this.handleError(error)),
      finalize(() => this.cleanupRequest(requestId))
    );
  }

  /**
   * Cancels an active HTTP request
   * @param requestId - Unique identifier of the request to cancel
   */
  public cancelRequest(requestId: string): void {
    const controller = this.activeRequests.get(requestId);
    if (controller) {
      controller.abort();
      this.cleanupRequest(requestId);
      console.log(`Request ${requestId} cancelled`);
    }
  }

  /**
   * Builds request options with headers and parameters
   * @private
   */
  private buildRequestOptions(
    options?: RequestOptions,
    params?: HttpParams | { [param: string]: string | string[] },
    signal?: AbortSignal
  ): any {
    const requestOptions: any = {
      headers: options?.headers || this.defaultHeaders,
      withCredentials: options?.withCredentials ?? true,
      signal
    };

    if (params) {
      requestOptions.params = params instanceof HttpParams ? params : new HttpParams({ fromObject: params });
    }

    return requestOptions;
  }

  /**
   * Transforms API response to ensure type safety
   * @private
   */
  private transformResponse<T>(response: any): ApiResponse<T> {
    return {
      data: response.data,
      status: response.status,
      message: response.message,
      timestamp: response.timestamp,
      requestId: response.requestId
    };
  }

  /**
   * Handles HTTP errors with detailed error mapping
   * @private
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    const errorResponse: ApiErrorResponse = {
      code: 'ERR_UNKNOWN',
      message: 'An unexpected error occurred',
      status: error.status,
      timestamp: new Date().toISOString(),
      details: [],
      requestId: this.generateRequestId(),
      path: error.url || 'unknown'
    };

    if (error.error instanceof ErrorEvent) {
      // Client-side error
      errorResponse.code = 'ERR_CLIENT';
      errorResponse.message = error.error.message;
    } else {
      // Server-side error
      switch (error.status) {
        case 401:
          errorResponse.code = 'ERR_UNAUTHORIZED';
          errorResponse.message = 'Authentication required';
          break;
        case 403:
          errorResponse.code = 'ERR_FORBIDDEN';
          errorResponse.message = 'Access denied';
          break;
        case 404:
          errorResponse.code = 'ERR_NOT_FOUND';
          errorResponse.message = 'Resource not found';
          break;
        case 422:
          errorResponse.code = 'ERR_VALIDATION';
          errorResponse.message = 'Validation failed';
          errorResponse.details = error.error?.details || [];
          break;
        case 0:
          errorResponse.code = 'ERR_NETWORK';
          errorResponse.message = 'Network error occurred';
          break;
        default:
          errorResponse.code = `ERR_${error.status}`;
          errorResponse.message = error.error?.message || 'Server error occurred';
      }
    }

    console.error('API Error:', errorResponse);
    return throwError(() => errorResponse);
  }

  /**
   * Generates a unique request identifier
   * @private
   */
  private generateRequestId(): string {
    return `req_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  /**
   * Cleans up request tracking
   * @private
   */
  private cleanupRequest(requestId: string): void {
    this.activeRequests.delete(requestId);
  }
}