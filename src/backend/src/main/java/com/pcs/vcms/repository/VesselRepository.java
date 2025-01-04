package com.pcs.vcms.repository;

import com.pcs.vcms.entity.Vessel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Vessel entities in the Port Community System.
 * Provides optimized database operations and custom query methods for vessel management.
 * Implements data integrity controls and efficient query execution patterns.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Repository
public interface VesselRepository extends JpaRepository<Vessel, Long> {

    /**
     * Finds a vessel by its IMO number using an optimized indexed query.
     * Leverages the unique index on imo_number column for efficient lookups.
     *
     * @param imoNumber the IMO number of the vessel (7-digit identifier)
     * @return Optional containing the vessel if found, empty otherwise
     */
    Optional<Vessel> findByImoNumber(String imoNumber);

    /**
     * Retrieves multiple vessels by their IMO numbers using batch processing.
     * Optimizes database access by fetching multiple records in a single query.
     *
     * @param imoNumbers list of IMO numbers to search for
     * @return List of vessels matching the provided IMO numbers
     */
    List<Vessel> findByImoNumberIn(List<String> imoNumbers);

    /**
     * Retrieves all vessels with pagination support for efficient data access.
     * Implements server-side pagination to handle large datasets effectively.
     *
     * @param pageable pagination parameters including page size, number, and sorting
     * @return Page of vessels based on the provided pagination parameters
     */
    @Override
    Page<Vessel> findAll(Pageable pageable);

    /**
     * Efficiently checks for the existence of a vessel with the given IMO number.
     * Uses EXISTS clause for optimal query performance.
     *
     * @param imoNumber the IMO number to check
     * @return true if a vessel with the given IMO number exists, false otherwise
     */
    boolean existsByImoNumber(String imoNumber);
}