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
 * 世界引导服务静态绑定：把 Spring ObjectProvider 喂入世界引导域服务自持的 fail-fast 单例通道。
 * World-bootstrap static bindings: feeds the Spring ObjectProviders into the fail-fast singleton accessors
 * held by the World-bootstrap services.
 * <p>本组件取代已退役的世界引导静态定位器（R5 批次）：不再暴露任何静态访问方法，
 * 消费方一律走域服务自持的 {@code getInstance()} 通道。
 * Replaces the retired World-bootstrap static locator: no static accessor is exposed anymore;
 * consumers use each domain service's fail-fast {@code getInstance()} channel.</p>
 */
@Component
public final class WorldBootstrapServiceBindings implements DisposableBean {

    /**
     * 构造并注册各世界引导服务的实例提供者。
     * Construct and register instance providers for World-bootstrap services.
     * @param idFactoryProvider IDFactory 提供者 / IDFactory provider
     * @param zoneServiceProvider ZoneService 提供者 / ZoneService provider
     * @param hotspotTeleportServiceProvider HotspotTeleportService 提供者 / HotspotTeleportService provider
     * @param roadServiceProvider RoadService 提供者 / RoadService provider
     * @param worldProvider World 提供者 / World provider
     */
    public WorldBootstrapServiceBindings(ObjectProvider<IDFactory> idFactoryProvider,
            ObjectProvider<ZoneService> zoneServiceProvider,
            ObjectProvider<HotspotTeleportService> hotspotTeleportServiceProvider,
            ObjectProvider<RoadService> roadServiceProvider, ObjectProvider<World> worldProvider) {
        IDFactory.setInstanceProvider(idFactoryProvider);
        ZoneService.setInstanceProvider(zoneServiceProvider);
        HotspotTeleportService.setInstanceProvider(hotspotTeleportServiceProvider);
        RoadService.setInstanceProvider(roadServiceProvider);
        World.setInstanceProvider(worldProvider);
    }

    /**
     * 销毁时清空各域服务的静态提供者通道。
     * Clears each domain service's static provider channel on destroy.
     */
    @Override
    public void destroy() {
        IDFactory.setInstanceProvider(null);
        ZoneService.setInstanceProvider(null);
        HotspotTeleportService.setInstanceProvider(null);
        RoadService.setInstanceProvider(null);
        World.setInstanceProvider(null);
    }
}
