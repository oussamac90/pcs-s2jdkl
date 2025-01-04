package com.pcs.vcms.controller;

import com.pcs.vcms.dto.PreArrivalNotificationDTO;
import com.pcs.vcms.service.PreArrivalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing pre-arrival notifications in the Vessel Call Management System.
 * Provides secure endpoints for submission and retrieval of pre-arrival information with
 * comprehensive validation and error handling.
 */
@RestController
@RequestMapping("/api/v1/pre-arrival")
@Validated
@SecurityRequirement(name = "bearer-key")
@Tag(name = "Pre-Arrival Notifications", description = "APIs for managing vessel pre-arrival notifications")
@Slf4j
public class PreArrivalController {

    private final PreArrivalService preArrivalService;

    /**
     * Constructs PreArrivalController with required service dependency.
     *
     * @param preArrivalService service layer for pre-arrival operations
     */
    public PreArrivalController(PreArrivalService preArrivalService) {
        this.preArrivalService = preArrivalService;
    }

    /**
     * Submits a new pre-arrival notification with comprehensive validation.
     *
     * @param notification the pre-arrival notification data
     * @return ResponseEntity containing the created notification
     */
    @PostMapping
    @Operation(summary = "Submit pre-arrival notification")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Notification created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid notification data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "403", description = "Forbidden access")
    })
    public ResponseEntity<PreArrivalNotificationDTO> submitPreArrivalNotification(
            @Valid @RequestBody PreArrivalNotificationDTO notification) {
        log.info("Received pre-arrival notification submission for vessel call: {}", 
                notification.getVesselCallId());
        
        PreArrivalNotificationDTO submittedNotification = 
                preArrivalService.submitPreArrivalNotification(notification);
        
        log.info("Successfully submitted pre-arrival notification with ID: {}", 
                submittedNotification.getId());
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(submittedNotification);
    }

    /**
     * Retrieves a specific pre-arrival notification by ID.
     *
     * @param id unique identifier of the notification
     * @return ResponseEntity containing the found notification or 404
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get pre-arrival notification by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notification found"),
        @ApiResponse(responseCode = "404", description = "Notification not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "403", description = "Forbidden access")
    })
    public ResponseEntity<PreArrivalNotificationDTO> getPreArrivalNotification(
            @PathVariable @Positive Long id) {
        log.debug("Retrieving pre-arrival notification with ID: {}", id);
        
        Optional<PreArrivalNotificationDTO> notification = 
                preArrivalService.getPreArrivalNotification(id);
        
        return notification
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves all pre-arrival notifications for a specific vessel call.
     *
     * @param vesselCallId unique identifier of the vessel call
     * @return ResponseEntity containing list of notifications
     */
    @GetMapping("/vessel-call/{vesselCallId}")
    @Operation(summary = "Get notifications by vessel call")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "403", description = "Forbidden access")
    })
    public ResponseEntity<List<PreArrivalNotificationDTO>> getPreArrivalNotificationsByVesselCall(
            @PathVariable @Positive Long vesselCallId) {
        log.debug("Retrieving pre-arrival notifications for vessel call: {}", vesselCallId);
        
        List<PreArrivalNotificationDTO> notifications = 
                preArrivalService.getPreArrivalNotificationsByVesselCall(vesselCallId);
        
        return ResponseEntity.ok(notifications);
    }

    /**
     * Retrieves the latest pre-arrival notification for a vessel call.
     *
     * @param vesselCallId unique identifier of the vessel call
     * @return ResponseEntity containing the latest notification or 404
     */
    @GetMapping("/vessel-call/{vesselCallId}/latest")
    @Operation(summary = "Get latest notification for vessel call")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Latest notification found"),
        @ApiResponse(responseCode = "404", description = "No notifications found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "403", description = "Forbidden access")
    })
    public ResponseEntity<PreArrivalNotificationDTO> getLatestPreArrivalNotification(
            @PathVariable @Positive Long vesselCallId) {
        log.debug("Retrieving latest pre-arrival notification for vessel call: {}", vesselCallId);
        
        List<PreArrivalNotificationDTO> notifications = 
                preArrivalService.getPreArrivalNotificationsByVesselCall(vesselCallId);
        
        return notifications.stream()
                .max((n1, n2) -> n1.getSubmittedAt().compareTo(n2.getSubmittedAt()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}