package com.aionemu.chatserver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionEmbeddedShutdownMode;
import com.aionemu.commons.utils.AionRuntimeMode;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ChatRestartRequestTest {

    @AfterEach
    void resetEmbeddedMode() {
        System.clearProperty(AionRuntimeMode.BOOT_EMBEDDED_PROPERTY);
        AionEmbeddedShutdownHandler.clear();
    }

    @Test
    void embeddedRestartRequestsBootShutdown() {
        AionRuntimeMode.enableBootEmbeddedMode();
        AtomicReference<AionEmbeddedShutdownMode> requestedMode = new AtomicReference<>();
        AionEmbeddedShutdownHandler.register(requestedMode::set);

        ChatRestartRequest.requestRestart();

        assertEquals(AionEmbeddedShutdownMode.RESTART, requestedMode.get());
    }
}
