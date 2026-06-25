package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.BaseService;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameSiegeScheduleGateway {

    private ObjectProvider<SiegeService> siegeServiceProvider;
    private ObjectProvider<BaseService> baseServiceProvider;
    private ObjectProvider<GameFeatureServicesRuntimeBridge> runtimeBridgeProvider;

    @Autowired(required = false)
    void setSiegeServiceProvider(ObjectProvider<SiegeService> siegeServiceProvider) {
        this.siegeServiceProvider = siegeServiceProvider;
    }

    @Autowired(required = false)
    void setBaseServiceProvider(ObjectProvider<BaseService> baseServiceProvider) {
        this.baseServiceProvider = baseServiceProvider;
    }

    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameFeatureServicesRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    public void start() {
        Util.printSection(" *** Sieges *** ");
        siegeService().initSieges();
        baseService().initBases();
    }

    private SiegeService siegeService() {
        if (siegeServiceProvider == null) {
            return runtimeBridge().siegeService();
        }
        return siegeServiceProvider.getIfAvailable(() -> runtimeBridge().siegeService());
    }

    private BaseService baseService() {
        if (baseServiceProvider == null) {
            return runtimeBridge().baseService();
        }
        return baseServiceProvider.getIfAvailable(() -> runtimeBridge().baseService());
    }

    private GameFeatureServicesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameFeatureServicesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameFeatureServicesRuntimeBridge::new);
    }
}
