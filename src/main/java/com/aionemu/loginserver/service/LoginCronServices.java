package com.aionemu.loginserver.service;

import com.aionemu.commons.services.CronService;
import com.aionemu.loginserver.utils.cron.ThreadPoolManagerRunnableRunner;
import lombok.experimental.UtilityClass;

/**
 * 登录服 Cron 服务的初始化与关闭入口。
 * Login-server Cron service bootstrap and shutdown entry.
 */
@UtilityClass
public class LoginCronServices {

    private volatile CronService resolvedCronService;

    /**
     * 初始化登录服 Cron 单例（使用线程池 Runnable 运行器）。
     * Initialize the login Cron singleton with the thread-pool runnable runner.
     */
    public void initialize() {
        resolvedCronService = CronService.initSingleton(ThreadPoolManagerRunnableRunner.class);
    }

    /**
     * 若已初始化则关闭 Cron；否则尝试关闭当前全局实例。
     * Shutdown Cron if initialized; otherwise try shutting down the global instance.
     */
    public void shutdownIfInitialized() {
        CronService cronService = resolvedCronService;
        if (cronService != null) {
            resolvedCronService = null;
            cronService.shutdown();
            return;
        }
        CronService.shutdownCurrentIfInitialized();
    }
}
