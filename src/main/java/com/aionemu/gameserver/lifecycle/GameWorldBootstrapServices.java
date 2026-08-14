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
 * World-bootstrap service locator: injects Spring providers into IDFactory / Zone / Hotspot / Road / World.
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

    private static volatile IDFactory resolvedIdFactory;
    private static volatile ZoneService resolvedZoneService;
    private static volatile HotspotTeleportService resolvedHotspotTeleportService;
    private static volatile World resolvedWorld;

    /**
     * 构造并注册各世界引导组件的实例提供者。
     * Construct and register instance providers for world-bootstrap components.
     *
     * @param idFactoryProvider IDFactory 提供者 / IDFactory provider
     * @param zoneServiceProvider ZoneService 提供者 / ZoneService provider
     * @param hotspotTeleportServiceProvider HotspotTeleportService 提供者 / HotspotTeleportService provider
     * @param roadServiceProvider RoadService 提供者 / RoadService provider
     * @param worldProvider World 提供者 / World provider
     */
    public GameWorldBootstrapServices(ObjectProvider<IDFactory> idFactoryProvider,
            ObjectProvider<ZoneService> zoneServiceProvider,
            ObjectProvider<HotspotTeleportService> hotspotTeleportServiceProvider,
            ObjectProvider<RoadService> roadServiceProvider, ObjectProvider<World> worldProvider) {
        GameWorldBootstrapServices.idFactoryProvider = idFactoryProvider;
        GameWorldBootstrapServices.zoneServiceProvider = zoneServiceProvider;
        GameWorldBootstrapServices.hotspotTeleportServiceProvider = hotspotTeleportServiceProvider;
        GameWorldBootstrapServices.worldProvider = worldProvider;
        resolvedIdFactory = null;
        resolvedZoneService = null;
        resolvedHotspotTeleportService = null;
        resolvedWorld = null;
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
     * @return IDFactory 实例 / IDFactory instance
     */
    public static IDFactory idFactory() {
        IDFactory resolved = resolvedIdFactory;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<IDFactory> provider = idFactoryProvider;
        resolved = provider == null ? GameWorldBootstrapFallbacks.idFactory()
                : provider.getIfAvailable(GameWorldBootstrapFallbacks::idFactory);
        resolvedIdFactory = resolved;
        return resolved;
    }

    /**
     * 解析 ZoneService：优先 Spring，否则回退。
     * Resolve ZoneService: prefer Spring, otherwise fallback.
     *
     * @return ZoneService 实例 / ZoneService instance
     */
    public static ZoneService zoneService() {
        ZoneService resolved = resolvedZoneService;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<ZoneService> provider = zoneServiceProvider;
        resolved = provider == null ? GameWorldBootstrapFallbacks.zoneService()
                : provider.getIfAvailable(GameWorldBootstrapFallbacks::zoneService);
        resolvedZoneService = resolved;
        return resolved;
    }

    /**
     * 解析 HotspotTeleportService：优先 Spring，否则回退。
     * Resolve HotspotTeleportService: prefer Spring, otherwise fallback.
     *
     * @return HotspotTeleportService 实例 / HotspotTeleportService instance
     */
    public static HotspotTeleportService hotspotTeleportService() {
        HotspotTeleportService resolved = resolvedHotspotTeleportService;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<HotspotTeleportService> provider = hotspotTeleportServiceProvider;
        resolved = provider == null ? GameWorldBootstrapFallbacks.hotspotTeleportService()
                : provider.getIfAvailable(GameWorldBootstrapFallbacks::hotspotTeleportService);
        resolvedHotspotTeleportService = resolved;
        return resolved;
    }

    /**
     * 解析 World：优先 Spring，否则回退。
     * Resolve World: prefer Spring, otherwise fallback.
     *
     * @return World 实例 / World instance
     */
    public static World world() {
        World resolved = resolvedWorld;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<World> provider = worldProvider;
        resolved = provider == null ? GameWorldBootstrapFallbacks.world()
                : provider.getIfAvailable(GameWorldBootstrapFallbacks::world);
        resolvedWorld = resolved;
        return resolved;
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
        resolvedIdFactory = null;
        resolvedZoneService = null;
        resolvedHotspotTeleportService = null;
        resolvedWorld = null;
        IDFactory.setInstanceProvider(null);
        ZoneService.setInstanceProvider(null);
        HotspotTeleportService.setInstanceProvider(null);
        RoadService.setInstanceProvider(null);
        World.setInstanceProvider(null);
    }
}
