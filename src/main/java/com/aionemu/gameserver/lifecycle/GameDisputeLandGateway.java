package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.DisputeLandService;
import com.aionemu.gameserver.services.OutpostService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameDisputeLandGateway {

    private ObjectProvider<DisputeLandService> disputeLandServiceProvider;
    private ObjectProvider<OutpostService> outpostServiceProvider;
    private ObjectProvider<GameFeatureServicesRuntimeBridge> runtimeBridgeProvider;

    @Autowired(required = false)
    void setDisputeLandServiceProvider(ObjectProvider<DisputeLandService> disputeLandServiceProvider) {
        this.disputeLandServiceProvider = disputeLandServiceProvider;
    }

    @Autowired(required = false)
    void setOutpostServiceProvider(ObjectProvider<OutpostService> outpostServiceProvider) {
        this.outpostServiceProvider = outpostServiceProvider;
    }

    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameFeatureServicesRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    public void start() {
        Util.printSection(" *** Dispute Land initialization *** ");
        disputeLandService().initDisputeLand();
        outpostService().initOutposts();
    }

    private DisputeLandService disputeLandService() {
        if (disputeLandServiceProvider == null) {
            return runtimeBridge().disputeLandService();
        }
        return disputeLandServiceProvider.getIfAvailable(() -> runtimeBridge().disputeLandService());
    }

    private OutpostService outpostService() {
        if (outpostServiceProvider == null) {
            return runtimeBridge().outpostService();
        }
        return outpostServiceProvider.getIfAvailable(() -> runtimeBridge().outpostService());
    }

    private GameFeatureServicesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameFeatureServicesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameFeatureServicesRuntimeBridge::new);
    }
}
