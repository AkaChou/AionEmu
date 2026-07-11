package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 赛季排名启动网关：打印分区并触发 {@link SeasonRankingUpdateService#onStart()}。
 * Gateway that starts season ranking: prints the section and invokes {@link SeasonRankingUpdateService#onStart()}.
 */
@Component
public class GameSeasonRankingGateway {

    /**
     * 赛季排名更新服务提供者（可选）。
     * Optional provider for the season ranking update service.
     */
    private ObjectProvider<SeasonRankingUpdateService> seasonRankingUpdateServiceProvider;

    /**
     * 维护类运行时桥接提供者（可选）。
     * Optional provider for the maintenance runtime bridge.
     */
    private ObjectProvider<GameMaintenanceServicesRuntimeBridge> runtimeBridgeProvider;

    /**
     * 注入赛季排名更新服务提供者。
     * Inject the season ranking update service provider.
     *
     * @param seasonRankingUpdateServiceProvider 更新服务提供者 / Update service provider
     */
    @Autowired(required = false)
    void setSeasonRankingUpdateServiceProvider(ObjectProvider<SeasonRankingUpdateService> seasonRankingUpdateServiceProvider) {
        this.seasonRankingUpdateServiceProvider = seasonRankingUpdateServiceProvider;
    }

    /**
     * 注入维护类运行时桥接提供者。
     * Inject the maintenance runtime bridge provider.
     *
     * @param runtimeBridgeProvider 运行时桥接提供者 / Runtime bridge provider
     */
    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameMaintenanceServicesRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    /**
     * 打印赛季排名分区并启动更新服务。
     * Print the season-ranking section and start the update service.
     */
    public void start() {
        Util.printSection(I18n.get("console.section.season_ranking"));
        seasonRankingUpdateService().onStart();
    }

    /**
     * 解析赛季排名更新服务。
     * Resolve the season ranking update service.
     *
     * Update service
     */
    private SeasonRankingUpdateService seasonRankingUpdateService() {
        if (seasonRankingUpdateServiceProvider == null) {
            return runtimeBridge().seasonRankingUpdateService();
        }
        return seasonRankingUpdateServiceProvider.getIfAvailable(() -> runtimeBridge().seasonRankingUpdateService());
    }

    /**
     * 解析维护类运行时桥接。
     * Resolve the maintenance runtime bridge.
     *
     * @return 运行时桥接 / Runtime bridge
     */
    private GameMaintenanceServicesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameMaintenanceServicesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameMaintenanceServicesRuntimeBridge::new);
    }
}
