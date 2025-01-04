package com.pcs.vcms.exception;

import java.io.Serializable;

/**
 * Custom exception class for handling resource not found scenarios in the Vessel Call Management System.
 * This exception is thrown when a requested resource cannot be found in the system.
 * Implements Serializable to support exception handling in distributed environments.
 *
 * @version 1.0
 * @since 2023-11-15
 */
public class ResourceNotFoundException extends RuntimeException implements Serializable {

    private static final long serialVersionUID = -1L;

    /**
     * Constructs a new ResourceNotFoundException with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception
     */
    public ResourceNotFoundException(String message) {
        super(sanitizeMessage(message));
    }

    /**
     * Constructs a new ResourceNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message explaining the reason for the exception
     * @param cause the cause of the exception
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(sanitizeMessage(message), validateCause(cause));
    }

    /**
     * Constructs a new ResourceNotFoundException with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public ResourceNotFoundException(Throwable cause) {
        super(validateCause(cause));
    }

    /**
     * Sanitizes the exception message to prevent potential security vulnerabilities.
     *
     * @param message the message to sanitize
     * @return the sanitized message
     */
    private static String sanitizeMessage(String message) {
        if (message == null) {
            return "Resource not found";
        }
        // Remove any potentially harmful characters and limit message length
        return message.replaceAll("[^a-zA-Z0-9\\s\\-_.,]", "")
                     .substring(0, Math.min(message.length(), 500));
    }

    /**
     * Validates the cause to ensure proper error chaining.
     *
     * @param cause the cause to validate
     * @return the validated cause
     */
    private static Throwable validateCause(Throwable cause) {
        if (cause == null) {
            throw new IllegalArgumentException("Cause cannot be null");
        }
        return cause;
    }
}