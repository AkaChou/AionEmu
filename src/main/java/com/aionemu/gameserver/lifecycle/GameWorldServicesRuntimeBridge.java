package com.aionemu.gameserver.lifecycle;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.nav.NavService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class GameWorldServicesRuntimeBridge {

    public GeoService geoService() {
        return GeoService.getInstance();
    }

    public NavService navService() {
        return NavService.getInstance();
    }

    public DropRegistrationService dropRegistrationService() {
        return DropRegistrationService.getInstance();
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
