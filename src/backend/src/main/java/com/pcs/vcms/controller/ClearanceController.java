package com.pcs.vcms.controller;

import com.pcs.vcms.dto.ClearanceDTO;
import com.pcs.vcms.service.ClearanceService;
import com.pcs.vcms.entity.Clearance.ClearanceStatus;
import com.pcs.vcms.audit.AuditLogger;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for managing vessel clearance operations.
 * Implements secure endpoints with comprehensive validation and workflow management.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@RestController
@RequestMapping("/api/v1/clearances")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Clearance Management", description = "APIs for vessel clearance operations")
@Validated
@RequiredArgsConstructor
@Slf4j
public class ClearanceController {

    private final ClearanceService clearanceService;
    private final AuditLogger auditLogger;

    @PostMapping
    @PreAuthorize("hasRole('CLEARANCE_SUBMIT')")
    @RateLimiter(name = "clearanceSubmit")
    @Operation(summary = "Submit new clearance request",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Clearance created successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid input data"),
                    @ApiResponse(responseCode = "403", description = "Insufficient permissions")
            })
    public ResponseEntity<ClearanceDTO> submitClearance(
            @Valid @RequestBody ClearanceDTO clearanceDTO) {
        log.info("Submitting new clearance request for vessel call: {}", clearanceDTO.getVesselCallId());
        ClearanceDTO createdClearance = clearanceService.submitClearance(clearanceDTO);
        auditLogger.logEvent("CLEARANCE_SUBMITTED", createdClearance.getId(), clearanceDTO.getSubmittedBy());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdClearance);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('CLEARANCE_UPDATE')")
    @Operation(summary = "Update clearance status",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Status updated successfully"),
                    @ApiResponse(responseCode = "404", description = "Clearance not found"),
                    @ApiResponse(responseCode = "409", description = "Invalid status transition")
            })
    public ResponseEntity<ClearanceDTO> updateClearanceStatus(
            @PathVariable @NotNull Long id,
            @RequestParam @NotNull ClearanceStatus status,
            @RequestParam(required = false) String remarks) {
        log.info("Updating clearance status: {} to {}", id, status);
        ClearanceDTO updatedClearance = clearanceService.updateClearanceStatus(id, status, remarks);
        auditLogger.logEvent("CLEARANCE_STATUS_UPDATED", id, status.toString());
        return ResponseEntity.ok(updatedClearance);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLEARANCE_VIEW', 'CLEARANCE_UPDATE')")
    @Operation(summary = "Get clearance by ID")
    public ResponseEntity<ClearanceDTO> getClearanceById(@PathVariable @NotNull Long id) {
        return clearanceService.getClearanceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/vessel-call/{vesselCallId}")
    @PreAuthorize("hasAnyRole('CLEARANCE_VIEW', 'CLEARANCE_UPDATE')")
    @Operation(summary = "Get clearances by vessel call")
    public ResponseEntity<List<ClearanceDTO>> getClearancesByVesselCall(
            @PathVariable @NotNull Long vesselCallId) {
        List<ClearanceDTO> clearances = clearanceService.getClearancesByVesselCall(vesselCallId);
        return ResponseEntity.ok(clearances);
    }

    @GetMapping
    @PreAuthorize("hasRole('CLEARANCE_VIEW')")
    @Operation(summary = "Get clearances with filtering")
    public ResponseEntity<Page<ClearanceDTO>> getClearances(
            Pageable pageable,
            @RequestParam(required = false) ClearanceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {
        Page<ClearanceDTO> clearances = clearanceService.getClearances(pageable, status, fromDate, toDate);
        return ResponseEntity.ok(clearances);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CLEARANCE_CANCEL')")
    @Operation(summary = "Cancel clearance request")
    public ResponseEntity<ClearanceDTO> cancelClearance(
            @PathVariable @NotNull Long id,
            @RequestParam(required = false) String remarks) {
        log.info("Cancelling clearance: {}", id);
        ClearanceDTO cancelledClearance = clearanceService.cancelClearance(id, remarks);
        auditLogger.logEvent("CLEARANCE_CANCELLED", id, remarks);
        return ResponseEntity.ok(cancelledClearance);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasRole('CLEARANCE_VIEW')")
    @Operation(summary = "Get clearance history")
    public ResponseEntity<List<ClearanceDTO>> getClearanceHistory(@PathVariable @NotNull Long id) {
        List<ClearanceDTO> history = clearanceService.getClearanceHistory(id);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/validate-departure/{vesselCallId}")
    @PreAuthorize("hasRole('CLEARANCE_VALIDATE')")
    @Operation(summary = "Validate departure clearances")
    public ResponseEntity<Boolean> validateDepartureClearances(
            @PathVariable @NotNull Long vesselCallId) {
        boolean isValid = clearanceService.validateDepartureClearances(vesselCallId);
        return ResponseEntity.ok(isValid);
    }

    @GetMapping("/expired")
    @PreAuthorize("hasRole('CLEARANCE_VIEW')")
    @Operation(summary = "Get expired clearances")
    public ResponseEntity<Page<ClearanceDTO>> getExpiredClearances(Pageable pageable) {
        Page<ClearanceDTO> expiredClearances = clearanceService.getExpiredClearances(pageable);
        return ResponseEntity.ok(expiredClearances);
    }
}