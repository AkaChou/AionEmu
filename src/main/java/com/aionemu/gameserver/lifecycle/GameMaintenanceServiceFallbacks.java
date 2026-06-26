package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.DatabaseCleaningService;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;

final class GameMaintenanceServiceFallbacks {

    private GameMaintenanceServiceFallbacks() {
    }

    static DatabaseCleaningService databaseCleaningService() {
        return DatabaseCleaningServiceFallback.INSTANCE;
    }

    static AbyssRankCleaningService abyssRankCleaningService() {
        return AbyssRankCleaningServiceFallback.INSTANCE;
    }

    static ShugoImperialTombSpawnManager shugoImperialTombSpawnManager() {
        return ShugoImperialTombSpawnManagerFallback.INSTANCE;
    }

    static SeasonRankingUpdateService seasonRankingUpdateService() {
        return SeasonRankingUpdateServiceFallback.INSTANCE;
    }

    private static final class DatabaseCleaningServiceFallback {
        private static final DatabaseCleaningService INSTANCE = DatabaseCleaningService.getInstance();
    }

    private static final class AbyssRankCleaningServiceFallback {
        private static final AbyssRankCleaningService INSTANCE = AbyssRankCleaningService.getInstance();
    }

    private static final class ShugoImperialTombSpawnManagerFallback {
        private static final ShugoImperialTombSpawnManager INSTANCE = ShugoImperialTombSpawnManager.getInstance();
    }

    private static final class SeasonRankingUpdateServiceFallback {
        private static final SeasonRankingUpdateService INSTANCE = SeasonRankingUpdateService.getInstance();
    }
}
