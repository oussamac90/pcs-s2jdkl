package com.pcs.vcms.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition; // v2.2.0
import io.swagger.v3.oas.annotations.info.Info; // v2.2.0
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme; // v2.2.0
import io.swagger.v3.oas.annotations.security.SecurityRequirement; // v2.2.0
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration; // v6.1.0
import org.springframework.context.annotation.Bean; // v6.1.0
import org.springdoc.core.GroupedOpenApi; // v2.2.0

/**
 * OpenAPI configuration class providing comprehensive documentation for the 
 * Vessel Call Management System REST APIs.
 * 
 * Configures API groups for:
 * - Vessel call management
 * - Berth management
 * - Service booking
 * - Clearance workflows
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Vessel Call Management System API",
        version = "1.0",
        description = "REST API for managing vessel calls, berth allocations, and port services",
        contact = @Contact(
            name = "Port Authority",
            email = "support@portauthority.com"
        ),
        license = @License(
            name = "Proprietary"
        )
    )
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT token authentication with Bearer scheme"
)
public class OpenApiConfig {

    /**
     * Configures OpenAPI documentation group for vessel call management endpoints.
     * Includes pre-arrival notifications, vessel tracking, and call status updates.
     *
     * @return Configured API group for vessel calls
     */
    @Bean
    public GroupedOpenApi vesselCallsApi() {
        return GroupedOpenApi.builder()
            .group("vessel-calls")
            .pathsToMatch("/api/v1/vessel-calls/**")
            .addOpenApiCustomiser(openApi -> {
                openApi.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
                // Add operation descriptions for vessel call endpoints
                openApi.getPaths().forEach((path, item) -> {
                    if (item.getPost() != null) {
                        item.getPost().description("Create a new vessel call with pre-arrival details");
                    }
                    if (item.getGet() != null) {
                        item.getGet().description("Retrieve vessel call information");
                    }
                    if (item.getPut() != null) {
                        item.getPut().description("Update vessel call status or details");
                    }
                });
            })
            .build();
    }

    /**
     * Configures OpenAPI documentation group for berth management endpoints.
     * Includes allocation, scheduling, and conflict resolution.
     *
     * @return Configured API group for berth management
     */
    @Bean
    public GroupedOpenApi berthManagementApi() {
        return GroupedOpenApi.builder()
            .group("berth-management")
            .pathsToMatch("/api/v1/berths/**")
            .addOpenApiCustomiser(openApi -> {
                openApi.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
                // Add operation descriptions for berth management endpoints
                openApi.getPaths().forEach((path, item) -> {
                    if (item.getPost() != null) {
                        item.getPost().description("Create new berth allocation");
                    }
                    if (item.getGet() != null) {
                        item.getGet().description("Retrieve berth schedule and availability");
                    }
                    if (item.getPut() != null) {
                        item.getPut().description("Update berth allocation or resolve conflicts");
                    }
                });
            })
            .build();
    }

    /**
     * Configures OpenAPI documentation group for service booking endpoints.
     * Includes tugboat, pilotage, and mooring services.
     *
     * @return Configured API group for service bookings
     */
    @Bean
    public GroupedOpenApi serviceBookingApi() {
        return GroupedOpenApi.builder()
            .group("service-booking")
            .pathsToMatch("/api/v1/services/**")
            .addOpenApiCustomiser(openApi -> {
                openApi.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
                // Add operation descriptions and examples for service booking endpoints
                openApi.getPaths().forEach((path, item) -> {
                    if (item.getPost() != null) {
                        item.getPost().description("Book new port service")
                            .addExample("Tugboat Service", createTugboatServiceExample())
                            .addExample("Pilotage Service", createPilotageServiceExample());
                    }
                    if (item.getGet() != null) {
                        item.getGet().description("Retrieve service booking details");
                    }
                    if (item.getPut() != null) {
                        item.getPut().description("Update service booking status");
                    }
                });
            })
            .build();
    }

    /**
     * Configures OpenAPI documentation group for clearance endpoints.
     * Includes customs, immigration, and port authority clearances.
     *
     * @return Configured API group for clearances
     */
    @Bean
    public GroupedOpenApi clearanceApi() {
        return GroupedOpenApi.builder()
            .group("clearance")
            .pathsToMatch("/api/v1/clearance/**")
            .addOpenApiCustomiser(openApi -> {
                openApi.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
                // Add operation descriptions and workflow documentation
                openApi.getPaths().forEach((path, item) -> {
                    if (item.getPost() != null) {
                        item.getPost().description("Submit clearance request")
                            .addExample("Customs Clearance", createCustomsClearanceExample())
                            .addExample("Immigration Clearance", createImmigrationClearanceExample());
                    }
                    if (item.getGet() != null) {
                        item.getGet().description("Check clearance status");
                    }
                    if (item.getPut() != null) {
                        item.getPut().description("Update clearance status or provide additional information");
                    }
                });
            })
            .build();
    }

    // Helper methods to create example objects for API documentation
    private Example createTugboatServiceExample() {
        return new Example()
            .value(Map.of(
                "vesselCallId", "VC-2023-001",
                "serviceType", "TUGBOAT",
                "quantity", 2,
                "requestedDateTime", "2023-11-15T08:00:00Z"
            ));
    }

    private Example createPilotageServiceExample() {
        return new Example()
            .value(Map.of(
                "vesselCallId", "VC-2023-001",
                "serviceType", "PILOTAGE",
                "requestedDateTime", "2023-11-15T08:00:00Z",
                "pilotageZone", "ZONE_A"
            ));
    }

    private Example createCustomsClearanceExample() {
        return new Example()
            .value(Map.of(
                "vesselCallId", "VC-2023-001",
                "clearanceType", "CUSTOMS",
                "cargoManifest", "manifest-data",
                "declarationType", "IMPORT"
            ));
    }

    private Example createImmigrationClearanceExample() {
        return new Example()
            .value(Map.of(
                "vesselCallId", "VC-2023-001",
                "clearanceType", "IMMIGRATION",
                "crewList", "crew-data",
                "passengerList", "passenger-data"
            ));
    }
}