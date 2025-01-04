package com.pcs.vcms.dto;

import com.pcs.vcms.entity.Clearance.ClearanceType;
import com.pcs.vcms.entity.Clearance.ClearanceStatus;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for vessel clearance information.
 * Handles digital clearance workflows and regulatory compliance data transfer
 * between API and service layers with enhanced audit capabilities and expiry tracking.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClearanceDTO {

    /**
     * Unique identifier for the clearance record
     */
    private Long id;

    /**
     * Reference to the associated vessel call
     */
    private Long vesselCallId;

    /**
     * Name of the vessel for display purposes
     */
    private String vesselName;

    /**
     * Type of clearance (CUSTOMS, IMMIGRATION, PORT_AUTHORITY, HEALTH, SECURITY)
     */
    private ClearanceType type;

    /**
     * Current status of the clearance request
     * (PENDING, IN_PROGRESS, APPROVED, REJECTED, CANCELLED)
     */
    private ClearanceStatus status;

    /**
     * Unique reference number for the clearance request
     * Format: CLR-XXXXXX-YYY
     */
    private String referenceNumber;

    /**
     * Username or identifier of the person who submitted the clearance request
     */
    private String submittedBy;

    /**
     * Username or identifier of the person who approved/rejected the clearance
     */
    private String approvedBy;

    /**
     * Additional notes or comments regarding the clearance
     */
    private String remarks;

    /**
     * Timestamp when the clearance request was submitted
     */
    private LocalDateTime submittedAt;

    /**
     * Timestamp when the clearance was approved/rejected
     */
    private LocalDateTime approvedAt;

    /**
     * Expiration date of the clearance if applicable
     */
    private LocalDateTime validUntil;

    /**
     * Timestamp when the record was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the record was last updated
     */
    private LocalDateTime updatedAt;
}