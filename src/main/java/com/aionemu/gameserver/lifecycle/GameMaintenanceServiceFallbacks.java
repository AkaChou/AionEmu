package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.DatabaseCleaningService;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;

/**
 * 维护服务回退工厂：Spring 提供者不可用时提供各维护单例。
 * Maintenance-service fallbacks: package-private holders for maintenance singletons when Spring providers are unavailable.
 */
final class GameMaintenanceServiceFallbacks {

    /**
     * 工具类禁止实例化。
     * Utility class; not instantiable.
     */
    private GameMaintenanceServiceFallbacks() {
    }

    /**
     * 数据库清理服务回退实例。
     * Database-cleaning service fallback instance.
     *
     * @return 数据库清理服务 / Database cleaning service
     */
    static DatabaseCleaningService databaseCleaningService() {
        return DatabaseCleaningServiceFallback.INSTANCE;
    }

    /**
     * 欧比斯排名清理服务回退实例。
     * Abyss-rank cleaning service fallback instance.
     *
     * @return 欧比斯排名清理服务 / Abyss-rank cleaning service
     */
    static AbyssRankCleaningService abyssRankCleaningService() {
        return AbyssRankCleaningServiceFallback.INSTANCE;
    }

    /**
     * 术古皇陵生成管理器回退实例。
     * Shugo Imperial Tomb spawn-manager fallback instance.
     *
     * @return 术古皇陵生成管理器 / Shugo Imperial Tomb spawn manager
     */
    static ShugoImperialTombSpawnManager shugoImperialTombSpawnManager() {
        return ShugoImperialTombSpawnManagerFallback.INSTANCE;
    }

    /**
     * 赛季排名更新服务回退实例。
     * Season-ranking update service fallback instance.
     *
     * @return 赛季排名更新服务 / Season-ranking update service
     */
    static SeasonRankingUpdateService seasonRankingUpdateService() {
        return SeasonRankingUpdateServiceFallback.INSTANCE;
    }

    /**
     * {@link DatabaseCleaningService} 懒加载单例持有者。
     * Lazy singleton holder for {@link DatabaseCleaningService}.
     */
    private static final class DatabaseCleaningServiceFallback {
        private static final DatabaseCleaningService INSTANCE = DatabaseCleaningService.getInstance();
    }

    /**
     * {@link AbyssRankCleaningService} 懒加载单例持有者。
     * Lazy singleton holder for {@link AbyssRankCleaningService}.
     */
    private static final class AbyssRankCleaningServiceFallback {
        private static final AbyssRankCleaningService INSTANCE = AbyssRankCleaningService.getInstance();
    }

    /**
     * {@link ShugoImperialTombSpawnManager} 懒加载单例持有者。
     * Lazy singleton holder for {@link ShugoImperialTombSpawnManager}.
     */
    private static final class ShugoImperialTombSpawnManagerFallback {
        private static final ShugoImperialTombSpawnManager INSTANCE = ShugoImperialTombSpawnManager.getInstance();
    }

    /**
     * {@link SeasonRankingUpdateService} 懒加载单例持有者。
     * Lazy singleton holder for {@link SeasonRankingUpdateService}.
     */
    private static final class SeasonRankingUpdateServiceFallback {
        private static final SeasonRankingUpdateService INSTANCE = SeasonRankingUpdateService.getInstance();
    }
}
