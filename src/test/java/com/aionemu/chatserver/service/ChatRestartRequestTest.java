package com.aionemu.chatserver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionRuntimeMode;
import java.util.concurrent.atomic.AtomicInteger;
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
        AtomicInteger shutdownRequests = new AtomicInteger();
        AionEmbeddedShutdownHandler.register(shutdownRequests::incrementAndGet);

        ChatRestartRequest.requestRestart();

        assertEquals(1, shutdownRequests.get());
    }
}
