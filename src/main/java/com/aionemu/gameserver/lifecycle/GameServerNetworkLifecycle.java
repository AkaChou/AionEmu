package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.GameServer;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class GameServerNetworkLifecycle {

    private final Consumer<GameServer> serverStarter;

    public GameServerNetworkLifecycle() {
        this(GameServer::startServers);
    }

    GameServerNetworkLifecycle(Consumer<GameServer> serverStarter) {
        this.serverStarter = serverStarter;
    }

    public void start(GameServer server) {
        serverStarter.accept(server);
    }
}
