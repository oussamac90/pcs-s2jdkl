package com.pcs.vcms;

import org.springframework.boot.SpringApplication; // Spring Boot 3.1.x
import org.springframework.boot.autoconfigure.SpringBootApplication; // Spring Boot 3.1.x
import org.springframework.scheduling.annotation.EnableScheduling; // Spring Framework 6.1.x
import org.springframework.scheduling.annotation.EnableAsync; // Spring Framework 6.1.x
import org.slf4j.Logger; // SLF4J 2.0.x
import org.slf4j.LoggerFactory; // SLF4J 2.0.x

import javax.annotation.PreDestroy;
import java.util.TimeZone;

/**
 * Main application class for the Vessel Call Management System.
 * Bootstraps the Spring Boot application with comprehensive configuration
 * for security, WebSocket communication, and async execution capabilities.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@SpringBootApplication(
    scanBasePackages = "com.pcs.vcms",
    proxyBeanMethods = false
)
@EnableScheduling
@EnableAsync
public class VesselCallManagementApplication {

    private static final Logger logger = LoggerFactory.getLogger(VesselCallManagementApplication.class);
    private static final String APPLICATION_NAME = "Vessel Call Management System";
    private static final String APPLICATION_VERSION = "1.0.0";

    /**
     * Application entry point that bootstraps the Spring Boot application
     * with comprehensive configuration and monitoring.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            // Set system timezone to UTC
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

            // Configure system properties
            System.setProperty("spring.application.name", APPLICATION_NAME);
            System.setProperty("spring.output.ansi.enabled", "ALWAYS");

            // Start performance monitoring
            long startTime = System.currentTimeMillis();

            // Bootstrap Spring Boot application
            SpringApplication app = new SpringApplication(VesselCallManagementApplication.class);
            
            // Add startup logging
            logger.info("Starting {} version {}", APPLICATION_NAME, APPLICATION_VERSION);
            logger.info("Java version: {}", System.getProperty("java.version"));
            logger.info("Operating System: {} ({})", 
                System.getProperty("os.name"), 
                System.getProperty("os.arch")
            );

            // Run the application
            app.run(args);

            // Log startup completion time
            long totalTime = System.currentTimeMillis() - startTime;
            logger.info("{} started successfully in {} seconds", 
                APPLICATION_NAME, 
                String.format("%.2f", totalTime / 1000.0)
            );

        } catch (Exception e) {
            logger.error("Application startup failed", e);
            System.exit(1);
        }
    }

    /**
     * Performs cleanup operations before application shutdown.
     * Ensures graceful termination of resources and connections.
     */
    @PreDestroy
    public void onShutdown() {
        try {
            logger.info("Initiating {} shutdown sequence", APPLICATION_NAME);
            
            // Allow time for in-flight requests to complete
            Thread.sleep(2000);
            
            logger.info("{} shutdown completed successfully", APPLICATION_NAME);
        } catch (InterruptedException e) {
            logger.warn("Shutdown sequence interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Error during application shutdown", e);
        }
    }
}