package com.pcs.vcms;

import com.pcs.vcms.config.AsyncConfig;
import com.pcs.vcms.config.SecurityConfig;
import com.pcs.vcms.config.WebSocketConfig;
import com.pcs.vcms.security.JwtAuthenticationFilter;
import com.pcs.vcms.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprehensive integration test suite for the Vessel Call Management System.
 * Verifies proper initialization and integration of all core components.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = VesselCallManagementApplication.class
)
@ActiveProfiles("test")
public class VesselCallManagementApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WebTestClient webTestClient;

    private MockMvc mockMvc;

    /**
     * Verifies that the Spring application context loads successfully
     * with all required components.
     */
    @Test
    public void contextLoads() {
        assertThat(applicationContext).isNotNull();
        assertThat(applicationContext.getEnvironment().getActiveProfiles())
            .contains("test");
        
        // Verify core beans are present
        assertThat(applicationContext.getBean(DataSource.class)).isNotNull();
        assertThat(applicationContext.getBean(SecurityConfig.class)).isNotNull();
        assertThat(applicationContext.getBean(AsyncConfig.class)).isNotNull();
        assertThat(applicationContext.getBean(WebSocketConfig.class)).isNotNull();
    }

    /**
     * Validates that all required Spring beans are properly configured
     * and initialized.
     */
    @Test
    public void verifyRequiredBeansExist() {
        // Security components
        assertThat(applicationContext.getBean(JwtTokenProvider.class)).isNotNull();
        assertThat(applicationContext.getBean(JwtAuthenticationFilter.class)).isNotNull();

        // Async configuration
        AsyncConfig asyncConfig = applicationContext.getBean(AsyncConfig.class);
        assertThat(asyncConfig.getAsyncExecutor()).isNotNull();
        assertThat(asyncConfig.getAsyncUncaughtExceptionHandler()).isNotNull();

        // WebSocket configuration
        WebSocketConfig wsConfig = applicationContext.getBean(WebSocketConfig.class);
        assertThat(wsConfig).isNotNull();
    }

    /**
     * Tests the security configuration including OAuth2 and JWT setup.
     */
    @Test
    public void verifySecurityConfiguration() throws Exception {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .build();

        // Test secured endpoint access
        mockMvc.perform(get("/api/v1/vessel-calls"))
            .andExpect(status().isUnauthorized());

        // Test public endpoint access
        mockMvc.perform(get("/api/v1/public/health"))
            .andExpect(status().isOk());

        // Verify security beans
        SecurityConfig securityConfig = applicationContext.getBean(SecurityConfig.class);
        assertThat(securityConfig).isNotNull();
        assertThat(applicationContext.getBean("securityFilterChain")).isNotNull();
    }

    /**
     * Tests the WebSocket configuration and STOMP messaging.
     */
    @Test
    public void verifyWebSocketConfiguration() throws ExecutionException, InterruptedException, TimeoutException {
        WebSocketStompClient stompClient = new WebSocketStompClient(
            new SockJsClient(
                Collections.singletonList(
                    new WebSocketTransport(
                        new StandardWebSocketClient()
                    )
                )
            )
        );

        StompSession session = stompClient
            .connect("ws://localhost:" + 
                webApplicationContext
                    .getEnvironment()
                    .getProperty("local.server.port") + "/ws", 
                new StompSessionHandlerAdapter() {})
            .get(5, TimeUnit.SECONDS);

        assertThat(session.isConnected()).isTrue();

        // Test subscription to vessel updates topic
        session.subscribe("/topic/vessel-updates", new StompSessionHandlerAdapter() {});
        assertThat(session.getSubscriptionCount()).isEqualTo(1);

        session.disconnect();
    }

    /**
     * Verifies the async execution configuration.
     */
    @Test
    public void verifyAsyncConfiguration() {
        AsyncConfig asyncConfig = applicationContext.getBean(AsyncConfig.class);
        assertThat(asyncConfig.getAsyncExecutor()).isNotNull();
        
        // Verify thread pool configuration
        assertThat(asyncConfig.getAsyncExecutor())
            .hasFieldOrPropertyWithValue("corePoolSize", 4)
            .hasFieldOrPropertyWithValue("maxPoolSize", 8)
            .hasFieldOrPropertyWithValue("queueCapacity", 100);
    }

    /**
     * Tests REST API endpoints using WebTestClient.
     */
    @Test
    public void verifyRestApiEndpoints() {
        webTestClient.get()
            .uri("/api/v1/public/health")
            .exchange()
            .expectStatus().isOk();

        webTestClient.get()
            .uri("/api/v1/vessel-calls")
            .exchange()
            .expectStatus().isUnauthorized();
    }

    /**
     * Verifies database connection and configuration.
     */
    @Test
    public void verifyDatabaseConfiguration() {
        DataSource dataSource = applicationContext.getBean(DataSource.class);
        assertThat(dataSource).isNotNull();
        
        // Verify connection pool settings
        assertThat(dataSource)
            .hasFieldOrPropertyWithValue("maxPoolSize", 10)
            .hasFieldOrPropertyWithValue("connectionTimeout", 30000L);
    }
}