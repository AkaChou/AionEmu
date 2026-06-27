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

    private static volatile ObjectProvider<SeasonRankingUpdateService> seasonRankingUpdateServiceProvider;

    public GameMaintenanceServices(ObjectProvider<DatabaseCleaningService> databaseCleaningServiceProvider,
            ObjectProvider<AbyssRankCleaningService> abyssRankCleaningServiceProvider,
            ObjectProvider<ShugoImperialTombSpawnManager> shugoImperialTombSpawnManagerProvider,
            ObjectProvider<SeasonRankingUpdateService> seasonRankingUpdateServiceProvider) {
        GameMaintenanceServices.seasonRankingUpdateServiceProvider = seasonRankingUpdateServiceProvider;
        DatabaseCleaningService.setInstanceProvider(databaseCleaningServiceProvider);
        AbyssRankCleaningService.setInstanceProvider(abyssRankCleaningServiceProvider);
        ShugoImperialTombSpawnManager.setInstanceProvider(shugoImperialTombSpawnManagerProvider);
        SeasonRankingUpdateService.setInstanceProvider(seasonRankingUpdateServiceProvider);
    }

    public static SeasonRankingUpdateService seasonRankingUpdateService() {
        ObjectProvider<SeasonRankingUpdateService> provider = seasonRankingUpdateServiceProvider;
        if (provider == null) {
            return GameMaintenanceServiceFallbacks.seasonRankingUpdateService();
        }
        return provider.getIfAvailable(GameMaintenanceServiceFallbacks::seasonRankingUpdateService);
    }

    @Override
    public void destroy() {
        seasonRankingUpdateServiceProvider = null;
        DatabaseCleaningService.setInstanceProvider(null);
        AbyssRankCleaningService.setInstanceProvider(null);
        ShugoImperialTombSpawnManager.setInstanceProvider(null);
        SeasonRankingUpdateService.setInstanceProvider(null);
    }
}
