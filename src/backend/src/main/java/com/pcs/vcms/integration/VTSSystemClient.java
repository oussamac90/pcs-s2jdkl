package com.pcs.vcms.integration;

import com.pcs.vcms.entity.Vessel;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.http.*;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.time.Duration;

/**
 * Enhanced client for integrating with external Vessel Traffic Service (VTS) system.
 * Provides real-time vessel tracking with robust error handling, monitoring, and security features.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Service
@Slf4j
public class VTSSystemClient {

    @Value("${vts.base-url}")
    private String vtsBaseUrl;

    @Value("${vts.api-key}")
    private String vtsApiKey;

    @Value("${vts.secret-key}")
    private String vtsSecretKey;

    private final RestTemplate restTemplate;
    private final Integer maxRetries;
    private final Long retryDelay;
    private final Integer connectionTimeout;
    private final Integer readTimeout;
    private final CircuitBreaker circuitBreaker;

    /**
     * Constructs VTSSystemClient with necessary configurations.
     */
    public VTSSystemClient(
            RestTemplate restTemplate,
            @Value("${vts.max-retries:3}") Integer maxRetries,
            @Value("${vts.retry-delay:1000}") Long retryDelay,
            @Value("${vts.connection-timeout:5000}") Integer connectionTimeout,
            @Value("${vts.read-timeout:10000}") Integer readTimeout,
            CircuitBreaker circuitBreaker) {
        
        this.restTemplate = restTemplate;
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        this.circuitBreaker = circuitBreaker;

        configureRestTemplate();
    }

    /**
     * Retrieves current position of a vessel from VTS.
     *
     * @param imoNumber IMO number of the vessel
     * @return VesselPosition containing current position data
     * @throws VTSIntegrationException if retrieval fails
     */
    @Cacheable(value = "vesselPositions", key = "#imoNumber", unless = "#result == null")
    @Retryable(maxAttempts = "#{@vtsSystemClient.maxRetries}", 
               backoff = @Backoff(delay = "#{@vtsSystemClient.retryDelay}"))
    public VesselPosition getVesselPosition(String imoNumber) {
        log.debug("Retrieving position for vessel with IMO: {}", imoNumber);
        
        return circuitBreaker.run(() -> {
            validateImoNumber(imoNumber);
            String url = buildUrl("/api/v1/vessels/" + imoNumber + "/position");
            
            HttpEntity<Void> requestEntity = createRequestEntity();
            
            try {
                ResponseEntity<VesselPosition> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    VesselPosition.class
                );
                
                log.info("Successfully retrieved position for vessel {}", imoNumber);
                return response.getBody();
            } catch (Exception e) {
                log.error("Failed to retrieve vessel position for IMO: {}", imoNumber, e);
                throw new VTSIntegrationException("Failed to retrieve vessel position", e);
            }
        }, throwable -> {
            log.error("Circuit breaker fallback for vessel position retrieval", throwable);
            return null;
        });
    }

    /**
     * Retrieves vessel movement history within specified time range.
     *
     * @param imoNumber IMO number of the vessel
     * @param fromTime Start time for movement history
     * @param toTime End time for movement history
     * @return List of vessel movements
     * @throws VTSIntegrationException if retrieval fails
     */
    @Retryable(maxAttempts = "#{@vtsSystemClient.maxRetries}", 
               backoff = @Backoff(delay = "#{@vtsSystemClient.retryDelay}"))
    public List<VesselMovement> getVesselMovements(String imoNumber, LocalDateTime fromTime, LocalDateTime toTime) {
        log.debug("Retrieving movements for vessel {} between {} and {}", imoNumber, fromTime, toTime);
        
        return circuitBreaker.run(() -> {
            validateTimeRange(fromTime, toTime);
            validateImoNumber(imoNumber);
            
            String url = buildUrl("/api/v1/vessels/" + imoNumber + "/movements")
                + "?fromTime=" + fromTime
                + "&toTime=" + toTime;
            
            HttpEntity<Void> requestEntity = createRequestEntity();
            
            try {
                ResponseEntity<List<VesselMovement>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<VesselMovement>>() {}
                );
                
                log.info("Successfully retrieved movements for vessel {}", imoNumber);
                return response.getBody();
            } catch (Exception e) {
                log.error("Failed to retrieve vessel movements for IMO: {}", imoNumber, e);
                throw new VTSIntegrationException("Failed to retrieve vessel movements", e);
            }
        }, throwable -> {
            log.error("Circuit breaker fallback for vessel movements retrieval", throwable);
            return Collections.emptyList();
        });
    }

    /**
     * Retrieves current port traffic information.
     *
     * @return PortTraffic containing current traffic data
     * @throws VTSIntegrationException if retrieval fails
     */
    @Cacheable(value = "portTraffic", unless = "#result == null")
    @Retryable(maxAttempts = "#{@vtsSystemClient.maxRetries}", 
               backoff = @Backoff(delay = "#{@vtsSystemClient.retryDelay}"))
    public PortTraffic getPortTraffic() {
        log.debug("Retrieving current port traffic information");
        
        return circuitBreaker.run(() -> {
            String url = buildUrl("/api/v1/port/traffic");
            HttpEntity<Void> requestEntity = createRequestEntity();
            
            try {
                ResponseEntity<PortTraffic> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    PortTraffic.class
                );
                
                log.info("Successfully retrieved port traffic information");
                return response.getBody();
            } catch (Exception e) {
                log.error("Failed to retrieve port traffic information", e);
                throw new VTSIntegrationException("Failed to retrieve port traffic", e);
            }
        }, throwable -> {
            log.error("Circuit breaker fallback for port traffic retrieval", throwable);
            return null;
        });
    }

    /**
     * Rotates the VTS API key for security purposes.
     */
    public void rotateApiKey() {
        log.info("Initiating API key rotation");
        
        try {
            String url = buildUrl("/api/v1/auth/rotate");
            HttpEntity<Void> requestEntity = createRequestEntity();
            
            ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<Map<String, String>>() {}
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                this.vtsApiKey = response.getBody().get("apiKey");
                log.info("Successfully rotated API key");
            }
        } catch (Exception e) {
            log.error("Failed to rotate API key", e);
            throw new VTSIntegrationException("Failed to rotate API key", e);
        }
    }

    private void configureRestTemplate() {
        HttpComponentsClientHttpRequestFactory factory = 
            new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(connectionTimeout);
        factory.setReadTimeout(readTimeout);
        restTemplate.setRequestFactory(factory);
    }

    private HttpEntity<Void> createRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", vtsApiKey);
        headers.set("X-Timestamp", String.valueOf(System.currentTimeMillis()));
        headers.set("X-Signature", generateSignature());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(headers);
    }

    private String generateSignature() {
        try {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String dataToSign = vtsApiKey + timestamp;
            
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                vtsSecretKey.getBytes(), "HmacSHA256");
            sha256Hmac.init(secretKey);
            
            return Base64.getEncoder().encodeToString(
                sha256Hmac.doFinal(dataToSign.getBytes()));
        } catch (Exception e) {
            log.error("Failed to generate request signature", e);
            throw new VTSIntegrationException("Failed to generate signature", e);
        }
    }

    private String buildUrl(String path) {
        return vtsBaseUrl + path;
    }

    private void validateImoNumber(String imoNumber) {
        if (imoNumber == null || !imoNumber.matches("^\\d{7}$")) {
            throw new IllegalArgumentException("Invalid IMO number format");
        }
    }

    private void validateTimeRange(LocalDateTime fromTime, LocalDateTime toTime) {
        if (fromTime == null || toTime == null) {
            throw new IllegalArgumentException("Time range cannot be null");
        }
        if (fromTime.isAfter(toTime)) {
            throw new IllegalArgumentException("Invalid time range");
        }
        if (Duration.between(fromTime, toTime).toDays() > 30) {
            throw new IllegalArgumentException("Time range cannot exceed 30 days");
        }
    }
}