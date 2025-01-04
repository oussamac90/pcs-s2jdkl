package com.pcs.vcms.service;

import com.pcs.vcms.dto.VesselCallDTO;
import com.pcs.vcms.entity.VesselCall.VesselCallStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service interface for managing vessel call operations with comprehensive validation,
 * security controls, and audit capabilities.
 *
 * @version 1.0
 * @since 2023-11-15
 */
public interface VesselCallService {

    /**
     * Creates a new vessel call with comprehensive validation and security checks.
     * Implements duplicate prevention and audit logging.
     *
     * @param vesselCallDTO the vessel call data transfer object
     * @return the created vessel call with audit information
     * @throws SecurityException if user lacks required permissions
     * @throws IllegalArgumentException if validation fails
     * @throws DuplicateCallSignException if call sign already exists
     */
    VesselCallDTO createVesselCall(VesselCallDTO vesselCallDTO);

    /**
     * Updates an existing vessel call with security validation and status transition checks.
     *
     * @param id the vessel call ID
     * @param vesselCallDTO the updated vessel call data
     * @return the updated vessel call with audit trail
     * @throws SecurityException if user lacks required permissions
     * @throws IllegalArgumentException if validation fails
     * @throws EntityNotFoundException if vessel call not found
     */
    VesselCallDTO updateVesselCall(Long id, VesselCallDTO vesselCallDTO);

    /**
     * Retrieves a vessel call by ID with security checks.
     *
     * @param id the vessel call ID
     * @return optional containing the vessel call if found
     * @throws SecurityException if user lacks required permissions
     */
    Optional<VesselCallDTO> getVesselCall(Long id);

    /**
     * Finds a vessel call by call sign with validation.
     *
     * @param callSign the unique call sign
     * @return optional containing the vessel call if found
     * @throws SecurityException if user lacks required permissions
     * @throws IllegalArgumentException if call sign format is invalid
     */
    Optional<VesselCallDTO> findByCallSign(String callSign);

    /**
     * Retrieves vessel calls by status with pagination.
     *
     * @param status the vessel call status
     * @param pageable pagination parameters
     * @return page of vessel calls matching the status
     * @throws SecurityException if user lacks required permissions
     */
    Page<VesselCallDTO> findByStatus(VesselCallStatus status, Pageable pageable);

    /**
     * Finds vessel calls within a date range with pagination.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @param pageable pagination parameters
     * @return page of vessel calls within the date range
     * @throws SecurityException if user lacks required permissions
     * @throws IllegalArgumentException if date range is invalid
     */
    Page<VesselCallDTO> findByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Updates vessel call status with transition validation and security checks.
     *
     * @param id the vessel call ID
     * @param newStatus the new status
     * @return the updated vessel call with audit information
     * @throws SecurityException if user lacks required permissions
     * @throws IllegalStateException if status transition is invalid
     * @throws EntityNotFoundException if vessel call not found
     */
    VesselCallDTO updateStatus(Long id, VesselCallStatus newStatus);

    /**
     * Cancels a vessel call with security validation and notification triggers.
     *
     * @param id the vessel call ID
     * @param reason the cancellation reason
     * @return the cancelled vessel call with audit trail
     * @throws SecurityException if user lacks required permissions
     * @throws IllegalStateException if cancellation is not allowed
     * @throws EntityNotFoundException if vessel call not found
     */
    VesselCallDTO cancelVesselCall(Long id, String reason);
}