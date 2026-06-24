package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.RoadService;
import com.aionemu.gameserver.services.teleport.HotspotTeleportService;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.zone.ZoneService;
import org.springframework.stereotype.Component;

@Component
public class GameWorldBootstrapGateway {

    private final StartupProgressReporter progressReporter;

    public GameWorldBootstrapGateway() {
        this(ConsoleStartupProgressReporter.forCurrentConsole());
    }

    GameWorldBootstrapGateway(StartupProgressReporter progressReporter) {
        this.progressReporter = progressReporter;
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
        IDFactory.getInstance();
    }

    protected void loadZoneService() {
        ZoneService.getInstance().load(null);
    }

    protected void initializeHotspotTeleportService() {
        HotspotTeleportService.getInstance();
    }

    protected void initializeRoadService() {
        RoadService.getInstance();
    }

    protected void initializeWorld() {
        World.getInstance();
    }

    private void loadStep(String stepName, Runnable loader) {
        progressReporter.stepStarted(stepName);
        loader.run();
        progressReporter.stepFinished(stepName);
    }
}
