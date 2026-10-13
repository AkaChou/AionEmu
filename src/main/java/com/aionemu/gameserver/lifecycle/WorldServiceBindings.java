package com.aionemu.gameserver.lifecycle;


import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.path.PathService;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 世界服务静态绑定：把 Spring ObjectProvider 喂入世界域服务自持的 fail-fast 单例通道。
 * World-service static bindings: feeds the Spring ObjectProviders into the fail-fast singleton
 * accessors held by the world services.
 * <p>本组件取代已退役的世界服务静态定位器（R4 批次）：不再暴露任何静态访问方法，
 * 消费方一律走域服务自持的 {@code getInstance()} 通道。
 * Replaces the retired world-service static locator: no static accessor is exposed anymore;
 * consumers use each domain service's fail-fast {@code getInstance()} channel.</p>
 */
@Component
public final class WorldServiceBindings implements DisposableBean {

    /**
     * 构造并注册各世界服务的实例提供者。
     * Construct and register instance providers for world services.
     * @param geoServiceProvider GeoService 提供者 / GeoService provider
     * @param pathServiceProvider PathService 提供者 / PathService provider
     * @param dropRegistrationServiceProvider DropRegistrationService 提供者 / DropRegistrationService provider
     */
    public WorldServiceBindings(ObjectProvider<GeoService> geoServiceProvider,
            ObjectProvider<PathService> pathServiceProvider,
            ObjectProvider<DropRegistrationService> dropRegistrationServiceProvider) {
        GeoService.setInstanceProvider(geoServiceProvider);
        PathService.setInstanceProvider(pathServiceProvider);
        DropRegistrationService.setInstanceProvider(dropRegistrationServiceProvider);
    }

    /**
     * 销毁时清空各域服务的静态提供者通道。
     * Clears each domain service's static provider channel on destroy.
     */
    @Override
    public void destroy() {
        GeoService.setInstanceProvider(null);
        PathService.setInstanceProvider(null);
        DropRegistrationService.setInstanceProvider(null);
    }
}
