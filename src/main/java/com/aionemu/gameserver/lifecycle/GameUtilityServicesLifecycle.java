package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 工具服务生命周期：按序初始化异常处理/配置/数据库/DAO/线程配置并启动线程池。
 * Utility-services lifecycle: initializes exception handler/config/DB/DAO/thread config and starts the thread pool.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GameUtilityServicesLifecycle {

    /**
     * 工具服务网关。
     * Utility-services gateway.
     */
    private final GameUtilityServicesGateway utilityServicesGateway;

    /**
     * 是否已加载。
     * Whether this stage is loaded.
     */
    private boolean loaded;

    /**
     * 加载耗时毫秒；未启动前为 -1。
     * Load time in milliseconds; {@code -1} before start.
     */
    private long loadTimeMillis = -1;

    /**
     * 最近一次失败。
     * Last failure, if any.
     */
    private Throwable lastFailure;

    /**
     * 启动本阶段：完成工具服务初始化并启动线程池生命周期。
     * Start this stage: finish utility-service init and start the thread-pool lifecycle.
     *
     * @param threadPoolLifecycle 线程池生命周期 / Thread-pool lifecycle
     */
    public synchronized void start(GameThreadPoolLifecycle threadPoolLifecycle) {
        if (loaded) {
            return;
        }

        long start = utilityServicesGateway.currentTimeMillis();
        try {
            utilityServicesGateway.initializeExceptionHandler();
            utilityServicesGateway.reportCallbackSupport();
            utilityServicesGateway.initializeCronService();
            utilityServicesGateway.printConfigSection();
            long configStart = utilityServicesGateway.currentTimeMillis();
            utilityServicesGateway.loadConfig();
            long configTime = utilityServicesGateway.currentTimeMillis() - configStart;
            log.info(I18n.get("console.startup.config_loaded", configTime));
            utilityServicesGateway.initializeDateTime();
            utilityServicesGateway.printDatabaseSection();
            long dbStart = utilityServicesGateway.currentTimeMillis();
            utilityServicesGateway.initializeDatabaseFactory();
            long dbInitTime = utilityServicesGateway.currentTimeMillis() - dbStart;
            log.info(I18n.get("console.startup.db_factory_init", dbInitTime));
            long daoStart = utilityServicesGateway.currentTimeMillis();
            utilityServicesGateway.initializeDaoManager();
            long daoTime = utilityServicesGateway.currentTimeMillis() - daoStart;
            log.info(I18n.get("console.startup.dao_init", daoTime));
            utilityServicesGateway.loadThreadConfig();
            threadPoolLifecycle.start();
            loaded = true;
            lastFailure = null;
        } catch (RuntimeException | Error e) {
            loaded = false;
            lastFailure = e;
            throw e;
        } finally {
            loadTimeMillis = utilityServicesGateway.currentTimeMillis() - start;
        }
    }

    /**
     * 是否已加载。
     * Whether this stage is loaded.
     *
     * @return 已加载为 {@code true} / {@code true} if loaded
     */
    public synchronized boolean isLoaded() {
        return loaded;
    }

    /**
     * 加载耗时毫秒。
     * Load time in milliseconds.
     *
     * @return 耗时毫秒，未启动为 -1 / Elapsed millis, or {@code -1} if not started
     */
    public synchronized long getLoadTimeMillis() {
        return loadTimeMillis;
    }

    /**
     * 最近失败。
     * Last failure.
     *
     * @return 最近异常，无则为 null / Last throwable, or {@code null}
     */
    public synchronized Throwable getLastFailure() {
        return lastFailure;
    }
}
