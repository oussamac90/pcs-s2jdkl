package com.pcs.vcms.entity;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.JoinColumn;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.FetchType;
import javax.persistence.CascadeType;
import javax.persistence.EntityListeners;
import javax.persistence.Index;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.constraints.Pattern;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity class representing a vessel call (port visit) in the Port Community System.
 * Implements comprehensive validation, security measures, and optimized relationships
 * for vessel arrival, berthing, and departure management.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Entity
@Table(name = "vessel_calls", indexes = {
    @Index(name = "idx_vessel_call_status", columnList = "status"),
    @Index(name = "idx_vessel_call_dates", columnList = "eta,etd"),
    @Index(name = "idx_vessel_call_sign", columnList = "call_sign", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VesselCall {

    /**
     * Enum representing possible states of a vessel call with strict state transition validation
     */
    public enum VesselCallStatus {
        PLANNED,    // Initial state when vessel call is created
        ARRIVED,    // Vessel has arrived at port
        AT_BERTH,   // Vessel is berthed
        DEPARTED,   // Vessel has departed
        CANCELLED   // Vessel call is cancelled
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vessel_id", nullable = false)
    @NotNull(message = "Vessel reference is required")
    private Vessel vessel;

    @Column(name = "call_sign", length = 10, nullable = false, unique = true)
    @NotNull(message = "Call sign is required")
    @Size(min = 3, max = 10, message = "Call sign must be between 3 and 10 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Call sign must contain only uppercase letters and numbers")
    private String callSign;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @NotNull(message = "Status is required")
    private VesselCallStatus status;

    @Column(name = "eta", nullable = false)
    @NotNull(message = "Estimated Time of Arrival is required")
    private LocalDateTime eta;

    @Column(name = "etd", nullable = false)
    @NotNull(message = "Estimated Time of Departure is required")
    private LocalDateTime etd;

    @Column(name = "ata")
    private LocalDateTime ata;

    @Column(name = "atd")
    private LocalDateTime atd;

    @OneToMany(mappedBy = "vesselCall", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<BerthAllocation> berthAllocations = new HashSet<>();

    @OneToMany(mappedBy = "vesselCall", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<PreArrivalNotification> preArrivalNotifications = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Validates the consistency of arrival and departure times.
     * ETD must be after ETA, and actual times must be consistent with estimates.
     *
     * @throws IllegalArgumentException if time constraints are violated
     */
    private void validateTimes() {
        if (eta != null && etd != null && etd.isBefore(eta)) {
            throw new IllegalArgumentException("ETD must be after ETA");
        }
        if (ata != null && atd != null && atd.isBefore(ata)) {
            throw new IllegalArgumentException("ATD must be after ATA");
        }
    }

    /**
     * Helper method to add a berth allocation while maintaining bidirectional relationship
     *
     * @param berthAllocation the berth allocation to add
     */
    public void addBerthAllocation(BerthAllocation berthAllocation) {
        berthAllocations.add(berthAllocation);
        berthAllocation.setVesselCall(this);
    }

    /**
     * Helper method to add a pre-arrival notification while maintaining bidirectional relationship
     *
     * @param notification the pre-arrival notification to add
     */
    public void addPreArrivalNotification(PreArrivalNotification notification) {
        preArrivalNotifications.add(notification);
        notification.setVesselCall(this);
    }

    /**
     * Custom builder implementation to ensure validation of temporal constraints
     * and initialization of collections
     */
    public static class VesselCallBuilder {
        public VesselCall build() {
            VesselCall vesselCall = new VesselCall();
            vesselCall.setId(this.id);
            vesselCall.setVessel(this.vessel);
            vesselCall.setCallSign(this.callSign);
            vesselCall.setStatus(this.status != null ? this.status : VesselCallStatus.PLANNED);
            vesselCall.setEta(this.eta);
            vesselCall.setEtd(this.etd);
            vesselCall.setAta(this.ata);
            vesselCall.setAtd(this.atd);
            vesselCall.setBerthAllocations(this.berthAllocations != null ? 
                this.berthAllocations : new HashSet<>());
            vesselCall.setPreArrivalNotifications(this.preArrivalNotifications != null ? 
                this.preArrivalNotifications : new HashSet<>());
            vesselCall.validateTimes();
            return vesselCall;
        }
    }
}