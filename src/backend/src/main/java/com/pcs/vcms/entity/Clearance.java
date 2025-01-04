package com.pcs.vcms.entity;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.JoinColumn;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.constraints.Pattern;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import java.time.LocalDateTime;

/**
 * Entity class representing vessel clearance information in the Port Community System.
 * Implements comprehensive validation, security measures, and audit trails for
 * regulatory compliance and clearance workflow management.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Entity
@Table(name = "clearances", indexes = {
    @Index(name = "idx_clearance_status", columnList = "status"),
    @Index(name = "idx_clearance_type", columnList = "type"),
    @Index(name = "idx_clearance_reference", columnList = "reference_number", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "vesselCall")
public class Clearance {

    /**
     * Enum representing different types of clearances required for vessel operations
     */
    public enum ClearanceType {
        CUSTOMS,
        IMMIGRATION,
        PORT_AUTHORITY,
        HEALTH,
        SECURITY
    }

    /**
     * Enum representing possible states of a clearance request with workflow transitions
     */
    public enum ClearanceStatus {
        PENDING,
        IN_PROGRESS,
        APPROVED,
        REJECTED,
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vessel_call_id", nullable = false)
    @NotNull(message = "Vessel call reference is required")
    private VesselCall vesselCall;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    @NotNull(message = "Clearance type is required")
    private ClearanceType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull(message = "Clearance status is required")
    private ClearanceStatus status;

    @Column(name = "reference_number", nullable = false, unique = true, length = 20)
    @NotNull(message = "Reference number is required")
    @Pattern(regexp = "^CLR-[0-9]{6}-[A-Z]{3}$", 
            message = "Reference number must follow format: CLR-XXXXXX-YYY")
    private String referenceNumber;

    @Column(name = "submitted_by", nullable = false, length = 50)
    @NotNull(message = "Submitter information is required")
    @Size(min = 3, max = 50, message = "Submitted by must be between 3 and 50 characters")
    private String submittedBy;

    @Column(name = "approved_by", length = 50)
    @Size(max = 50, message = "Approved by must not exceed 50 characters")
    private String approvedBy;

    @Column(name = "remarks", length = 500)
    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;

    @Column(name = "submitted_at", nullable = false)
    @NotNull(message = "Submission timestamp is required")
    private LocalDateTime submittedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Validates status transitions based on business rules.
     * Ensures that status changes follow the allowed workflow.
     *
     * @param newStatus the new status to validate
     * @throws IllegalStateException if the status transition is invalid
     */
    private void validateStatusTransition(ClearanceStatus newStatus) {
        if (this.status == null) return;
        
        switch (this.status) {
            case PENDING:
                if (newStatus != ClearanceStatus.IN_PROGRESS && 
                    newStatus != ClearanceStatus.CANCELLED) {
                    throw new IllegalStateException("Invalid status transition from PENDING");
                }
                break;
            case IN_PROGRESS:
                if (newStatus != ClearanceStatus.APPROVED && 
                    newStatus != ClearanceStatus.REJECTED) {
                    throw new IllegalStateException("Invalid status transition from IN_PROGRESS");
                }
                break;
            case APPROVED:
            case REJECTED:
            case CANCELLED:
                throw new IllegalStateException("Cannot change status once finalized");
        }
    }

    /**
     * JPA callback method executed before persisting the entity.
     * Initializes timestamps and default status.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = ClearanceStatus.PENDING;
        }
    }

    /**
     * JPA callback method executed before updating the entity.
     * Updates timestamp and validates status transitions.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.status != null) {
            validateStatusTransition(this.status);
        }
    }

    /**
     * Custom builder implementation to ensure proper initialization
     * and validation of clearance objects.
     */
    public static class ClearanceBuilder {
        public Clearance build() {
            if (this.status == null) {
                this.status = ClearanceStatus.PENDING;
            }
            if (this.submittedAt == null) {
                this.submittedAt = LocalDateTime.now();
            }
            return new Clearance(
                this.id,
                this.vesselCall,
                this.type,
                this.status,
                this.referenceNumber,
                this.submittedBy,
                this.approvedBy,
                this.remarks,
                this.submittedAt,
                this.approvedAt,
                this.validUntil,
                this.createdAt,
                this.updatedAt
            );
        }
    }
}