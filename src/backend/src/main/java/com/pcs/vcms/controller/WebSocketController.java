package com.pcs.vcms.controller;

import com.pcs.vcms.service.NotificationService;
import com.pcs.vcms.entity.VesselCall;
import com.pcs.vcms.entity.BerthAllocation;
import com.pcs.vcms.entity.ServiceBooking;
import com.pcs.vcms.entity.Clearance;

import org.springframework.stereotype.Controller;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * WebSocket controller for managing real-time updates in the Vessel Call Management System.
 * Implements enhanced reliability, security, and performance features.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class WebSocketController {

    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MeterRegistry meterRegistry;

    // Connection tracking with heartbeat monitoring
    private final ConcurrentHashMap<String, ConnectionState> connections = new ConcurrentHashMap<>();
    
    // Performance metrics
    private final Counter messageCounter;
    private final Timer messageLatencyTimer;
    private final Counter errorCounter;

    // Message compression
    private static final int COMPRESSION_LEVEL = 6;
    private static final int BUFFER_SIZE = 8192;

    /**
     * Constructor initializing metrics and connection management
     */
    public WebSocketController(NotificationService notificationService, 
                             SimpMessagingTemplate messagingTemplate,
                             MeterRegistry meterRegistry) {
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
        this.meterRegistry = meterRegistry;

        // Initialize metrics
        this.messageCounter = Counter.builder("websocket.messages")
                                   .description("Number of WebSocket messages processed")
                                   .register(meterRegistry);
        
        this.messageLatencyTimer = Timer.builder("websocket.message.latency")
                                      .description("WebSocket message processing latency")
                                      .register(meterRegistry);
        
        this.errorCounter = Counter.builder("websocket.errors")
                                 .description("Number of WebSocket errors")
                                 .register(meterRegistry);
    }

    /**
     * Handles subscription to vessel call updates with enhanced reliability
     */
    @MessageMapping("/subscribe/vessel-calls")
    @PreAuthorize("hasAnyRole('ROLE_PORT_AUTHORITY', 'ROLE_VESSEL_AGENT')")
    public void subscribeToVesselCalls(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        Timer.Sample timer = Timer.start(meterRegistry);
        String sessionId = headerAccessor.getSessionId();
        
        try {
            log.info("User {} subscribing to vessel calls updates", principal.getName());
            
            // Track connection state
            connections.put(sessionId, new ConnectionState(principal.getName(), System.currentTimeMillis()));
            
            // Initialize subscription
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/vessel-calls",
                createSubscriptionAck("vessel-calls")
            );
            
            messageCounter.increment();
            timer.stop(messageLatencyTimer);
            
        } catch (Exception e) {
            errorCounter.increment();
            log.error("Error processing vessel calls subscription for user {}: {}", 
                     principal.getName(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Handles subscription to berth allocation updates
     */
    @MessageMapping("/subscribe/berth-allocations")
    @PreAuthorize("hasAnyRole('ROLE_PORT_AUTHORITY', 'ROLE_VESSEL_AGENT')")
    public void subscribeToBerthAllocations(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        Timer.Sample timer = Timer.start(meterRegistry);
        String sessionId = headerAccessor.getSessionId();
        
        try {
            log.info("User {} subscribing to berth allocation updates", principal.getName());
            
            connections.put(sessionId, new ConnectionState(principal.getName(), System.currentTimeMillis()));
            
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/berth-allocations",
                createSubscriptionAck("berth-allocations")
            );
            
            messageCounter.increment();
            timer.stop(messageLatencyTimer);
            
        } catch (Exception e) {
            errorCounter.increment();
            log.error("Error processing berth allocation subscription for user {}: {}", 
                     principal.getName(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Handles subscription to service booking updates
     */
    @MessageMapping("/subscribe/service-bookings")
    @PreAuthorize("hasAnyRole('ROLE_SERVICE_PROVIDER', 'ROLE_VESSEL_AGENT')")
    public void subscribeToServiceBookings(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        Timer.Sample timer = Timer.start(meterRegistry);
        String sessionId = headerAccessor.getSessionId();
        
        try {
            log.info("User {} subscribing to service booking updates", principal.getName());
            
            connections.put(sessionId, new ConnectionState(principal.getName(), System.currentTimeMillis()));
            
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/service-bookings",
                createSubscriptionAck("service-bookings")
            );
            
            messageCounter.increment();
            timer.stop(messageLatencyTimer);
            
        } catch (Exception e) {
            errorCounter.increment();
            log.error("Error processing service booking subscription for user {}: {}", 
                     principal.getName(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Handles subscription to clearance updates
     */
    @MessageMapping("/subscribe/clearances")
    @PreAuthorize("hasAnyRole('ROLE_PORT_AUTHORITY', 'ROLE_CUSTOMS')")
    public void subscribeToClearances(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        Timer.Sample timer = Timer.start(meterRegistry);
        String sessionId = headerAccessor.getSessionId();
        
        try {
            log.info("User {} subscribing to clearance updates", principal.getName());
            
            connections.put(sessionId, new ConnectionState(principal.getName(), System.currentTimeMillis()));
            
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/clearances",
                createSubscriptionAck("clearances")
            );
            
            messageCounter.increment();
            timer.stop(messageLatencyTimer);
            
        } catch (Exception e) {
            errorCounter.increment();
            log.error("Error processing clearance subscription for user {}: {}", 
                     principal.getName(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Handles WebSocket connection closure and cleanup
     */
    public void handleDisconnect(String sessionId) {
        ConnectionState state = connections.remove(sessionId);
        if (state != null) {
            log.info("User {} disconnected from WebSocket", state.getUsername());
        }
    }

    /**
     * Compresses message payload for efficient transmission
     */
    private byte[] compressMessage(byte[] data) {
        Deflater deflater = new Deflater(COMPRESSION_LEVEL);
        deflater.setInput(data);
        deflater.finish();
        
        byte[] buffer = new byte[BUFFER_SIZE];
        int compressedLength = deflater.deflate(buffer);
        
        byte[] output = new byte[compressedLength];
        System.arraycopy(buffer, 0, output, 0, compressedLength);
        
        deflater.end();
        return output;
    }

    /**
     * Creates subscription acknowledgment message
     */
    private SubscriptionAck createSubscriptionAck(String topic) {
        return new SubscriptionAck(topic, System.currentTimeMillis());
    }

    /**
     * Inner class for tracking connection state
     */
    private static class ConnectionState {
        private final String username;
        private final long connectionTime;
        private long lastHeartbeat;

        public ConnectionState(String username, long connectionTime) {
            this.username = username;
            this.connectionTime = connectionTime;
            this.lastHeartbeat = connectionTime;
        }

        public String getUsername() {
            return username;
        }

        public void updateHeartbeat() {
            this.lastHeartbeat = System.currentTimeMillis();
        }
    }

    /**
     * Inner class for subscription acknowledgment
     */
    private static class SubscriptionAck {
        private final String topic;
        private final long timestamp;

        public SubscriptionAck(String topic, long timestamp) {
            this.topic = topic;
            this.timestamp = timestamp;
        }
    }
}