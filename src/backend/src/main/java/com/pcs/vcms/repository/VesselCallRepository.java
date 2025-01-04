package com.pcs.vcms.repository;

import com.pcs.vcms.entity.VesselCall;
import com.pcs.vcms.entity.VesselCall.VesselCallStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.hibernate.annotations.QueryHints.HINT_READONLY;
import static org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.QueryHints.HINT_CACHEABLE;

/**
 * Repository interface for VesselCall entity providing optimized data access operations
 * with security considerations and performance optimization through query hints.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Repository
@Transactional(readOnly = true)
public interface VesselCallRepository extends JpaRepository<VesselCall, Long> {

    /**
     * Finds a vessel call by its unique call sign with validation.
     * Optimized for read-only operations with caching enabled.
     *
     * @param callSign the unique call sign of the vessel call
     * @return Optional containing the vessel call if found
     */
    @QueryHints(value = {
        @QueryHint(name = HINT_READONLY, value = "true"),
        @QueryHint(name = HINT_CACHEABLE, value = "true")
    })
    Optional<VesselCall> findByCallSign(String callSign);

    /**
     * Finds all vessel calls with the given status with pagination support.
     * Optimized for read-only operations and batch fetching.
     *
     * @param status the status of vessel calls to find
     * @param pageable pagination parameters
     * @return Page of vessel calls matching the status
     */
    @QueryHints(value = {
        @QueryHint(name = HINT_READONLY, value = "true"),
        @QueryHint(name = HINT_FETCH_SIZE, value = "50")
    })
    Page<VesselCall> findByStatus(VesselCallStatus status, Pageable pageable);

    /**
     * Finds vessel calls with ETA within the given date range with pagination.
     * Optimized for read-only operations and batch fetching.
     *
     * @param startDate start of the date range
     * @param endDate end of the date range
     * @param pageable pagination parameters
     * @return Page of vessel calls within the date range
     */
    @QueryHints(value = {
        @QueryHint(name = HINT_READONLY, value = "true"),
        @QueryHint(name = HINT_FETCH_SIZE, value = "50")
    })
    Page<VesselCall> findByEtaBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Checks if a vessel call exists with the given call sign.
     * Optimized for read-only operations with caching enabled.
     *
     * @param callSign the call sign to check
     * @return true if vessel call exists, false otherwise
     */
    @QueryHints(value = {
        @QueryHint(name = HINT_READONLY, value = "true"),
        @QueryHint(name = HINT_CACHEABLE, value = "true")
    })
    boolean existsByCallSign(String callSign);

    /**
     * Finds all active vessel calls (PLANNED, ARRIVED, or AT_BERTH status).
     * Optimized for read-only operations and batch fetching.
     *
     * @param pageable pagination parameters
     * @return Page of active vessel calls
     */
    @QueryHints(value = {
        @QueryHint(name = HINT_READONLY, value = "true"),
        @QueryHint(name = HINT_FETCH_SIZE, value = "50")
    })
    Page<VesselCall> findByStatusIn(List<VesselCallStatus> statuses, Pageable pageable);

    /**
     * Finds vessel calls by vessel IMO number with pagination.
     * Optimized for read-only operations with caching enabled.
     *
     * @param imoNumber the IMO number of the vessel
     * @param pageable pagination parameters
     * @return Page of vessel calls for the specified vessel
     */
    @QueryHints(value = {
        @QueryHint(name = HINT_READONLY, value = "true"),
        @QueryHint(name = HINT_CACHEABLE, value = "true")
    })
    Page<VesselCall> findByVessel_ImoNumber(String imoNumber, Pageable pageable);
}