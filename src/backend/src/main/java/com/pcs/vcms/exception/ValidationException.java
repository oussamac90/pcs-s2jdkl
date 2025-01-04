package com.pcs.vcms.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Thread-safe custom exception class for handling validation errors in maritime operations.
 * Provides immutable error collection and comprehensive error tracking capabilities.
 * 
 * @version 1.0
 * @since 2023-11-15
 */
public class ValidationException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Thread-safe immutable collection of validation error messages.
     */
    private final List<String> errors;
    
    /**
     * Constructs a ValidationException with a single error message.
     * 
     * @param message The validation error message (must not be null)
     * @throws NullPointerException if message is null
     */
    public ValidationException(String message) {
        super(Objects.requireNonNull(message, "Validation error message must not be null"));
        this.errors = new ArrayList<>();
        this.errors.add(message);
    }
    
    /**
     * Constructs a ValidationException with multiple error messages.
     * 
     * @param errors List of validation error messages (must not be null or empty)
     * @throws NullPointerException if errors list is null
     * @throws IllegalArgumentException if errors list is empty
     */
    public ValidationException(List<String> errors) {
        super(validateErrorsList(errors));
        this.errors = new ArrayList<>();
        this.errors.addAll(errors);
    }
    
    /**
     * Validates the error list and returns the first error message for the super constructor.
     * 
     * @param errors List of validation error messages to validate
     * @return The first error message from the list
     * @throws NullPointerException if errors list is null
     * @throws IllegalArgumentException if errors list is empty
     */
    private static String validateErrorsList(List<String> errors) {
        Objects.requireNonNull(errors, "Validation errors list must not be null");
        if (errors.isEmpty()) {
            throw new IllegalArgumentException("Validation errors list must not be empty");
        }
        return errors.get(0);
    }
    
    /**
     * Returns an immutable view of the validation error messages.
     * 
     * @return Unmodifiable list of validation error messages
     */
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }
    
    /**
     * Returns the number of validation errors.
     * 
     * @return Count of validation errors
     */
    public int size() {
        return errors.size();
    }
    
    /**
     * Checks if there are any validation errors.
     * 
     * @return true if no errors exist, false otherwise
     */
    public boolean isEmpty() {
        return errors.isEmpty();
    }
    
    /**
     * Returns a formatted string representation of all validation errors.
     * 
     * @return String containing the class name, error count, and all error messages
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ValidationException[")
               .append("errorCount=")
               .append(size())
               .append(", errors=[");
        
        for (int i = 0; i < errors.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(errors.get(i));
        }
        builder.append("]]");
        
        return builder.toString();
    }
}