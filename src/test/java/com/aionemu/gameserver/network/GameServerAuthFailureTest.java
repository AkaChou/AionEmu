package com.aionemu.gameserver.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.commons.utils.AionEmbeddedFailureHandler;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.commons.utils.ExitCode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GameServerAuthFailureTest {

    @AfterEach
    void resetHooks() {
        System.clearProperty(AionRuntimeMode.BOOT_EMBEDDED_PROPERTY);
        AionEmbeddedFailureHandler.clear();
        GameServerAuthFailure.clearExitActionForTesting();
    }

    @Test
    void embeddedAuthFailureReportsToRegisteredHandler() {
        System.setProperty(AionRuntimeMode.BOOT_EMBEDDED_PROPERTY, "true");
        List<RuntimeException> failures = new ArrayList<>();
        AtomicInteger exitCode = new AtomicInteger(-1);
        AionEmbeddedFailureHandler.register(failures::add);
        GameServerAuthFailure.setExitActionForTesting(exitCode::set);

        GameServerAuthFailure.notAuthenticated("LoginServer");

        assertEquals(-1, exitCode.get());
        assertEquals(1, failures.size());
        assertTrue(failures.getFirst().getMessage().contains("LoginServer"));
    }

    @Test
    void standaloneAuthFailureUsesLegacyErrorExitCode() {
        AtomicInteger exitCode = new AtomicInteger(-1);
        GameServerAuthFailure.setExitActionForTesting(exitCode::set);

        GameServerAuthFailure.notAuthenticated("ChatServer");

        assertEquals(ExitCode.CODE_ERROR, exitCode.get());
    }
}
