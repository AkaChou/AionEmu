package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.configs.main.ThreadConfig;
import com.aionemu.gameserver.utils.ThreadUncaughtExceptionHandler;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.gametime.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 工具服务网关：编排异常处理、配置与线程配置初始化。
 * Utility-services gateway: orchestrates exception handler, config and thread-config init.
 */
@Component
@Slf4j
public class GameUtilityServicesGateway {

    /**
     * 初始化默认未捕获异常处理器。
     * Initialize the default uncaught-exception handler.
     */
    public void initializeExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new ThreadUncaughtExceptionHandler());
    }

    /**
     * 报告 Java Agent 回调支持状态。
     * Report Java-agent callback support status.
     */
    public void reportCallbackSupport() {
        // 构建期织入后回调系统恒就绪：仅保留启动提示，不再保留 javaagent 探测外壳。
        // The callback system is always ready after build-time weaving: keep the startup hint only.
        log.info(I18n.get("console.startup.callback_ok"));
    }

    /**
     * 初始化 Cron 服务。
     * Initialize the cron service.
     */
    public void initializeCronService() {
        GameCronServices.initialize();
    }

    /**
     * 打印配置分区标题。
     * Print the config section header.
     */
    public void printConfigSection() {
        Util.printSection(I18n.get("console.section.config"));
    }

    /**
     * 加载配置并应用国家码到 I18n。
     * Load configuration and apply the country code to I18n.
     */
    public void loadConfig() {
        Config.load();
        GSConfig.resolveServerCountryCode();
        I18n.applyCountryCode(GSConfig.SERVER_COUNTRY_CODE);
    }

    /**
     * 初始化日期时间工具。
     * Initialize date-time utilities.
     */
    public void initializeDateTime() {
        DateTimeUtil.init();
    }

    /**
     * 打印数据库分区标题。
     * Print the database section header.
     */
    public void printDatabaseSection() {
        Util.printSection(I18n.get("console.section.database"));
    }

    /**
     * 加载线程配置。
     * Load thread configuration.
     */
    public void loadThreadConfig() {
        ThreadConfig.load();
    }

    /**
     * 返回当前时间毫秒数。
     * Return the current time in milliseconds.
     * @return 当前时间毫秒 / Current time millis
     */
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
