package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.path.PathService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 地理与 PATH 网关：初始化地理数据和真端 PATH。
 * Geo/path gateway: initializes geodata and retail path data.
 */
@Component
public class GameGeoPathGateway {

    /**
     * 地理服务提供者。
     * Geo-service provider.
     */
    private ObjectProvider<GeoService> geoServiceProvider;
    /**
     * PATH 服务提供者。
     * Path-service provider.
     */
    private ObjectProvider<PathService> pathServiceProvider;
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
     * 可选注入 PATH 服务提供者。
     * Optionally inject the path-service provider.
     *
     * @param pathServiceProvider PATH 服务提供者 / Path-service provider
     */
    @Autowired(required = false)
    void setPathServiceProvider(ObjectProvider<PathService> pathServiceProvider) {
        this.pathServiceProvider = pathServiceProvider;
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
     * 初始化地理数据与 PATH。
     * Initialize geodata and path data.
     */
    public void initialize() {
        Util.printSection(I18n.get("console.section.geodata"));
        geoService().initializeGeo();
        pathService().initializePath();
    }

    /**
     * 解析地理服务。
     * Resolve the geo service.
     *
     * @return 地理服务 / Geo service
     */
    private GeoService geoService() {
        if (geoServiceProvider == null) {
            return runtimeBridge().geoService();
        }
        return geoServiceProvider.getIfAvailable(() -> runtimeBridge().geoService());
    }

    /**
     * 解析 PATH 服务。
     * Resolve the path service.
     *
     * @return PATH 服务 / Path service
     */
    private PathService pathService() {
        if (pathServiceProvider == null) {
            return runtimeBridge().pathService();
        }
        return pathServiceProvider.getIfAvailable(() -> runtimeBridge().pathService());
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
