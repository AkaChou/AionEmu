package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.nav.NavData;
import com.aionemu.gameserver.world.geo.nav.NavService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 世界服务定位器：向 Geo / Nav / Drop 相关组件注入 Spring 提供者。
 * Nav / Drop related components.
 */
@Component
public final class GameWorldServices implements DisposableBean {

    /**
     * GeoService 提供者的静态缓存。
     * Static cache of the GeoService provider.
     */
    private static volatile ObjectProvider<GeoService> geoServiceProvider;

    /**
     * NavService 提供者的静态缓存。
     * Static cache of the NavService provider.
     */
    private static volatile ObjectProvider<NavService> navServiceProvider;

    /**
     * NavData 提供者的静态缓存。
     * Static cache of the NavData provider.
     */
    private static volatile ObjectProvider<NavData> navDataProvider;

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
     * NavService provider
     * NavData provider
     * DropRegistrationService provider
     */
    public GameWorldServices(ObjectProvider<GeoService> geoServiceProvider, ObjectProvider<NavService> navServiceProvider,
            ObjectProvider<NavData> navDataProvider,
            ObjectProvider<DropRegistrationService> dropRegistrationServiceProvider) {
        GameWorldServices.geoServiceProvider = geoServiceProvider;
        GameWorldServices.navServiceProvider = navServiceProvider;
        GameWorldServices.navDataProvider = navDataProvider;
        GameWorldServices.dropRegistrationServiceProvider = dropRegistrationServiceProvider;
        GeoService.setInstanceProvider(geoServiceProvider);
        NavService.setInstanceProvider(navServiceProvider);
        NavData.setInstanceProvider(navDataProvider);
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
     * 解析 NavService：优先 Spring，否则回退。
     * Resolve NavService: prefer Spring, otherwise fallback.
     *
     * NavService instance
     */
    public static NavService navService() {
        ObjectProvider<NavService> provider = navServiceProvider;
        if (provider == null) {
            return GameWorldServiceFallbacks.navService();
        }
        return provider.getIfAvailable(GameWorldServiceFallbacks::navService);
    }

    /**
     * 解析 NavData：优先 Spring，否则回退。
     * Resolve NavData: prefer Spring, otherwise fallback.
     *
     * NavData instance
     */
    public static NavData navData() {
        ObjectProvider<NavData> provider = navDataProvider;
        if (provider == null) {
            return GameWorldServiceFallbacks.navData();
        }
        return provider.getIfAvailable(GameWorldServiceFallbacks::navData);
    }

    /**
     * 销毁时清空静态提供者与单例注册。
     * Clear static providers and singleton registrations on destroy.
     */
    @Override
    public void destroy() {
        geoServiceProvider = null;
        navServiceProvider = null;
        navDataProvider = null;
        dropRegistrationServiceProvider = null;
        GeoService.setInstanceProvider(null);
        NavService.setInstanceProvider(null);
        NavData.setInstanceProvider(null);
        DropRegistrationService.setInstanceProvider(null);
    }
}
