package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.services.DatabaseCleaningService;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import com.aionemu.gameserver.services.events.PigPoppyEventService;
import com.aionemu.gameserver.services.events.TreasureAbyssService;
import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class GameMaintenanceServicesRuntimeBridge {

    public DatabaseCleaningService databaseCleaningService() {
        return DatabaseCleaningService.getInstance();
    }

    public AbyssRankCleaningService abyssRankCleaningService() {
        return AbyssRankCleaningService.getInstance();
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
        return ShugoImperialTombSpawnManager.getInstance();
    }

    public SeasonRankingUpdateService seasonRankingUpdateService() {
        return SeasonRankingUpdateService.getInstance();
    }
}
