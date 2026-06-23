package com.aionemu.gameserver.lifecycle;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import org.springframework.stereotype.Component;

@Component
public class GameWorldActivationGateway {

    public GameServer activate() {
        DropRegistrationService.getInstance();
        GameServer server = new GameServer();
        GameServer.activateServer(server);
        DAOManager.getDAO(PlayerDAO.class).setPlayersOffline(false);
        return server;
    }
}
