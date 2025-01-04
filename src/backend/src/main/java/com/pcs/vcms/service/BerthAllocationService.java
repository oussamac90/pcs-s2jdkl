package com.pcs.vcms.service;

import com.pcs.vcms.dto.BerthAllocationDTO;
import com.pcs.vcms.entity.BerthAllocation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface defining comprehensive business logic operations for managing berth allocations.
 * Implements automated optimization, conflict resolution, and utilization tracking capabilities.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Validated
public interface BerthAllocationService {

    /**
     * Creates a new berth allocation with automated optimization and conflict detection.
     * Implements intelligent allocation algorithms to maximize berth utilization.
     *
     * @param allocationDTO the berth allocation request details
     * @return created berth allocation with optimization results
     * @throws javax.validation.ValidationException if allocation constraints are violated
     * @throws com.pcs.vcms.exception.BerthConflictException if allocation conflicts with existing bookings
     */
    @PreAuthorize("hasRole('BERTH_OPERATOR')")
    BerthAllocationDTO createBerthAllocation(@Valid @NotNull BerthAllocationDTO allocationDTO);

    /**
     * Retrieves a specific berth allocation by its identifier.
     *
     * @param id the berth allocation identifier
     * @return optional containing the berth allocation if found
     */
    @PreAuthorize("hasAnyRole('BERTH_OPERATOR', 'BERTH_PLANNER', 'PORT_ADMIN')")
    Optional<BerthAllocationDTO> getBerthAllocation(@NotNull Long id);

    /**
     * Updates an existing berth allocation with conflict detection.
     *
     * @param id the berth allocation identifier
     * @param allocationDTO updated allocation details
     * @return updated berth allocation
     * @throws javax.validation.ValidationException if allocation constraints are violated
     * @throws com.pcs.vcms.exception.BerthConflictException if update creates conflicts
     */
    @PreAuthorize("hasRole('BERTH_OPERATOR')")
    BerthAllocationDTO updateBerthAllocation(@NotNull Long id, @Valid @NotNull BerthAllocationDTO allocationDTO);

    /**
     * Cancels an existing berth allocation and triggers reoptimization.
     *
     * @param id the berth allocation identifier
     * @throws com.pcs.vcms.exception.ResourceNotFoundException if allocation not found
     */
    @PreAuthorize("hasRole('BERTH_OPERATOR')")
    void cancelBerthAllocation(@NotNull Long id);

    /**
     * Optimizes berth schedule for maximum utilization within a time window.
     * Implements advanced algorithms for optimal berth assignment and conflict resolution.
     *
     * @param startTime start of optimization window
     * @param endTime end of optimization window
     * @return optimized berth allocation schedule
     */
    @PreAuthorize("hasRole('BERTH_PLANNER')")
    List<BerthAllocationDTO> optimizeBerthSchedule(
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime
    );

    /**
     * Retrieves comprehensive berth utilization metrics and statistics.
     *
     * @param startTime start of analysis period
     * @param endTime end of analysis period
     * @return detailed utilization metrics and optimization suggestions
     */
    @PreAuthorize("hasAnyRole('BERTH_PLANNER', 'PORT_ADMIN')")
    Map<String, Object> getBerthUtilizationMetrics(
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime
    );

    /**
     * Checks for potential conflicts with existing allocations.
     *
     * @param allocationDTO allocation to check for conflicts
     * @return list of conflicting allocations if any
     */
    @PreAuthorize("hasAnyRole('BERTH_OPERATOR', 'BERTH_PLANNER')")
    List<BerthAllocationDTO> checkAllocationConflicts(@Valid @NotNull BerthAllocationDTO allocationDTO);

    /**
     * Retrieves all berth allocations for a specific vessel call.
     *
     * @param vesselCallId the vessel call identifier
     * @return list of berth allocations for the vessel call
     */
    @PreAuthorize("hasAnyRole('BERTH_OPERATOR', 'BERTH_PLANNER', 'PORT_ADMIN')")
    List<BerthAllocationDTO> getAllocationsByVesselCall(@NotNull Long vesselCallId);

    /**
     * Retrieves all berth allocations for a specific berth within a time window.
     *
     * @param berthId the berth identifier
     * @param startTime start of time window
     * @param endTime end of time window
     * @return list of berth allocations for the specified berth and time window
     */
    @PreAuthorize("hasAnyRole('BERTH_OPERATOR', 'BERTH_PLANNER', 'PORT_ADMIN')")
    List<BerthAllocationDTO> getAllocationsByBerth(
        @NotNull Long berthId,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime
    );
}