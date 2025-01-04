package com.pcs.vcms.service.impl;

import com.pcs.vcms.service.NotificationService;
import com.pcs.vcms.entity.VesselCall;
import com.pcs.vcms.entity.BerthAllocation;
import com.pcs.vcms.entity.ServiceBooking;
import com.pcs.vcms.entity.Clearance;
import com.pcs.vcms.common.tracking.NotificationDeliveryTracker;
import com.pcs.vcms.security.MessageSignatureService;

import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.RateLimiter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

/**
 * Enhanced implementation of NotificationService providing secure, reliable real-time notifications
 * with comprehensive delivery tracking and monitoring capabilities.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationDeliveryTracker deliveryTracker;
    private final MessageSignatureService signatureService;
    private final RateLimiter rateLimiter;

    private static final String VESSEL_TOPIC = "/topic/vessel-calls";
    private static final String BERTH_TOPIC = "/topic/berth-allocations";
    private static final String SERVICE_TOPIC = "/topic/service-bookings";
    private static final String CLEARANCE_TOPIC = "/topic/clearances";
    private static final String USER_TOPIC = "/topic/user/";

    /**
     * Constructs a new NotificationServiceImpl with required dependencies.
     */
    public NotificationServiceImpl(
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper,
            NotificationDeliveryTracker deliveryTracker,
            MessageSignatureService signatureService,
            RateLimiter rateLimiter) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.deliveryTracker = deliveryTracker;
        this.signatureService = signatureService;
        this.rateLimiter = rateLimiter;
    }

    @Override
    @Retry(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public NotificationResult sendVesselCallUpdate(VesselCall vesselCall) {
        String trackingId = UUID.randomUUID().toString();
        log.info("Sending vessel call update notification. TrackingId: {}, VesselCall: {}", 
                trackingId, vesselCall.getCallSign());

        try {
            Map<String, Object> payload = createNotificationPayload(
                "VESSEL_UPDATE",
                vesselCall,
                trackingId
            );

            String signature = signatureService.signMessage(objectMapper.writeValueAsString(payload));
            payload.put("signature", signature);

            deliveryTracker.trackDeliveryStart(trackingId);
            messagingTemplate.convertAndSend(VESSEL_TOPIC, payload);
            deliveryTracker.trackDeliverySuccess(trackingId);

            return createSuccessResult(trackingId);
        } catch (Exception e) {
            log.error("Failed to send vessel call update. TrackingId: {}", trackingId, e);
            deliveryTracker.trackDeliveryFailure(trackingId, e.getMessage());
            return createErrorResult(trackingId, e.getMessage());
        }
    }

    @Override
    @Retry(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public NotificationResult sendBerthAllocationUpdate(BerthAllocation berthAllocation) {
        String trackingId = UUID.randomUUID().toString();
        log.info("Sending berth allocation update notification. TrackingId: {}, Berth: {}", 
                trackingId, berthAllocation.getBerth().getName());

        try {
            Map<String, Object> payload = createNotificationPayload(
                "BERTH_UPDATE",
                berthAllocation,
                trackingId
            );

            String signature = signatureService.signMessage(objectMapper.writeValueAsString(payload));
            payload.put("signature", signature);

            deliveryTracker.trackDeliveryStart(trackingId);
            messagingTemplate.convertAndSend(BERTH_TOPIC, payload);
            deliveryTracker.trackDeliverySuccess(trackingId);

            return createSuccessResult(trackingId);
        } catch (Exception e) {
            log.error("Failed to send berth allocation update. TrackingId: {}", trackingId, e);
            deliveryTracker.trackDeliveryFailure(trackingId, e.getMessage());
            return createErrorResult(trackingId, e.getMessage());
        }
    }

    @Override
    @Retry(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public NotificationResult sendServiceStatusUpdate(ServiceBooking serviceBooking) {
        String trackingId = UUID.randomUUID().toString();
        log.info("Sending service status update notification. TrackingId: {}, Service: {}", 
                trackingId, serviceBooking.getServiceType());

        try {
            Map<String, Object> payload = createNotificationPayload(
                "SERVICE_UPDATE",
                serviceBooking,
                trackingId
            );

            String signature = signatureService.signMessage(objectMapper.writeValueAsString(payload));
            payload.put("signature", signature);

            deliveryTracker.trackDeliveryStart(trackingId);
            messagingTemplate.convertAndSend(SERVICE_TOPIC, payload);
            deliveryTracker.trackDeliverySuccess(trackingId);

            return createSuccessResult(trackingId);
        } catch (Exception e) {
            log.error("Failed to send service status update. TrackingId: {}", trackingId, e);
            deliveryTracker.trackDeliveryFailure(trackingId, e.getMessage());
            return createErrorResult(trackingId, e.getMessage());
        }
    }

    @Override
    @Retry(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public NotificationResult sendClearanceUpdate(Clearance clearance) {
        String trackingId = UUID.randomUUID().toString();
        log.info("Sending clearance update notification. TrackingId: {}, Reference: {}", 
                trackingId, clearance.getReferenceNumber());

        try {
            Map<String, Object> payload = createNotificationPayload(
                "CLEARANCE_UPDATE",
                clearance,
                trackingId
            );

            String signature = signatureService.signMessage(objectMapper.writeValueAsString(payload));
            payload.put("signature", signature);

            deliveryTracker.trackDeliveryStart(trackingId);
            messagingTemplate.convertAndSend(CLEARANCE_TOPIC, payload);
            deliveryTracker.trackDeliverySuccess(trackingId);

            return createSuccessResult(trackingId);
        } catch (Exception e) {
            log.error("Failed to send clearance update. TrackingId: {}", trackingId, e);
            deliveryTracker.trackDeliveryFailure(trackingId, e.getMessage());
            return createErrorResult(trackingId, e.getMessage());
        }
    }

    @Override
    public NotificationResult sendUserNotification(String userId, String message) {
        String trackingId = UUID.randomUUID().toString();
        log.info("Sending user notification. TrackingId: {}, UserId: {}", trackingId, userId);

        try {
            rateLimiter.acquirePermission();

            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "USER_NOTIFICATION");
            payload.put("message", message);
            payload.put("timestamp", LocalDateTime.now());
            payload.put("trackingId", trackingId);

            String signature = signatureService.signMessage(objectMapper.writeValueAsString(payload));
            payload.put("signature", signature);

            deliveryTracker.trackDeliveryStart(trackingId);
            messagingTemplate.convertAndSend(USER_TOPIC + userId, payload);
            deliveryTracker.trackDeliverySuccess(trackingId);

            return createSuccessResult(trackingId);
        } catch (Exception e) {
            log.error("Failed to send user notification. TrackingId: {}", trackingId, e);
            deliveryTracker.trackDeliveryFailure(trackingId, e.getMessage());
            return createErrorResult(trackingId, e.getMessage());
        }
    }

    private Map<String, Object> createNotificationPayload(String type, Object data, String trackingId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type);
        payload.put("data", data);
        payload.put("timestamp", LocalDateTime.now());
        payload.put("trackingId", trackingId);
        return payload;
    }

    private NotificationResult createSuccessResult(String trackingId) {
        return new NotificationResult() {
            @Override
            public String getTrackingId() {
                return trackingId;
            }

            @Override
            public boolean isDelivered() {
                return true;
            }

            @Override
            public long getDeliveryTimestamp() {
                return System.currentTimeMillis();
            }

            @Override
            public int getRetryCount() {
                return deliveryTracker.getRetryCount(trackingId);
            }

            @Override
            public String getDeliveryStatus() {
                return "DELIVERED";
            }

            @Override
            public String getErrorMessage() {
                return null;
            }
        };
    }

    private NotificationResult createErrorResult(String trackingId, String errorMessage) {
        return new NotificationResult() {
            @Override
            public String getTrackingId() {
                return trackingId;
            }

            @Override
            public boolean isDelivered() {
                return false;
            }

            @Override
            public long getDeliveryTimestamp() {
                return System.currentTimeMillis();
            }

            @Override
            public int getRetryCount() {
                return deliveryTracker.getRetryCount(trackingId);
            }

            @Override
            public String getDeliveryStatus() {
                return "FAILED";
            }

            @Override
            public String getErrorMessage() {
                return errorMessage;
            }
        };
    }
}