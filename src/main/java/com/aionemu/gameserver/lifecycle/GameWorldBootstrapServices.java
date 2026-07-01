package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.RoadService;
import com.aionemu.gameserver.services.teleport.HotspotTeleportService;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.zone.ZoneService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameWorldBootstrapServices implements DisposableBean {

    private static volatile ObjectProvider<IDFactory> idFactoryProvider;
    private static volatile ObjectProvider<ZoneService> zoneServiceProvider;
    private static volatile ObjectProvider<HotspotTeleportService> hotspotTeleportServiceProvider;
    private static volatile ObjectProvider<World> worldProvider;

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

    public static IDFactory idFactory() {
        ObjectProvider<IDFactory> provider = idFactoryProvider;
        if (provider == null) {
            return GameWorldBootstrapFallbacks.idFactory();
        }
        return provider.getIfAvailable(GameWorldBootstrapFallbacks::idFactory);
    }

    public static ZoneService zoneService() {
        ObjectProvider<ZoneService> provider = zoneServiceProvider;
        if (provider == null) {
            return GameWorldBootstrapFallbacks.zoneService();
        }
        return provider.getIfAvailable(GameWorldBootstrapFallbacks::zoneService);
    }

    public static HotspotTeleportService hotspotTeleportService() {
        ObjectProvider<HotspotTeleportService> provider = hotspotTeleportServiceProvider;
        if (provider == null) {
            return GameWorldBootstrapFallbacks.hotspotTeleportService();
        }
        return provider.getIfAvailable(GameWorldBootstrapFallbacks::hotspotTeleportService);
    }

    public static World world() {
        ObjectProvider<World> provider = worldProvider;
        if (provider == null) {
            return GameWorldBootstrapFallbacks.world();
        }
        return provider.getIfAvailable(GameWorldBootstrapFallbacks::world);
    }

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
