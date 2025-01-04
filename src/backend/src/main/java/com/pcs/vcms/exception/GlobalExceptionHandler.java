package com.pcs.vcms.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for the Vessel Call Management System.
 * Provides centralized error handling with enhanced security monitoring and logging capabilities.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String CORRELATION_ID = "correlationId";
    private static final String TIMESTAMP = "timestamp";
    private static final String STATUS = "status";
    private static final String ERROR = "error";
    private static final String MESSAGE = "message";
    private static final String PATH = "path";
    private static final String DETAILS = "details";

    /**
     * Handles ResourceNotFoundException with enhanced security context tracking.
     *
     * @param ex the ResourceNotFoundException to handle
     * @return ResponseEntity containing error details and NOT_FOUND status
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException ex) {
        String correlationId = generateCorrelationId();
        try {
            MDC.put(CORRELATION_ID, correlationId);
            LOGGER.error("Resource not found exception: {}", ex.getMessage());

            Map<String, Object> errorResponse = createErrorResponse(
                HttpStatus.NOT_FOUND,
                "Resource Not Found",
                ex.getMessage(),
                correlationId
            );

            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        } finally {
            MDC.remove(CORRELATION_ID);
        }
    }

    /**
     * Handles ValidationException with detailed error tracking and security logging.
     *
     * @param ex the ValidationException to handle
     * @return ResponseEntity containing validation errors and BAD_REQUEST status
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> handleValidationException(ValidationException ex) {
        String correlationId = generateCorrelationId();
        try {
            MDC.put(CORRELATION_ID, correlationId);
            LOGGER.error("Validation exception occurred: {}", ex.toString());

            Map<String, Object> errorResponse = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation Error",
                "Multiple validation errors occurred",
                correlationId
            );
            errorResponse.put(DETAILS, ex.getErrors());

            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } finally {
            MDC.remove(CORRELATION_ID);
        }
    }

    /**
     * Handles all uncaught exceptions with comprehensive security monitoring.
     *
     * @param ex the Exception to handle
     * @return ResponseEntity containing error details and INTERNAL_SERVER_ERROR status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception ex) {
        String correlationId = generateCorrelationId();
        try {
            MDC.put(CORRELATION_ID, correlationId);
            LOGGER.error("Unexpected error occurred: ", ex);

            Map<String, Object> errorResponse = createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please contact system administrator.",
                correlationId
            );

            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            MDC.remove(CORRELATION_ID);
        }
    }

    /**
     * Creates a standardized error response with security context and timestamp.
     *
     * @param status HTTP status of the error
     * @param error error type description
     * @param message detailed error message
     * @param correlationId unique identifier for error tracking
     * @return Map containing structured error response
     */
    private Map<String, Object> createErrorResponse(
            HttpStatus status,
            String error,
            String message,
            String correlationId) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put(TIMESTAMP, LocalDateTime.now().toString());
        errorResponse.put(STATUS, status.value());
        errorResponse.put(ERROR, error);
        errorResponse.put(MESSAGE, sanitizeErrorMessage(message));
        errorResponse.put(CORRELATION_ID, correlationId);
        return errorResponse;
    }

    /**
     * Generates a unique correlation ID for error tracking.
     *
     * @return UUID string for correlation
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Sanitizes error messages to prevent security vulnerabilities.
     *
     * @param message the error message to sanitize
     * @return sanitized error message
     */
    private String sanitizeErrorMessage(String message) {
        if (message == null) {
            return "No additional information available";
        }
        // Remove potentially harmful characters and limit message length
        return message.replaceAll("[^a-zA-Z0-9\\s\\-_.,]", "")
                     .substring(0, Math.min(message.length(), 500));
    }
}