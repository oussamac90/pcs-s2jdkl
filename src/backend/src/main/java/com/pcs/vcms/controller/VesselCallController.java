package com.pcs.vcms.controller;

import com.pcs.vcms.dto.VesselCallDTO;
import com.pcs.vcms.entity.VesselCall.VesselCallStatus;
import com.pcs.vcms.service.VesselCallService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * REST controller for managing vessel call operations with comprehensive security,
 * validation, and monitoring capabilities.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@RestController
@RequestMapping("/api/v1/vessel-calls")
@Tag(name = "Vessel Calls", description = "Vessel call management endpoints")
@Validated
@Slf4j
@RequiredArgsConstructor
public class VesselCallController {

    private final VesselCallService vesselCallService;
    private final ApplicationEventPublisher eventPublisher;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VESSEL_AGENT')")
    @Operation(summary = "Create new vessel call")
    @ApiResponse(responseCode = "201", description = "Vessel call created successfully")
    @Timed(value = "vessel.call.create", description = "Time taken to create vessel call")
    public ResponseEntity<VesselCallDTO> createVesselCall(
            @Valid @RequestBody VesselCallDTO vesselCallDTO) {
        log.info("Creating new vessel call with call sign: {}", vesselCallDTO.getCallSign());
        VesselCallDTO created = vesselCallService.createVesselCall(vesselCallDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('VESSEL_AGENT', 'PORT_AUTHORITY')")
    @Operation(summary = "Update vessel call")
    @ApiResponse(responseCode = "200", description = "Vessel call updated successfully")
    @Timed(value = "vessel.call.update", description = "Time taken to update vessel call")
    public ResponseEntity<VesselCallDTO> updateVesselCall(
            @PathVariable @NotNull Long id,
            @Valid @RequestBody VesselCallDTO vesselCallDTO) {
        log.info("Updating vessel call with ID: {}", id);
        VesselCallDTO updated = vesselCallService.updateVesselCall(id, vesselCallDTO);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VESSEL_AGENT', 'PORT_AUTHORITY', 'SERVICE_PROVIDER')")
    @Operation(summary = "Get vessel call by ID")
    @ApiResponse(responseCode = "200", description = "Vessel call retrieved successfully")
    @Timed(value = "vessel.call.get", description = "Time taken to retrieve vessel call")
    public ResponseEntity<VesselCallDTO> getVesselCall(
            @PathVariable @NotNull Long id) {
        log.debug("Retrieving vessel call with ID: {}", id);
        return vesselCallService.getVesselCall(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/call-sign/{callSign}")
    @PreAuthorize("hasAnyRole('VESSEL_AGENT', 'PORT_AUTHORITY', 'SERVICE_PROVIDER')")
    @Operation(summary = "Find vessel call by call sign")
    @ApiResponse(responseCode = "200", description = "Vessel call retrieved successfully")
    @Timed(value = "vessel.call.findByCallSign", description = "Time taken to find vessel call by call sign")
    public ResponseEntity<VesselCallDTO> findByCallSign(
            @PathVariable @NotBlank String callSign) {
        log.debug("Finding vessel call with call sign: {}", callSign);
        return vesselCallService.findByCallSign(callSign)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('PORT_AUTHORITY', 'SERVICE_PROVIDER')")
    @Operation(summary = "Find vessel calls by status")
    @ApiResponse(responseCode = "200", description = "Vessel calls retrieved successfully")
    @Timed(value = "vessel.call.findByStatus", description = "Time taken to find vessel calls by status")
    public ResponseEntity<Page<VesselCallDTO>> findByStatus(
            @PathVariable @NotNull VesselCallStatus status,
            Pageable pageable) {
        log.debug("Finding vessel calls with status: {}", status);
        return ResponseEntity.ok(vesselCallService.findByStatus(status, pageable));
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('PORT_AUTHORITY', 'SERVICE_PROVIDER')")
    @Operation(summary = "Find vessel calls within date range")
    @ApiResponse(responseCode = "200", description = "Vessel calls retrieved successfully")
    @Timed(value = "vessel.call.findByDateRange", description = "Time taken to find vessel calls by date range")
    public ResponseEntity<Page<VesselCallDTO>> findByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable) {
        log.debug("Finding vessel calls between {} and {}", startDate, endDate);
        return ResponseEntity.ok(vesselCallService.findByDateRange(startDate, endDate, pageable));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('PORT_AUTHORITY')")
    @Operation(summary = "Update vessel call status")
    @ApiResponse(responseCode = "200", description = "Status updated successfully")
    @Timed(value = "vessel.call.updateStatus", description = "Time taken to update vessel call status")
    public ResponseEntity<VesselCallDTO> updateStatus(
            @PathVariable @NotNull Long id,
            @RequestParam @NotNull VesselCallStatus status) {
        log.info("Updating status of vessel call ID: {} to {}", id, status);
        VesselCallDTO updated = vesselCallService.updateStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PORT_AUTHORITY')")
    @Operation(summary = "Cancel vessel call")
    @ApiResponse(responseCode = "200", description = "Vessel call cancelled successfully")
    @Timed(value = "vessel.call.cancel", description = "Time taken to cancel vessel call")
    public ResponseEntity<VesselCallDTO> cancelVesselCall(
            @PathVariable @NotNull Long id,
            @RequestParam @NotBlank String reason) {
        log.info("Cancelling vessel call ID: {} with reason: {}", id, reason);
        VesselCallDTO cancelled = vesselCallService.cancelVesselCall(id, reason);
        return ResponseEntity.ok(cancelled);
    }
}