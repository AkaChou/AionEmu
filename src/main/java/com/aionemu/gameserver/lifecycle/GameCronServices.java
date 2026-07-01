package com.aionemu.gameserver.lifecycle;

import com.aionemu.commons.services.CronService;
import com.aionemu.gameserver.utils.cron.ThreadPoolManagerRunnableRunner;

public final class GameCronServices {

    private static volatile CronService resolvedCronService;

    private GameCronServices() {
    }

    public static void initialize() {
        resolvedCronService = CronService.initSingleton(ThreadPoolManagerRunnableRunner.class);
    }

    public static CronService cronService() {
        CronService cronService = resolvedCronService;
        if (cronService != null) {
            return cronService;
        }
        return CronService.requireCurrent();
    }

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
