package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameScheduledServicesGateway {

    private ObjectProvider<ShugoImperialTombSpawnManager> shugoImperialTombSpawnManagerProvider;
    private ObjectProvider<GameMaintenanceServicesRuntimeBridge> runtimeBridgeProvider;

    @Autowired(required = false)
    void setShugoImperialTombSpawnManagerProvider(ObjectProvider<ShugoImperialTombSpawnManager> shugoImperialTombSpawnManagerProvider) {
        this.shugoImperialTombSpawnManagerProvider = shugoImperialTombSpawnManagerProvider;
    }

    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameMaintenanceServicesRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    public void start() {
        Util.printSection(" *** Scheduled Services *** ");
        GameMaintenanceServicesRuntimeBridge runtimeBridge = runtimeBridge();
        if (runtimeBridge.isPigPoppyEventEnabled()) {
            runtimeBridge.schedulePigPoppyEvent();
        }
        if (runtimeBridge.isAbyssEventEnabled()) {
            runtimeBridge.scheduleAbyssEvent();
        }
        if (runtimeBridge.isImperialTombEnabled()) {
            shugoImperialTombSpawnManager(runtimeBridge).start();
        }
    }

    private ShugoImperialTombSpawnManager shugoImperialTombSpawnManager(GameMaintenanceServicesRuntimeBridge runtimeBridge) {
        if (shugoImperialTombSpawnManagerProvider == null) {
            return runtimeBridge.shugoImperialTombSpawnManager();
        }
        return shugoImperialTombSpawnManagerProvider.getIfAvailable(runtimeBridge::shugoImperialTombSpawnManager);
    }

    private GameMaintenanceServicesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameMaintenanceServicesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameMaintenanceServicesRuntimeBridge::new);
    }
}
