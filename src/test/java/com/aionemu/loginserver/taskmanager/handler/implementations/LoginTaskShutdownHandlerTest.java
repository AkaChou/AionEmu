package com.aionemu.loginserver.taskmanager.handler.implementations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionEmbeddedShutdownMode;
import com.aionemu.commons.utils.AionRuntimeMode;
import java.util.concurrent.atomic.AtomicReference;
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
        AtomicReference<AionEmbeddedShutdownMode> requestedMode = new AtomicReference<>();
        AionEmbeddedShutdownHandler.register(requestedMode::set);

        ShutdownHandler handler = new ShutdownHandler();
        handler.setTaskId(1);
        handler.trigger();

        assertEquals(AionEmbeddedShutdownMode.SHUTDOWN, requestedMode.get());
    }

    @Test
    void embeddedRestartTaskRequestsBootShutdown() {
        AionRuntimeMode.enableBootEmbeddedMode();
        AtomicReference<AionEmbeddedShutdownMode> requestedMode = new AtomicReference<>();
        AionEmbeddedShutdownHandler.register(requestedMode::set);

        RestartHandler handler = new RestartHandler();
        handler.setTaskId(2);
        handler.trigger();

        assertEquals(AionEmbeddedShutdownMode.RESTART, requestedMode.get());
    }
}
