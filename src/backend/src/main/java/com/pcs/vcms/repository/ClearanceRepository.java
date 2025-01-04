package com.pcs.vcms.repository;

import com.pcs.vcms.entity.Clearance;
import com.pcs.vcms.entity.Clearance.ClearanceType;
import com.pcs.vcms.entity.Clearance.ClearanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository interface for managing Clearance entity persistence and database operations.
 * Provides comprehensive methods for querying and managing vessel clearance records
 * with support for regulatory compliance checks and automated workflows.
 *
 * @version 1.0
 * @since 2023-11-15
 */
public interface ClearanceRepository extends JpaRepository<Clearance, Long> {

    /**
     * Retrieves all clearances for a specific vessel call with pagination support.
     *
     * @param vesselCallId the ID of the vessel call
     * @param pageable pagination parameters
     * @return page of clearances for the vessel call
     */
    Page<Clearance> findByVesselCallId(Long vesselCallId, Pageable pageable);

    /**
     * Retrieves clearances with a specific status with pagination support.
     *
     * @param status the clearance status to filter by
     * @param pageable pagination parameters
     * @return page of clearances with the specified status
     */
    Page<Clearance> findByStatus(ClearanceStatus status, Pageable pageable);

    /**
     * Retrieves clearances of a specific type and status with pagination support.
     *
     * @param type the clearance type to filter by
     * @param status the clearance status to filter by
     * @param pageable pagination parameters
     * @return page of clearances matching type and status
     */
    Page<Clearance> findByTypeAndStatus(ClearanceType type, ClearanceStatus status, Pageable pageable);

    /**
     * Retrieves a clearance by its reference number.
     *
     * @param referenceNumber the unique reference number of the clearance
     * @return Optional containing the clearance if found
     */
    Optional<Clearance> findByReferenceNumber(String referenceNumber);

    /**
     * Counts clearances for a vessel call with specific status.
     *
     * @param vesselCallId the ID of the vessel call
     * @param status the clearance status to count
     * @return number of matching clearances
     */
    Long countByVesselCallIdAndStatus(Long vesselCallId, ClearanceStatus status);

    /**
     * Retrieves clearances by type and status with validity check.
     *
     * @param type the clearance type to filter by
     * @param status the clearance status to filter by
     * @param validityDate the date to check validity against
     * @param pageable pagination parameters
     * @return page of valid clearances matching criteria
     */
    @Query("SELECT c FROM Clearance c WHERE c.type = ?1 AND c.status = ?2 AND (c.validUntil IS NULL OR c.validUntil > ?3)")
    Page<Clearance> findByTypeAndStatusAndValidUntil(ClearanceType type, ClearanceStatus status, LocalDateTime validityDate, Pageable pageable);

    /**
     * Retrieves latest clearances for a vessel call by type.
     *
     * @param vesselCallId the ID of the vessel call
     * @param type the clearance type to filter by
     * @param pageable pagination parameters
     * @return page of clearances ordered by submission time
     */
    Page<Clearance> findByVesselCallIdAndTypeOrderBySubmittedAtDesc(Long vesselCallId, ClearanceType type, Pageable pageable);

    /**
     * Checks existence of clearance with specific criteria.
     *
     * @param vesselCallId the ID of the vessel call
     * @param type the clearance type to check
     * @param status the clearance status to check
     * @return true if matching clearance exists
     */
    boolean existsByVesselCallIdAndTypeAndStatus(Long vesselCallId, ClearanceType type, ClearanceStatus status);

    /**
     * Deletes expired clearances before specified date.
     * This operation is transactional and modifying.
     *
     * @param expiryDate the date before which clearances are considered expired
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Clearance c WHERE c.validUntil < ?1")
    void deleteByValidUntilBefore(LocalDateTime expiryDate);
}