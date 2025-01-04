package com.pcs.vcms.util;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Maritime-focused utility class providing comprehensive date and time manipulation functions
 * for vessel scheduling, berth management, and international time zone handling.
 * 
 * This class is thread-safe and uses cached formatters for optimal performance.
 * 
 * @version 1.0
 * @since 2023-11-15
 */
public final class DateTimeUtils {

    // Thread-safe cached formatters for performance optimization
    private static final DateTimeFormatter CACHED_DATE_TIME_FORMATTER = 
        DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT);
    
    private static final DateTimeFormatter CACHED_DATE_FORMATTER = 
        DateTimeFormatter.ofPattern(Constants.DATE_FORMAT);
    
    private static final DateTimeFormatter CACHED_NAUTICAL_FORMATTER = 
        DateTimeFormatter.ofPattern("HHmm'Z'");

    /**
     * Private constructor to prevent instantiation of utility class.
     *
     * @throws IllegalStateException if instantiation is attempted
     */
    private DateTimeUtils() {
        throw new IllegalStateException("Utility class cannot be instantiated");
    }

    /**
     * Formats LocalDateTime to string using system default format with thread-safe formatter.
     *
     * @param dateTime the LocalDateTime to format
     * @return formatted date time string
     * @throws IllegalArgumentException if dateTime is null
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            throw new IllegalArgumentException("DateTime cannot be null");
        }
        try {
            return CACHED_DATE_TIME_FORMATTER.format(dateTime);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Error formatting datetime: " + e.getMessage(), e);
        }
    }

    /**
     * Parses date time string to LocalDateTime with validation.
     *
     * @param dateTimeStr the date time string to parse
     * @return parsed LocalDateTime object
     * @throws IllegalArgumentException if dateTimeStr is invalid or null
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("DateTime string cannot be null or empty");
        }
        try {
            return LocalDateTime.parse(dateTimeStr, CACHED_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid datetime format: " + e.getMessage(), e);
        }
    }

    /**
     * Converts UTC time to vessel's local time based on position.
     * Handles international date line crossing and maritime time conventions.
     *
     * @param utcDateTime the UTC datetime to convert
     * @param vesselTimeZone the vessel's current timezone
     * @return time in vessel's local zone
     * @throws IllegalArgumentException if any parameter is null
     */
    public static ZonedDateTime convertToVesselLocalTime(ZonedDateTime utcDateTime, ZoneId vesselTimeZone) {
        if (utcDateTime == null || vesselTimeZone == null) {
            throw new IllegalArgumentException("DateTime and timezone cannot be null");
        }
        
        try {
            // Convert from UTC to vessel's local time
            ZonedDateTime vesselLocalTime = utcDateTime.withZoneSameInstant(vesselTimeZone);
            
            // Handle international date line crossing
            if (vesselLocalTime.getOffset().getTotalSeconds() > 43200 || 
                vesselLocalTime.getOffset().getTotalSeconds() < -43200) {
                vesselLocalTime = vesselLocalTime.minusDays(1);
            }
            
            return vesselLocalTime;
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Error converting timezone: " + e.getMessage(), e);
        }
    }

    /**
     * Calculates available berth window considering tidal constraints.
     * Optimizes berthing time based on required duration and operational constraints.
     *
     * @param startTime the start of the potential berth window
     * @param endTime the end of the potential berth window
     * @param requiredDuration the required berthing duration
     * @return Optional containing optimal berth time if available
     * @throws IllegalArgumentException if any parameter is null or invalid
     */
    public static Optional<LocalDateTime> calculateBerthWindow(
            LocalDateTime startTime, 
            LocalDateTime endTime, 
            Duration requiredDuration) {
        
        if (startTime == null || endTime == null || requiredDuration == null) {
            throw new IllegalArgumentException("All parameters must be non-null");
        }
        
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("End time cannot be before start time");
        }
        
        if (requiredDuration.isNegative() || requiredDuration.isZero()) {
            throw new IllegalArgumentException("Required duration must be positive");
        }
        
        try {
            Duration availableWindow = Duration.between(startTime, endTime);
            
            // Check if the available window is sufficient
            if (availableWindow.compareTo(requiredDuration) < 0) {
                return Optional.empty();
            }
            
            // Calculate optimal berthing time (centered in available window)
            LocalDateTime optimalTime = startTime.plus(
                availableWindow.minus(requiredDuration).dividedBy(2)
            );
            
            return Optional.of(optimalTime);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Error calculating berth window: " + e.getMessage(), e);
        }
    }
}