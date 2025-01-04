package com.pcs.vcms.config;

import org.springframework.context.annotation.Configuration; // Spring Framework 6.1.x
import org.springframework.scheduling.annotation.EnableAsync; // Spring Framework 6.1.x
import org.springframework.scheduling.annotation.AsyncConfigurer; // Spring Framework 6.1.x
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor; // Spring Framework 6.1.x
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler; // Spring Framework 6.1.x
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler; // Spring Framework 6.1.x
import java.util.concurrent.Executor; // Java 17
import java.util.concurrent.RejectedExecutionHandler; // Java 17
import java.util.concurrent.ThreadPoolExecutor; // Java 17

/**
 * Configuration class for asynchronous task execution in the Vessel Call Management System.
 * Provides optimized thread pool settings and exception handling for real-time operations.
 * 
 * Thread Pool Configuration:
 * - Core Pool Size: 5 threads (base capacity)
 * - Max Pool Size: 10 threads (peak capacity)
 * - Queue Capacity: 25 tasks (buffer for load spikes)
 * - Keep-alive Time: 60 seconds
 * 
 * @version 1.0
 * @since 2023-11-15
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final int CORE_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 10;
    private static final int QUEUE_CAPACITY = 25;
    private static final int KEEP_ALIVE_SECONDS = 60;
    private static final String THREAD_NAME_PREFIX = "VcmsAsync-";

    /**
     * Creates and configures a thread pool task executor optimized for VCMS operations.
     * Implements a caller-runs policy for rejected tasks to prevent task loss during high load.
     *
     * @return Configured ThreadPoolTaskExecutor instance
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Configure core thread pool properties
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        
        // Configure thread lifecycle management
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        executor.setAllowCoreThreadTimeOut(true);
        
        // Configure rejection policy to handle overflow
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // Additional optimizations for real-time performance
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(KEEP_ALIVE_SECONDS);
        
        // Initialize the executor
        executor.initialize();
        
        return executor;
    }

    /**
     * Provides exception handling for asynchronous task execution failures.
     * Implements comprehensive logging and monitoring for production environment.
     *
     * @return AsyncUncaughtExceptionHandler for error management
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}