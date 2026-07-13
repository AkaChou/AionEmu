package com.aionemu.gameserver.lifecycle;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.path.PathService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 世界服务运行时桥：解析 Geo/Path/Drop，并创建/激活 GameServer、标记玩家离线。
 * World-services runtime bridge: resolves Geo/Path/Drop, creates/activates GameServer, marks players offline.
 */
@Component
public class GameWorldServicesRuntimeBridge {

    /**
     * GeoService 的可选提供者。
     * Optional provider for GeoService.
     */
    private ObjectProvider<GeoService> geoServiceProvider;

    /**
     * PathService 的可选提供者。
     * Optional provider for PathService.
     */
    private ObjectProvider<PathService> pathServiceProvider;

    /**
     * DropRegistrationService 的可选提供者。
     * Optional provider for DropRegistrationService.
     */
    private ObjectProvider<DropRegistrationService> dropRegistrationServiceProvider;

    /**
     * 注入 GeoService 提供者。
     * Inject the GeoService provider.
     *
     * GeoService provider
     */
    @Autowired(required = false)
    void setGeoServiceProvider(ObjectProvider<GeoService> geoServiceProvider) {
        this.geoServiceProvider = geoServiceProvider;
    }

    /**
     * 注入 PathService 提供者。
     * Inject the PathService provider.
     *
     * PathService provider
     */
    @Autowired(required = false)
    void setPathServiceProvider(ObjectProvider<PathService> pathServiceProvider) {
        this.pathServiceProvider = pathServiceProvider;
    }

    /**
     * 注入 DropRegistrationService 提供者。
     * Inject the DropRegistrationService provider.
     *
     * DropRegistrationService provider
     */
    @Autowired(required = false)
    void setDropRegistrationServiceProvider(ObjectProvider<DropRegistrationService> dropRegistrationServiceProvider) {
        this.dropRegistrationServiceProvider = dropRegistrationServiceProvider;
    }

    /**
     * 解析 GeoService：优先 Spring，否则回退。
     * Resolve GeoService: prefer Spring, otherwise fallback.
     *
     * GeoService instance
     */
    public GeoService geoService() {
        if (geoServiceProvider == null) {
            return GameWorldServiceFallbacks.geoService();
        }
        return geoServiceProvider.getIfAvailable(GameWorldServiceFallbacks::geoService);
    }

    /**
     * 解析 PathService：优先 Spring，否则回退。
     * Resolve PathService: prefer Spring, otherwise fallback.
     *
     * PathService instance
     */
    public PathService pathService() {
        if (pathServiceProvider == null) {
            return GameWorldServiceFallbacks.pathService();
        }
        return pathServiceProvider.getIfAvailable(GameWorldServiceFallbacks::pathService);
    }

    /**
     * 解析 DropRegistrationService：优先 Spring，否则回退。
     * Resolve DropRegistrationService: prefer Spring, otherwise fallback.
     *
     * DropRegistrationService instance
     */
    public DropRegistrationService dropRegistrationService() {
        if (dropRegistrationServiceProvider == null) {
            return GameWorldServiceFallbacks.dropRegistrationService();
        }
        return dropRegistrationServiceProvider.getIfAvailable(GameWorldServiceFallbacks::dropRegistrationService);
    }

    /**
     * 创建新的 GameServer 实例。
     * Create a new GameServer instance.
     *
     * New GameServer
     */
    public GameServer createGameServer() {
        return new GameServer();
    }

    /**
     * 激活给定的 GameServer。
     * Activate the given GameServer.
     *
     * @param server 待激活的服务器 / Server to activate
     */
    public void activateGameServer(GameServer server) {
        GameServer.activateServer(server);
    }

    /**
     * 将所有玩家标记为离线。
     * Mark all players as offline.
     */
    public void markPlayersOffline() {
        DAOManager.getDAO(PlayerDAO.class).setPlayersOffline(false);
    }
}
