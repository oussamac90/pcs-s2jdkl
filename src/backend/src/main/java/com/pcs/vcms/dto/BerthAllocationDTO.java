package com.pcs.vcms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.pcs.vcms.entity.BerthAllocation.BerthAllocationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.FutureOrPresent;
import java.time.LocalDateTime;
import java.time.Duration;

/**
 * Data Transfer Object for berth allocation information.
 * Provides a secure and validated data structure for API communications
 * while abstracting internal entity representations.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BerthAllocationDTO {

    private Long id;

    @NotNull(message = "Vessel call ID is required")
    private Long vesselCallId;

    @NotNull(message = "Vessel name is required")
    private String vesselName;

    @NotNull(message = "Berth ID is required")
    private Long berthId;

    @NotNull(message = "Berth name is required")
    private String berthName;

    @NotNull(message = "Start time is required")
    @FutureOrPresent(message = "Start time must be in present or future")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @FutureOrPresent(message = "End time must be in present or future")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private LocalDateTime endTime;

    @NotNull(message = "Status is required")
    private BerthAllocationStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private LocalDateTime updatedAt;

    /**
     * Validates the time range for berth allocation.
     * Ensures that:
     * 1. Start time is before end time
     * 2. Minimum allocation duration is respected
     * 3. Time range is within acceptable bounds
     *
     * @return true if time range is valid
     * @throws IllegalArgumentException if validation fails
     */
    public boolean validateTimeRange() {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Both start and end times must be specified");
        }

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        Duration duration = Duration.between(startTime, endTime);
        if (duration.toMinutes() < 30) {
            throw new IllegalArgumentException("Minimum allocation duration is 30 minutes");
        }

        if (duration.toDays() > 30) {
            throw new IllegalArgumentException("Maximum allocation duration is 30 days");
        }

        return true;
    }

    /**
     * Custom builder implementation with validation.
     */
    public static class BerthAllocationDTOBuilder {
        /**
         * Builds the DTO with validation.
         *
         * @return validated BerthAllocationDTO
         * @throws IllegalArgumentException if validation fails
         */
        public BerthAllocationDTO build() {
            BerthAllocationDTO dto = new BerthAllocationDTO();
            dto.setId(this.id);
            dto.setVesselCallId(this.vesselCallId);
            dto.setVesselName(this.vesselName);
            dto.setBerthId(this.berthId);
            dto.setBerthName(this.berthName);
            dto.setStartTime(this.startTime);
            dto.setEndTime(this.endTime);
            dto.setStatus(this.status != null ? this.status : BerthAllocationStatus.SCHEDULED);
            dto.setCreatedAt(this.createdAt);
            dto.setUpdatedAt(this.updatedAt);
            
            // Validate time range
            dto.validateTimeRange();
            
            return dto;
        }
    }
}