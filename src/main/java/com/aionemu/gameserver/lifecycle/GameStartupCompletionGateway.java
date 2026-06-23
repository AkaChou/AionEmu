package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.GameServer;
import org.springframework.stereotype.Component;

@Component
public class GameStartupCompletionGateway {

    public void logStartupComplete(long startupTime) {
        GameServer.log.info("=== Server initialization COMPLETE ===");
        GameServer.log.info("Total initialization time: {} seconds", startupTime);
        GameServer.log.info("Server is now ready to accept connections");
    }

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
