package com.pcs.vcms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pcs.vcms.entity.ServiceBooking.ServiceType;
import com.pcs.vcms.entity.ServiceBooking.ServiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import javax.validation.constraints.Future;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for service booking information in the Vessel Call Management System.
 * Implements comprehensive validation and secure data transfer between API and service layers.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceBookingDTO {

    /**
     * Unique identifier for the service booking
     */
    private Long id;

    /**
     * Reference to the associated vessel call
     */
    @NotNull(message = "Vessel call ID is required")
    private Long vesselCallId;

    /**
     * Name of the vessel for display purposes
     */
    @Size(max = 100, message = "Vessel name must not exceed 100 characters")
    private String vesselName;

    /**
     * Type of port service being booked
     */
    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    /**
     * Current status of the service booking
     */
    @Builder.Default
    private ServiceStatus status = ServiceStatus.REQUESTED;

    /**
     * Quantity of service units required (e.g., number of tugboats)
     */
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    /**
     * Scheduled time for service delivery
     */
    @NotNull(message = "Service time is required")
    @Future(message = "Service time must be in the future")
    private LocalDateTime serviceTime;

    /**
     * Additional notes or special requirements for the service
     */
    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;

    /**
     * Timestamp when the booking was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp of the last update to the booking
     */
    private LocalDateTime updatedAt;

    /**
     * Custom builder implementation to ensure proper initialization of default values
     * and maintain immutability of the DTO
     */
    public static class ServiceBookingDTOBuilder {
        private ServiceStatus status = ServiceStatus.REQUESTED;
        private Integer quantity = 1;

        /**
         * Custom build method to ensure proper initialization
         * @return A fully initialized ServiceBookingDTO instance
         */
        public ServiceBookingDTO build() {
            if (this.status == null) {
                this.status = ServiceStatus.REQUESTED;
            }
            if (this.quantity == null) {
                this.quantity = 1;
            }
            return new ServiceBookingDTO(
                this.id,
                this.vesselCallId,
                this.vesselName,
                this.serviceType,
                this.status,
                this.quantity,
                this.serviceTime,
                this.remarks,
                this.createdAt,
                this.updatedAt
            );
        }
    }
}