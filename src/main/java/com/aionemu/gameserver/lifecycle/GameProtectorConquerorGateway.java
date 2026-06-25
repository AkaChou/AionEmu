package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.ProtectorConquerorService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameProtectorConquerorGateway {

    private ObjectProvider<ProtectorConquerorService> protectorConquerorServiceProvider;
    private ObjectProvider<GameFeatureServicesRuntimeBridge> runtimeBridgeProvider;

    @Autowired(required = false)
    void setProtectorConquerorServiceProvider(ObjectProvider<ProtectorConquerorService> protectorConquerorServiceProvider) {
        this.protectorConquerorServiceProvider = protectorConquerorServiceProvider;
    }

    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameFeatureServicesRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    public void start() {
        Util.printSection(" *** Protector/Conqueror initialization *** ");
        protectorConquerorService().initSystem();
    }

    private ProtectorConquerorService protectorConquerorService() {
        if (protectorConquerorServiceProvider == null) {
            return runtimeBridge().protectorConquerorService();
        }
        return protectorConquerorServiceProvider.getIfAvailable(() -> runtimeBridge().protectorConquerorService());
    }

    private GameFeatureServicesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameFeatureServicesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameFeatureServicesRuntimeBridge::new);
    }
}
