package com.pcs.vcms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pcs.vcms.entity.VesselCall.VesselCallStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.io.Serializable;

/**
 * Data Transfer Object for vessel call information.
 * Provides a secure and validated data structure for transferring vessel call information
 * between API and service layers with comprehensive input validation and security measures.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VesselCallDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("vesselId")
    @NotNull(message = "Vessel ID is required")
    private Long vesselId;

    @JsonProperty("vesselName")
    @NotNull(message = "Vessel name is required")
    @Size(min = 2, max = 100, message = "Vessel name must be between 2 and 100 characters")
    private String vesselName;

    @JsonProperty("imoNumber")
    @NotNull(message = "IMO number is required")
    @Pattern(regexp = "^IMO\\d{7}$", message = "Invalid IMO number format")
    private String imoNumber;

    @JsonProperty("callSign")
    @NotNull(message = "Call sign is required")
    @Size(min = 3, max = 10, message = "Call sign must be between 3 and 10 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Call sign must contain only uppercase letters and numbers")
    private String callSign;

    @JsonProperty("status")
    @NotNull(message = "Status is required")
    private VesselCallStatus status;

    @JsonProperty("eta")
    @NotNull(message = "ETA is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime eta;

    @JsonProperty("etd")
    @NotNull(message = "ETD is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime etd;

    @JsonProperty("ata")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime ata;

    @JsonProperty("atd")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime atd;

    @JsonProperty("createdAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonProperty("version")
    private Integer version;

    /**
     * Validates the consistency of arrival and departure times.
     * ETD must be after ETA, and actual times must be consistent with estimates.
     *
     * @throws IllegalArgumentException if time constraints are violated
     */
    public void validateTimes() {
        if (eta != null && etd != null && etd.isBefore(eta)) {
            throw new IllegalArgumentException("ETD must be after ETA");
        }
        if (ata != null && atd != null && atd.isBefore(ata)) {
            throw new IllegalArgumentException("ATD must be after ATA");
        }
    }

    /**
     * Custom builder implementation to ensure validation of temporal constraints
     */
    public static class VesselCallDTOBuilder {
        public VesselCallDTO build() {
            VesselCallDTO dto = new VesselCallDTO();
            dto.setId(this.id);
            dto.setVesselId(this.vesselId);
            dto.setVesselName(this.vesselName);
            dto.setImoNumber(this.imoNumber);
            dto.setCallSign(this.callSign);
            dto.setStatus(this.status != null ? this.status : VesselCallStatus.PLANNED);
            dto.setEta(this.eta);
            dto.setEtd(this.etd);
            dto.setAta(this.ata);
            dto.setAtd(this.atd);
            dto.setCreatedAt(this.createdAt);
            dto.setUpdatedAt(this.updatedAt);
            dto.setVersion(this.version);
            dto.validateTimes();
            return dto;
        }
    }
}