package com.aionemu.gameserver.lifecycle;

import com.aionemu.commons.services.CronService;
import com.aionemu.gameserver.utils.cron.ThreadPoolManagerRunnableRunner;

/**
 * Cron 服务门面：初始化、解析与关闭 CronService 单例。
 * Cron services facade: initialize, resolve and shut down the CronService singleton.
 */
public final class GameCronServices {

    /**
     * 已解析的 Cron 服务实例。
     * Resolved Cron service instance.
     */
    private static volatile CronService resolvedCronService;

    /**
     * 工具类禁止实例化。
     * Utility class; not instantiable.
     */
    private GameCronServices() {
    }

    /**
     * 以 {@link ThreadPoolManagerRunnableRunner} 初始化 Cron 单例。
     * Initialize the Cron singleton with {@link ThreadPoolManagerRunnableRunner}.
     */
    public static void initialize() {
        resolvedCronService = CronService.initSingleton(ThreadPoolManagerRunnableRunner.class);
    }

    /**
     * 获取 Cron 服务：优先本地缓存，否则要求当前全局实例。
     * Obtain Cron service: prefer local cache, otherwise require the current global instance.
     *
     * Cron service
     */
    public static CronService cronService() {
        CronService cronService = resolvedCronService;
        if (cronService != null) {
            return cronService;
        }
        return CronService.requireCurrent();
    }

    /**
     * 若已初始化则关闭并清空；否则尝试关闭当前全局实例。
     * Shut down and clear if initialized; otherwise try shutting down the current global instance.
     */
    public static void shutdownIfInitialized() {
        CronService cronService = resolvedCronService;
        if (cronService != null) {
            resolvedCronService = null;
            cronService.shutdown();
            return;
        }
        CronService.shutdownCurrentIfInitialized();
    }
}
