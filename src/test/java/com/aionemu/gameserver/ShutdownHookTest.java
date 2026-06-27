package com.aionemu.gameserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionEmbeddedShutdownMode;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.gameserver.ShutdownHook.ShutdownMode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Test
    void finalShutdownUsesLifecycleBridgesInsteadOfDirectServiceSingletons() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/ShutdownHook.java"));

        assertFalse(source.contains("import com.aionemu.commons.services.CronService;"));
        assertFalse(source.contains("import com.aionemu.gameserver.utils.ThreadPoolManager;"));
        assertFalse(source.contains("CronService.getInstance()"));
        assertFalse(source.contains("ThreadPoolManager.getInstance()"));
    }
}
