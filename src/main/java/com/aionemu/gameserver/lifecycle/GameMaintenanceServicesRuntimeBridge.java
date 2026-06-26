package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.services.DatabaseCleaningService;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import com.aionemu.gameserver.services.events.PigPoppyEventService;
import com.aionemu.gameserver.services.events.TreasureAbyssService;
import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class GameMaintenanceServicesRuntimeBridge {

    private ObjectProvider<DatabaseCleaningService> databaseCleaningServiceProvider;
    private ObjectProvider<AbyssRankCleaningService> abyssRankCleaningServiceProvider;
    private ObjectProvider<ShugoImperialTombSpawnManager> shugoImperialTombSpawnManagerProvider;
    private ObjectProvider<SeasonRankingUpdateService> seasonRankingUpdateServiceProvider;

    @Autowired(required = false)
    void setDatabaseCleaningServiceProvider(ObjectProvider<DatabaseCleaningService> databaseCleaningServiceProvider) {
        this.databaseCleaningServiceProvider = databaseCleaningServiceProvider;
    }

    @Autowired(required = false)
    void setAbyssRankCleaningServiceProvider(ObjectProvider<AbyssRankCleaningService> abyssRankCleaningServiceProvider) {
        this.abyssRankCleaningServiceProvider = abyssRankCleaningServiceProvider;
    }

    @Autowired(required = false)
    void setShugoImperialTombSpawnManagerProvider(ObjectProvider<ShugoImperialTombSpawnManager> shugoImperialTombSpawnManagerProvider) {
        this.shugoImperialTombSpawnManagerProvider = shugoImperialTombSpawnManagerProvider;
    }

    @Autowired(required = false)
    void setSeasonRankingUpdateServiceProvider(ObjectProvider<SeasonRankingUpdateService> seasonRankingUpdateServiceProvider) {
        this.seasonRankingUpdateServiceProvider = seasonRankingUpdateServiceProvider;
    }

    public DatabaseCleaningService databaseCleaningService() {
        if (databaseCleaningServiceProvider == null) {
            return GameMaintenanceServiceFallbacks.databaseCleaningService();
        }
        return databaseCleaningServiceProvider.getIfAvailable(GameMaintenanceServiceFallbacks::databaseCleaningService);
    }

    public AbyssRankCleaningService abyssRankCleaningService() {
        if (abyssRankCleaningServiceProvider == null) {
            return GameMaintenanceServiceFallbacks.abyssRankCleaningService();
        }
        return abyssRankCleaningServiceProvider.getIfAvailable(GameMaintenanceServiceFallbacks::abyssRankCleaningService);
    }

    public boolean isPigPoppyEventEnabled() {
        return EventsConfig.ENABLE_PIG_POPPY_EVENT;
    }

    public void schedulePigPoppyEvent() {
        PigPoppyEventService.ScheduleCron();
    }

    public boolean isAbyssEventEnabled() {
        return EventsConfig.ENABLE_ABYSS_EVENT;
    }

    public void scheduleAbyssEvent() {
        TreasureAbyssService.ScheduleCron();
    }

    public boolean isImperialTombEnabled() {
        return EventsConfig.IMPERIAL_TOMB_ENABLE;
    }

    public ShugoImperialTombSpawnManager shugoImperialTombSpawnManager() {
        if (shugoImperialTombSpawnManagerProvider == null) {
            return GameMaintenanceServiceFallbacks.shugoImperialTombSpawnManager();
        }
        return shugoImperialTombSpawnManagerProvider.getIfAvailable(GameMaintenanceServiceFallbacks::shugoImperialTombSpawnManager);
    }

    public SeasonRankingUpdateService seasonRankingUpdateService() {
        if (seasonRankingUpdateServiceProvider == null) {
            return GameMaintenanceServiceFallbacks.seasonRankingUpdateService();
        }
        return seasonRankingUpdateServiceProvider.getIfAvailable(GameMaintenanceServiceFallbacks::seasonRankingUpdateService);
    }
}
