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
        return DatabaseCleaningService.getInstance();
    }

    /**
     * 欧比斯排名清理服务回退实例。
     * Abyss-rank cleaning service fallback instance.
     *
     * @return 欧比斯排名清理服务 / Abyss-rank cleaning service
     */
    static AbyssRankCleaningService abyssRankCleaningService() {
        return AbyssRankCleaningService.getInstance();
    }

    /**
     * 术古皇陵生成管理器回退实例。
     * Shugo Imperial Tomb spawn-manager fallback instance.
     *
     * @return 术古皇陵生成管理器 / Shugo Imperial Tomb spawn manager
     */
    static ShugoImperialTombSpawnManager shugoImperialTombSpawnManager() {
        return ShugoImperialTombSpawnManager.getInstance();
    }

    /**
     * 赛季排名更新服务回退实例。
     * Season-ranking update service fallback instance.
     *
     * @return 赛季排名更新服务 / Season-ranking update service
     */
    static SeasonRankingUpdateService seasonRankingUpdateService() {
        return SeasonRankingUpdateService.getInstance();
    }
}
