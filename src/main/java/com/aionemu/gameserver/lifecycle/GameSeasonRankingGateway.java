package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameSeasonRankingGateway {

    private ObjectProvider<SeasonRankingUpdateService> seasonRankingUpdateServiceProvider;
    private ObjectProvider<GameMaintenanceServicesRuntimeBridge> runtimeBridgeProvider;

    @Autowired(required = false)
    void setSeasonRankingUpdateServiceProvider(ObjectProvider<SeasonRankingUpdateService> seasonRankingUpdateServiceProvider) {
        this.seasonRankingUpdateServiceProvider = seasonRankingUpdateServiceProvider;
    }

    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameMaintenanceServicesRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    public void start() {
        Util.printSection(" *** Season Ranking *** ");
        seasonRankingUpdateService().onStart();
    }

    private SeasonRankingUpdateService seasonRankingUpdateService() {
        if (seasonRankingUpdateServiceProvider == null) {
            return runtimeBridge().seasonRankingUpdateService();
        }
        return seasonRankingUpdateServiceProvider.getIfAvailable(() -> runtimeBridge().seasonRankingUpdateService());
    }

    private GameMaintenanceServicesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameMaintenanceServicesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameMaintenanceServicesRuntimeBridge::new);
    }
}
