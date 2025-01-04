package com.pcs.vcms.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.pcs.vcms.exception.ValidationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Thread-safe utility class providing comprehensive validation methods for maritime operations data.
 * Implements caching for performance optimization and follows international maritime standards.
 *
 * @version 1.0
 * @since 2023-11-15
 */
public final class ValidationUtils {

    private static final Logger logger = LoggerFactory.getLogger(ValidationUtils.class);

    // Regex patterns for maritime identifiers
    private static final Pattern IMO_NUMBER_PATTERN = Pattern.compile("^IMO\\d{7}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CALL_SIGN_PATTERN = Pattern.compile("^[A-Z]{2,3}[0-9]{3,4}[A-Z]?$", Pattern.CASE_INSENSITIVE);

    // Date formatters with system timezone
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern(Constants.DATE_FORMAT).withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
        DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT).withZone(ZoneId.systemDefault());

    // Cache for IMO number validation results
    private static final Cache<String, Boolean> IMO_VALIDATION_CACHE = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build();

    // Port operation hours (assumed 24/7 operation)
    private static final int MIN_PORT_HOUR = 0;
    private static final int MAX_PORT_HOUR = 23;
    private static final int MIN_BERTH_TIME_HOURS = 2;

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private ValidationUtils() {
        throw new IllegalStateException("Utility class cannot be instantiated");
    }

    /**
     * Validates IMO number format and checksum with caching for performance optimization.
     *
     * @param imoNumber the IMO number to validate
     * @return true if the IMO number is valid
     * @throws ValidationException if the IMO number is invalid
     */
    public static boolean validateImoNumber(String imoNumber) {
        if (StringUtils.isBlank(imoNumber)) {
            throw new ValidationException("IMO number cannot be empty");
        }

        // Check cache first
        Boolean cachedResult = IMO_VALIDATION_CACHE.getIfPresent(imoNumber);
        if (cachedResult != null) {
            return cachedResult;
        }

        // Validate format
        if (!IMO_NUMBER_PATTERN.matcher(imoNumber).matches()) {
            logger.warn("Invalid IMO number format: {}", imoNumber);
            throw new ValidationException("Invalid IMO number format");
        }

        // Extract numeric part and validate checksum
        String numericPart = imoNumber.substring(3);
        int checksum = calculateImoChecksum(numericPart);
        boolean isValid = checksum == Character.getNumericValue(numericPart.charAt(6));

        // Cache the result
        IMO_VALIDATION_CACHE.put(imoNumber, isValid);
        
        if (!isValid) {
            logger.warn("Invalid IMO number checksum: {}", imoNumber);
            throw new ValidationException("Invalid IMO number checksum");
        }

        logger.debug("Valid IMO number: {}", imoNumber);
        return true;
    }

    /**
     * Validates vessel call sign format against international maritime standards.
     *
     * @param callSign the call sign to validate
     * @return true if the call sign is valid
     * @throws ValidationException if the call sign is invalid
     */
    public static boolean validateCallSign(String callSign) {
        if (StringUtils.isBlank(callSign)) {
            throw new ValidationException("Call sign cannot be empty");
        }

        if (!CALL_SIGN_PATTERN.matcher(callSign).matches()) {
            logger.warn("Invalid call sign format: {}", callSign);
            throw new ValidationException("Invalid call sign format");
        }

        logger.debug("Valid call sign: {}", callSign);
        return true;
    }

    /**
     * Validates date-time string format and logic with timezone handling.
     *
     * @param dateTimeStr the date-time string to validate
     * @return parsed LocalDateTime if valid
     * @throws ValidationException if the date-time is invalid
     */
    public static LocalDateTime validateDateTime(String dateTimeStr) {
        if (StringUtils.isBlank(dateTimeStr)) {
            throw new ValidationException("Date-time cannot be empty");
        }

        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
            
            // Validate against current time
            if (dateTime.isBefore(LocalDateTime.now())) {
                throw new ValidationException("Date-time cannot be in the past");
            }

            // Validate port operating hours
            int hour = dateTime.getHour();
            if (hour < MIN_PORT_HOUR || hour > MAX_PORT_HOUR) {
                throw new ValidationException("Time is outside port operating hours");
            }

            logger.debug("Valid date-time: {}", dateTimeStr);
            return dateTime;
        } catch (DateTimeParseException e) {
            logger.warn("Invalid date-time format: {}", dateTimeStr);
            throw new ValidationException("Invalid date-time format");
        }
    }

    /**
     * Validates ETA is before ETD with port-specific rules.
     *
     * @param eta estimated time of arrival
     * @param etd estimated time of departure
     * @throws ValidationException if the validation fails
     */
    public static void validateEtaEtd(LocalDateTime eta, LocalDateTime etd) {
        if (eta == null || etd == null) {
            throw new ValidationException("ETA and ETD cannot be null");
        }

        if (!eta.isBefore(etd)) {
            throw new ValidationException("ETA must be before ETD");
        }

        // Check minimum berth time
        if (eta.plusHours(MIN_BERTH_TIME_HOURS).isAfter(etd)) {
            throw new ValidationException(
                String.format("Minimum berth time is %d hours", MIN_BERTH_TIME_HOURS));
        }

        logger.debug("Valid ETA/ETD pair: {} - {}", eta, etd);
    }

    /**
     * Validates vessel dimensions against berth capacity with safety margins.
     *
     * @param vesselLength vessel length in meters
     * @param vesselDraft vessel draft in meters
     * @param berthLength berth length in meters
     * @param berthDepth berth depth in meters
     * @return true if the vessel fits in the berth
     * @throws ValidationException if the validation fails
     */
    public static boolean validateBerthDimensions(
            double vesselLength, double vesselDraft, double berthLength, double berthDepth) {
        
        if (vesselLength <= 0 || vesselDraft <= 0 || berthLength <= 0 || berthDepth <= 0) {
            throw new ValidationException("Dimensions must be positive values");
        }

        // Apply safety margins
        double safeBerthLength = berthLength - Constants.SAFETY_MARGIN_LENGTH;
        double safeBerthDepth = berthDepth - Constants.SAFETY_MARGIN_DEPTH;

        if (vesselLength > safeBerthLength) {
            logger.warn("Vessel length {} exceeds safe berth length {}", vesselLength, safeBerthLength);
            throw new ValidationException("Vessel length exceeds safe berth length");
        }

        if (vesselDraft > safeBerthDepth) {
            logger.warn("Vessel draft {} exceeds safe berth depth {}", vesselDraft, safeBerthDepth);
            throw new ValidationException("Vessel draft exceeds safe berth depth");
        }

        logger.debug("Valid berth dimensions for vessel: length={}, draft={}", vesselLength, vesselDraft);
        return true;
    }

    /**
     * Calculates the checksum for an IMO number.
     *
     * @param numericPart the numeric part of the IMO number
     * @return calculated checksum
     */
    private static int calculateImoChecksum(String numericPart) {
        int sum = 0;
        for (int i = 0; i < 6; i++) {
            sum += Character.getNumericValue(numericPart.charAt(i)) * (7 - i);
        }
        return sum % 10;
    }
}