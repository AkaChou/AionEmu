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

/**
 * 世界引导网关：并行加载 IDFactory / Zone / Hotspot / Road / World。
 * World-bootstrap gateway: loads IDFactory / Zone / Hotspot / Road / World in parallel.
 */
@Component
public class GameWorldBootstrapGateway {

    /**
     * 启动进度报告器。
     * Startup progress reporter.
     */
    private final StartupProgressReporter progressReporter;

    /**
     * IDFactory 的可选提供者。
     * Optional provider for IDFactory.
     */
    private ObjectProvider<IDFactory> idFactoryProvider;

    /**
     * ZoneService 的可选提供者。
     * Optional provider for ZoneService.
     */
    private ObjectProvider<ZoneService> zoneServiceProvider;

    /**
     * HotspotTeleportService 的可选提供者。
     * Optional provider for HotspotTeleportService.
     */
    private ObjectProvider<HotspotTeleportService> hotspotTeleportServiceProvider;

    /**
     * RoadService 的可选提供者。
     * Optional provider for RoadService.
     */
    private ObjectProvider<RoadService> roadServiceProvider;

    /**
     * World 的可选提供者。
     * Optional provider for World.
     */
    private ObjectProvider<World> worldProvider;

    /**
     * 世界引导运行时桥的可选提供者。
     * Optional provider for the world-bootstrap runtime bridge.
     */
    private ObjectProvider<GameWorldBootstrapRuntimeBridge> runtimeBridgeProvider;

    /**
     * 默认构造：使用当前控制台进度报告器。
     * Default constructor: uses the current-console progress reporter.
     */
    public GameWorldBootstrapGateway() {
        this(ConsoleStartupProgressReporter.forCurrentConsole());
    }

    /**
     * 使用指定进度报告器构造（包内/测试用）。
     * Construct with a given progress reporter (package/test use).
     *
     * @param progressReporter 启动进度报告器 / Startup progress reporter
     */
    GameWorldBootstrapGateway(StartupProgressReporter progressReporter) {
        this.progressReporter = progressReporter;
    }

    /**
     * 注入 IDFactory 提供者。
     * Inject the IDFactory provider.
     *
     * @param idFactoryProvider IDFactory 提供者 / IDFactory provider
     */
    @Autowired(required = false)
    void setIdFactoryProvider(ObjectProvider<IDFactory> idFactoryProvider) {
        this.idFactoryProvider = idFactoryProvider;
    }

    /**
     * 注入 ZoneService 提供者。
     * Inject the ZoneService provider.
     *
     * @param zoneServiceProvider ZoneService 提供者 / ZoneService provider
     */
    @Autowired(required = false)
    void setZoneServiceProvider(ObjectProvider<ZoneService> zoneServiceProvider) {
        this.zoneServiceProvider = zoneServiceProvider;
    }

    /**
     * 注入 HotspotTeleportService 提供者。
     * Inject the HotspotTeleportService provider.
     *
     * @param hotspotTeleportServiceProvider HotspotTeleportService 提供者 / HotspotTeleportService provider
     */
    @Autowired(required = false)
    void setHotspotTeleportServiceProvider(ObjectProvider<HotspotTeleportService> hotspotTeleportServiceProvider) {
        this.hotspotTeleportServiceProvider = hotspotTeleportServiceProvider;
    }

    /**
     * 注入 RoadService 提供者。
     * Inject the RoadService provider.
     *
     * @param roadServiceProvider RoadService 提供者 / RoadService provider
     */
    @Autowired(required = false)
    void setRoadServiceProvider(ObjectProvider<RoadService> roadServiceProvider) {
        this.roadServiceProvider = roadServiceProvider;
    }

    /**
     * 注入 World 提供者。
     * Inject the World provider.
     *
     * @param worldProvider World 提供者 / World provider
     */
    @Autowired(required = false)
    void setWorldProvider(ObjectProvider<World> worldProvider) {
        this.worldProvider = worldProvider;
    }

    /**
     * 注入世界引导运行时桥提供者。
     * Inject the world-bootstrap runtime-bridge provider.
     *
     * @param runtimeBridgeProvider 运行时桥提供者 / Runtime-bridge provider
     */
    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameWorldBootstrapRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    /**
     * 引导世界：并行执行各引导步骤并报告进度。
     * Bootstrap the world: run bootstrap steps in parallel and report progress.
     */
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

    /**
     * 初始化 IDFactory。
     * Initialize IDFactory.
     */
    protected void initializeIDFactory() {
        idFactory();
    }

    /**
     * 加载 ZoneService。
     * Load ZoneService.
     */
    protected void loadZoneService() {
        zoneService().load(null);
    }

    /**
     * 初始化 HotspotTeleportService。
     * Initialize HotspotTeleportService.
     */
    protected void initializeHotspotTeleportService() {
        hotspotTeleportService();
    }

    /**
     * 初始化 RoadService。
     * Initialize RoadService.
     */
    protected void initializeRoadService() {
        roadService();
    }

    /**
     * 初始化 World。
     * Initialize World.
     */
    protected void initializeWorld() {
        world();
    }

    /**
     * 解析 IDFactory：优先 Spring，否则运行时桥。
     * Resolve IDFactory: prefer Spring, otherwise runtime bridge.
     *
     * @return IDFactory 实例 / IDFactory instance
     */
    private IDFactory idFactory() {
        if (idFactoryProvider == null) {
            return runtimeBridge().idFactory();
        }
        return idFactoryProvider.getIfAvailable(() -> runtimeBridge().idFactory());
    }

    /**
     * 解析 ZoneService：优先 Spring，否则运行时桥。
     * Resolve ZoneService: prefer Spring, otherwise runtime bridge.
     *
     * @return ZoneService 实例 / ZoneService instance
     */
    private ZoneService zoneService() {
        if (zoneServiceProvider == null) {
            return runtimeBridge().zoneService();
        }
        return zoneServiceProvider.getIfAvailable(() -> runtimeBridge().zoneService());
    }

    /**
     * 解析 HotspotTeleportService：优先 Spring，否则运行时桥。
     * Resolve HotspotTeleportService: prefer Spring, otherwise runtime bridge.
     *
     * @return HotspotTeleportService 实例 / HotspotTeleportService instance
     */
    private HotspotTeleportService hotspotTeleportService() {
        if (hotspotTeleportServiceProvider == null) {
            return runtimeBridge().hotspotTeleportService();
        }
        return hotspotTeleportServiceProvider.getIfAvailable(() -> runtimeBridge().hotspotTeleportService());
    }

    /**
     * 解析 RoadService：优先 Spring，否则运行时桥。
     * Resolve RoadService: prefer Spring, otherwise runtime bridge.
     *
     * @return RoadService 实例 / RoadService instance
     */
    private RoadService roadService() {
        if (roadServiceProvider == null) {
            return runtimeBridge().roadService();
        }
        return roadServiceProvider.getIfAvailable(() -> runtimeBridge().roadService());
    }

    /**
     * 解析 World：优先 Spring，否则运行时桥。
     * Resolve World: prefer Spring, otherwise runtime bridge.
     *
     * @return World 实例 / World instance
     */
    private World world() {
        if (worldProvider == null) {
            return runtimeBridge().world();
        }
        return worldProvider.getIfAvailable(() -> runtimeBridge().world());
    }

    /**
     * 解析世界引导运行时桥：优先 Spring，否则新建。
     * Resolve the world-bootstrap runtime bridge: prefer Spring, otherwise create new.
     *
     * @return 运行时桥 / Runtime bridge
     */
    private GameWorldBootstrapRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameWorldBootstrapRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameWorldBootstrapRuntimeBridge::new);
    }

    /**
     * 执行单步加载并报告开始/结束。
     * Run a single load step and report start/finish.
     *
     * @param stepName 步骤名 / Step name
     * @param loader 加载逻辑 / Loader logic
     */
    private void loadStep(String stepName, Runnable loader) {
        progressReporter.stepStarted(stepName);
        loader.run();
        progressReporter.stepFinished(stepName);
    }

    /**
     * 用虚拟线程并行执行引导步骤。
     * Run bootstrap steps in parallel with virtual threads.
     *
     * @param steps 引导步骤列表 / Bootstrap step list
     */
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

    /**
     * 等待 Future 完成并展开异常。
     * Await a Future and unwrap exceptions.
     *
     * @param future 待等待的 Future / Future to await
     */
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

    /**
     * 引导步骤：名称与加载逻辑。
     * Bootstrap step: name and loader.
     *
     * @param name 步骤名 / Step name
     * @param loader 加载逻辑 / Loader logic
     */
    private record BootstrapStep(String name, Runnable loader) {
    }
}
