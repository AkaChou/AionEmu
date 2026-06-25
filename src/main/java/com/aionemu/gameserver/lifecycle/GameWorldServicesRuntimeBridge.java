package com.aionemu.gameserver.lifecycle;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.nav.NavService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class GameWorldServicesRuntimeBridge {

    private ObjectProvider<GeoService> geoServiceProvider;
    private ObjectProvider<NavService> navServiceProvider;
    private ObjectProvider<DropRegistrationService> dropRegistrationServiceProvider;

    @Autowired(required = false)
    void setGeoServiceProvider(ObjectProvider<GeoService> geoServiceProvider) {
        this.geoServiceProvider = geoServiceProvider;
    }

    @Autowired(required = false)
    void setNavServiceProvider(ObjectProvider<NavService> navServiceProvider) {
        this.navServiceProvider = navServiceProvider;
    }

    @Autowired(required = false)
    void setDropRegistrationServiceProvider(ObjectProvider<DropRegistrationService> dropRegistrationServiceProvider) {
        this.dropRegistrationServiceProvider = dropRegistrationServiceProvider;
    }

    public GeoService geoService() {
        if (geoServiceProvider == null) {
            return GeoService.getInstance();
        }
        return geoServiceProvider.getIfAvailable(GeoService::getInstance);
    }

    public NavService navService() {
        if (navServiceProvider == null) {
            return NavService.getInstance();
        }
        return navServiceProvider.getIfAvailable(NavService::getInstance);
    }

    public DropRegistrationService dropRegistrationService() {
        if (dropRegistrationServiceProvider == null) {
            return DropRegistrationService.getInstance();
        }
        return dropRegistrationServiceProvider.getIfAvailable(DropRegistrationService::getInstance);
    }

    public GameServer createGameServer() {
        return new GameServer();
    }

    public void activateGameServer(GameServer server) {
        GameServer.activateServer(server);
    }

    public void markPlayersOffline() {
        DAOManager.getDAO(PlayerDAO.class).setPlayersOffline(false);
    }
}
