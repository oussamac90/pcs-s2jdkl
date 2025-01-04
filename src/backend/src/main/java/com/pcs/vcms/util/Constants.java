package com.pcs.vcms.util;

/**
 * Centralized constants class for the Vessel Call Management System (VCMS).
 * Contains system-wide configuration values, API settings, security constants,
 * business status enums, and WebSocket topic definitions.
 * 
 * This class is final and cannot be instantiated as it only provides static constants.
 * 
 * @version 1.0
 * @since 2023-11-15
 */
public final class Constants {

    // API Configuration Constants
    /** API version identifier */
    public static final String API_VERSION = "v1";
    
    /** Base path for all API endpoints */
    public static final String API_BASE_PATH = "/api/v1";
    
    // Pagination Constants
    /** Default number of items per page for paginated responses */
    public static final int DEFAULT_PAGE_SIZE = 20;
    
    /** Maximum allowed items per page for paginated responses */
    public static final int MAX_PAGE_SIZE = 100;
    
    // Security Constants
    /** JWT token validity duration in milliseconds (24 hours) */
    public static final long JWT_TOKEN_VALIDITY = 86400000L;
    
    /** Bearer token prefix for Authorization header */
    public static final String BEARER_PREFIX = "Bearer ";
    
    /** Authorization header name */
    public static final String AUTH_HEADER = "Authorization";
    
    // Date Format Constants
    /** Standard date format pattern */
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    
    /** Standard date-time format pattern with timezone */
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    
    // Vessel Status Constants
    /** Status for planned vessel calls */
    public static final String VESSEL_STATUS_PLANNED = "PLANNED";
    
    /** Status for vessels that have arrived */
    public static final String VESSEL_STATUS_ARRIVED = "ARRIVED";
    
    /** Status for vessels currently at berth */
    public static final String VESSEL_STATUS_AT_BERTH = "AT_BERTH";
    
    /** Status for vessels that have departed */
    public static final String VESSEL_STATUS_DEPARTED = "DEPARTED";
    
    /** Status for cancelled vessel calls */
    public static final String VESSEL_STATUS_CANCELLED = "CANCELLED";
    
    // Service Status Constants
    /** Status for requested services */
    public static final String SERVICE_STATUS_REQUESTED = "REQUESTED";
    
    /** Status for confirmed service bookings */
    public static final String SERVICE_STATUS_CONFIRMED = "CONFIRMED";
    
    /** Status for services currently in progress */
    public static final String SERVICE_STATUS_IN_PROGRESS = "IN_PROGRESS";
    
    /** Status for completed services */
    public static final String SERVICE_STATUS_COMPLETED = "COMPLETED";
    
    /** Status for cancelled services */
    public static final String SERVICE_STATUS_CANCELLED = "CANCELLED";
    
    // Cache Configuration
    /** Default cache time-to-live in seconds */
    public static final int CACHE_TTL = 3600;
    
    // WebSocket Configuration
    /** WebSocket endpoint base path */
    public static final String WEBSOCKET_ENDPOINT = "/ws";
    
    /** Topic for real-time vessel updates */
    public static final String WEBSOCKET_TOPIC_VESSEL_UPDATES = "/topic/vessel-updates";
    
    /** Topic for real-time berth updates */
    public static final String WEBSOCKET_TOPIC_BERTH_UPDATES = "/topic/berth-updates";
    
    /** Topic for real-time service updates */
    public static final String WEBSOCKET_TOPIC_SERVICE_UPDATES = "/topic/service-updates";
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     * 
     * @throws IllegalStateException if instantiation is attempted
     */
    private Constants() {
        throw new IllegalStateException("Utility class - cannot be instantiated");
    }
}