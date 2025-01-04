package com.pcs.vcms.service.impl;

import com.pcs.vcms.dto.ServiceBookingDTO;
import com.pcs.vcms.entity.ServiceBooking;
import com.pcs.vcms.entity.ServiceBooking.ServiceStatus;
import com.pcs.vcms.entity.ServiceBooking.ServiceType;
import com.pcs.vcms.entity.VesselCall;
import com.pcs.vcms.exception.InsufficientResourcesException;
import com.pcs.vcms.exception.ResourceConflictException;
import com.pcs.vcms.exception.ResourceNotFoundException;
import com.pcs.vcms.mapper.ServiceBookingMapper;
import com.pcs.vcms.repository.ServiceBookingRepository;
import com.pcs.vcms.repository.VesselCallRepository;
import com.pcs.vcms.service.ServiceBookingService;
import com.pcs.vcms.service.NotificationService;
import com.pcs.vcms.service.AuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Implementation of ServiceBookingService providing comprehensive business logic
 * for managing port service bookings with advanced features.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Service
@Validated
@Slf4j
@RequiredArgsConstructor
@CacheConfig(cacheNames = "serviceBookings")
@Transactional
public class ServiceBookingServiceImpl implements ServiceBookingService {

    private final ServiceBookingRepository serviceBookingRepository;
    private final VesselCallRepository vesselCallRepository;
    private final ServiceBookingMapper serviceBookingMapper;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final RetryTemplate retryTemplate;

    private static final int MAX_RESOURCE_WINDOW_HOURS = 4;
    private static final int RESOURCE_CONFLICT_THRESHOLD = 80;

    @Override
    @Transactional
    @CachePut(key = "#result.id")
    public ServiceBookingDTO createServiceBooking(@Valid @NotNull ServiceBookingDTO bookingDTO) {
        log.debug("Creating service booking for vessel call: {}", bookingDTO.getVesselCallId());

        // Validate vessel call existence
        VesselCall vesselCall = vesselCallRepository.findById(bookingDTO.getVesselCallId())
            .orElseThrow(() -> new ResourceNotFoundException("Vessel call not found: " + bookingDTO.getVesselCallId()));

        // Validate service booking
        validateServiceBooking(bookingDTO);

        // Check resource availability
        if (!checkResourceAvailability(
                bookingDTO.getServiceType(),
                bookingDTO.getServiceTime(),
                bookingDTO.getQuantity())) {
            throw new InsufficientResourcesException("Insufficient resources available for requested service");
        }

        // Create and save booking
        ServiceBooking booking = serviceBookingMapper.toEntity(bookingDTO);
        booking.setVesselCall(vesselCall);
        booking.setStatus(ServiceStatus.REQUESTED);
        booking = serviceBookingRepository.save(booking);

        // Audit trail
        auditService.logServiceBookingCreation(booking);

        // Send notifications
        notificationService.notifyServiceBookingCreated(booking);

        return serviceBookingMapper.toDTO(booking);
    }

    @Override
    @Transactional
    @CachePut(key = "#bookingId")
    public ServiceBookingDTO updateServiceBookingStatus(
            @NotNull Long bookingId,
            @NotNull ServiceStatus newStatus) {
        log.debug("Updating service booking status: {} to {}", bookingId, newStatus);

        ServiceBooking booking = serviceBookingRepository.findByIdAndNotDeleted(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Service booking not found: " + bookingId));

        validateStatusTransition(booking.getStatus(), newStatus);

        booking.setStatus(newStatus);
        booking = serviceBookingRepository.save(booking);

        // Audit trail
        auditService.logServiceBookingStatusUpdate(booking, newStatus);

        // Send notifications
        notificationService.notifyServiceBookingStatusChanged(booking);

        return serviceBookingMapper.toDTO(booking);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(key = "#bookingId")
    public ServiceBookingDTO getServiceBooking(@NotNull Long bookingId) {
        return serviceBookingRepository.findByIdAndNotDeleted(bookingId)
            .map(serviceBookingMapper::toDTO)
            .orElseThrow(() -> new ResourceNotFoundException("Service booking not found: " + bookingId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceBookingDTO> getServiceBookingsByVesselCall(
            @NotNull Long vesselCallId,
            Pageable pageable) {
        return serviceBookingRepository.findByVesselCallId(vesselCallId)
            .map(serviceBookingMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceBookingDTO> getServiceBookingsByTypeAndStatus(
            ServiceType serviceType,
            ServiceStatus status,
            Pageable pageable) {
        return serviceBookingRepository.findByServiceTypeAndStatus(serviceType, status, pageable)
            .map(serviceBookingMapper::toDTO);
    }

    @Override
    @Transactional
    @CacheEvict(key = "#bookingId")
    public ServiceBookingDTO cancelServiceBooking(
            @NotNull Long bookingId,
            String reason) {
        log.debug("Cancelling service booking: {} with reason: {}", bookingId, reason);

        ServiceBooking booking = serviceBookingRepository.findByIdAndNotDeleted(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Service booking not found: " + bookingId));

        if (!booking.getStatus().isAllowsModification()) {
            throw new IllegalStateException("Booking cannot be cancelled in current status: " + booking.getStatus());
        }

        booking.setStatus(ServiceStatus.CANCELLED);
        booking.setRemarks(reason);
        booking = serviceBookingRepository.save(booking);

        // Audit trail
        auditService.logServiceBookingCancellation(booking, reason);

        // Send notifications
        notificationService.notifyServiceBookingCancelled(booking);

        return serviceBookingMapper.toDTO(booking);
    }

    @Override
    public boolean checkResourceAvailability(
            @NotNull ServiceType serviceType,
            @NotNull LocalDateTime scheduledTime,
            @NotNull Integer quantity) {
        LocalDateTime windowStart = scheduledTime.minus(MAX_RESOURCE_WINDOW_HOURS, ChronoUnit.HOURS);
        LocalDateTime windowEnd = scheduledTime.plus(MAX_RESOURCE_WINDOW_HOURS, ChronoUnit.HOURS);

        Long existingBookings = serviceBookingRepository
            .countByServiceTypeAndServiceTimeBetween(serviceType, windowStart, windowEnd);

        return (existingBookings + quantity) <= getMaxResourcesForType(serviceType);
    }

    @Override
    public void validateServiceBooking(@Valid ServiceBookingDTO bookingDTO) {
        // Validate service time is in future
        if (bookingDTO.getServiceTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Service time must be in the future");
        }

        // Check for booking conflicts
        List<ServiceBooking> overlappingBookings = serviceBookingRepository
            .findOverlappingBookings(
                bookingDTO.getServiceType(),
                bookingDTO.getServiceTime(),
                bookingDTO.getServiceTime().plus(MAX_RESOURCE_WINDOW_HOURS, ChronoUnit.HOURS));

        if (!overlappingBookings.isEmpty()) {
            throw new ResourceConflictException("Conflicting service bookings exist for the requested time");
        }
    }

    private void validateStatusTransition(ServiceStatus currentStatus, ServiceStatus newStatus) {
        if (!isValidStatusTransition(currentStatus, newStatus)) {
            throw new IllegalStateException(
                String.format("Invalid status transition from %s to %s", currentStatus, newStatus));
        }
    }

    private boolean isValidStatusTransition(ServiceStatus current, ServiceStatus next) {
        switch (current) {
            case REQUESTED:
                return next == ServiceStatus.CONFIRMED || next == ServiceStatus.CANCELLED;
            case CONFIRMED:
                return next == ServiceStatus.IN_PROGRESS || next == ServiceStatus.CANCELLED;
            case IN_PROGRESS:
                return next == ServiceStatus.COMPLETED || next == ServiceStatus.CANCELLED;
            default:
                return false;
        }
    }

    private int getMaxResourcesForType(ServiceType serviceType) {
        switch (serviceType) {
            case PILOTAGE:
                return 5;
            case TUGBOAT:
                return 8;
            case MOORING:
            case UNMOORING:
                return 4;
            default:
                return 1;
        }
    }
}