package com.aionemu.gameserver.lifecycle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GameStartupLogGateway {

    public long start() {
        long startupTimeMillis = System.currentTimeMillis();
        log.info("GameServer starting...");
        return startupTimeMillis;
    }
}
