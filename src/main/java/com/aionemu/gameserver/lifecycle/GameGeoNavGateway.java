package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.nav.NavService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 地理导航网关：初始化地理数据与导航网格。
 * Geo/nav gateway: initializes geodata and navigation meshes.
 */
@Component
public class GameGeoNavGateway {

    /**
     * 地理服务提供者。
     * Geo-service provider.
     */
    private ObjectProvider<GeoService> geoServiceProvider;
    /**
     * 导航服务提供者。
     * Nav-service provider.
     */
    private ObjectProvider<NavService> navServiceProvider;
    /**
     * 世界服务运行时桥接提供者。
     * World-services runtime-bridge provider.
     */
    private ObjectProvider<GameWorldServicesRuntimeBridge> runtimeBridgeProvider;

    /**
     * 可选注入地理服务提供者。
     * Optionally inject the geo-service provider.
     *
     * @param geoServiceProvider 地理服务提供者 / Geo-service provider
     */
    @Autowired(required = false)
    void setGeoServiceProvider(ObjectProvider<GeoService> geoServiceProvider) {
        this.geoServiceProvider = geoServiceProvider;
    }

    /**
     * 可选注入导航服务提供者。
     * Optionally inject the nav-service provider.
     *
     * @param navServiceProvider 导航服务提供者 / Nav-service provider
     */
    @Autowired(required = false)
    void setNavServiceProvider(ObjectProvider<NavService> navServiceProvider) {
        this.navServiceProvider = navServiceProvider;
    }

    /**
     * 可选注入世界服务运行时桥接提供者。
     * Optionally inject the world-services runtime-bridge provider.
     *
     * @param runtimeBridgeProvider 运行时桥接提供者 / Runtime-bridge provider
     */
    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameWorldServicesRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    /**
     * 初始化地理数据与导航。
     * Initialize geodata and navigation.
     */
    public void initialize() {
        Util.printSection(I18n.get("console.section.geodata"));
        geoService().initializeGeo();
        navService().initializeNav();
    }

    /**
     * 解析地理服务。
     * Resolve the geo service.
     *
     * Geo service
     */
    private GeoService geoService() {
        if (geoServiceProvider == null) {
            return runtimeBridge().geoService();
        }
        return geoServiceProvider.getIfAvailable(() -> runtimeBridge().geoService());
    }

    /**
     * 解析导航服务。
     * Resolve the nav service.
     *
     * Nav service
     */
    private NavService navService() {
        if (navServiceProvider == null) {
            return runtimeBridge().navService();
        }
        return navServiceProvider.getIfAvailable(() -> runtimeBridge().navService());
    }

    /**
     * 解析世界服务运行时桥接。
     * Resolve the world-services runtime bridge.
     *
     * @return 运行时桥接 / Runtime bridge
     */
    private GameWorldServicesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameWorldServicesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameWorldServicesRuntimeBridge::new);
    }
}
