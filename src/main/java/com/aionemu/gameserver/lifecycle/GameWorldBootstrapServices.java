package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.RoadService;
import com.aionemu.gameserver.services.teleport.HotspotTeleportService;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.zone.ZoneService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 世界引导服务定位器：向 IDFactory / Zone / Hotspot / Road / World 注入 Spring 提供者。
 * Zone / Hotspot / Road / World.
 */
@Component
public final class GameWorldBootstrapServices implements DisposableBean {

    /**
     * IDFactory 提供者的静态缓存。
     * Static cache of the IDFactory provider.
     */
    private static volatile ObjectProvider<IDFactory> idFactoryProvider;

    /**
     * ZoneService 提供者的静态缓存。
     * Static cache of the ZoneService provider.
     */
    private static volatile ObjectProvider<ZoneService> zoneServiceProvider;

    /**
     * HotspotTeleportService 提供者的静态缓存。
     * Static cache of the HotspotTeleportService provider.
     */
    private static volatile ObjectProvider<HotspotTeleportService> hotspotTeleportServiceProvider;

    /**
     * World 提供者的静态缓存。
     * Static cache of the World provider.
     */
    private static volatile ObjectProvider<World> worldProvider;

    /**
     * 构造并注册各世界引导组件的实例提供者。
     * Construct and register instance providers for world-bootstrap components.
     *
     * IDFactory provider
     * ZoneService provider
     * HotspotTeleportService provider
     * RoadService provider
     * World provider
     */
    public GameWorldBootstrapServices(ObjectProvider<IDFactory> idFactoryProvider,
            ObjectProvider<ZoneService> zoneServiceProvider,
            ObjectProvider<HotspotTeleportService> hotspotTeleportServiceProvider,
            ObjectProvider<RoadService> roadServiceProvider, ObjectProvider<World> worldProvider) {
        GameWorldBootstrapServices.idFactoryProvider = idFactoryProvider;
        GameWorldBootstrapServices.zoneServiceProvider = zoneServiceProvider;
        GameWorldBootstrapServices.hotspotTeleportServiceProvider = hotspotTeleportServiceProvider;
        GameWorldBootstrapServices.worldProvider = worldProvider;
        IDFactory.setInstanceProvider(idFactoryProvider);
        ZoneService.setInstanceProvider(zoneServiceProvider);
        HotspotTeleportService.setInstanceProvider(hotspotTeleportServiceProvider);
        RoadService.setInstanceProvider(roadServiceProvider);
        World.setInstanceProvider(worldProvider);
    }

    /**
     * 解析 IDFactory：优先 Spring，否则回退。
     * Resolve IDFactory: prefer Spring, otherwise fallback.
     *
     * IDFactory instance
     */
    public static IDFactory idFactory() {
        ObjectProvider<IDFactory> provider = idFactoryProvider;
        if (provider == null) {
            return GameWorldBootstrapFallbacks.idFactory();
        }
        return provider.getIfAvailable(GameWorldBootstrapFallbacks::idFactory);
    }

    /**
     * 解析 ZoneService：优先 Spring，否则回退。
     * Resolve ZoneService: prefer Spring, otherwise fallback.
     *
     * ZoneService instance
     */
    public static ZoneService zoneService() {
        ObjectProvider<ZoneService> provider = zoneServiceProvider;
        if (provider == null) {
            return GameWorldBootstrapFallbacks.zoneService();
        }
        return provider.getIfAvailable(GameWorldBootstrapFallbacks::zoneService);
    }

    /**
     * 解析 HotspotTeleportService：优先 Spring，否则回退。
     * Resolve HotspotTeleportService: prefer Spring, otherwise fallback.
     *
     * HotspotTeleportService instance
     */
    public static HotspotTeleportService hotspotTeleportService() {
        ObjectProvider<HotspotTeleportService> provider = hotspotTeleportServiceProvider;
        if (provider == null) {
            return GameWorldBootstrapFallbacks.hotspotTeleportService();
        }
        return provider.getIfAvailable(GameWorldBootstrapFallbacks::hotspotTeleportService);
    }

    /**
     * 解析 World：优先 Spring，否则回退。
     * Resolve World: prefer Spring, otherwise fallback.
     *
     * World instance
     */
    public static World world() {
        ObjectProvider<World> provider = worldProvider;
        if (provider == null) {
            return GameWorldBootstrapFallbacks.world();
        }
        return provider.getIfAvailable(GameWorldBootstrapFallbacks::world);
    }

    /**
     * 销毁时清空静态提供者与单例注册。
     * Clear static providers and singleton registrations on destroy.
     */
    @Override
    public void destroy() {
        idFactoryProvider = null;
        zoneServiceProvider = null;
        hotspotTeleportServiceProvider = null;
        worldProvider = null;
        IDFactory.setInstanceProvider(null);
        ZoneService.setInstanceProvider(null);
        HotspotTeleportService.setInstanceProvider(null);
        RoadService.setInstanceProvider(null);
        World.setInstanceProvider(null);
    }
}
