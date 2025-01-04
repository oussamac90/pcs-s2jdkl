package com.pcs.vcms.service;

import com.pcs.vcms.dto.ServiceBookingDTO;
import com.pcs.vcms.entity.ServiceBooking.ServiceType;
import com.pcs.vcms.entity.ServiceBooking.ServiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

/**
 * Service interface defining comprehensive business logic operations for managing port service bookings.
 * Handles pilotage, tugboat, and mooring services with enhanced validation, notification integration,
 * and resource tracking capabilities.
 *
 * @version 1.0
 * @since 2023-11-15
 */
public interface ServiceBookingService {

    /**
     * Creates a new service booking with comprehensive validation and conflict detection.
     *
     * @param bookingDTO the service booking request data
     * @return created service booking with confirmation details
     * @throws IllegalArgumentException if booking data is invalid
     * @throws ResourceNotFoundException if vessel call not found
     * @throws ResourceConflictException if booking conflicts with existing reservations
     * @throws InsufficientResourcesException if required resources are unavailable
     */
    ServiceBookingDTO createServiceBooking(ServiceBookingDTO bookingDTO);

    /**
     * Updates the status of an existing service booking with validation and notifications.
     *
     * @param bookingId unique identifier of the booking
     * @param newStatus new status to be applied
     * @return updated service booking with transition details
     * @throws ResourceNotFoundException if booking not found
     * @throws IllegalStateException if status transition is invalid
     * @throws InsufficientResourcesException if resources unavailable for new status
     */
    ServiceBookingDTO updateServiceBookingStatus(Long bookingId, ServiceStatus newStatus);

    /**
     * Retrieves paginated and filtered service bookings with enhanced sorting capabilities.
     *
     * @param serviceType type of service to filter by (optional)
     * @param status booking status to filter by (optional)
     * @param pageable pagination and sorting parameters
     * @return page of filtered service bookings with metadata
     */
    Page<ServiceBookingDTO> getServiceBookingsByTypeAndStatus(
        ServiceType serviceType,
        ServiceStatus status,
        Pageable pageable
    );

    /**
     * Cancels an existing service booking with reason tracking and stakeholder notifications.
     *
     * @param bookingId unique identifier of the booking
     * @param reason cancellation reason
     * @return cancelled service booking with cancellation details
     * @throws ResourceNotFoundException if booking not found
     * @throws IllegalStateException if booking cannot be cancelled
     */
    ServiceBookingDTO cancelServiceBooking(Long bookingId, String reason);

    /**
     * Retrieves service booking details by ID with enhanced validation.
     *
     * @param bookingId unique identifier of the booking
     * @return service booking details
     * @throws ResourceNotFoundException if booking not found
     */
    ServiceBookingDTO getServiceBooking(Long bookingId);

    /**
     * Retrieves all service bookings for a specific vessel call.
     *
     * @param vesselCallId unique identifier of the vessel call
     * @param pageable pagination and sorting parameters
     * @return page of service bookings for the vessel call
     * @throws ResourceNotFoundException if vessel call not found
     */
    Page<ServiceBookingDTO> getServiceBookingsByVesselCall(Long vesselCallId, Pageable pageable);

    /**
     * Checks resource availability for a specific service type and time slot.
     *
     * @param serviceType type of service to check
     * @param scheduledTime proposed service time
     * @param quantity required resource quantity
     * @return true if resources are available, false otherwise
     */
    boolean checkResourceAvailability(
        ServiceType serviceType,
        LocalDateTime scheduledTime,
        Integer quantity
    );

    /**
     * Updates service booking schedule with conflict validation.
     *
     * @param bookingId unique identifier of the booking
     * @param newScheduledTime new proposed service time
     * @return updated service booking with schedule changes
     * @throws ResourceNotFoundException if booking not found
     * @throws ResourceConflictException if new schedule conflicts with existing bookings
     * @throws IllegalStateException if booking cannot be rescheduled
     */
    ServiceBookingDTO rescheduleServiceBooking(Long bookingId, LocalDateTime newScheduledTime);

    /**
     * Validates service booking request against business rules and resource availability.
     *
     * @param bookingDTO service booking request to validate
     * @throws ValidationException if booking request violates business rules
     * @throws ResourceConflictException if booking conflicts with existing reservations
     */
    void validateServiceBooking(ServiceBookingDTO bookingDTO);
}