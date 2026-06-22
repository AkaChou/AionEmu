package com.aionemu.commons.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AionEmbeddedShutdownHandlerTest {

    @AfterEach
    void clearHandler() {
        AionEmbeddedShutdownHandler.clear();
    }

    @Test
    void requestShutdownDefaultsToShutdownMode() {
        AtomicReference<AionEmbeddedShutdownMode> requestedMode = new AtomicReference<>();
        AionEmbeddedShutdownHandler.register(requestedMode::set);

        assertTrue(AionEmbeddedShutdownHandler.requestShutdown());

        assertEquals(AionEmbeddedShutdownMode.SHUTDOWN, requestedMode.get());
    }

    @Test
    void requestShutdownCarriesRestartMode() {
        AtomicReference<AionEmbeddedShutdownMode> requestedMode = new AtomicReference<>();
        AionEmbeddedShutdownHandler.register(requestedMode::set);

        assertTrue(AionEmbeddedShutdownHandler.requestShutdown(AionEmbeddedShutdownMode.RESTART));

        assertEquals(AionEmbeddedShutdownMode.RESTART, requestedMode.get());
    }
}
