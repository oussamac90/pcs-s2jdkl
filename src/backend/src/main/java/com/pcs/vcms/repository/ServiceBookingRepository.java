package com.pcs.vcms.repository;

import com.pcs.vcms.entity.ServiceBooking;
import com.pcs.vcms.entity.ServiceBooking.ServiceType;
import com.pcs.vcms.entity.ServiceBooking.ServiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing ServiceBooking entities with optimized database operations.
 * Provides methods for querying and managing port service bookings with performance monitoring.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Repository
public interface ServiceBookingRepository extends JpaRepository<ServiceBooking, Long> {

    /**
     * Find all service bookings for a specific vessel call with optimized query execution.
     *
     * @param vesselCallId the ID of the vessel call
     * @return list of service bookings for the vessel call
     */
    @Query("SELECT sb FROM ServiceBooking sb WHERE sb.vesselCall.id = :vesselCallId AND sb.deleted = false")
    List<ServiceBooking> findByVesselCallId(@Param("vesselCallId") Long vesselCallId);

    /**
     * Find service bookings by type and status with pagination support for large datasets.
     *
     * @param serviceType type of the service
     * @param status status of the service booking
     * @param pageable pagination parameters
     * @return paginated result of service bookings
     */
    @Query("SELECT sb FROM ServiceBooking sb " +
           "WHERE sb.serviceType = :serviceType " +
           "AND sb.status = :status " +
           "AND sb.deleted = false " +
           "ORDER BY sb.serviceTime ASC")
    Page<ServiceBooking> findByServiceTypeAndStatus(
            @Param("serviceType") ServiceType serviceType,
            @Param("status") ServiceStatus status,
            Pageable pageable);

    /**
     * Find service bookings for a specific time period and status with optimized time-based query.
     *
     * @param startTime start of the time period
     * @param endTime end of the time period
     * @param status status of the service booking
     * @return list of service bookings in the time period
     */
    @Query("SELECT sb FROM ServiceBooking sb " +
           "WHERE sb.serviceTime BETWEEN :startTime AND :endTime " +
           "AND sb.status = :status " +
           "AND sb.deleted = false " +
           "ORDER BY sb.serviceTime ASC")
    List<ServiceBooking> findByServiceTimeAndStatus(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("status") ServiceStatus status);

    /**
     * Count service bookings by type within a time period for resource utilization tracking.
     *
     * @param serviceType type of the service
     * @param startTime start of the time period
     * @param endTime end of the time period
     * @return count of service bookings
     */
    @Query("SELECT COUNT(sb) FROM ServiceBooking sb " +
           "WHERE sb.serviceType = :serviceType " +
           "AND sb.serviceTime BETWEEN :startTime AND :endTime " +
           "AND sb.deleted = false")
    Long countByServiceTypeAndServiceTimeBetween(
            @Param("serviceType") ServiceType serviceType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Find overlapping service bookings for resource conflict detection.
     *
     * @param serviceType type of the service
     * @param startTime start of the time period
     * @param endTime end of the time period
     * @return list of overlapping service bookings
     */
    @Query("SELECT sb FROM ServiceBooking sb " +
           "WHERE sb.serviceType = :serviceType " +
           "AND sb.deleted = false " +
           "AND sb.status NOT IN ('COMPLETED', 'CANCELLED') " +
           "AND ((sb.serviceTime BETWEEN :startTime AND :endTime) OR " +
           "(:startTime BETWEEN sb.serviceTime AND sb.serviceTime))")
    List<ServiceBooking> findOverlappingBookings(
            @Param("serviceType") ServiceType serviceType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Find active service bookings for a specific service type.
     *
     * @param serviceType type of the service
     * @return list of active service bookings
     */
    @Query("SELECT sb FROM ServiceBooking sb " +
           "WHERE sb.serviceType = :serviceType " +
           "AND sb.status IN ('REQUESTED', 'CONFIRMED', 'IN_PROGRESS') " +
           "AND sb.deleted = false " +
           "ORDER BY sb.serviceTime ASC")
    List<ServiceBooking> findActiveBookingsByServiceType(
            @Param("serviceType") ServiceType serviceType);

    /**
     * Find service booking by ID with optimistic locking support.
     *
     * @param id the ID of the service booking
     * @return optional containing the service booking if found
     */
    @Query("SELECT sb FROM ServiceBooking sb " +
           "WHERE sb.id = :id AND sb.deleted = false")
    Optional<ServiceBooking> findByIdAndNotDeleted(@Param("id") Long id);
}