package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameWorldActivationLifecycleTest {

    @Test
    void startRunsDropRegistrationServerActivationAndOfflineMarkerOnce() {
        List<String> events = new ArrayList<>();
        GameWorldActivationLifecycle lifecycle = new GameWorldActivationLifecycle(
            () -> events.add("dropRegistration"),
            () -> events.add("playersOffline")
        );

        lifecycle.start(() -> events.add("activeServer"));
        lifecycle.start(() -> events.add("activeServerAgain"));

        assertTrue(lifecycle.isActivated());
        assertEquals(List.of("dropRegistration", "activeServer", "playersOffline"), events);
        assertTrue(lifecycle.getActivationTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("active server failed");
        GameWorldActivationLifecycle lifecycle = new GameWorldActivationLifecycle(
            () -> events.add("dropRegistration"),
            () -> events.add("playersOffline")
        );

        IllegalStateException thrown = assertThrows(
            IllegalStateException.class,
            () -> lifecycle.start(() -> {
                events.add("activeServer");
                if (events.size() == 2) {
                    throw failure;
                }
            })
        );

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isActivated());

        lifecycle.start(() -> events.add("activeServer"));

        assertTrue(lifecycle.isActivated());
        assertEquals(
            List.of("dropRegistration", "activeServer", "dropRegistration", "activeServer", "playersOffline"),
            events
        );
        assertEquals(null, lifecycle.getLastFailure());
    }
}
