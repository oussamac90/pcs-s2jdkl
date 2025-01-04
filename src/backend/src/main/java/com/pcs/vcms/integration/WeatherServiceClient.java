package com.pcs.vcms.integration;

import com.fasterxml.jackson.databind.ObjectMapper; // v2.15.0
import lombok.extern.slf4j.Slf4j; // v1.18.22
import org.springframework.beans.factory.annotation.Value; // v6.1.0
import org.springframework.cache.annotation.Cacheable; // v6.1.0
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker; // v4.0.0
import org.springframework.http.HttpEntity; // v6.1.0
import org.springframework.http.HttpHeaders; // v6.1.0
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity; // v6.1.0
import org.springframework.stereotype.Service; // v6.1.0
import org.springframework.web.client.RestTemplate; // v6.1.0

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class WeatherServiceClient {

    @Value("${openweather.api.key}")
    private String apiKey;

    @Value("${openweather.api.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CircuitBreaker circuitBreaker;
    private final int maxRetries = 3;
    private final long retryDelay = 1000L; // 1 second
    private final HttpHeaders defaultHeaders;

    public WeatherServiceClient(RestTemplate restTemplate, 
                              ObjectMapper objectMapper,
                              CircuitBreaker circuitBreaker) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreaker;
        
        // Initialize default headers
        this.defaultHeaders = new HttpHeaders();
        this.defaultHeaders.set("User-Agent", "VCMS-Weather-Client/1.0");
        this.defaultHeaders.set("Accept", "application/json");
        this.defaultHeaders.set("Content-Type", "application/json");
    }

    @Cacheable(value = "weatherCache", key = "#latitude + #longitude")
    public ResponseEntity<Map<String, Object>> getMarineWeather(double latitude, double longitude) {
        log.debug("Fetching marine weather data for coordinates: lat={}, lon={}", latitude, longitude);
        
        // Validate coordinates
        validateCoordinates(latitude, longitude);

        String url = String.format("%s/marine?lat=%f&lon=%f&appid=%s", baseUrl, latitude, longitude, apiKey);
        
        return circuitBreaker.run(() -> {
            return executeWithRetry(() -> {
                HttpEntity<?> requestEntity = new HttpEntity<>(defaultHeaders);
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    requestEntity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
                );
                
                validateResponse(response);
                return response;
            });
        }, throwable -> getFallbackWeatherData());
    }

    @Cacheable(value = "forecastCache", key = "#latitude + #longitude + #days")
    public ResponseEntity<Map<String, Object>> getWeatherForecast(double latitude, double longitude, int days) {
        log.debug("Fetching weather forecast for coordinates: lat={}, lon={}, days={}", latitude, longitude, days);
        
        // Validate parameters
        validateCoordinates(latitude, longitude);
        if (days < 1 || days > 7) {
            throw new IllegalArgumentException("Forecast days must be between 1 and 7");
        }

        String url = String.format("%s/forecast?lat=%f&lon=%f&days=%d&appid=%s", 
            baseUrl, latitude, longitude, days, apiKey);

        return circuitBreaker.run(() -> {
            return executeWithRetry(() -> {
                HttpEntity<?> requestEntity = new HttpEntity<>(defaultHeaders);
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    requestEntity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
                );
                
                validateResponse(response);
                return response;
            });
        }, throwable -> getFallbackForecastData(days));
    }

    @Cacheable(value = "marineCache", key = "#latitude + #longitude")
    public ResponseEntity<Map<String, Object>> getMarineConditions(double latitude, double longitude) {
        log.debug("Fetching marine conditions for coordinates: lat={}, lon={}", latitude, longitude);
        
        // Validate coordinates
        validateCoordinates(latitude, longitude);

        String url = String.format("%s/marine/conditions?lat=%f&lon=%f&appid=%s", 
            baseUrl, latitude, longitude, apiKey);

        return circuitBreaker.run(() -> {
            return executeWithRetry(() -> {
                HttpEntity<?> requestEntity = new HttpEntity<>(defaultHeaders);
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    requestEntity,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
                );
                
                validateResponse(response);
                return response;
            });
        }, throwable -> getFallbackMarineConditions());
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees");
        }
    }

    private void validateResponse(ResponseEntity<Map<String, Object>> response) {
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            log.error("Invalid response received from weather service: {}", response.getStatusCode());
            throw new RuntimeException("Invalid response from weather service");
        }
    }

    private <T> T executeWithRetry(java.util.function.Supplier<T> operation) {
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                return operation.get();
            } catch (Exception e) {
                attempts++;
                if (attempts == maxRetries) {
                    log.error("Max retry attempts reached", e);
                    throw e;
                }
                log.warn("Retry attempt {} of {} failed", attempts, maxRetries, e);
                try {
                    TimeUnit.MILLISECONDS.sleep(retryDelay * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
        throw new RuntimeException("Unexpected retry failure");
    }

    private ResponseEntity<Map<String, Object>> getFallbackWeatherData() {
        log.warn("Returning fallback weather data");
        Map<String, Object> fallbackData = new HashMap<>();
        fallbackData.put("status", "fallback");
        fallbackData.put("temperature", 20.0);
        fallbackData.put("wind_speed", 5.0);
        fallbackData.put("humidity", 70);
        return new ResponseEntity<>(fallbackData, HttpStatus.OK);
    }

    private ResponseEntity<Map<String, Object>> getFallbackForecastData(int days) {
        log.warn("Returning fallback forecast data for {} days", days);
        Map<String, Object> fallbackData = new HashMap<>();
        fallbackData.put("status", "fallback");
        fallbackData.put("forecast_days", days);
        fallbackData.put("forecasts", Collections.emptyList());
        return new ResponseEntity<>(fallbackData, HttpStatus.OK);
    }

    private ResponseEntity<Map<String, Object>> getFallbackMarineConditions() {
        log.warn("Returning fallback marine conditions");
        Map<String, Object> fallbackData = new HashMap<>();
        fallbackData.put("status", "fallback");
        fallbackData.put("wave_height", 1.0);
        fallbackData.put("sea_temperature", 15.0);
        fallbackData.put("visibility", "good");
        return new ResponseEntity<>(fallbackData, HttpStatus.OK);
    }
}