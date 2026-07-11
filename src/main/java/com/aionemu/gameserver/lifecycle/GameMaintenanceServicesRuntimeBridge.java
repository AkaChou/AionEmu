package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.services.DatabaseCleaningService;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import com.aionemu.gameserver.services.events.PigPoppyEventService;
import com.aionemu.gameserver.services.events.TreasureAbyssService;
import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 维护服务运行时桥接：经提供者或回退解析数据库/欧比斯清理、皇陵与赛季排名，并调度活动事件。
 * Maintenance-services runtime bridge: resolves DatabaseCleaning/AbyssRankCleaning/ShugoImperialTomb/SeasonRanking via providers or fallbacks, and schedules event crons.
 */
@Component
public class GameMaintenanceServicesRuntimeBridge {

    /**
     * 数据库清理服务提供者。
     * Database-cleaning service provider.
     */
    private ObjectProvider<DatabaseCleaningService> databaseCleaningServiceProvider;
    /**
     * 欧比斯排名清理服务提供者。
     * Abyss-rank cleaning service provider.
     */
    private ObjectProvider<AbyssRankCleaningService> abyssRankCleaningServiceProvider;
    /**
     * 术古皇陵生成管理器提供者。
     * Shugo Imperial Tomb spawn-manager provider.
     */
    private ObjectProvider<ShugoImperialTombSpawnManager> shugoImperialTombSpawnManagerProvider;
    /**
     * 赛季排名更新服务提供者。
     * Season-ranking update service provider.
     */
    private ObjectProvider<SeasonRankingUpdateService> seasonRankingUpdateServiceProvider;

    /**
     * 可选注入数据库清理服务提供者。
     * Optionally inject the database-cleaning service provider.
     *
     * @param databaseCleaningServiceProvider 数据库清理服务提供者 / Database-cleaning service provider
     */
    @Autowired(required = false)
    void setDatabaseCleaningServiceProvider(ObjectProvider<DatabaseCleaningService> databaseCleaningServiceProvider) {
        this.databaseCleaningServiceProvider = databaseCleaningServiceProvider;
    }

    /**
     * 可选注入欧比斯排名清理服务提供者。
     * Optionally inject the abyss-rank cleaning service provider.
     *
     * @param abyssRankCleaningServiceProvider 欧比斯排名清理服务提供者 / Abyss-rank cleaning service provider
     */
    @Autowired(required = false)
    void setAbyssRankCleaningServiceProvider(ObjectProvider<AbyssRankCleaningService> abyssRankCleaningServiceProvider) {
        this.abyssRankCleaningServiceProvider = abyssRankCleaningServiceProvider;
    }

    /**
     * 可选注入术古皇陵生成管理器提供者。
     * Optionally inject the Shugo Imperial Tomb spawn-manager provider.
     *
     * @param shugoImperialTombSpawnManagerProvider 术古皇陵生成管理器提供者 / Shugo Imperial Tomb spawn-manager provider
     */
    @Autowired(required = false)
    void setShugoImperialTombSpawnManagerProvider(ObjectProvider<ShugoImperialTombSpawnManager> shugoImperialTombSpawnManagerProvider) {
        this.shugoImperialTombSpawnManagerProvider = shugoImperialTombSpawnManagerProvider;
    }

    /**
     * 可选注入赛季排名更新服务提供者。
     * Optionally inject the season-ranking update service provider.
     *
     * @param seasonRankingUpdateServiceProvider 赛季排名更新服务提供者 / Season-ranking update service provider
     */
    @Autowired(required = false)
    void setSeasonRankingUpdateServiceProvider(ObjectProvider<SeasonRankingUpdateService> seasonRankingUpdateServiceProvider) {
        this.seasonRankingUpdateServiceProvider = seasonRankingUpdateServiceProvider;
    }

    /**
     * 解析数据库清理服务：优先 Spring 提供者，否则回退工厂。
     * Resolve the database-cleaning service: prefer Spring provider, otherwise fallback factory.
     *
     * @return 数据库清理服务 / Database cleaning service
     */
    public DatabaseCleaningService databaseCleaningService() {
        if (databaseCleaningServiceProvider == null) {
            return GameMaintenanceServiceFallbacks.databaseCleaningService();
        }
        return databaseCleaningServiceProvider.getIfAvailable(GameMaintenanceServiceFallbacks::databaseCleaningService);
    }

    /**
     * 解析欧比斯排名清理服务：优先 Spring 提供者，否则回退工厂。
     * Resolve the abyss-rank cleaning service: prefer Spring provider, otherwise fallback factory.
     *
     * @return 欧比斯排名清理服务 / Abyss-rank cleaning service
     */
    public AbyssRankCleaningService abyssRankCleaningService() {
        if (abyssRankCleaningServiceProvider == null) {
            return GameMaintenanceServiceFallbacks.abyssRankCleaningService();
        }
        return abyssRankCleaningServiceProvider.getIfAvailable(GameMaintenanceServiceFallbacks::abyssRankCleaningService);
    }

    /**
     * 猪猪爆米花活动是否启用。
     * Whether the pig-poppy event is enabled.
     *
     * @return {@code true} if enabled。 / {@code true} if enabled
     */
    public boolean isPigPoppyEventEnabled() {
        return EventsConfig.ENABLE_PIG_POPPY_EVENT;
    }

    /**
     * 调度猪猪爆米花活动 cron。
     * Schedule the pig-poppy event cron.
     */
    public void schedulePigPoppyEvent() {
        PigPoppyEventService.ScheduleCron();
    }

    /**
     * 欧比斯宝藏活动是否启用。
     * Whether the abyss treasure event is enabled.
     *
     * @return {@code true} if enabled。 / {@code true} if enabled
     */
    public boolean isAbyssEventEnabled() {
        return EventsConfig.ENABLE_ABYSS_EVENT;
    }

    /**
     * 调度欧比斯宝藏活动 cron。
     * Schedule the abyss treasure event cron.
     */
    public void scheduleAbyssEvent() {
        TreasureAbyssService.ScheduleCron();
    }

    /**
     * 术古皇陵是否启用。
     * Whether the Imperial Tomb is enabled.
     *
     * @return {@code true} if enabled。 / {@code true} if enabled
     */
    public boolean isImperialTombEnabled() {
        return EventsConfig.IMPERIAL_TOMB_ENABLE;
    }

    /**
     * 解析术古皇陵生成管理器：优先 Spring 提供者，否则回退工厂。
     * Resolve the Shugo Imperial Tomb spawn manager: prefer Spring provider, otherwise fallback factory.
     *
     * @return 术古皇陵生成管理器 / Shugo Imperial Tomb spawn manager
     */
    public ShugoImperialTombSpawnManager shugoImperialTombSpawnManager() {
        if (shugoImperialTombSpawnManagerProvider == null) {
            return GameMaintenanceServiceFallbacks.shugoImperialTombSpawnManager();
        }
        return shugoImperialTombSpawnManagerProvider.getIfAvailable(GameMaintenanceServiceFallbacks::shugoImperialTombSpawnManager);
    }

    /**
     * 解析赛季排名更新服务：优先 Spring 提供者，否则回退工厂。
     * Resolve the season-ranking update service: prefer Spring provider, otherwise fallback factory.
     *
     * @return 赛季排名更新服务 / Season-ranking update service
     */
    public SeasonRankingUpdateService seasonRankingUpdateService() {
        if (seasonRankingUpdateServiceProvider == null) {
            return GameMaintenanceServiceFallbacks.seasonRankingUpdateService();
        }
        return seasonRankingUpdateServiceProvider.getIfAvailable(GameMaintenanceServiceFallbacks::seasonRankingUpdateService);
    }
}
