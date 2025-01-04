package com.pcs.vcms.integration;

import com.pcs.vcms.entity.PreArrivalNotification;
import com.pcs.vcms.entity.Clearance;
import com.pcs.vcms.entity.Clearance.ClearanceType;
import com.pcs.vcms.entity.Clearance.ClearanceStatus;

import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Retry;
import org.springframework.retry.annotation.Backoff;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor;
import org.springframework.ws.soap.security.wss4j2.support.CryptoFactoryBean;
import org.springframework.cache.CacheManager;
import org.springframework.ws.client.WebServiceIOException;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.xml.transform.Source;
import java.time.LocalDateTime;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

/**
 * Secure client for integrating with external Immigration System via SOAP protocol.
 * Implements retry mechanisms, circuit breaker, monitoring, and comprehensive error handling.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Service
@Slf4j
@Validated
public class ImmigrationSystemClient {

    private final WebServiceTemplate webServiceTemplate;
    private final CircuitBreaker circuitBreaker;
    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;
    private final Timer validationTimer;
    private final Timer clearanceTimer;

    @Value("${immigration.service.url}")
    private String immigrationServiceUrl;

    @Value("${immigration.service.username}")
    private String username;

    @Value("${immigration.service.password}")
    private String password;

    @Value("${immigration.service.keystore.location}")
    private String keystoreLocation;

    @Value("${immigration.service.keystore.password}")
    private String keystorePassword;

    @Value("${immigration.service.timeout.connection:5000}")
    private int connectionTimeout;

    @Value("${immigration.service.timeout.read:10000}")
    private int readTimeout;

    public ImmigrationSystemClient(
            WebServiceTemplate webServiceTemplate,
            CircuitBreakerFactory circuitBreakerFactory,
            CacheManager cacheManager,
            MeterRegistry meterRegistry) {
        this.webServiceTemplate = webServiceTemplate;
        this.circuitBreaker = circuitBreakerFactory.create("immigrationService");
        this.cacheManager = cacheManager;
        this.meterRegistry = meterRegistry;
        this.validationTimer = Timer.builder("immigration.validation.time")
                .description("Time taken for crew validation")
                .register(meterRegistry);
        this.clearanceTimer = Timer.builder("immigration.clearance.time")
                .description("Time taken for clearance processing")
                .register(meterRegistry);
    }

    @PostConstruct
    public void initialize() throws Exception {
        configureSecurityInterceptor();
        configureTimeouts();
        log.info("Immigration System Client initialized with endpoint: {}", immigrationServiceUrl);
    }

    /**
     * Validates crew documents with the immigration system.
     *
     * @param notification PreArrivalNotification containing crew information
     * @return ValidationResult with detailed validation status
     */
    @Retry(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    @Timed(value = "immigration.validation.time")
    @Secured("ROLE_IMMIGRATION_VALIDATOR")
    public ValidationResult validateCrewDocuments(PreArrivalNotification notification) {
        log.debug("Starting crew validation for vessel call: {}", 
                notification.getVesselCall().getCallSign());

        return circuitBreaker.run(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            try {
                String cacheKey = "crew_validation_" + notification.getId();
                ValidationResult cachedResult = cacheManager.getCache("validations")
                        .get(cacheKey, ValidationResult.class);
                if (cachedResult != null) {
                    log.debug("Returning cached validation result for notification: {}", 
                            notification.getId());
                    return cachedResult;
                }

                CrewValidationRequest request = buildCrewValidationRequest(notification);
                Source response = webServiceTemplate.sendSourceAndReceiveToResult(
                        immigrationServiceUrl + "/validate", 
                        request);

                ValidationResult result = processValidationResponse(response);
                cacheManager.getCache("validations").put(cacheKey, result);

                meterRegistry.counter("immigration.validation.success").increment();
                return result;

            } catch (WebServiceIOException e) {
                meterRegistry.counter("immigration.validation.error.connection").increment();
                log.error("Connection error during crew validation: {}", e.getMessage());
                throw new ImmigrationServiceException("Connection error during validation", e);
            } catch (Exception e) {
                meterRegistry.counter("immigration.validation.error.general").increment();
                log.error("Error during crew validation: {}", e.getMessage());
                throw new ImmigrationServiceException("Validation processing error", e);
            } finally {
                sample.stop(validationTimer);
            }
        });
    }

    /**
     * Requests immigration clearance for vessel crew.
     *
     * @param notification PreArrivalNotification with crew details
     * @param clearance Clearance request details
     * @return Updated clearance with status
     */
    @Retry(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    @Timed(value = "immigration.clearance.time")
    @Secured("ROLE_IMMIGRATION_OFFICER")
    public Clearance requestImmigrationClearance(PreArrivalNotification notification, 
            Clearance clearance) {
        log.debug("Requesting immigration clearance for vessel call: {}", 
                notification.getVesselCall().getCallSign());

        return circuitBreaker.run(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            try {
                ClearanceRequest request = buildClearanceRequest(notification, clearance);
                Source response = webServiceTemplate.sendSourceAndReceiveToResult(
                        immigrationServiceUrl + "/clearance", 
                        request);

                Clearance updatedClearance = processClearanceResponse(response, clearance);
                meterRegistry.counter("immigration.clearance.success").increment();
                return updatedClearance;

            } catch (WebServiceIOException e) {
                meterRegistry.counter("immigration.clearance.error.connection").increment();
                log.error("Connection error during clearance request: {}", e.getMessage());
                throw new ImmigrationServiceException("Connection error during clearance", e);
            } catch (Exception e) {
                meterRegistry.counter("immigration.clearance.error.general").increment();
                log.error("Error during clearance request: {}", e.getMessage());
                throw new ImmigrationServiceException("Clearance processing error", e);
            } finally {
                sample.stop(clearanceTimer);
            }
        });
    }

    private void configureSecurityInterceptor() throws Exception {
        Wss4jSecurityInterceptor securityInterceptor = new Wss4jSecurityInterceptor();
        securityInterceptor.setSecurementUsername(username);
        securityInterceptor.setSecurementPassword(password);
        securityInterceptor.setSecurementActions("Timestamp Signature Encrypt");

        CryptoFactoryBean cryptoFactoryBean = new CryptoFactoryBean();
        cryptoFactoryBean.setKeyStoreLocation(keystoreLocation);
        cryptoFactoryBean.setKeyStorePassword(keystorePassword);
        cryptoFactoryBean.afterPropertiesSet();

        securityInterceptor.setSecurementSignatureCrypto(cryptoFactoryBean.getObject());
        securityInterceptor.setSecurementEncryptionCrypto(cryptoFactoryBean.getObject());

        webServiceTemplate.setInterceptors(new ClientInterceptor[]{securityInterceptor});
    }

    private void configureTimeouts() {
        webServiceTemplate.setDefaultUri(immigrationServiceUrl);
        HttpComponentsMessageSender messageSender = new HttpComponentsMessageSender();
        messageSender.setConnectionTimeout(connectionTimeout);
        messageSender.setReadTimeout(readTimeout);
        webServiceTemplate.setMessageSender(messageSender);
    }

    private CrewValidationRequest buildCrewValidationRequest(PreArrivalNotification notification) {
        // Implementation details for building the SOAP request
        return new CrewValidationRequest();
    }

    private ClearanceRequest buildClearanceRequest(PreArrivalNotification notification, 
            Clearance clearance) {
        // Implementation details for building the clearance request
        return new ClearanceRequest();
    }

    private ValidationResult processValidationResponse(Source response) {
        // Implementation details for processing the validation response
        return new ValidationResult();
    }

    private Clearance processClearanceResponse(Source response, Clearance clearance) {
        // Implementation details for processing the clearance response
        return clearance;
    }
}