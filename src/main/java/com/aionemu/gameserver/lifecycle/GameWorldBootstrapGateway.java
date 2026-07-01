package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.RoadService;
import com.aionemu.gameserver.services.teleport.HotspotTeleportService;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.zone.ZoneService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
    private ObjectProvider<GameWorldBootstrapRuntimeBridge> runtimeBridgeProvider;

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

    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameWorldBootstrapRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    public void bootstrap() {
        long start = System.currentTimeMillis();
        progressReporter.start("game world");
        try {
            loadStepsInParallel(List.of(
                new BootstrapStep("IDFactory", this::initializeIDFactory),
                new BootstrapStep("Zone", this::loadZoneService),
                new BootstrapStep("Hotspot Teleport", this::initializeHotspotTeleportService),
                new BootstrapStep("Road", this::initializeRoadService),
                new BootstrapStep("World", this::initializeWorld)
            ));
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
            return runtimeBridge().idFactory();
        }
        return idFactoryProvider.getIfAvailable(() -> runtimeBridge().idFactory());
    }

    private ZoneService zoneService() {
        if (zoneServiceProvider == null) {
            return runtimeBridge().zoneService();
        }
        return zoneServiceProvider.getIfAvailable(() -> runtimeBridge().zoneService());
    }

    private HotspotTeleportService hotspotTeleportService() {
        if (hotspotTeleportServiceProvider == null) {
            return runtimeBridge().hotspotTeleportService();
        }
        return hotspotTeleportServiceProvider.getIfAvailable(() -> runtimeBridge().hotspotTeleportService());
    }

    private RoadService roadService() {
        if (roadServiceProvider == null) {
            return runtimeBridge().roadService();
        }
        return roadServiceProvider.getIfAvailable(() -> runtimeBridge().roadService());
    }

    private World world() {
        if (worldProvider == null) {
            return runtimeBridge().world();
        }
        return worldProvider.getIfAvailable(() -> runtimeBridge().world());
    }

    private GameWorldBootstrapRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameWorldBootstrapRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameWorldBootstrapRuntimeBridge::new);
    }

    private void loadStep(String stepName, Runnable loader) {
        progressReporter.stepStarted(stepName);
        loader.run();
        progressReporter.stepFinished(stepName);
    }

    private void loadStepsInParallel(List<BootstrapStep> steps) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>(steps.size());
            for (BootstrapStep step : steps) {
                futures.add(executor.submit(() -> loadStep(step.name(), step.loader())));
            }
            for (Future<?> future : futures) {
                await(future);
            }
        }
    }

    private void await(Future<?> future) {
        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while loading game world", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Failed to load game world", cause);
        }
    }

    private record BootstrapStep(String name, Runnable loader) {
    }
}
