package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.DatabaseCleaningService;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameMaintenanceServices implements DisposableBean {

    public GameMaintenanceServices(ObjectProvider<DatabaseCleaningService> databaseCleaningServiceProvider,
            ObjectProvider<AbyssRankCleaningService> abyssRankCleaningServiceProvider,
            ObjectProvider<ShugoImperialTombSpawnManager> shugoImperialTombSpawnManagerProvider,
            ObjectProvider<SeasonRankingUpdateService> seasonRankingUpdateServiceProvider) {
        DatabaseCleaningService.setInstanceProvider(databaseCleaningServiceProvider);
        AbyssRankCleaningService.setInstanceProvider(abyssRankCleaningServiceProvider);
        ShugoImperialTombSpawnManager.setInstanceProvider(shugoImperialTombSpawnManagerProvider);
        SeasonRankingUpdateService.setInstanceProvider(seasonRankingUpdateServiceProvider);
    }

    @Override
    public void destroy() {
        DatabaseCleaningService.setInstanceProvider(null);
        AbyssRankCleaningService.setInstanceProvider(null);
        ShugoImperialTombSpawnManager.setInstanceProvider(null);
        SeasonRankingUpdateService.setInstanceProvider(null);
    }
}
