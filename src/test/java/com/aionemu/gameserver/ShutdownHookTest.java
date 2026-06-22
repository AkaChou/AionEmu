package com.aionemu.gameserver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.gameserver.ShutdownHook.ShutdownMode;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ShutdownHookTest {

    @AfterEach
    void resetEmbeddedMode() {
        System.clearProperty(AionRuntimeMode.BOOT_EMBEDDED_PROPERTY);
        AionEmbeddedShutdownHandler.clear();
    }

    @Test
    void embeddedShutdownRequestsBootShutdownAfterCountdown() {
        AionRuntimeMode.enableBootEmbeddedMode();
        AtomicInteger shutdownRequests = new AtomicInteger();
        AionEmbeddedShutdownHandler.register(shutdownRequests::incrementAndGet);

        ShutdownHook.getInstance().doShutdown(0, 1, ShutdownMode.RESTART);

        assertEquals(1, shutdownRequests.get());
    }
}
