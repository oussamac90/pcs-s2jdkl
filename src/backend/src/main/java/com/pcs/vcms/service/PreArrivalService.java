package com.pcs.vcms.service;

import com.pcs.vcms.dto.PreArrivalNotificationDTO;
import javax.validation.ValidationResult; // version: 2.0.1.Final
import java.time.LocalDateTime; // version: 17
import java.util.List; // version: 17
import java.util.Optional; // version: 17

/**
 * Service interface for managing pre-arrival notifications in the Vessel Call Management System.
 * Provides comprehensive operations for submission, validation, and processing of vessel pre-arrival
 * information with strict compliance to maritime regulations and security standards.
 */
public interface PreArrivalService {

    /**
     * Submits and processes a new pre-arrival notification with comprehensive validation checks.
     * Handles secure submission of vessel information, cargo manifests, crew details, and associated documentation.
     *
     * @param notification The pre-arrival notification data to be submitted
     * @return Processed notification with validation status and complete processing results
     * @throws IllegalArgumentException if the notification data is invalid
     * @throws SecurityException if the submission violates security policies
     */
    PreArrivalNotificationDTO submitPreArrivalNotification(PreArrivalNotificationDTO notification);

    /**
     * Performs comprehensive validation of pre-arrival notification data against maritime regulations
     * and business rules.
     *
     * @param notification The pre-arrival notification to validate
     * @return Detailed validation results with specific error messages
     */
    ValidationResult validatePreArrivalNotification(PreArrivalNotificationDTO notification);

    /**
     * Retrieves a specific pre-arrival notification by its unique identifier.
     *
     * @param id The unique identifier of the pre-arrival notification
     * @return Optional containing the notification if found
     */
    Optional<PreArrivalNotificationDTO> getPreArrivalNotification(Long id);

    /**
     * Retrieves all pre-arrival notifications for a specific vessel call.
     *
     * @param vesselCallId The unique identifier of the vessel call
     * @return List of pre-arrival notifications associated with the vessel call
     */
    List<PreArrivalNotificationDTO> getPreArrivalNotificationsByVesselCall(Long vesselCallId);

    /**
     * Updates an existing pre-arrival notification with new information.
     *
     * @param id The unique identifier of the notification to update
     * @param notification Updated notification data
     * @return Updated pre-arrival notification with validation status
     * @throws IllegalArgumentException if the notification data is invalid
     * @throws SecurityException if the update violates security policies
     */
    PreArrivalNotificationDTO updatePreArrivalNotification(Long id, PreArrivalNotificationDTO notification);

    /**
     * Retrieves pre-arrival notifications submitted within a specific time range.
     *
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return List of pre-arrival notifications within the specified time range
     */
    List<PreArrivalNotificationDTO> getPreArrivalNotificationsByTimeRange(
            LocalDateTime startTime, 
            LocalDateTime endTime
    );

    /**
     * Validates and processes attached documents for a pre-arrival notification.
     *
     * @param notificationId The unique identifier of the notification
     * @param documentReferences List of document reference identifiers
     * @return Updated pre-arrival notification with processed document references
     * @throws IllegalArgumentException if document references are invalid
     */
    PreArrivalNotificationDTO processDocuments(Long notificationId, List<String> documentReferences);

    /**
     * Cancels a previously submitted pre-arrival notification.
     *
     * @param id The unique identifier of the notification to cancel
     * @param reason The reason for cancellation
     * @return Cancelled pre-arrival notification with updated status
     * @throws IllegalStateException if the notification cannot be cancelled
     */
    PreArrivalNotificationDTO cancelPreArrivalNotification(Long id, String reason);

    /**
     * Retrieves the validation history for a pre-arrival notification.
     *
     * @param id The unique identifier of the notification
     * @return List of validation results with timestamps
     */
    List<ValidationResult> getValidationHistory(Long id);
}