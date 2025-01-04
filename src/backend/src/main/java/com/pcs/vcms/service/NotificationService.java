package com.pcs.vcms.service;

import com.pcs.vcms.entity.VesselCall;
import com.pcs.vcms.entity.BerthAllocation;
import com.pcs.vcms.entity.ServiceBooking;
import com.pcs.vcms.entity.Clearance;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.constraints.Pattern;

import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.scheduling.annotation.Async;

/**
 * Service interface for managing real-time notifications in the Vessel Call Management System.
 * Provides secure, reliable, and guaranteed delivery of notifications to stakeholders.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Validated
public interface NotificationService {

    /**
     * Sends real-time notification for vessel call updates to subscribed clients.
     * Implements guaranteed delivery with retry mechanism and delivery confirmation.
     *
     * @param vesselCall The vessel call entity containing updated information
     * @return NotificationResult containing delivery status and tracking information
     * @throws IllegalArgumentException if vesselCall is invalid
     * @throws SecurityException if caller lacks required permissions
     */
    @Async
    @Secured({"ROLE_PORT_AUTHORITY", "ROLE_VESSEL_AGENT"})
    NotificationResult sendVesselCallUpdate(@NotNull @Valid VesselCall vesselCall);

    /**
     * Sends real-time notification for berth allocation changes with conflict detection.
     * Includes automated conflict resolution notifications to affected stakeholders.
     *
     * @param berthAllocation The berth allocation entity containing updated information
     * @return NotificationResult containing delivery status and tracking information
     * @throws IllegalArgumentException if berthAllocation is invalid
     * @throws SecurityException if caller lacks required permissions
     */
    @Async
    @Secured("ROLE_PORT_AUTHORITY")
    NotificationResult sendBerthAllocationUpdate(@NotNull @Valid BerthAllocation berthAllocation);

    /**
     * Sends real-time notification for service booking status changes with priority handling.
     * Implements priority-based delivery for time-sensitive service updates.
     *
     * @param serviceBooking The service booking entity containing updated information
     * @return NotificationResult containing delivery status and tracking information
     * @throws IllegalArgumentException if serviceBooking is invalid
     * @throws SecurityException if caller lacks required permissions
     */
    @Async
    @Secured({"ROLE_SERVICE_PROVIDER", "ROLE_VESSEL_AGENT"})
    NotificationResult sendServiceStatusUpdate(@NotNull @Valid ServiceBooking serviceBooking);

    /**
     * Sends real-time notification for clearance status changes with compliance tracking.
     * Includes regulatory compliance validation and audit trail generation.
     *
     * @param clearance The clearance entity containing updated information
     * @return NotificationResult containing delivery status and tracking information
     * @throws IllegalArgumentException if clearance is invalid
     * @throws SecurityException if caller lacks required permissions
     */
    @Async
    @Secured({"ROLE_PORT_AUTHORITY", "ROLE_CUSTOMS"})
    NotificationResult sendClearanceUpdate(@NotNull @Valid Clearance clearance);

    /**
     * Sends targeted notification to a specific user with delivery confirmation.
     * Implements rate limiting and user-specific message queuing.
     *
     * @param userId The target user's unique identifier
     * @param message The notification message content
     * @return NotificationResult containing delivery status and tracking information
     * @throws IllegalArgumentException if userId or message is invalid
     * @throws SecurityException if caller lacks required permissions
     */
    @Async
    @Validated
    NotificationResult sendUserNotification(
        @NotNull @Pattern(regexp = "^[A-Za-z0-9-]+$") String userId,
        @NotNull @Size(min = 1, max = 500) String message
    );

    /**
     * Result class containing notification delivery status and tracking information.
     */
    interface NotificationResult {
        String getTrackingId();
        boolean isDelivered();
        long getDeliveryTimestamp();
        int getRetryCount();
        String getDeliveryStatus();
        String getErrorMessage();
    }
}