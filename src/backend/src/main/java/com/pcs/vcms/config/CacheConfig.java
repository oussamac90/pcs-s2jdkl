package com.pcs.vcms.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache configuration for the Vessel Call Management System.
 * Implements distributed caching with specific TTL values for different data types
 * to optimize performance and maintain data freshness.
 * 
 * @version 1.0
 * @since 2023-11-15
 */
@Configuration
@EnableCaching
public class CacheConfig {

    // Cache names as constants to prevent typos and enable reuse
    public static final String VESSEL_CALLS_CACHE = "vesselCalls";
    public static final String BERTH_ALLOCATIONS_CACHE = "berthAllocations";
    public static final String SERVICE_BOOKINGS_CACHE = "serviceBookings";
    public static final String CLEARANCE_STATUS_CACHE = "clearanceStatus";

    /**
     * Configures the Redis cache manager with specific cache settings for different data types.
     * Implements optimized TTL values based on data update frequency and freshness requirements.
     *
     * @param connectionFactory Redis connection factory for establishing Redis connections
     * @return Configured Redis cache manager
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .prefixCacheNameWith("vcms::")  // Prefix for all cache keys
            .entryTtl(Duration.ofMinutes(5)) // Default TTL
            .disableCachingNullValues()      // Prevent caching null values
            .computePrefixWith(cacheName -> "vcms::" + cacheName + "::"); // Custom key prefix

        // Specific cache configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Vessel calls cache - 5 minute TTL for optimal balance
        cacheConfigurations.put(VESSEL_CALLS_CACHE, 
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .prefixCacheNameWith("vcms::" + VESSEL_CALLS_CACHE + "::")
        );

        // Berth allocations cache - 2 minute TTL for frequent updates
        cacheConfigurations.put(BERTH_ALLOCATIONS_CACHE,
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(2))
                .prefixCacheNameWith("vcms::" + BERTH_ALLOCATIONS_CACHE + "::")
        );

        // Service bookings cache - 5 minute TTL for moderate update frequency
        cacheConfigurations.put(SERVICE_BOOKINGS_CACHE,
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .prefixCacheNameWith("vcms::" + SERVICE_BOOKINGS_CACHE + "::")
        );

        // Clearance status cache - 1 minute TTL for high data freshness
        cacheConfigurations.put(CLEARANCE_STATUS_CACHE,
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(1))
                .prefixCacheNameWith("vcms::" + CLEARANCE_STATUS_CACHE + "::")
        );

        // Build and configure the Redis cache manager
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .enableStatistics() // Enable statistics for monitoring
            .build();
    }
}