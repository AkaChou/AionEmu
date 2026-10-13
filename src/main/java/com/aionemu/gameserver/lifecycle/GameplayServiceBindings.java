package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.DuelService;
import com.aionemu.gameserver.services.LifeStatsRestoreService;
import com.aionemu.gameserver.services.ranking.SeasonRankingService;
import com.aionemu.gameserver.services.rift.RiftManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 玩法服务静态绑定：把 Spring ObjectProvider 喂入玩法域服务自持的 fail-fast 单例通道。
 * Gameplay static bindings: feeds the Spring ObjectProviders into the fail-fast singleton accessors
 * held by the Gameplay services.
 * <p>本组件取代已退役的玩法静态定位器（R5 批次）：不再暴露任何静态访问方法，
 * 消费方一律走域服务自持的 {@code getInstance()} 通道。
 * Replaces the retired Gameplay static locator: no static accessor is exposed anymore;
 * consumers use each domain service's fail-fast {@code getInstance()} channel.</p>
 */
@Component
public final class GameplayServiceBindings implements DisposableBean {

    /**
     * 构造并注册各玩法服务的实例提供者。
     * Construct and register instance providers for Gameplay services.
     * @param duelServiceProvider DuelService 提供者 / DuelService provider
     * @param lifeStatsRestoreServiceProvider LifeStatsRestoreService 提供者 / LifeStatsRestoreService provider
     * @param seasonRankingServiceProvider SeasonRankingService 提供者 / SeasonRankingService provider
     * @param riftManagerProvider RiftManager 提供者 / RiftManager provider
     */
    public GameplayServiceBindings(ObjectProvider<DuelService> duelServiceProvider,
            ObjectProvider<LifeStatsRestoreService> lifeStatsRestoreServiceProvider,
            ObjectProvider<SeasonRankingService> seasonRankingServiceProvider,
            ObjectProvider<RiftManager> riftManagerProvider) {
        DuelService.setInstanceProvider(duelServiceProvider);
        LifeStatsRestoreService.setInstanceProvider(lifeStatsRestoreServiceProvider);
        SeasonRankingService.setInstanceProvider(seasonRankingServiceProvider);
        RiftManager.setInstanceProvider(riftManagerProvider);
    }

    /**
     * 销毁时清空各域服务的静态提供者通道。
     * Clears each domain service's static provider channel on destroy.
     */
    @Override
    public void destroy() {
        DuelService.setInstanceProvider(null);
        LifeStatsRestoreService.setInstanceProvider(null);
        SeasonRankingService.setInstanceProvider(null);
        RiftManager.setInstanceProvider(null);
    }
}
