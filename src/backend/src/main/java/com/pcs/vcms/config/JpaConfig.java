package com.pcs.vcms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import javax.persistence.EntityManagerFactory;
import java.util.Properties;

/**
 * JPA Configuration class for the Vessel Call Management System.
 * Configures JPA/Hibernate with PostgreSQL support, master-slave replication,
 * connection pooling, and performance optimizations.
 *
 * @version 1.0
 * @since 2023-11-15
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.pcs.vcms.repository")
@EnableTransactionManagement
public class JpaConfig {

    private static final String ENTITY_PACKAGES_TO_SCAN = "com.pcs.vcms.entity";
    private static final String REPOSITORY_PACKAGES_TO_SCAN = "com.pcs.vcms.repository";

    /**
     * Configures the JPA EntityManagerFactory with PostgreSQL settings and optimizations.
     * Includes master-slave replication, connection pooling, and caching configurations.
     *
     * @return Configured LocalContainerEntityManagerFactoryBean
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactory.setPackagesToScan(ENTITY_PACKAGES_TO_SCAN);

        // Configure Hibernate as JPA provider
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setDatabasePlatform("org.hibernate.dialect.PostgreSQL95Dialect");
        vendorAdapter.setShowSql(true);
        vendorAdapter.setGenerateDdl(false);
        entityManagerFactory.setJpaVendorAdapter(vendorAdapter);

        // Set JPA properties
        Properties jpaProperties = new Properties();

        // Database connection properties
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQL95Dialect");
        jpaProperties.put("hibernate.temp.use_jdbc_metadata_defaults", false);

        // Connection pool settings
        jpaProperties.put("hibernate.hikari.minimumIdle", 5);
        jpaProperties.put("hibernate.hikari.maximumPoolSize", 20);
        jpaProperties.put("hibernate.hikari.idleTimeout", 300000);
        jpaProperties.put("hibernate.hikari.poolName", "VcmsHikariCP");
        jpaProperties.put("hibernate.hikari.maxLifetime", 1200000);
        jpaProperties.put("hibernate.hikari.connectionTimeout", 20000);

        // Master-slave replication configuration
        jpaProperties.put("hibernate.connection.provider_class", "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
        jpaProperties.put("hibernate.read_only.slave.data-source.url", "${vcms.slave.datasource.url}");
        jpaProperties.put("hibernate.read_only.slave.data-source.user", "${vcms.slave.datasource.username}");
        jpaProperties.put("hibernate.read_only.slave.data-source.password", "${vcms.slave.datasource.password}");

        // Performance optimizations
        jpaProperties.put("hibernate.jdbc.batch_size", 50);
        jpaProperties.put("hibernate.order_inserts", true);
        jpaProperties.put("hibernate.order_updates", true);
        jpaProperties.put("hibernate.batch_versioned_data", true);
        jpaProperties.put("hibernate.jdbc.fetch_size", 100);

        // Second-level cache configuration
        jpaProperties.put("hibernate.cache.use_second_level_cache", true);
        jpaProperties.put("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");
        jpaProperties.put("hibernate.cache.use_query_cache", true);
        jpaProperties.put("hibernate.cache.default_cache_concurrency_strategy", "READ_WRITE");

        // Statement caching
        jpaProperties.put("hibernate.jdbc.use_get_generated_keys", true);
        jpaProperties.put("hibernate.jdbc.wrap_result_sets", true);

        // Lazy loading settings
        jpaProperties.put("hibernate.enable_lazy_load_no_trans", false);
        jpaProperties.put("hibernate.jdbc.lob.non_contextual_creation", true);

        // Statistics and logging (development environment)
        jpaProperties.put("hibernate.generate_statistics", true);
        jpaProperties.put("hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS", 1000);

        entityManagerFactory.setJpaProperties(jpaProperties);
        return entityManagerFactory;
    }

    /**
     * Configures the JPA transaction manager with timeout and isolation settings.
     *
     * @param entityManagerFactory The JPA entity manager factory
     * @return Configured JpaTransactionManager
     */
    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        
        // Configure transaction timeout (30 seconds)
        transactionManager.setDefaultTimeout(30);
        
        // Enable validation
        transactionManager.setValidateExistingTransaction(true);
        
        // Configure transaction synchronization
        transactionManager.setNestedTransactionAllowed(false);
        transactionManager.setGlobalRollbackOnParticipationFailure(true);
        
        return transactionManager;
    }
}