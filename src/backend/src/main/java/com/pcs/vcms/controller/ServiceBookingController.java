package com.pcs.vcms.controller;

import com.pcs.vcms.dto.ServiceBookingDTO;
import com.pcs.vcms.service.ServiceBookingService;
import com.pcs.vcms.entity.ServiceBooking.ServiceType;
import com.pcs.vcms.entity.ServiceBooking.ServiceStatus;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.net.URI;
import java.util.Objects;

/**
 * REST controller for managing port service bookings with enhanced security and validation.
 * Implements comprehensive service booking operations with rate limiting and caching.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@RestController
@RequestMapping("/api/v1/service-bookings")
@Validated
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Service Bookings", description = "Port service booking operations API")
public class ServiceBookingController {

    private final ServiceBookingService serviceBookingService;

    public ServiceBookingController(@NotNull ServiceBookingService serviceBookingService) {
        this.serviceBookingService = Objects.requireNonNull(serviceBookingService, 
            "ServiceBookingService must not be null");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SERVICE_BOOKING_CREATE')")
    @RateLimiter(name = "createBooking")
    @Operation(summary = "Create new service booking", 
              description = "Creates a new port service booking with validation")
    @ApiResponse(responseCode = "201", description = "Service booking created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid booking request")
    @ApiResponse(responseCode = "409", description = "Booking conflicts with existing reservations")
    public ResponseEntity<ServiceBookingDTO> createServiceBooking(
            @Valid @RequestBody ServiceBookingDTO bookingDTO) {
        ServiceBookingDTO created = serviceBookingService.createServiceBooking(bookingDTO);
        return ResponseEntity
            .created(URI.create("/api/v1/service-bookings/" + created.getId()))
            .body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SERVICE_BOOKING_READ')")
    @Cacheable(value = "bookings", key = "#id")
    @Operation(summary = "Get service booking by ID")
    @ApiResponse(responseCode = "200", description = "Service booking retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Service booking not found")
    public ResponseEntity<ServiceBookingDTO> getServiceBooking(
            @PathVariable @NotNull Long id) {
        return ResponseEntity.ok(serviceBookingService.getServiceBooking(id));
    }

    @GetMapping("/vessel-call/{vesselCallId}")
    @PreAuthorize("hasRole('SERVICE_BOOKING_READ')")
    @Cacheable(value = "vesselCallBookings", key = "#vesselCallId + #pageable.pageNumber")
    @Operation(summary = "Get service bookings for vessel call")
    public ResponseEntity<Page<ServiceBookingDTO>> getServiceBookingsForVesselCall(
            @PathVariable @NotNull Long vesselCallId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
            serviceBookingService.getServiceBookingsForVesselCall(vesselCallId, pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('SERVICE_BOOKING_READ')")
    @Cacheable(value = "bookingSearches", 
               key = "#serviceType + #status + #pageable.pageNumber")
    @Operation(summary = "Search service bookings with filters")
    public ResponseEntity<Page<ServiceBookingDTO>> getServiceBookingsByTypeAndStatus(
            @Parameter(description = "Service type filter")
            @RequestParam(required = false) ServiceType serviceType,
            @Parameter(description = "Service status filter")
            @RequestParam(required = false) ServiceStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
            serviceBookingService.getServiceBookingsByTypeAndStatus(
                serviceType, status, pageable));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('SERVICE_BOOKING_CANCEL')")
    @CacheEvict(value = {"bookings", "vesselCallBookings", "bookingSearches"}, 
                allEntries = true)
    @Operation(summary = "Cancel service booking")
    @ApiResponse(responseCode = "200", description = "Service booking cancelled successfully")
    @ApiResponse(responseCode = "404", description = "Service booking not found")
    @ApiResponse(responseCode = "409", description = "Booking cannot be cancelled")
    public ResponseEntity<ServiceBookingDTO> cancelServiceBooking(
            @PathVariable @NotNull Long id,
            @RequestParam @NotNull @Size(max = 500) String reason) {
        return ResponseEntity.ok(serviceBookingService.cancelServiceBooking(id, reason));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('SERVICE_BOOKING_UPDATE')")
    @CacheEvict(value = {"bookings", "vesselCallBookings", "bookingSearches"}, 
                allEntries = true)
    @Operation(summary = "Update service booking status")
    public ResponseEntity<ServiceBookingDTO> updateServiceBookingStatus(
            @PathVariable @NotNull Long id,
            @RequestParam @NotNull ServiceStatus newStatus) {
        return ResponseEntity.ok(
            serviceBookingService.updateServiceBookingStatus(id, newStatus));
    }
}