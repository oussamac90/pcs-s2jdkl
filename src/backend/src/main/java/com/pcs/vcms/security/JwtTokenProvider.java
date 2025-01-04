package com.pcs.vcms.security;

import io.jsonwebtoken.Claims; // jsonwebtoken 0.11.5
import io.jsonwebtoken.JwtException; // jsonwebtoken 0.11.5
import io.jsonwebtoken.Jwts; // jsonwebtoken 0.11.5
import io.jsonwebtoken.SignatureAlgorithm; // jsonwebtoken 0.11.5
import org.slf4j.Logger; // slf4j 2.0.x
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value; // spring-boot 6.1.x
import org.springframework.stereotype.Component; // spring-boot 6.1.x
import org.springframework.util.StringUtils;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides secure JWT token generation and validation functionality for the Vessel Call Management System.
 * Implements enterprise-grade security features including HS512 signature algorithm, token expiration,
 * and comprehensive validation checks.
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS512;
    
    private final String jwtSecret;
    private final long jwtExpirationInMs;
    private final Key signingKey;

    /**
     * Initializes the JWT token provider with configuration properties.
     * Creates a secure signing key using HS512 algorithm.
     *
     * @param jwtSecret JWT secret key injected from application properties
     * @param jwtExpirationInMs JWT token expiration time in milliseconds
     */
    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.expiration}") long jwtExpirationInMs) {
        
        if (!StringUtils.hasText(jwtSecret)) {
            throw new IllegalArgumentException("JWT secret cannot be empty");
        }
        if (jwtExpirationInMs <= 0) {
            throw new IllegalArgumentException("JWT expiration must be positive");
        }

        this.jwtSecret = jwtSecret;
        this.jwtExpirationInMs = jwtExpirationInMs;
        this.signingKey = new SecretKeySpec(
            jwtSecret.getBytes(StandardCharsets.UTF_8),
            SIGNATURE_ALGORITHM.getJcaName()
        );
        
        logger.info("Initialized JwtTokenProvider with HS512 algorithm");
    }

    /**
     * Generates a secure JWT token for the authenticated user.
     *
     * @param userPrincipal authenticated user details
     * @return JWT token string
     * @throws IllegalArgumentException if userPrincipal is null
     */
    public String generateToken(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new IllegalArgumentException("UserPrincipal cannot be null");
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userPrincipal.getId());
        claims.put("authorities", userPrincipal.getAuthorities());

        try {
            String token = Jwts.builder()
                    .setClaims(claims)
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(signingKey, SIGNATURE_ALGORITHM)
                    .compact();

            logger.debug("Generated JWT token for user: {}", userPrincipal.getId());
            return token;
        } catch (Exception ex) {
            logger.error("Error generating JWT token", ex);
            throw new JwtException("Could not generate token", ex);
        }
    }

    /**
     * Extracts the user ID from a JWT token after validation.
     *
     * @param token JWT token string
     * @return user ID from token claims
     * @throws JwtException if token is invalid or expired
     */
    public String getUserIdFromToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Token string cannot be empty");
        }

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String userId = claims.get("userId", String.class);
            if (!StringUtils.hasText(userId)) {
                throw new JwtException("Invalid user ID in token");
            }

            logger.debug("Extracted user ID from token: {}", userId);
            return userId;
        } catch (JwtException ex) {
            logger.error("Error parsing JWT token", ex);
            throw new JwtException("Invalid JWT token", ex);
        }
    }

    /**
     * Validates a JWT token for authenticity and expiration.
     *
     * @param token JWT token string
     * @return true if token is valid and not expired
     */
    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            logger.warn("Empty token provided for validation");
            return false;
        }

        try {
            Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token);

            logger.debug("Successfully validated JWT token");
            return true;
        } catch (JwtException ex) {
            logger.warn("JWT token validation failed: {}", ex.getMessage());
            return false;
        }
    }
}