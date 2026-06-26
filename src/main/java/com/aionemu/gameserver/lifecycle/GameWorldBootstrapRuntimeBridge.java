package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.RoadService;
import com.aionemu.gameserver.services.teleport.HotspotTeleportService;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.zone.ZoneService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class GameWorldBootstrapRuntimeBridge {

    private ObjectProvider<IDFactory> idFactoryProvider;
    private ObjectProvider<ZoneService> zoneServiceProvider;
    private ObjectProvider<HotspotTeleportService> hotspotTeleportServiceProvider;
    private ObjectProvider<RoadService> roadServiceProvider;
    private ObjectProvider<World> worldProvider;

    @Autowired(required = false)
    void setIdFactoryProvider(ObjectProvider<IDFactory> idFactoryProvider) {
        this.idFactoryProvider = idFactoryProvider;
    }

    @Autowired(required = false)
    void setZoneServiceProvider(ObjectProvider<ZoneService> zoneServiceProvider) {
        this.zoneServiceProvider = zoneServiceProvider;
    }

    @Autowired(required = false)
    void setHotspotTeleportServiceProvider(ObjectProvider<HotspotTeleportService> hotspotTeleportServiceProvider) {
        this.hotspotTeleportServiceProvider = hotspotTeleportServiceProvider;
    }

    @Autowired(required = false)
    void setRoadServiceProvider(ObjectProvider<RoadService> roadServiceProvider) {
        this.roadServiceProvider = roadServiceProvider;
    }

    @Autowired(required = false)
    void setWorldProvider(ObjectProvider<World> worldProvider) {
        this.worldProvider = worldProvider;
    }

    public IDFactory idFactory() {
        if (idFactoryProvider == null) {
            return GameWorldBootstrapFallbacks.idFactory();
        }
        return idFactoryProvider.getIfAvailable(GameWorldBootstrapFallbacks::idFactory);
    }

    public ZoneService zoneService() {
        if (zoneServiceProvider == null) {
            return GameWorldBootstrapFallbacks.zoneService();
        }
        return zoneServiceProvider.getIfAvailable(GameWorldBootstrapFallbacks::zoneService);
    }

    public HotspotTeleportService hotspotTeleportService() {
        if (hotspotTeleportServiceProvider == null) {
            return GameWorldBootstrapFallbacks.hotspotTeleportService();
        }
        return hotspotTeleportServiceProvider.getIfAvailable(GameWorldBootstrapFallbacks::hotspotTeleportService);
    }

    public RoadService roadService() {
        if (roadServiceProvider == null) {
            return GameWorldBootstrapFallbacks.roadService();
        }
        return roadServiceProvider.getIfAvailable(GameWorldBootstrapFallbacks::roadService);
    }

    public World world() {
        if (worldProvider == null) {
            return GameWorldBootstrapFallbacks.world();
        }
        return worldProvider.getIfAvailable(GameWorldBootstrapFallbacks::world);
    }
}
