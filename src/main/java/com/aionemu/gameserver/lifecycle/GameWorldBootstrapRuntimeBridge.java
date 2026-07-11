package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.RoadService;
import com.aionemu.gameserver.services.teleport.HotspotTeleportService;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.zone.ZoneService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 世界引导运行时桥：解析 IDFactory / Zone / Hotspot / Road / World。
 * Zone / Hotspot / Road / World. / Zone / Hotspot / Road / World.
 */
@Component
public class GameWorldBootstrapRuntimeBridge {

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
     * 注入 IDFactory 提供者。
     * Inject the IDFactory provider.
     *
     * IDFactory provider
     */
    @Autowired(required = false)
    void setIdFactoryProvider(ObjectProvider<IDFactory> idFactoryProvider) {
        this.idFactoryProvider = idFactoryProvider;
    }

    /**
     * 注入 ZoneService 提供者。
     * Inject the ZoneService provider.
     *
     * ZoneService provider
     */
    @Autowired(required = false)
    void setZoneServiceProvider(ObjectProvider<ZoneService> zoneServiceProvider) {
        this.zoneServiceProvider = zoneServiceProvider;
    }

    /**
     * 注入 HotspotTeleportService 提供者。
     * Inject the HotspotTeleportService provider.
     *
     * HotspotTeleportService provider
     */
    @Autowired(required = false)
    void setHotspotTeleportServiceProvider(ObjectProvider<HotspotTeleportService> hotspotTeleportServiceProvider) {
        this.hotspotTeleportServiceProvider = hotspotTeleportServiceProvider;
    }

    /**
     * 注入 RoadService 提供者。
     * Inject the RoadService provider.
     *
     * RoadService provider
     */
    @Autowired(required = false)
    void setRoadServiceProvider(ObjectProvider<RoadService> roadServiceProvider) {
        this.roadServiceProvider = roadServiceProvider;
    }

    /**
     * 注入 World 提供者。
     * Inject the World provider.
     *
     * World provider
     */
    @Autowired(required = false)
    void setWorldProvider(ObjectProvider<World> worldProvider) {
        this.worldProvider = worldProvider;
    }

    /**
     * 解析 IDFactory：优先 Spring，否则回退。
     * Resolve IDFactory: prefer Spring, otherwise fallback.
     *
     * IDFactory instance
     */
    public IDFactory idFactory() {
        if (idFactoryProvider == null) {
            return GameWorldBootstrapFallbacks.idFactory();
        }
        return idFactoryProvider.getIfAvailable(GameWorldBootstrapFallbacks::idFactory);
    }

    /**
     * 解析 ZoneService：优先 Spring，否则回退。
     * Resolve ZoneService: prefer Spring, otherwise fallback.
     *
     * ZoneService instance
     */
    public ZoneService zoneService() {
        if (zoneServiceProvider == null) {
            return GameWorldBootstrapFallbacks.zoneService();
        }
        return zoneServiceProvider.getIfAvailable(GameWorldBootstrapFallbacks::zoneService);
    }

    /**
     * 解析 HotspotTeleportService：优先 Spring，否则回退。
     * Resolve HotspotTeleportService: prefer Spring, otherwise fallback.
     *
     * HotspotTeleportService instance
     */
    public HotspotTeleportService hotspotTeleportService() {
        if (hotspotTeleportServiceProvider == null) {
            return GameWorldBootstrapFallbacks.hotspotTeleportService();
        }
        return hotspotTeleportServiceProvider.getIfAvailable(GameWorldBootstrapFallbacks::hotspotTeleportService);
    }

    /**
     * 解析 RoadService：优先 Spring，否则回退。
     * Resolve RoadService: prefer Spring, otherwise fallback.
     *
     * RoadService instance
     */
    public RoadService roadService() {
        if (roadServiceProvider == null) {
            return GameWorldBootstrapFallbacks.roadService();
        }
        return roadServiceProvider.getIfAvailable(GameWorldBootstrapFallbacks::roadService);
    }

    /**
     * 解析 World：优先 Spring，否则回退。
     * Resolve World: prefer Spring, otherwise fallback.
     *
     * World instance
     */
    public World world() {
        if (worldProvider == null) {
            return GameWorldBootstrapFallbacks.world();
        }
        return worldProvider.getIfAvailable(GameWorldBootstrapFallbacks::world);
    }
}
