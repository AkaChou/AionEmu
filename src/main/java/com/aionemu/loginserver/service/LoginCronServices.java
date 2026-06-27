package com.aionemu.loginserver.service;

import com.aionemu.commons.services.CronService;
import com.aionemu.loginserver.utils.cron.ThreadPoolManagerRunnableRunner;

public final class LoginCronServices {

    private static volatile CronService resolvedCronService;

    private LoginCronServices() {
    }

    public static void initialize() {
        CronService.initSingleton(ThreadPoolManagerRunnableRunner.class);
        resolvedCronService = CronService.getInstance();
    }

    public static void shutdownIfInitialized() {
        CronService cronService = resolvedCronService;
        if (cronService != null) {
            resolvedCronService = null;
            cronService.shutdown();
            return;
        }
        if (CronService.isInitialized()) {
            cronService = CronService.getInstance();
            if (cronService != null) {
                cronService.shutdown();
            }
        }
    }
}
