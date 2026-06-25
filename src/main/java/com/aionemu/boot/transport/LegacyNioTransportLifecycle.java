package com.aionemu.boot.transport;

import com.aionemu.boot.config.AionServicesProperties.TransportMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Deprecated(forRemoval = true)
@Component
public class LegacyNioTransportLifecycle implements AionTransportLifecycle {

    private static final Logger log = LoggerFactory.getLogger(LegacyNioTransportLifecycle.class);

    @Override
    public TransportMode mode() {
        return TransportMode.LEGACY_NIO;
    }

    @Override
    public boolean nettyEnabled() {
        return false;
    }

    @Override
    public void start() {
        log.info("Using legacy NIO transport managed by existing game/login/chat startup code.");
    }

    @Override
    public void stop() {
        log.info("Legacy NIO transport remains managed by existing game/login/chat shutdown code.");
    }
}
