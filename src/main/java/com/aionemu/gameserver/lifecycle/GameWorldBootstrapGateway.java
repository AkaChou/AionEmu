package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.RoadService;
import com.aionemu.gameserver.services.teleport.HotspotTeleportService;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.zone.ZoneService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameWorldBootstrapGateway {

    private final StartupProgressReporter progressReporter;
    private ObjectProvider<IDFactory> idFactoryProvider;
    private ObjectProvider<ZoneService> zoneServiceProvider;
    private ObjectProvider<HotspotTeleportService> hotspotTeleportServiceProvider;
    private ObjectProvider<RoadService> roadServiceProvider;
    private ObjectProvider<World> worldProvider;

    public GameWorldBootstrapGateway() {
        this(ConsoleStartupProgressReporter.forCurrentConsole());
    }

    GameWorldBootstrapGateway(StartupProgressReporter progressReporter) {
        this.progressReporter = progressReporter;
    }

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

    public void bootstrap() {
        long start = System.currentTimeMillis();
        progressReporter.start("game world");
        try {
            loadStep("IDFactory", this::initializeIDFactory);
            loadStep("Zone", this::loadZoneService);
            loadStep("Hotspot Teleport", this::initializeHotspotTeleportService);
            loadStep("Road", this::initializeRoadService);
            loadStep("World", this::initializeWorld);
            progressReporter.finish("game world", System.currentTimeMillis() - start);
        } catch (RuntimeException | Error e) {
            progressReporter.failed();
            throw e;
        }
    }

    protected void initializeIDFactory() {
        idFactory();
    }

    protected void loadZoneService() {
        zoneService().load(null);
    }

    protected void initializeHotspotTeleportService() {
        hotspotTeleportService();
    }

    protected void initializeRoadService() {
        roadService();
    }

    protected void initializeWorld() {
        world();
    }

    private IDFactory idFactory() {
        if (idFactoryProvider == null) {
            return IDFactory.getInstance();
        }
        return idFactoryProvider.getIfAvailable(IDFactory::getInstance);
    }

    private ZoneService zoneService() {
        if (zoneServiceProvider == null) {
            return ZoneService.getInstance();
        }
        return zoneServiceProvider.getIfAvailable(ZoneService::getInstance);
    }

    private HotspotTeleportService hotspotTeleportService() {
        if (hotspotTeleportServiceProvider == null) {
            return HotspotTeleportService.getInstance();
        }
        return hotspotTeleportServiceProvider.getIfAvailable(HotspotTeleportService::getInstance);
    }

    private RoadService roadService() {
        if (roadServiceProvider == null) {
            return RoadService.getInstance();
        }
        return roadServiceProvider.getIfAvailable(RoadService::getInstance);
    }

    private World world() {
        if (worldProvider == null) {
            return World.getInstance();
        }
        return worldProvider.getIfAvailable(World::getInstance);
    }

    private void loadStep(String stepName, Runnable loader) {
        progressReporter.stepStarted(stepName);
        loader.run();
        progressReporter.stepFinished(stepName);
    }
}
