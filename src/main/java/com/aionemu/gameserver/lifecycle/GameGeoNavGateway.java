package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.nav.NavService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameGeoNavGateway {

    private ObjectProvider<GeoService> geoServiceProvider;
    private ObjectProvider<NavService> navServiceProvider;
    private ObjectProvider<GameWorldServicesRuntimeBridge> runtimeBridgeProvider;

    @Autowired(required = false)
    void setGeoServiceProvider(ObjectProvider<GeoService> geoServiceProvider) {
        this.geoServiceProvider = geoServiceProvider;
    }

    @Autowired(required = false)
    void setNavServiceProvider(ObjectProvider<NavService> navServiceProvider) {
        this.navServiceProvider = navServiceProvider;
    }

    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameWorldServicesRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    public void initialize() {
        Util.printSection(" *** Geodata *** ");
        geoService().initializeGeo();
        navService().initializeNav();
    }

    private GeoService geoService() {
        if (geoServiceProvider == null) {
            return runtimeBridge().geoService();
        }
        return geoServiceProvider.getIfAvailable(() -> runtimeBridge().geoService());
    }

    private NavService navService() {
        if (navServiceProvider == null) {
            return runtimeBridge().navService();
        }
        return navServiceProvider.getIfAvailable(() -> runtimeBridge().navService());
    }

    private GameWorldServicesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameWorldServicesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameWorldServicesRuntimeBridge::new);
    }
}
