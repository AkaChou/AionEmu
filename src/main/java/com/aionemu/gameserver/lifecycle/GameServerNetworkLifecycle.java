package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.GameServer;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class GameServerNetworkLifecycle {

    private final Consumer<GameServer> serverStarter;
    private final Runnable serverStopper;

    public GameServerNetworkLifecycle() {
        this(GameServer::startServers, GameServer::stop);
    }

    GameServerNetworkLifecycle(Consumer<GameServer> serverStarter, Runnable serverStopper) {
        this.serverStarter = serverStarter;
        this.serverStopper = serverStopper;
    }

    public void start(GameServer server) {
        serverStarter.accept(server);
    }

    public void stop() {
        serverStopper.run();
    }
}
