package com.aionemu.boot.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LegacyNioTransportLifecycle {

    private static final Logger log = LoggerFactory.getLogger(LegacyNioTransportLifecycle.class);

    public void start() {
        log.info("Using legacy NIO transport managed by existing game/login/chat startup code.");
    }

    public void stop() {
        log.info("Legacy NIO transport remains managed by existing game/login/chat shutdown code.");
    }
}
