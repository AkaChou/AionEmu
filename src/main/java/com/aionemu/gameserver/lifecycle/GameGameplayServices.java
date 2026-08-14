package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.DuelService;
import com.aionemu.gameserver.services.LifeStatsRestoreService;
import com.aionemu.gameserver.services.ranking.SeasonRankingService;
import com.aionemu.gameserver.services.rift.RiftManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 核心玩法 Spring 服务门面 / 静态访问桥：决斗、生命恢复、赛季排名与裂隙。
 * static access bridge: duel, life restore, season ranking, and rifts.
 */
@Component
public final class GameGameplayServices implements DisposableBean {

    /**
     * 决斗服务的 Spring 提供者。
     * Spring provider for the duel service.
     */
    private static volatile ObjectProvider<DuelService> duelServiceProvider;
    /**
     * 生命恢复服务的 Spring 提供者。
     * Spring provider for the life-stats restore service.
     */
    private static volatile ObjectProvider<LifeStatsRestoreService> lifeStatsRestoreServiceProvider;
    /**
     * 赛季排名服务的 Spring 提供者。
     * Spring provider for the season-ranking service.
     */
    private static volatile ObjectProvider<SeasonRankingService> seasonRankingServiceProvider;
    /**
     * 裂隙管理器的 Spring 提供者。
     * Spring provider for the rift manager.
     */
    private static volatile ObjectProvider<RiftManager> riftManagerProvider;

    /**
     * 构造并注册各核心玩法实例提供者。
     * Construct and register instance providers for core gameplay services.
     *
     * @param duelServiceProvider 决斗服务提供者 / Duel-service provider
     * @param lifeStatsRestoreServiceProvider 生命恢复服务提供者 / Life-stats restore service provider
     * @param seasonRankingServiceProvider 赛季排名服务提供者 / Season-ranking service provider
     * @param riftManagerProvider 裂隙管理器提供者 / Rift-manager provider
     */
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

    /**
     * 解析决斗服务。
     * Resolve the duel service.
     *
     * @return 决斗服务 / Duel service
     */
    public static DuelService duelService() {
        ObjectProvider<DuelService> provider = duelServiceProvider;
        if (provider == null) {
            return DuelService.getInstance();
        }
        return provider.getIfAvailable(DuelService::getInstance);
    }

    /**
     * 解析生命恢复服务。
     * Resolve the life-stats restore service.
     *
     * @return 生命恢复服务 / Life-stats restore service
     */
    public static LifeStatsRestoreService lifeStatsRestoreService() {
        ObjectProvider<LifeStatsRestoreService> provider = lifeStatsRestoreServiceProvider;
        if (provider == null) {
            return LifeStatsRestoreService.getInstance();
        }
        return provider.getIfAvailable(LifeStatsRestoreService::getInstance);
    }

    /**
     * 解析赛季排名服务。
     * Resolve the season-ranking service.
     *
     * @return 赛季排名服务 / Season-ranking service
     */
    public static SeasonRankingService seasonRankingService() {
        ObjectProvider<SeasonRankingService> provider = seasonRankingServiceProvider;
        if (provider == null) {
            return SeasonRankingService.getInstance();
        }
        return provider.getIfAvailable(SeasonRankingService::getInstance);
    }

    /**
     * 解析裂隙管理器。
     * Resolve the rift manager.
     *
     * @return 裂隙管理器 / Rift manager
     */
    public static RiftManager riftManager() {
        ObjectProvider<RiftManager> provider = riftManagerProvider;
        if (provider == null) {
            return RiftManager.getInstance();
        }
        return provider.getIfAvailable(RiftManager::getInstance);
    }

    /**
     * 销毁时清理静态提供者与服务实例桥。
     * Clear static providers and service instance bridges on destroy.
     */
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
