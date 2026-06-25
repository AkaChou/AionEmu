package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameWorldActivationGateway {

    private ObjectProvider<DropRegistrationService> dropRegistrationServiceProvider;
    private ObjectProvider<GameWorldServicesRuntimeBridge> runtimeBridgeProvider;

    @Autowired(required = false)
    void setDropRegistrationServiceProvider(ObjectProvider<DropRegistrationService> dropRegistrationServiceProvider) {
        this.dropRegistrationServiceProvider = dropRegistrationServiceProvider;
    }

    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameWorldServicesRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    public GameServer activate() {
        dropRegistrationService();
        GameWorldServicesRuntimeBridge runtimeBridge = runtimeBridge();
        GameServer server = runtimeBridge.createGameServer();
        runtimeBridge.activateGameServer(server);
        runtimeBridge.markPlayersOffline();
        return server;
    }

    private DropRegistrationService dropRegistrationService() {
        if (dropRegistrationServiceProvider == null) {
            return runtimeBridge().dropRegistrationService();
        }
        return dropRegistrationServiceProvider.getIfAvailable(() -> runtimeBridge().dropRegistrationService());
    }

    private GameWorldServicesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameWorldServicesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameWorldServicesRuntimeBridge::new);
    }
}
