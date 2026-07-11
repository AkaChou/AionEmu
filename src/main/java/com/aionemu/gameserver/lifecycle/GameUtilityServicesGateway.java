package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 工具服务网关：编排异常处理、配置、数据库、DAO 与线程配置初始化。
 * Utility-services gateway: orchestrates exception handler, config, DB, DAO and thread-config init.
 */
@Component
public class GameUtilityServicesGateway {

    /**
     * 工具服务运行时桥的可选提供者。
     * Optional provider for the utility-services runtime bridge.
     */
    private ObjectProvider<GameUtilityServicesRuntimeBridge> runtimeBridgeProvider;

    /**
     * 注入工具服务运行时桥提供者。
     * Inject the utility-services runtime-bridge provider.
     *
     * @param runtimeBridgeProvider 运行时桥提供者 / Runtime-bridge provider
     */
    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameUtilityServicesRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    /**
     * 初始化默认未捕获异常处理器。
     * Initialize the default uncaught-exception handler.
     */
    public void initializeExceptionHandler() {
        runtimeBridge().initializeExceptionHandler();
    }

    /**
     * 报告 Java Agent 回调支持状态。
     * Report Java-agent callback support status.
     */
    public void reportCallbackSupport() {
        runtimeBridge().reportCallbackSupport();
    }

    /**
     * 初始化 Cron 服务。
     * Initialize the cron service.
     */
    public void initializeCronService() {
        runtimeBridge().initializeCronService();
    }

    /**
     * 打印配置分区标题。
     * Print the config section header.
     */
    public void printConfigSection() {
        Util.printSection(I18n.get("console.section.config"));
    }

    /**
     * 加载配置。
     * Load configuration.
     */
    public void loadConfig() {
        runtimeBridge().loadConfig();
    }

    /**
     * 初始化日期时间工具。
     * Initialize date-time utilities.
     */
    public void initializeDateTime() {
        runtimeBridge().initializeDateTime();
    }

    /**
     * 打印数据库分区标题。
     * Print the database section header.
     */
    public void printDatabaseSection() {
        Util.printSection(I18n.get("console.section.database"));
    }

    /**
     * 初始化数据库工厂。
     * Initialize the database factory.
     */
    public void initializeDatabaseFactory() {
        runtimeBridge().initializeDatabaseFactory();
    }

    /**
     * 初始化 DAO 管理器。
     * Initialize the DAO manager.
     */
    public void initializeDaoManager() {
        runtimeBridge().initializeDaoManager();
    }

    /**
     * 加载线程配置。
     * Load thread configuration.
     */
    public void loadThreadConfig() {
        runtimeBridge().loadThreadConfig();
    }

    /**
     * 返回当前时间毫秒数。
     * Return the current time in milliseconds.
     *
     * @return 当前时间毫秒 / Current time millis
     */
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 解析工具服务运行时桥：优先 Spring，否则新建。
     * Resolve the utility-services runtime bridge: prefer Spring, otherwise create new.
     *
     * Runtime bridge
     */
    private GameUtilityServicesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameUtilityServicesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameUtilityServicesRuntimeBridge::new);
    }
}
