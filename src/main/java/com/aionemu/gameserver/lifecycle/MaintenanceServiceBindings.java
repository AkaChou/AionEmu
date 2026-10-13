package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.DatabaseCleaningService;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 维护服务静态绑定：把 Spring ObjectProvider 喂入维护域服务自持的 fail-fast 单例通道。
 * Maintenance static bindings: feeds the Spring ObjectProviders into the fail-fast singleton accessors
 * held by the Maintenance services.
 * <p>本组件取代已退役的维护静态定位器（R5 批次）：不再暴露任何静态访问方法，
 * 消费方一律走域服务自持的 {@code getInstance()} 通道。
 * Replaces the retired Maintenance static locator: no static accessor is exposed anymore;
 * consumers use each domain service's fail-fast {@code getInstance()} channel.</p>
 */
@Component
public final class MaintenanceServiceBindings implements DisposableBean {

    /**
     * 构造并注册各维护服务的实例提供者。
     * Construct and register instance providers for Maintenance services.
     * @param databaseCleaningServiceProvider DatabaseCleaningService 提供者 / DatabaseCleaningService provider
     * @param abyssRankCleaningServiceProvider AbyssRankCleaningService 提供者 / AbyssRankCleaningService provider
     * @param shugoImperialTombSpawnManagerProvider ShugoImperialTombSpawnManager 提供者 / ShugoImperialTombSpawnManager provider
     * @param seasonRankingUpdateServiceProvider SeasonRankingUpdateService 提供者 / SeasonRankingUpdateService provider
     */
    public MaintenanceServiceBindings(ObjectProvider<DatabaseCleaningService> databaseCleaningServiceProvider,
            ObjectProvider<AbyssRankCleaningService> abyssRankCleaningServiceProvider,
            ObjectProvider<ShugoImperialTombSpawnManager> shugoImperialTombSpawnManagerProvider,
            ObjectProvider<SeasonRankingUpdateService> seasonRankingUpdateServiceProvider) {
        DatabaseCleaningService.setInstanceProvider(databaseCleaningServiceProvider);
        AbyssRankCleaningService.setInstanceProvider(abyssRankCleaningServiceProvider);
        ShugoImperialTombSpawnManager.setInstanceProvider(shugoImperialTombSpawnManagerProvider);
        SeasonRankingUpdateService.setInstanceProvider(seasonRankingUpdateServiceProvider);
    }

    /**
     * 销毁时清空各域服务的静态提供者通道。
     * Clears each domain service's static provider channel on destroy.
     */
    @Override
    public void destroy() {
        DatabaseCleaningService.setInstanceProvider(null);
        AbyssRankCleaningService.setInstanceProvider(null);
        ShugoImperialTombSpawnManager.setInstanceProvider(null);
        SeasonRankingUpdateService.setInstanceProvider(null);
    }
}
