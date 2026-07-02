package com.aionemu.gameserver.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GameStartupCompletionGateway {

    public void logStartupComplete(long startupTime) {
        log.info("=== Server initialization COMPLETE ===");
        log.info("Total initialization time: {} seconds", startupTime);
        log.info("Server is now ready to accept connections");
    }

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
