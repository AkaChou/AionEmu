package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameMaintenanceServices implements DisposableBean {

    public GameMaintenanceServices(ObjectProvider<ShugoImperialTombSpawnManager> shugoImperialTombSpawnManagerProvider,
            ObjectProvider<SeasonRankingUpdateService> seasonRankingUpdateServiceProvider) {
        ShugoImperialTombSpawnManager.setInstanceProvider(shugoImperialTombSpawnManagerProvider);
        SeasonRankingUpdateService.setInstanceProvider(seasonRankingUpdateServiceProvider);
    }

    @Override
    public void destroy() {
        ShugoImperialTombSpawnManager.setInstanceProvider(null);
        SeasonRankingUpdateService.setInstanceProvider(null);
    }
}
