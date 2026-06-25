package com.aionemu.gameserver.lifecycle;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameWorldActivationGateway {

    private ObjectProvider<DropRegistrationService> dropRegistrationServiceProvider;

    @Autowired(required = false)
    void setDropRegistrationServiceProvider(ObjectProvider<DropRegistrationService> dropRegistrationServiceProvider) {
        this.dropRegistrationServiceProvider = dropRegistrationServiceProvider;
    }

    public GameServer activate() {
        dropRegistrationService();
        GameServer server = new GameServer();
        GameServer.activateServer(server);
        DAOManager.getDAO(PlayerDAO.class).setPlayersOffline(false);
        return server;
    }

    private DropRegistrationService dropRegistrationService() {
        if (dropRegistrationServiceProvider == null) {
            return DropRegistrationService.getInstance();
        }
        return dropRegistrationServiceProvider.getIfAvailable(DropRegistrationService::getInstance);
    }
}
