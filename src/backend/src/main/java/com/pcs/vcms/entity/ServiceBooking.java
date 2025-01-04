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
import javax.persistence.EntityListeners;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import javax.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import java.time.LocalDateTime;

/**
 * Entity class representing a port service booking in the Vessel Call Management System.
 * Manages bookings for various port services with comprehensive tracking and validation.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Entity
@Table(name = "service_bookings", indexes = {
    @Index(name = "idx_service_status_time", columnList = "status,service_time"),
    @Index(name = "idx_service_vessel_call", columnList = "vessel_call_id")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "vesselCall")
public class ServiceBooking {

    /**
     * Enum representing types of port services available for booking
     */
    public enum ServiceType {
        PILOTAGE("PIL", "Pilotage service for vessel navigation"),
        TUGBOAT("TUG", "Tugboat assistance service"),
        MOORING("MOR", "Vessel mooring service"),
        UNMOORING("UNM", "Vessel unmooring service");

        private final String code;
        private final String description;

        ServiceType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Enum representing possible states of a service booking
     */
    public enum ServiceStatus {
        REQUESTED("REQ", true),
        CONFIRMED("CNF", true),
        IN_PROGRESS("INP", false),
        COMPLETED("COM", false),
        CANCELLED("CAN", false);

        private final String code;
        private final boolean allowsModification;

        ServiceStatus(String code, boolean allowsModification) {
            this.code = code;
            this.allowsModification = allowsModification;
        }

        public String getCode() {
            return code;
        }

        public boolean isAllowsModification() {
            return allowsModification;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vessel_call_id", nullable = false)
    @NotNull(message = "Vessel call reference is required")
    private VesselCall vesselCall;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 20)
    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull(message = "Service status is required")
    @Builder.Default
    private ServiceStatus status = ServiceStatus.REQUESTED;

    @Column(name = "quantity")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 10, message = "Quantity cannot exceed 10")
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "service_time", nullable = false)
    @NotNull(message = "Service time is required")
    private LocalDateTime serviceTime;

    @Column(name = "remarks", length = 500)
    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;

    @Version
    @Column(name = "version")
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    @Size(max = 50, message = "Created by cannot exceed 50 characters")
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    @Size(max = 50, message = "Updated by cannot exceed 50 characters")
    private String updatedBy;

    @Column(name = "deleted")
    @Builder.Default
    private boolean deleted = false;

    /**
     * Validates service booking time against vessel call schedule
     * @throws IllegalArgumentException if service time is outside vessel call window
     */
    private void validateServiceTime() {
        if (serviceTime != null && vesselCall != null) {
            if (serviceTime.isBefore(vesselCall.getEta()) || 
                serviceTime.isAfter(vesselCall.getEtd())) {
                throw new IllegalArgumentException(
                    "Service time must be within vessel call window");
            }
        }
    }

    /**
     * Custom builder implementation to ensure validation of service booking constraints
     */
    public static class ServiceBookingBuilder {
        public ServiceBooking build() {
            ServiceBooking booking = new ServiceBooking();
            booking.setId(this.id);
            booking.setVesselCall(this.vesselCall);
            booking.setServiceType(this.serviceType);
            booking.setStatus(this.status != null ? this.status : ServiceStatus.REQUESTED);
            booking.setQuantity(this.quantity != null ? this.quantity : 1);
            booking.setServiceTime(this.serviceTime);
            booking.setRemarks(this.remarks);
            booking.setVersion(this.version);
            booking.setCreatedAt(this.createdAt);
            booking.setUpdatedAt(this.updatedAt);
            booking.setCreatedBy(this.createdBy);
            booking.setUpdatedBy(this.updatedBy);
            booking.setDeleted(this.deleted);
            booking.validateServiceTime();
            return booking;
        }
    }
}