package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.GameServer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameWorldActivationLifecycleTest {

    @Test
    void startRunsDropRegistrationServerActivationAndOfflineMarkerOnce() {
        List<String> events = new ArrayList<>();
        GameServer server = new GameServer();
        GameWorldActivationLifecycle lifecycle = new GameWorldActivationLifecycle(
            () -> events.add("dropRegistration"),
            () -> server,
            activatedServer -> events.add(activatedServer == server ? "activeServer" : "wrongServer"),
            () -> events.add("playersOffline")
        );

        GameServer firstServer = lifecycle.start();
        GameServer secondServer = lifecycle.start();

        assertSame(server, firstServer);
        assertSame(server, secondServer);
        assertTrue(lifecycle.isActivated());
        assertEquals(List.of("dropRegistration", "activeServer", "playersOffline"), events);
        assertTrue(lifecycle.getActivationTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("active server failed");
        GameServer server = new GameServer();
        GameWorldActivationLifecycle lifecycle = new GameWorldActivationLifecycle(
            () -> events.add("dropRegistration"),
            () -> server,
            activatedServer -> {
                events.add(activatedServer == server ? "activeServer" : "wrongServer");
                if (events.size() == 2) {
                    throw failure;
                }
            },
            () -> events.add("playersOffline")
        );

        IllegalStateException thrown = assertThrows(
            IllegalStateException.class,
            lifecycle::start
        );

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isActivated());

        GameServer activatedServer = lifecycle.start();

        assertSame(server, activatedServer);
        assertTrue(lifecycle.isActivated());
        assertEquals(
            List.of("dropRegistration", "activeServer", "dropRegistration", "activeServer", "playersOffline"),
            events
        );
        assertEquals(null, lifecycle.getLastFailure());
    }
}
