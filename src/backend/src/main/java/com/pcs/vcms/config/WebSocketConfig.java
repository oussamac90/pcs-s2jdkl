package com.pcs.vcms.config;

import org.springframework.beans.factory.annotation.Value; // Spring Boot 6.1.x
import org.springframework.context.annotation.Configuration; // Spring Boot 6.1.x
import org.springframework.messaging.simp.config.MessageBrokerRegistry; // Spring Boot 6.1.x
import org.springframework.messaging.simp.config.StompEndpointRegistry; // Spring Boot 6.1.x
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker; // Spring Boot 6.1.x
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer; // Spring Boot 6.1.x

/**
 * WebSocket Configuration for Vessel Call Management System
 * Provides secure real-time communication capabilities with comprehensive
 * configuration for STOMP messaging, security, and scalability.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${vcms.websocket.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${vcms.websocket.broker.host:localhost}")
    private String brokerRelayHost;

    @Value("${vcms.websocket.broker.port:61613}")
    private Integer brokerRelayPort;

    @Value("${vcms.websocket.broker.enabled:false}")
    private boolean externalBrokerEnabled;

    /**
     * Configures STOMP endpoints for WebSocket communication with security
     * and fallback options.
     *
     * @param registry StompEndpointRegistry for endpoint configuration
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins.split(","))
                .withSockJS()
                .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1.6.1/dist/sockjs.min.js")
                .setWebSocketEnabled(true)
                .setSessionCookieNeeded(false)
                .setHeartbeatTime(25000)
                .setDisconnectDelay(5000)
                .setStreamBytesLimit(512 * 1024)
                .setHttpMessageCacheSize(1000)
                .setInterceptors(new WebSocketHandshakeInterceptor());

        // Additional endpoint for admin-specific communications
        registry.addEndpoint("/ws/admin")
                .setAllowedOrigins(allowedOrigins.split(","))
                .withSockJS();
    }

    /**
     * Configures message broker settings for scalable real-time messaging.
     * Supports both simple broker and external STOMP broker relay configurations.
     *
     * @param registry MessageBrokerRegistry for broker configuration
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Configure message broker prefixes
        registry.setApplicationDestinationPrefixes("/app")
                .setUserDestinationPrefix("/user");

        if (externalBrokerEnabled) {
            // External STOMP broker relay configuration
            registry.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost(brokerRelayHost)
                    .setRelayPort(brokerRelayPort)
                    .setClientLogin("guest")
                    .setClientPasscode("guest")
                    .setSystemLogin("guest")
                    .setSystemPasscode("guest")
                    .setSystemHeartbeatSendInterval(5000)
                    .setSystemHeartbeatReceiveInterval(4000);
        } else {
            // Simple in-memory broker configuration
            registry.enableSimpleBroker("/topic", "/queue")
                    .setHeartbeatValue(new long[]{10000, 10000})
                    .setTaskScheduler(new ConcurrentTaskScheduler())
                    .setPreservePublishOrder(true);
        }
    }

    /**
     * Inner class for WebSocket handshake interception
     * Handles security and session management during connection establishment
     */
    private static class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
        
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                     WebSocketHandler wsHandler, Map<String, Object> attributes) {
            // Security headers
            response.getHeaders().add("X-Frame-Options", "DENY");
            response.getHeaders().add("X-Content-Type-Options", "nosniff");
            response.getHeaders().add("X-XSS-Protection", "1; mode=block");
            
            // Add session attributes
            attributes.put("sessionId", UUID.randomUUID().toString());
            attributes.put("connectionTime", System.currentTimeMillis());
            
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                 WebSocketHandler wsHandler, Exception exception) {
            // Post-handshake processing if needed
        }
    }
}