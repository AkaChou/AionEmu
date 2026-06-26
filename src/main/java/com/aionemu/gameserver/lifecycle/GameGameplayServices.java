package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.DuelService;
import com.aionemu.gameserver.services.LifeStatsRestoreService;
import com.aionemu.gameserver.services.ranking.SeasonRankingService;
import com.aionemu.gameserver.services.rift.RiftManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameGameplayServices implements DisposableBean {

    public GameGameplayServices(ObjectProvider<DuelService> duelServiceProvider,
            ObjectProvider<LifeStatsRestoreService> lifeStatsRestoreServiceProvider,
            ObjectProvider<SeasonRankingService> seasonRankingServiceProvider,
            ObjectProvider<RiftManager> riftManagerProvider) {
        DuelService.setInstanceProvider(duelServiceProvider);
        LifeStatsRestoreService.setInstanceProvider(lifeStatsRestoreServiceProvider);
        SeasonRankingService.setInstanceProvider(seasonRankingServiceProvider);
        RiftManager.setInstanceProvider(riftManagerProvider);
    }

    @Override
    public void destroy() {
        DuelService.setInstanceProvider(null);
        LifeStatsRestoreService.setInstanceProvider(null);
        SeasonRankingService.setInstanceProvider(null);
        RiftManager.setInstanceProvider(null);
    }
}
