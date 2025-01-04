package com.pcs.vcms.repository;

import com.pcs.vcms.entity.Berth;
import com.pcs.vcms.entity.Berth.BerthStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.locationtech.jts.geom.Point;

import javax.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Berth entities.
 * Provides comprehensive database operations including CRUD operations,
 * custom queries, and spatial search capabilities for efficient berth management.
 */
@Repository
public interface BerthRepository extends JpaRepository<Berth, Integer> {

    /**
     * Find all berths with the specified status.
     * Results are cached for improved performance.
     *
     * @param status The berth status to filter by
     * @param pageable Pagination information
     * @return Page of berths matching the status
     */
    @QueryHints(value = {@QueryHint(name = "org.hibernate.cacheable", value = "true")})
    Page<Berth> findByStatus(BerthStatus status, Pageable pageable);

    /**
     * Find berths by partial name match (case-insensitive).
     *
     * @param name The partial name to search for
     * @param pageable Pagination information
     * @return Page of berths matching the name pattern
     */
    @QueryHints(value = {@QueryHint(name = "org.hibernate.cacheable", value = "true")})
    Page<Berth> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Find berths suitable for vessel dimensions.
     *
     * @param length Minimum required length
     * @param depth Minimum required depth
     * @param pageable Pagination information
     * @return Page of berths meeting the dimensional requirements
     */
    @QueryHints(value = {@QueryHint(name = "org.hibernate.cacheable", value = "true")})
    Page<Berth> findByLengthGreaterThanEqualAndDepthGreaterThanEqual(
        Double length, Double depth, Pageable pageable);

    /**
     * Find berths near a given geographical point within specified distance.
     *
     * @param location Center point for the search
     * @param distance Maximum distance in meters
     * @param pageable Pagination information
     * @return Page of berths within the specified distance
     */
    @Query(value = "SELECT b FROM Berth b WHERE ST_DWithin(b.location, :location, :distance)")
    Page<Berth> findByLocationNear(
        @Param("location") Point location,
        @Param("distance") Double distance,
        Pageable pageable);

    /**
     * Find available berths for a specific time window.
     * Excludes berths that have overlapping allocations or are under maintenance.
     *
     * @param startTime Start of the time window
     * @param endTime End of the time window
     * @param pageable Pagination information
     * @return Page of available berths
     */
    @Query(value = """
        SELECT DISTINCT b FROM Berth b 
        LEFT JOIN b.allocations a 
        WHERE b.status = 'AVAILABLE' 
        AND (a IS NULL OR NOT (
            a.startTime < :endTime AND 
            a.endTime > :startTime
        ))
        """)
    @QueryHints(value = {@QueryHint(name = "org.hibernate.cacheable", value = "true")})
    Page<Berth> findAvailableBerthsForTimeWindow(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        Pageable pageable);

    /**
     * Find all available berths ordered by length.
     *
     * @param pageable Pagination information
     * @return Page of available berths
     */
    @QueryHints(value = {@QueryHint(name = "org.hibernate.cacheable", value = "true")})
    Page<Berth> findByStatusOrderByLengthDesc(BerthStatus status, Pageable pageable);

    /**
     * Find berths by maximum vessel size.
     *
     * @param maxVesselSize Maximum vessel size specification
     * @param pageable Pagination information
     * @return Page of berths matching the vessel size requirement
     */
    @QueryHints(value = {@QueryHint(name = "org.hibernate.cacheable", value = "true")})
    Page<Berth> findByMaxVesselSizeAndStatus(String maxVesselSize, BerthStatus status, Pageable pageable);

    /**
     * Find a berth by its unique name.
     *
     * @param name The berth name
     * @return Optional containing the berth if found
     */
    @QueryHints(value = {@QueryHint(name = "org.hibernate.cacheable", value = "true")})
    Optional<Berth> findByName(String name);
}