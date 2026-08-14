package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.DatabaseCleaningService;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 维护服务 Spring 门面：将 ObjectProvider 注入各维护服务 setInstanceProvider，并提供静态访问。
 * Maintenance-services Spring facade: wires ObjectProviders into each service setInstanceProvider and exposes static accessors.
 */
@Component
public final class GameMaintenanceServices implements DisposableBean {

    /**
     * 术古皇陵生成管理器的 Spring 提供者。
     * Spring provider for the Shugo Imperial Tomb spawn manager.
     */
    private static volatile ObjectProvider<ShugoImperialTombSpawnManager> shugoImperialTombSpawnManagerProvider;

    /**
     * 赛季排名更新服务的 Spring 提供者。
     * Spring provider for the season-ranking update service.
     */
    private static volatile ObjectProvider<SeasonRankingUpdateService> seasonRankingUpdateServiceProvider;

    /**
     * 构造并注册各维护服务的实例提供者。
     * Construct and register instance providers for each maintenance service.
     *
     * @param databaseCleaningServiceProvider 数据库清理服务提供者 / Database-cleaning service provider
     * @param abyssRankCleaningServiceProvider 欧比斯排名清理服务提供者 / Abyss-rank cleaning service provider
     * @param shugoImperialTombSpawnManagerProvider 术古皇陵生成管理器提供者 / Shugo Imperial Tomb spawn-manager provider
     * @param seasonRankingUpdateServiceProvider 赛季排名更新服务提供者 / Season-ranking update service provider
     */
    public GameMaintenanceServices(ObjectProvider<DatabaseCleaningService> databaseCleaningServiceProvider,
            ObjectProvider<AbyssRankCleaningService> abyssRankCleaningServiceProvider,
            ObjectProvider<ShugoImperialTombSpawnManager> shugoImperialTombSpawnManagerProvider,
            ObjectProvider<SeasonRankingUpdateService> seasonRankingUpdateServiceProvider) {
        GameMaintenanceServices.shugoImperialTombSpawnManagerProvider = shugoImperialTombSpawnManagerProvider;
        GameMaintenanceServices.seasonRankingUpdateServiceProvider = seasonRankingUpdateServiceProvider;
        DatabaseCleaningService.setInstanceProvider(databaseCleaningServiceProvider);
        AbyssRankCleaningService.setInstanceProvider(abyssRankCleaningServiceProvider);
        ShugoImperialTombSpawnManager.setInstanceProvider(shugoImperialTombSpawnManagerProvider);
        SeasonRankingUpdateService.setInstanceProvider(seasonRankingUpdateServiceProvider);
    }

    /**
     * 解析赛季排名更新服务：优先 Spring 提供者，否则回退工厂。
     * Resolve the season-ranking update service: prefer Spring provider, otherwise fallback factory.
     *
     * @return 赛季排名更新服务 / Season-ranking update service
     */
    public static SeasonRankingUpdateService seasonRankingUpdateService() {
        ObjectProvider<SeasonRankingUpdateService> provider = seasonRankingUpdateServiceProvider;
        if (provider == null) {
            return GameMaintenanceServiceFallbacks.seasonRankingUpdateService();
        }
        return provider.getIfAvailable(GameMaintenanceServiceFallbacks::seasonRankingUpdateService);
    }

    /**
     * 解析术古皇陵生成管理器：优先 Spring 提供者，否则回退工厂。
     * Resolve the Shugo Imperial Tomb spawn manager: prefer Spring provider, otherwise fallback factory.
     *
     * @return 术古皇陵生成管理器 / Shugo Imperial Tomb spawn manager
     */
    public static ShugoImperialTombSpawnManager shugoImperialTombSpawnManager() {
        ObjectProvider<ShugoImperialTombSpawnManager> provider = shugoImperialTombSpawnManagerProvider;
        if (provider == null) {
            return GameMaintenanceServiceFallbacks.shugoImperialTombSpawnManager();
        }
        return provider.getIfAvailable(GameMaintenanceServiceFallbacks::shugoImperialTombSpawnManager);
    }

    /**
     * 销毁时清理静态提供者与服务实例桥。
     * Clear static providers and service instance bridges on destroy.
     */
    @Override
    public void destroy() {
        shugoImperialTombSpawnManagerProvider = null;
        seasonRankingUpdateServiceProvider = null;
        DatabaseCleaningService.setInstanceProvider(null);
        AbyssRankCleaningService.setInstanceProvider(null);
        ShugoImperialTombSpawnManager.setInstanceProvider(null);
        SeasonRankingUpdateService.setInstanceProvider(null);
    }
}
