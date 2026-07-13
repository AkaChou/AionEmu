package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.path.PathService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 世界服务定位器：向 Geo / Path / Drop 相关组件注入 Spring 提供者。
 * Geo / Path / Drop related components.
 */
@Component
public final class GameWorldServices implements DisposableBean {

    /**
     * GeoService 提供者的静态缓存。
     * Static cache of the GeoService provider.
     */
    private static volatile ObjectProvider<GeoService> geoServiceProvider;

    /**
     * PathService 提供者的静态缓存。
     * Static cache of the PathService provider.
     */
    private static volatile ObjectProvider<PathService> pathServiceProvider;

    /**
     * DropRegistrationService 提供者的静态缓存。
     * Static cache of the DropRegistrationService provider.
     */
    private static volatile ObjectProvider<DropRegistrationService> dropRegistrationServiceProvider;

    /**
     * 构造并注册各世界服务组件的实例提供者。
     * Construct and register instance providers for world-service components.
     *
     * GeoService provider
     * PathService provider
     * DropRegistrationService provider
     */
    public GameWorldServices(ObjectProvider<GeoService> geoServiceProvider, ObjectProvider<PathService> pathServiceProvider,
            ObjectProvider<DropRegistrationService> dropRegistrationServiceProvider) {
        GameWorldServices.geoServiceProvider = geoServiceProvider;
        GameWorldServices.pathServiceProvider = pathServiceProvider;
        GameWorldServices.dropRegistrationServiceProvider = dropRegistrationServiceProvider;
        GeoService.setInstanceProvider(geoServiceProvider);
        PathService.setInstanceProvider(pathServiceProvider);
        DropRegistrationService.setInstanceProvider(dropRegistrationServiceProvider);
    }

    /**
     * 解析 GeoService：优先 Spring，否则回退。
     * Resolve GeoService: prefer Spring, otherwise fallback.
     *
     * GeoService instance
     */
    public static GeoService geoService() {
        ObjectProvider<GeoService> provider = geoServiceProvider;
        if (provider == null) {
            return GameWorldServiceFallbacks.geoService();
        }
        return provider.getIfAvailable(GameWorldServiceFallbacks::geoService);
    }

    /**
     * 解析 DropRegistrationService：优先 Spring，否则回退。
     * Resolve DropRegistrationService: prefer Spring, otherwise fallback.
     *
     * DropRegistrationService instance
     */
    public static DropRegistrationService dropRegistrationService() {
        ObjectProvider<DropRegistrationService> provider = dropRegistrationServiceProvider;
        if (provider == null) {
            return GameWorldServiceFallbacks.dropRegistrationService();
        }
        return provider.getIfAvailable(GameWorldServiceFallbacks::dropRegistrationService);
    }

    /**
     * 解析 PathService：优先 Spring，否则回退。
     * Resolve PathService: prefer Spring, otherwise fallback.
     *
     * PathService instance
     */
    public static PathService pathService() {
        ObjectProvider<PathService> provider = pathServiceProvider;
        if (provider == null) {
            return GameWorldServiceFallbacks.pathService();
        }
        return provider.getIfAvailable(GameWorldServiceFallbacks::pathService);
    }

    /**
     * 销毁时清空静态提供者与单例注册。
     * Clear static providers and singleton registrations on destroy.
     */
    @Override
    public void destroy() {
        geoServiceProvider = null;
        pathServiceProvider = null;
        dropRegistrationServiceProvider = null;
        GeoService.setInstanceProvider(null);
        PathService.setInstanceProvider(null);
        DropRegistrationService.setInstanceProvider(null);
    }
}
