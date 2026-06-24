package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.GameServer;
import org.springframework.stereotype.Component;

@Component
public class GameStartupHooksGateway {

    public void start() {
        GameServer.runStartupHooks();
    }
}
