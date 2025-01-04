package com.pcs.vcms.service;

import com.pcs.vcms.dto.ClearanceDTO;
import com.pcs.vcms.entity.Clearance.ClearanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing vessel clearance operations.
 * Provides comprehensive business operations for clearance submission,
 * approval workflows, and regulatory compliance validation.
 *
 * @version 1.0
 * @since 2023-11-15
 */
public interface ClearanceService {

    /**
     * Submits a new clearance request with comprehensive validation.
     *
     * @param clearanceDTO the clearance request details
     * @return created clearance with generated ID and initial status
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException if vessel call is not in valid state
     */
    ClearanceDTO submitClearance(@Valid ClearanceDTO clearanceDTO);

    /**
     * Updates the status of an existing clearance with transition validation.
     *
     * @param clearanceId the ID of the clearance to update
     * @param newStatus the new status to set
     * @param remarks optional remarks for the status change
     * @return updated clearance with new status
     * @throws IllegalArgumentException if clearance not found
     * @throws IllegalStateException if status transition is invalid
     */
    ClearanceDTO updateClearanceStatus(Long clearanceId, ClearanceStatus newStatus, String remarks);

    /**
     * Retrieves a clearance by its ID.
     *
     * @param clearanceId the ID of the clearance
     * @return optional containing the clearance if found
     */
    Optional<ClearanceDTO> getClearanceById(Long clearanceId);

    /**
     * Retrieves all clearances for a specific vessel call.
     *
     * @param vesselCallId the ID of the vessel call
     * @return list of clearances associated with the vessel call
     */
    List<ClearanceDTO> getClearancesByVesselCall(Long vesselCallId);

    /**
     * Retrieves clearances with pagination and filtering support.
     *
     * @param pageable pagination parameters
     * @param status optional status filter
     * @param fromDate optional from date filter
     * @param toDate optional to date filter
     * @return page of clearances matching the criteria
     */
    Page<ClearanceDTO> getClearances(Pageable pageable, 
                                   ClearanceStatus status,
                                   LocalDateTime fromDate,
                                   LocalDateTime toDate);

    /**
     * Validates if all required clearances are approved for vessel departure.
     *
     * @param vesselCallId the ID of the vessel call
     * @return true if all clearances are approved, false otherwise
     * @throws IllegalArgumentException if vessel call not found
     */
    boolean validateDepartureClearances(Long vesselCallId);

    /**
     * Cancels a pending clearance request.
     *
     * @param clearanceId the ID of the clearance to cancel
     * @param remarks cancellation remarks
     * @return cancelled clearance details
     * @throws IllegalArgumentException if clearance not found
     * @throws IllegalStateException if clearance cannot be cancelled
     */
    ClearanceDTO cancelClearance(Long clearanceId, String remarks);

    /**
     * Retrieves expired clearances that need renewal.
     *
     * @param pageable pagination parameters
     * @return page of expired clearances
     */
    Page<ClearanceDTO> getExpiredClearances(Pageable pageable);

    /**
     * Validates regulatory compliance for a clearance request.
     *
     * @param clearanceDTO the clearance to validate
     * @return true if compliant, false otherwise
     */
    boolean validateRegulatoryCompliance(ClearanceDTO clearanceDTO);

    /**
     * Retrieves clearance history with audit trail.
     *
     * @param clearanceId the ID of the clearance
     * @return list of status changes with timestamps and users
     */
    List<ClearanceDTO> getClearanceHistory(Long clearanceId);
}