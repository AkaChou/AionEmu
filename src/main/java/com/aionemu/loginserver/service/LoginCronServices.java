package com.aionemu.loginserver.service;

import com.aionemu.commons.services.CronService;
import com.aionemu.loginserver.utils.cron.ThreadPoolManagerRunnableRunner;

public final class LoginCronServices {

    private LoginCronServices() {
    }

    public static void initialize() {
        CronService.initSingleton(ThreadPoolManagerRunnableRunner.class);
    }

    public static void shutdownIfInitialized() {
        if (CronService.isInitialized()) {
            CronService.getInstance().shutdown();
        }
    }
}
