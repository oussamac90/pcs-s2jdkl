/**
 * Generic interface for successful API responses with audit support.
 * Provides type safety and standardization for all successful API communications.
 * @template T The type of data contained in the response
 */
export interface ApiResponse<T> {
  /** The actual response data of type T */
  readonly data: T;
  
  /** HTTP status code of the response */
  readonly status: number;
  
  /** Human-readable success message */
  readonly message: string;
  
  /** ISO 8601 timestamp of when the response was generated */
  readonly timestamp: string;
  
  /** Unique identifier for request tracing */
  readonly requestId: string;
}

/**
 * Comprehensive interface for API error responses with debugging support.
 * Provides detailed error information for proper error handling and debugging.
 */
export interface ApiErrorResponse {
  /** Unique error code for error categorization */
  readonly code: string;
  
  /** Human-readable error message */
  readonly message: string;
  
  /** HTTP status code of the error */
  readonly status: number;
  
  /** ISO 8601 timestamp of when the error occurred */
  readonly timestamp: string;
  
  /** Additional error details or validation messages */
  readonly details: string[];
  
  /** Unique identifier for error tracing */
  readonly requestId: string;
  
  /** The API endpoint path where the error occurred */
  readonly path: string;
}

/**
 * Interface for paginated API responses with navigation metadata.
 * Supports server-side pagination with comprehensive page information.
 * @template T The type of items in the paginated response
 */
export interface ApiPaginatedResponse<T> {
  /** Array of items for the current page */
  readonly data: T[];
  
  /** Total number of items across all pages */
  readonly totalItems: number;
  
  /** Total number of available pages */
  readonly totalPages: number;
  
  /** Current page number (0-based) */
  readonly currentPage: number;
  
  /** Number of items per page */
  readonly pageSize: number;
  
  /** Indicates if there is a next page available */
  readonly hasNext: boolean;
  
  /** Indicates if there is a previous page available */
  readonly hasPrevious: boolean;
}