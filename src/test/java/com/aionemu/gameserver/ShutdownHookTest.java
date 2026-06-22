package com.aionemu.gameserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionEmbeddedShutdownMode;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.gameserver.ShutdownHook.ShutdownMode;
import java.util.concurrent.atomic.AtomicReference;
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
        AtomicReference<AionEmbeddedShutdownMode> requestedMode = new AtomicReference<>();
        AionEmbeddedShutdownHandler.register(requestedMode::set);

        ShutdownHook.getInstance().doShutdown(0, 1, ShutdownMode.RESTART);

        assertEquals(AionEmbeddedShutdownMode.RESTART, requestedMode.get());
    }

    @Test
    void stopReportsFalseWhenGameServerWasNotStarted() {
        assertFalse(GameServer.stop());
    }
}
