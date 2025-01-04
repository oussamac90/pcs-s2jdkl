package com.pcs.vcms.repository;

import com.pcs.vcms.entity.BerthAllocation;
import com.pcs.vcms.entity.BerthAllocation.BerthAllocationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing berth allocation data persistence and querying operations.
 * Provides comprehensive data access methods for berth scheduling, conflict detection,
 * and status management in the port management system.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Repository
public interface BerthAllocationRepository extends JpaRepository<BerthAllocation, Long> {

    /**
     * Finds all berth allocations for a specific berth within a time range.
     * Useful for schedule management and availability checking.
     *
     * @param berthId the ID of the berth
     * @param startTime the start of the time range
     * @param endTime the end of the time range
     * @return List of berth allocations within the specified time range
     */
    @Query("SELECT ba FROM BerthAllocation ba " +
           "WHERE ba.berth.id = :berthId " +
           "AND ba.startTime >= :startTime " +
           "AND ba.startTime <= :endTime " +
           "ORDER BY ba.startTime ASC")
    List<BerthAllocation> findByBerthIdAndStartTimeBetween(
        @Param("berthId") Integer berthId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * Retrieves berth allocation details for a specific vessel call.
     *
     * @param vesselCallId the ID of the vessel call
     * @return Optional containing berth allocation if found
     */
    Optional<BerthAllocation> findByVesselCall_Id(Long vesselCallId);

    /**
     * Detects scheduling conflicts by finding overlapping berth allocations.
     * Excludes cancelled allocations from conflict detection.
     *
     * @param berthId the ID of the berth
     * @param startTime proposed allocation start time
     * @param endTime proposed allocation end time
     * @return List of overlapping berth allocations
     */
    @Query("SELECT ba FROM BerthAllocation ba " +
           "WHERE ba.berth.id = :berthId " +
           "AND ba.status != 'CANCELLED' " +
           "AND (:startTime < ba.endTime AND :endTime > ba.startTime) " +
           "ORDER BY ba.startTime ASC")
    List<BerthAllocation> findOverlappingAllocations(
        @Param("berthId") Integer berthId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * Retrieves all berth allocations with a specific status.
     * Useful for monitoring and managing berth utilization.
     *
     * @param status the allocation status to filter by
     * @return List of berth allocations matching the specified status
     */
    @Query("SELECT ba FROM BerthAllocation ba " +
           "WHERE ba.status = :status " +
           "ORDER BY ba.startTime ASC")
    List<BerthAllocation> findByStatus(@Param("status") BerthAllocationStatus status);

    /**
     * Finds active berth allocations for a specific berth.
     * Active allocations are those with status SCHEDULED or OCCUPIED.
     *
     * @param berthId the ID of the berth
     * @return List of active berth allocations
     */
    @Query("SELECT ba FROM BerthAllocation ba " +
           "WHERE ba.berth.id = :berthId " +
           "AND ba.status IN ('SCHEDULED', 'OCCUPIED') " +
           "ORDER BY ba.startTime ASC")
    List<BerthAllocation> findActiveBerthAllocations(@Param("berthId") Integer berthId);

    /**
     * Finds all berth allocations that are scheduled to start within the next specified hours.
     *
     * @param hours number of hours to look ahead
     * @return List of upcoming berth allocations
     */
    @Query("SELECT ba FROM BerthAllocation ba " +
           "WHERE ba.status = 'SCHEDULED' " +
           "AND ba.startTime BETWEEN CURRENT_TIMESTAMP AND CURRENT_TIMESTAMP + :hours " +
           "ORDER BY ba.startTime ASC")
    List<BerthAllocation> findUpcomingAllocations(@Param("hours") Integer hours);
}