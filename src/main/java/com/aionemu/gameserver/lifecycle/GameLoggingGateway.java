package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.GameServer;
import org.springframework.stereotype.Component;

@Component
public class GameLoggingGateway {

    public void start() {
        GameServer.initializeLogger();
    }
}
