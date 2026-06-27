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

    private static volatile ObjectProvider<DuelService> duelServiceProvider;

    public GameGameplayServices(ObjectProvider<DuelService> duelServiceProvider,
            ObjectProvider<LifeStatsRestoreService> lifeStatsRestoreServiceProvider,
            ObjectProvider<SeasonRankingService> seasonRankingServiceProvider,
            ObjectProvider<RiftManager> riftManagerProvider) {
        GameGameplayServices.duelServiceProvider = duelServiceProvider;
        DuelService.setInstanceProvider(duelServiceProvider);
        LifeStatsRestoreService.setInstanceProvider(lifeStatsRestoreServiceProvider);
        SeasonRankingService.setInstanceProvider(seasonRankingServiceProvider);
        RiftManager.setInstanceProvider(riftManagerProvider);
    }

    public static DuelService duelService() {
        ObjectProvider<DuelService> provider = duelServiceProvider;
        if (provider == null) {
            return DuelService.getInstance();
        }
        return provider.getIfAvailable(DuelService::getInstance);
    }

    @Override
    public void destroy() {
        duelServiceProvider = null;
        DuelService.setInstanceProvider(null);
        LifeStatsRestoreService.setInstanceProvider(null);
        SeasonRankingService.setInstanceProvider(null);
        RiftManager.setInstanceProvider(null);
    }
}
