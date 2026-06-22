package com.aionemu.loginserver.taskmanager.handler.implementations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionRuntimeMode;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LoginTaskShutdownHandlerTest {

    @AfterEach
    void resetEmbeddedMode() {
        System.clearProperty(AionRuntimeMode.BOOT_EMBEDDED_PROPERTY);
        AionEmbeddedShutdownHandler.clear();
    }

    @Test
    void embeddedShutdownTaskRequestsBootShutdown() {
        AionRuntimeMode.enableBootEmbeddedMode();
        AtomicInteger shutdownRequests = new AtomicInteger();
        AionEmbeddedShutdownHandler.register(shutdownRequests::incrementAndGet);

        ShutdownHandler handler = new ShutdownHandler();
        handler.setTaskId(1);
        handler.trigger();

        assertEquals(1, shutdownRequests.get());
    }

    @Test
    void embeddedRestartTaskRequestsBootShutdown() {
        AionRuntimeMode.enableBootEmbeddedMode();
        AtomicInteger shutdownRequests = new AtomicInteger();
        AionEmbeddedShutdownHandler.register(shutdownRequests::incrementAndGet);

        RestartHandler handler = new RestartHandler();
        handler.setTaskId(2);
        handler.trigger();

        assertEquals(1, shutdownRequests.get());
    }
}
