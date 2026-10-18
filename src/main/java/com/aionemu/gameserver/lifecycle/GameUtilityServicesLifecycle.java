package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.services.CronService;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.configs.main.ThreadConfig;
import com.aionemu.gameserver.utils.ThreadUncaughtExceptionHandler;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.cron.ThreadPoolManagerRunnableRunner;
import com.aionemu.gameserver.utils.gametime.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 工具服务生命周期：按序初始化异常处理/配置/数据库/DAO/线程配置并启动线程池。
 * Utility-services lifecycle: initializes exception handler/config/DB/DAO/thread config and starts the thread pool.
 */
@Component
@Slf4j
public class GameUtilityServicesLifecycle {

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
     * @param threadPoolLifecycle 线程池生命周期 / Thread-pool lifecycle
     */
    public synchronized void start(GameThreadPoolLifecycle threadPoolLifecycle) {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            initializeExceptionHandler();
            reportCallbackSupport();
            initializeCronService();
            printConfigSection();
            long configStart = System.currentTimeMillis();
            loadConfig();
            long configTime = System.currentTimeMillis() - configStart;
            log.info(I18n.get("console.startup.config_loaded", configTime));
            initializeDateTime();
            printDatabaseSection();
            loadThreadConfig();
            threadPoolLifecycle.start();
            loaded = true;
            lastFailure = null;
        } catch (RuntimeException | Error e) {
            loaded = false;
            lastFailure = e;
            throw e;
        } finally {
            loadTimeMillis = System.currentTimeMillis() - start;
        }
    }

    /**
     * 初始化默认未捕获异常处理器。
     * Initialize the default uncaught-exception handler.
     */
    protected void initializeExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new ThreadUncaughtExceptionHandler());
    }

    /**
     * 报告 Java Agent 回调支持状态。
     * Report Java-agent callback support status.
     */
    protected void reportCallbackSupport() {
        // 构建期织入后回调系统恒就绪：仅保留启动提示，不再保留 javaagent 探测外壳。
        // The callback system is always ready after build-time weaving: keep the startup hint only.
        log.debug(I18n.get("console.startup.callback_ok"));
    }

    /**
     * 初始化 Cron 服务。
     * Initialize the cron service.
     */
    protected void initializeCronService() {
        CronService.initSingleton(ThreadPoolManagerRunnableRunner.class);
    }

    /**
     * 打印配置分区标题。
     * Print the config section header.
     */
    protected void printConfigSection() {
        Util.printSectionIfVerbose(I18n.get("console.section.config"));
    }

    /**
     * 加载配置并应用国家码到 I18n。
     * Load configuration and apply the country code to I18n.
     */
    protected void loadConfig() {
        Config.load();
        GSConfig.resolveServerCountryCode();
        I18n.applyCountryCode(GSConfig.SERVER_COUNTRY_CODE);
    }

    /**
     * 初始化日期时间工具。
     * Initialize date-time utilities.
     */
    protected void initializeDateTime() {
        DateTimeUtil.init();
    }

    /**
     * 打印数据库分区标题。
     * Print the database section header.
     */
    protected void printDatabaseSection() {
        Util.printSectionIfVerbose(I18n.get("console.section.database"));
    }

    /**
     * 加载线程配置。
     * Load thread configuration.
     */
    protected void loadThreadConfig() {
        ThreadConfig.load();
    }

    /**
     * 是否已加载。
     * Whether this stage is loaded.
     * @return 已加载为 {@code true} / {@code true} if loaded
     */
    public synchronized boolean isLoaded() {
        return loaded;
    }

    /**
     * 加载耗时毫秒。
     * Load time in milliseconds.
     * @return 耗时毫秒，未启动为 -1 / Elapsed millis, or {@code -1} if not started
     */
    public synchronized long getLoadTimeMillis() {
        return loadTimeMillis;
    }

    /**
     * 最近失败。
     * Last failure.
     * @return 最近异常，无则为 null / Last throwable, or {@code null}
     */
    public synchronized Throwable getLastFailure() {
        return lastFailure;
    }
}
