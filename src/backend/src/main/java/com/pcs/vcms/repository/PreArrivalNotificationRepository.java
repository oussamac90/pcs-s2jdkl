package com.pcs.vcms.repository;

import com.pcs.vcms.entity.PreArrivalNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository interface for managing Pre-Arrival Notification entities.
 * Provides secure and optimized data access operations with support for pagination and custom queries.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Repository
public interface PreArrivalNotificationRepository extends JpaRepository<PreArrivalNotification, Long> {

    /**
     * Finds all pre-arrival notifications for a specific vessel call with pagination support.
     * Results are ordered by submission date in descending order.
     *
     * @param vesselCallId ID of the vessel call
     * @param pageable pagination parameters
     * @return Page of PreArrivalNotification entities
     */
    @Query("SELECT p FROM PreArrivalNotification p WHERE p.vesselCall.id = :vesselCallId " +
           "ORDER BY p.submittedAt DESC")
    Page<PreArrivalNotification> findByVesselCallId(
        @Param("vesselCallId") Long vesselCallId,
        Pageable pageable
    );

    /**
     * Finds the most recent pre-arrival notification for a vessel call.
     * Optimized query using limit 1 for better performance.
     *
     * @param vesselCallId ID of the vessel call
     * @return Optional containing the latest notification if exists
     */
    @Query("SELECT p FROM PreArrivalNotification p WHERE p.vesselCall.id = :vesselCallId " +
           "ORDER BY p.submittedAt DESC")
    Optional<PreArrivalNotification> findLatestByVesselCallId(@Param("vesselCallId") Long vesselCallId);

    /**
     * Checks if any pre-arrival notification exists for a vessel call.
     * Optimized exists query for better performance.
     *
     * @param vesselCallId ID of the vessel call
     * @return true if notification exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM PreArrivalNotification p " +
           "WHERE p.vesselCall.id = :vesselCallId")
    boolean existsByVesselCallId(@Param("vesselCallId") Long vesselCallId);

    /**
     * Finds notifications for a vessel call within a specified date range.
     * Results are ordered by submission date in descending order.
     *
     * @param vesselCallId ID of the vessel call
     * @param startDate start of the date range
     * @param endDate end of the date range
     * @param pageable pagination parameters
     * @return Page of PreArrivalNotification entities within the date range
     */
    @Query("SELECT p FROM PreArrivalNotification p WHERE p.vesselCall.id = :vesselCallId " +
           "AND p.submittedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY p.submittedAt DESC")
    Page<PreArrivalNotification> findByVesselCallIdAndSubmittedAtBetween(
        @Param("vesselCallId") Long vesselCallId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
}