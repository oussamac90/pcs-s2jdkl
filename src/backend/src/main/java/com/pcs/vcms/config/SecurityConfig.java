package com.pcs.vcms.config;

import com.pcs.vcms.security.JwtAuthenticationFilter;
import com.pcs.vcms.security.JwtTokenProvider;
import io.github.bucket4j.Bandwidth; // bucket4j 8.1.0
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

/**
 * Enhanced security configuration for the Vessel Call Management System.
 * Implements comprehensive security measures including JWT authentication,
 * role-based access control, rate limiting, and secure headers.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtTokenProvider jwtTokenProvider;
    private final Bucket4j.Builder rateLimiter;

    /**
     * Creates a new SecurityConfig with required security components.
     *
     * @param jwtAuthenticationFilter JWT authentication filter
     * @param jwtTokenProvider JWT token provider
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                         JwtTokenProvider jwtTokenProvider) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtTokenProvider = jwtTokenProvider;
        
        // Configure rate limiting - 100 requests per minute
        Bandwidth limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)));
        this.rateLimiter = Bucket4j.builder().addLimit(limit);
    }

    /**
     * Configures the security filter chain with comprehensive security rules.
     *
     * @param http HttpSecurity configuration object
     * @return Configured SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            // Disable CSRF for stateless API
            .csrf(csrf -> csrf.disable())
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configure session management
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure security headers
            .headers(headers -> headers
                .frameOptions().deny()
                .xssProtection().block(true)
                .contentSecurityPolicy("default-src 'self'")
                .referrerPolicy().sameOrigin()
                .permissionsPolicy().none())
            
            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/public/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                
                // Port Authority endpoints
                .requestMatchers("/api/v1/admin/**").hasRole("PORT_AUTHORITY")
                .requestMatchers(HttpMethod.POST, "/api/v1/berths/**").hasRole("PORT_AUTHORITY")
                .requestMatchers(HttpMethod.PUT, "/api/v1/berths/**").hasRole("PORT_AUTHORITY")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/berths/**").hasRole("PORT_AUTHORITY")
                
                // Vessel Agent endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/vessel-calls/**").hasAnyRole("VESSEL_AGENT", "PORT_AUTHORITY")
                .requestMatchers(HttpMethod.POST, "/api/v1/vessel-calls").hasRole("VESSEL_AGENT")
                .requestMatchers(HttpMethod.PUT, "/api/v1/vessel-calls/*").hasRole("VESSEL_AGENT")
                
                // Service Provider endpoints
                .requestMatchers("/api/v1/services/**").hasAnyRole("SERVICE_PROVIDER", "PORT_AUTHORITY")
                
                // Require authentication for all other endpoints
                .anyRequest().authenticated())
            
            // Add JWT authentication filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Configure exception handling
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.sendError(401, "Unauthorized");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.sendError(403, "Access Denied");
                }))
            
            .build();
    }

    /**
     * Configures password encoder with strong hashing.
     *
     * @return BCrypt password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Configures CORS with strict security policies.
     *
     * @return CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Collections.singletonList("https://vcms.portauthority.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(Arrays.asList(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}