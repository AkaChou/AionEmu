package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.configs.main.ThreadConfig;
import com.aionemu.gameserver.utils.ThreadUncaughtExceptionHandler;
import com.aionemu.gameserver.utils.gametime.DateTimeUtil;
import com.aionemu.gameserver.utils.javaagent.JavaAgentUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 工具服务运行时桥：对接异常处理、配置、数据库、DAO 与线程配置的具体实现。
 * Utility-services runtime bridge: wires concrete exception handler, config, DB, DAO and thread config.
 */
@Component
@Slf4j
public class GameUtilityServicesRuntimeBridge {

    /**
     * 设置默认未捕获异常处理器。
     * Set the default uncaught-exception handler.
     */
    public void initializeExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new ThreadUncaughtExceptionHandler());
    }

    /**
     * 根据 Java Agent 配置报告回调支持状态。
     * Report callback support status based on Java-agent configuration.
     */
    public void reportCallbackSupport() {
        if (JavaAgentUtils.isConfigured()) {
            log.info(I18n.get("console.startup.callback_ok"));
        } else {
            log.warn(I18n.get("console.startup.callback_missing"));
        }
    }

    /**
     * 初始化 Cron 服务。
     * Initialize the cron service.
     */
    public void initializeCronService() {
        GameCronServices.initialize();
    }

    /**
     * 加载配置并应用国家码到 I18n。
     * Load configuration and apply the country code to I18n.
     */
    public void loadConfig() {
        Config.load();
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
     * 初始化数据库工厂。
     * Initialize the database factory.
     */
    public void initializeDatabaseFactory() {
        DatabaseFactory.init();
    }

    /**
     * 初始化 DAO 管理器。
     * Initialize the DAO manager.
     */
    public void initializeDaoManager() {
        DAOManager.init();
    }

    /**
     * 加载线程配置。
     * Load thread configuration.
     */
    public void loadThreadConfig() {
        ThreadConfig.load();
    }
}
