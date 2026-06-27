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
    private static volatile ObjectProvider<LifeStatsRestoreService> lifeStatsRestoreServiceProvider;
    private static volatile ObjectProvider<SeasonRankingService> seasonRankingServiceProvider;
    private static volatile ObjectProvider<RiftManager> riftManagerProvider;

    public GameGameplayServices(ObjectProvider<DuelService> duelServiceProvider,
            ObjectProvider<LifeStatsRestoreService> lifeStatsRestoreServiceProvider,
            ObjectProvider<SeasonRankingService> seasonRankingServiceProvider,
            ObjectProvider<RiftManager> riftManagerProvider) {
        GameGameplayServices.duelServiceProvider = duelServiceProvider;
        GameGameplayServices.lifeStatsRestoreServiceProvider = lifeStatsRestoreServiceProvider;
        GameGameplayServices.seasonRankingServiceProvider = seasonRankingServiceProvider;
        GameGameplayServices.riftManagerProvider = riftManagerProvider;
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

    public static LifeStatsRestoreService lifeStatsRestoreService() {
        ObjectProvider<LifeStatsRestoreService> provider = lifeStatsRestoreServiceProvider;
        if (provider == null) {
            return LifeStatsRestoreService.getInstance();
        }
        return provider.getIfAvailable(LifeStatsRestoreService::getInstance);
    }

    public static SeasonRankingService seasonRankingService() {
        ObjectProvider<SeasonRankingService> provider = seasonRankingServiceProvider;
        if (provider == null) {
            return SeasonRankingService.getInstance();
        }
        return provider.getIfAvailable(SeasonRankingService::getInstance);
    }

    public static RiftManager riftManager() {
        ObjectProvider<RiftManager> provider = riftManagerProvider;
        if (provider == null) {
            return RiftManager.getInstance();
        }
        return provider.getIfAvailable(RiftManager::getInstance);
    }

    @Override
    public void destroy() {
        duelServiceProvider = null;
        DuelService.setInstanceProvider(null);
        lifeStatsRestoreServiceProvider = null;
        LifeStatsRestoreService.setInstanceProvider(null);
        seasonRankingServiceProvider = null;
        SeasonRankingService.setInstanceProvider(null);
        riftManagerProvider = null;
        RiftManager.setInstanceProvider(null);
    }
}
