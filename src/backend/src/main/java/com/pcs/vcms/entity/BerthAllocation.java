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
import javax.persistence.Version;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.FutureOrPresent;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity class representing a berth allocation in the Port Community System.
 * Manages the assignment of vessels to berths including scheduling, status tracking,
 * and audit information with comprehensive validation and relationship management.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Entity
@Table(name = "berth_allocations", indexes = {
    @Index(name = "idx_berth_allocation_dates", columnList = "start_time,end_time"),
    @Index(name = "idx_berth_allocation_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BerthAllocation {

    /**
     * Enum representing possible states of a berth allocation with comprehensive status tracking
     */
    public enum BerthAllocationStatus {
        SCHEDULED,   // Allocation is planned but not yet active
        OCCUPIED,    // Berth is currently occupied by the vessel
        COMPLETED,   // Vessel has departed and allocation is finished
        CANCELLED    // Allocation was cancelled
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Vessel call reference is required")
    @ManyToOne
    @JoinColumn(name = "vessel_call_id", nullable = false)
    private VesselCall vesselCall;

    @NotNull(message = "Berth reference is required")
    @ManyToOne
    @JoinColumn(name = "berth_id", nullable = false)
    private Berth berth;

    @NotNull(message = "Start time is required")
    @FutureOrPresent(message = "Start time must be in present or future")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @FutureOrPresent(message = "End time must be in present or future")
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BerthAllocationStatus status;

    @Version
    @Column(name = "version")
    private Integer version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Validates the consistency of allocation times and updates audit timestamps.
     * Start time must be before end time.
     *
     * @throws IllegalArgumentException if time constraints are violated
     */
    private void validateTimes() {
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
    }

    /**
     * JPA lifecycle callback executed before persisting the entity
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = BerthAllocationStatus.SCHEDULED;
        }
        if (version == null) {
            version = 0;
        }
        validateTimes();
    }

    /**
     * JPA lifecycle callback executed before updating the entity
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        validateTimes();
    }

    /**
     * Custom builder implementation to ensure validation of temporal constraints
     */
    public static class BerthAllocationBuilder {
        public BerthAllocation build() {
            BerthAllocation allocation = new BerthAllocation();
            allocation.setId(this.id);
            allocation.setVesselCall(this.vesselCall);
            allocation.setBerth(this.berth);
            allocation.setStartTime(this.startTime);
            allocation.setEndTime(this.endTime);
            allocation.setStatus(this.status != null ? this.status : BerthAllocationStatus.SCHEDULED);
            allocation.setVersion(this.version);
            allocation.setCreatedAt(this.createdAt != null ? this.createdAt : LocalDateTime.now());
            allocation.setUpdatedAt(this.updatedAt != null ? this.updatedAt : LocalDateTime.now());
            allocation.validateTimes();
            return allocation;
        }
    }
}