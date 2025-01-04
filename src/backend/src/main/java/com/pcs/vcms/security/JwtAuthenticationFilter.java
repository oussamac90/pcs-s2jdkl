package com.pcs.vcms.security;

import org.slf4j.Logger; // slf4j 1.7.x
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // spring-security 6.1.x
import org.springframework.security.core.context.SecurityContextHolder; // spring-security 6.1.x
import org.springframework.stereotype.Component; // spring-boot 6.1.x
import org.springframework.web.filter.OncePerRequestFilter; // spring-web 6.1.x
import org.springframework.util.StringUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * Thread-safe filter component that processes and validates JWT tokens for each HTTP request.
 * Implements comprehensive security logging and error handling for the Vessel Call Management System.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    private final JwtTokenProvider tokenProvider;

    /**
     * Creates a new JWT authentication filter with the specified token provider.
     *
     * @param tokenProvider the JWT token validation provider
     * @throws IllegalArgumentException if tokenProvider is null
     */
    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        if (tokenProvider == null) {
            throw new IllegalArgumentException("Token provider cannot be null");
        }
        this.tokenProvider = tokenProvider;
        logger.info("Initialized JwtAuthenticationFilter with token provider");
    }

    /**
     * Processes each HTTP request for JWT authentication with comprehensive error handling.
     * Thread-safe implementation ensures correct security context management.
     *
     * @param request incoming HTTP request
     * @param response HTTP response
     * @param filterChain filter processing chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            logger.debug("Processing request: {} {}", request.getMethod(), request.getRequestURI());

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String userId = tokenProvider.getUserIdFromToken(jwt);
                
                if (StringUtils.hasText(userId)) {
                    // Create authentication token with minimal necessary information
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                    
                    // Set authentication in thread-local security context
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    logger.debug("Set authentication in security context for user: {}", userId);
                } else {
                    logger.warn("Valid JWT token but invalid user ID");
                }
            } else if (StringUtils.hasText(jwt)) {
                logger.warn("Invalid JWT token detected");
            }

            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            logger.error("Security filter error: {}", ex.getMessage(), ex);
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
        } finally {
            // Ensure security context is cleared after request processing
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityContextHolder.clearContext();
            }
        }
    }

    /**
     * Securely extracts JWT token from request Authorization header.
     * Implements null safety and validation checks.
     *
     * @param request HTTP request containing Authorization header
     * @return JWT token string or null if not found/invalid
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            String token = bearerToken.substring(BEARER_PREFIX.length());
            if (StringUtils.hasText(token)) {
                logger.trace("Extracted JWT token from request");
                return token;
            }
        }
        
        logger.trace("No JWT token found in request");
        return null;
    }
}