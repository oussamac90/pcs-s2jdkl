package com.pcs.vcms.controller;

import com.pcs.vcms.dto.BerthAllocationDTO;
import com.pcs.vcms.service.BerthAllocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
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
 * REST controller for managing berth allocations with enhanced security and performance features.
 * Implements comprehensive CRUD operations with role-based access control and caching.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@RestController
@RequestMapping("/api/v1/berth-allocations")
@Tag(name = "Berth Allocation", description = "Berth allocation management endpoints")
@SecurityRequirement(name = "bearerAuth")
@Validated
@Slf4j
@CacheConfig(cacheNames = "berthAllocations")
public class BerthAllocationController {

    private final BerthAllocationService berthAllocationService;

    @Autowired
    public BerthAllocationController(BerthAllocationService berthAllocationService) {
        this.berthAllocationService = berthAllocationService;
    }

    @PostMapping
    @Operation(summary = "Create new berth allocation", description = "Creates a new berth allocation with conflict detection")
    @ApiResponse(responseCode = "201", description = "Berth allocation created successfully")
    @PreAuthorize("hasRole('BERTH_OPERATOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<BerthAllocationDTO> createBerthAllocation(
            @Valid @RequestBody BerthAllocationDTO allocationDTO) {
        log.info("REST request to create berth allocation for vessel: {}", allocationDTO.getVesselName());
        BerthAllocationDTO result = berthAllocationService.createBerthAllocation(allocationDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update berth allocation", description = "Updates existing berth allocation with conflict detection")
    @PreAuthorize("hasRole('BERTH_OPERATOR')")
    @CachePut(key = "#id")
    public ResponseEntity<BerthAllocationDTO> updateBerthAllocation(
            @PathVariable @NotNull Long id,
            @Valid @RequestBody BerthAllocationDTO allocationDTO) {
        log.info("REST request to update berth allocation ID: {}", id);
        BerthAllocationDTO result = berthAllocationService.updateBerthAllocation(id, allocationDTO);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get berth allocation by ID")
    @PreAuthorize("hasAnyRole('BERTH_OPERATOR', 'BERTH_PLANNER', 'PORT_ADMIN')")
    @Cacheable(key = "#id")
    public ResponseEntity<BerthAllocationDTO> getBerthAllocation(@PathVariable @NotNull Long id) {
        log.debug("REST request to get berth allocation ID: {}", id);
        return berthAllocationService.getBerthAllocation(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel berth allocation")
    @PreAuthorize("hasRole('BERTH_OPERATOR')")
    @CacheEvict(key = "#id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelBerthAllocation(@PathVariable @NotNull Long id) {
        log.info("REST request to cancel berth allocation ID: {}", id);
        berthAllocationService.cancelBerthAllocation(id);
    }

    @GetMapping("/vessel-call/{vesselCallId}")
    @Operation(summary = "Get allocations by vessel call")
    @PreAuthorize("hasAnyRole('BERTH_OPERATOR', 'BERTH_PLANNER', 'PORT_ADMIN')")
    public ResponseEntity<List<BerthAllocationDTO>> getAllocationsByVesselCall(
            @PathVariable @NotNull Long vesselCallId) {
        log.debug("REST request to get berth allocations for vessel call ID: {}", vesselCallId);
        List<BerthAllocationDTO> allocations = berthAllocationService.getAllocationsByVesselCall(vesselCallId);
        return ResponseEntity.ok(allocations);
    }

    @GetMapping("/berth/{berthId}")
    @Operation(summary = "Get allocations by berth and time window")
    @PreAuthorize("hasAnyRole('BERTH_OPERATOR', 'BERTH_PLANNER', 'PORT_ADMIN')")
    public ResponseEntity<List<BerthAllocationDTO>> getAllocationsByBerth(
            @PathVariable @NotNull Long berthId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        log.debug("REST request to get berth allocations for berth ID: {} between {} and {}", 
                berthId, startTime, endTime);
        List<BerthAllocationDTO> allocations = berthAllocationService.getAllocationsByBerth(
                berthId, startTime, endTime);
        return ResponseEntity.ok(allocations);
    }

    @GetMapping("/conflicts")
    @Operation(summary = "Check allocation conflicts")
    @PreAuthorize("hasAnyRole('BERTH_OPERATOR', 'BERTH_PLANNER')")
    public ResponseEntity<List<BerthAllocationDTO>> checkAllocationConflicts(
            @Valid @RequestBody BerthAllocationDTO allocationDTO) {
        log.debug("REST request to check conflicts for berth allocation");
        List<BerthAllocationDTO> conflicts = berthAllocationService.checkAllocationConflicts(allocationDTO);
        return ResponseEntity.ok(conflicts);
    }
}