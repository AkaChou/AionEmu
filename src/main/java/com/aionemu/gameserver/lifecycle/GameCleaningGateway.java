package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.DatabaseCleaningService;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameCleaningGateway {

    private ObjectProvider<DatabaseCleaningService> databaseCleaningServiceProvider;
    private ObjectProvider<AbyssRankCleaningService> abyssRankCleaningServiceProvider;
    private ObjectProvider<GameMaintenanceServicesRuntimeBridge> runtimeBridgeProvider;

    @Autowired(required = false)
    void setDatabaseCleaningServiceProvider(ObjectProvider<DatabaseCleaningService> databaseCleaningServiceProvider) {
        this.databaseCleaningServiceProvider = databaseCleaningServiceProvider;
    }

    @Autowired(required = false)
    void setAbyssRankCleaningServiceProvider(ObjectProvider<AbyssRankCleaningService> abyssRankCleaningServiceProvider) {
        this.abyssRankCleaningServiceProvider = abyssRankCleaningServiceProvider;
    }

    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameMaintenanceServicesRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    public void clean() {
        databaseCleaningService();
        abyssRankCleaningService();
    }

    private DatabaseCleaningService databaseCleaningService() {
        if (databaseCleaningServiceProvider == null) {
            return runtimeBridge().databaseCleaningService();
        }
        return databaseCleaningServiceProvider.getIfAvailable(() -> runtimeBridge().databaseCleaningService());
    }

    private AbyssRankCleaningService abyssRankCleaningService() {
        if (abyssRankCleaningServiceProvider == null) {
            return runtimeBridge().abyssRankCleaningService();
        }
        return abyssRankCleaningServiceProvider.getIfAvailable(() -> runtimeBridge().abyssRankCleaningService());
    }

    private GameMaintenanceServicesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameMaintenanceServicesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameMaintenanceServicesRuntimeBridge::new);
    }
}
