package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.path.PathService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 地理与 PATH 网关：初始化地理数据和真实 PATH。
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
     * 可选注入地理服务提供者。
     * Optionally inject the geo-service provider.
     * @param geoServiceProvider 地理服务提供者 / Geo-service provider
     */
    @Autowired(required = false)
    void setGeoServiceProvider(ObjectProvider<GeoService> geoServiceProvider) {
        this.geoServiceProvider = geoServiceProvider;
    }

    /**
     * 可选注入 PATH 服务提供者。
     * Optionally inject the path-service provider.
     * @param pathServiceProvider PATH 服务提供者 / Path-service provider
     */
    @Autowired(required = false)
    void setPathServiceProvider(ObjectProvider<PathService> pathServiceProvider) {
        this.pathServiceProvider = pathServiceProvider;
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
     * @return 地理服务 / Geo service
     */
    private GeoService geoService() {
        return geoServiceProvider.getObject();
    }

    /**
     * 解析 PATH 服务。
     * Resolve the path service.
     * @return PATH 服务 / Path service
     */
    private PathService pathService() {
        return pathServiceProvider.getObject();
    }

}
