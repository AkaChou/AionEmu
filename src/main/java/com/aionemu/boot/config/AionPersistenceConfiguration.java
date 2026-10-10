package com.aionemu.boot.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.aionemu.commons.database.DatabaseFactory;

/**
 * Spring Boot 持久化与事务核心配置类。
 * Spring Boot persistence and transaction core configuration.
 * <p>
 * 统一将多租户上下文感知的数据源、流式 {@link JdbcClient} 以及 Spring 事务管理器注入为标准容器 Bean。
 * Exposes the context-aware routing DataSource, fluent JdbcClient, and Spring TransactionManager as standard Spring Beans.
 * </p>
 */
@Configuration
public class AionPersistenceConfiguration {

    /**
     * 提供上下文路由数据源 Bean。
     * Provides the context-routing DataSource Bean.
     * @return 路由数据源 / Routing DataSource
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        return DatabaseFactory.getContextRoutingDataSource();
    }

    /**
     * 提供全局流式 SQL 客户端 Bean。
     * Provides the global fluent SQL client Bean.
     * @param dataSource 路由数据源 / Routing DataSource
     * @return JdbcClient 实例 / JdbcClient instance
     */
    @Bean
    @Primary
    public JdbcClient jdbcClient(DataSource dataSource) {
        return DatabaseFactory.getJdbcClient();
    }

    /**
     * 提供基于 JdbcTransactionManager 的 Spring 平台事务管理器。
     * Provides Spring PlatformTransactionManager backed by JdbcTransactionManager.
     * @param dataSource 路由数据源 / Routing DataSource
     * @return 事务管理器 / Transaction Manager
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }

    /**
     * 提供声明式与编程式事务模版。
     * Provides TransactionTemplate for programmatic transaction control.
     * @param transactionManager 平台事务管理器 / PlatformTransactionManager
     * @return 事务模版 / TransactionTemplate
     */
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
