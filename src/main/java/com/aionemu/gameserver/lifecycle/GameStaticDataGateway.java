package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.dataholders.DataManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameStaticDataGateway {

    private ObjectProvider<DataManager> dataManagerProvider;
    private ObjectProvider<GameCoreServicesRuntimeBridge> runtimeBridgeProvider;

    @Autowired(required = false)
    void setDataManagerProvider(ObjectProvider<DataManager> dataManagerProvider) {
        this.dataManagerProvider = dataManagerProvider;
    }

    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameCoreServicesRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    public void load() {
        dataManager();
    }

    private DataManager dataManager() {
        if (dataManagerProvider == null) {
            return runtimeBridge().dataManager();
        }
        return dataManagerProvider.getIfAvailable(() -> runtimeBridge().dataManager());
    }

    private GameCoreServicesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameCoreServicesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameCoreServicesRuntimeBridge::new);
    }
}
