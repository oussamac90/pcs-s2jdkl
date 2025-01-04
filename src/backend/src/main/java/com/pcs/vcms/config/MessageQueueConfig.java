package com.pcs.vcms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for RabbitMQ message queue infrastructure in the Vessel Call Management System.
 * Implements high-availability clustering, message persistence, and comprehensive error handling.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Configuration
public class MessageQueueConfig {

    // Queue names
    private static final String VESSEL_CALL_QUEUE = "vessel-call-queue";
    private static final String SERVICE_BOOKING_QUEUE = "service-booking-queue";

    // Exchange names
    private static final String VESSEL_CALL_EXCHANGE = "vessel-call-exchange";
    private static final String SERVICE_BOOKING_EXCHANGE = "service-booking-exchange";

    // Routing keys
    private static final String VESSEL_CALL_ROUTING_KEY = "vessel.call.#";
    private static final String SERVICE_BOOKING_ROUTING_KEY = "service.booking.#";

    // Configuration constants
    private static final int MESSAGE_TTL = 86400000; // 24 hours in milliseconds
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int INITIAL_RETRY_INTERVAL = 1000;
    private static final int MAX_RETRY_INTERVAL = 10000;
    private static final int QUEUE_LENGTH_LIMIT = 10000;

    /**
     * Configures the vessel call queue with high availability and dead letter handling.
     *
     * @return Queue configured for vessel call events
     */
    @Bean
    public Queue vesselCallQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", MESSAGE_TTL);
        args.put("x-dead-letter-exchange", "dlx.vessel-call");
        args.put("x-dead-letter-routing-key", "deadletter.vessel-call");
        args.put("x-max-length", QUEUE_LENGTH_LIMIT);
        args.put("x-queue-mode", "lazy");
        args.put("x-ha-policy", "all"); // Enable queue mirroring
        args.put("x-ha-sync-mode", "automatic");

        return new Queue(VESSEL_CALL_QUEUE, true, false, false, args);
    }

    /**
     * Configures the service booking queue with high availability and dead letter handling.
     *
     * @return Queue configured for service booking events
     */
    @Bean
    public Queue serviceBookingQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", MESSAGE_TTL);
        args.put("x-dead-letter-exchange", "dlx.service-booking");
        args.put("x-dead-letter-routing-key", "deadletter.service-booking");
        args.put("x-max-length", QUEUE_LENGTH_LIMIT);
        args.put("x-queue-mode", "lazy");
        args.put("x-ha-policy", "all"); // Enable queue mirroring
        args.put("x-ha-sync-mode", "automatic");

        return new Queue(SERVICE_BOOKING_QUEUE, true, false, false, args);
    }

    /**
     * Configures the vessel call exchange with high availability settings.
     *
     * @return TopicExchange configured for vessel call events
     */
    @Bean
    public TopicExchange vesselCallExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("alternate-exchange", "ae.vessel-call");
        args.put("x-ha-policy", "all");

        return new TopicExchange(VESSEL_CALL_EXCHANGE, true, false, args);
    }

    /**
     * Configures the service booking exchange with high availability settings.
     *
     * @return TopicExchange configured for service booking events
     */
    @Bean
    public TopicExchange serviceBookingExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("alternate-exchange", "ae.service-booking");
        args.put("x-ha-policy", "all");

        return new TopicExchange(SERVICE_BOOKING_EXCHANGE, true, false, args);
    }

    /**
     * Configures the binding between vessel call queue and exchange.
     *
     * @return Binding for vessel call events
     */
    @Bean
    public Binding vesselCallBinding(Queue vesselCallQueue, TopicExchange vesselCallExchange) {
        return BindingBuilder
                .bind(vesselCallQueue)
                .to(vesselCallExchange)
                .with(VESSEL_CALL_ROUTING_KEY);
    }

    /**
     * Configures the binding between service booking queue and exchange.
     *
     * @return Binding for service booking events
     */
    @Bean
    public Binding serviceBookingBinding(Queue serviceBookingQueue, TopicExchange serviceBookingExchange) {
        return BindingBuilder
                .bind(serviceBookingQueue)
                .to(serviceBookingExchange)
                .with(SERVICE_BOOKING_ROUTING_KEY);
    }

    /**
     * Configures the RabbitTemplate with retry policy and message conversion.
     *
     * @param connectionFactory the RabbitMQ connection factory
     * @return Configured RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        template.setChannelTransacted(true);
        template.setMandatory(true);

        // Configure retry policy
        RetryTemplate retryTemplate = new RetryTemplate();
        
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(INITIAL_RETRY_INTERVAL);
        backOffPolicy.setMaxInterval(MAX_RETRY_INTERVAL);
        backOffPolicy.setMultiplier(2.0);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(MAX_RETRY_ATTEMPTS);
        retryTemplate.setRetryPolicy(retryPolicy);

        template.setRetryTemplate(retryTemplate);

        // Configure confirmation callback
        template.setConfirmCallback((correlation, ack, reason) -> {
            if (!ack) {
                // Handle nack - message not confirmed
                // Logging or error handling would be implemented here
            }
        });

        // Configure return callback for undeliverable messages
        template.setReturnsCallback(returned -> {
            // Handle returned message
            // Logging or error handling would be implemented here
        });

        return template;
    }
}