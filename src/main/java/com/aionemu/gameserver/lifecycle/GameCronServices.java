package com.aionemu.gameserver.lifecycle;

import com.aionemu.commons.services.CronService;
import com.aionemu.gameserver.utils.cron.ThreadPoolManagerRunnableRunner;

public final class GameCronServices {

    private static volatile CronService resolvedCronService;

    private GameCronServices() {
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
        CronService.shutdownCurrentIfInitialized();
    }
}
