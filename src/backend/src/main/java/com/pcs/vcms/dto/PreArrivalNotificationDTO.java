package com.pcs.vcms.dto;

import com.fasterxml.jackson.annotation.JsonFormat; // version: 2.13.0
import com.fasterxml.jackson.annotation.JsonInclude; // version: 2.13.0
import io.swagger.v3.oas.annotations.Schema; // version: 1.6.0
import lombok.AllArgsConstructor; // version: 1.18.22
import lombok.Builder; // version: 1.18.22
import lombok.Data; // version: 1.18.22
import lombok.NoArgsConstructor; // version: 1.18.22

import javax.validation.constraints.NotBlank; // version: 2.0.1.Final
import javax.validation.constraints.NotNull; // version: 2.0.1.Final
import javax.validation.constraints.PastOrPresent; // version: 2.0.1.Final
import javax.validation.constraints.Size; // version: 2.0.1.Final
import java.time.LocalDateTime; // version: 17

/**
 * Data Transfer Object for Pre-arrival Notifications in the Vessel Call Management System.
 * This class handles the transfer of pre-arrival notification data between different layers
 * of the application with proper validation and documentation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pre-arrival notification data transfer object")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreArrivalNotificationDTO {

    /**
     * Unique identifier for the pre-arrival notification
     */
    @Schema(description = "Unique identifier")
    private Long id;

    /**
     * Reference to the associated vessel call
     */
    @NotNull
    @Schema(description = "Associated vessel call ID", required = true)
    private Long vesselCallId;

    /**
     * Username or identifier of the person who submitted the notification
     */
    @NotBlank
    @Size(max = 100)
    @Schema(description = "User who submitted the notification", required = true)
    private String submittedBy;

    /**
     * Detailed information about the cargo being carried
     */
    @Size(max = 4000)
    @Schema(description = "Detailed cargo information")
    private String cargoDetails;

    /**
     * List of crew members on board
     */
    @Size(max = 4000)
    @Schema(description = "List of crew members")
    private String crewList;

    /**
     * Timestamp when the notification was submitted
     */
    @PastOrPresent
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Submission timestamp")
    private LocalDateTime submittedAt;
}