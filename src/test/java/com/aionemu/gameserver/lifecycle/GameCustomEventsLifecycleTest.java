package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameCustomEventsLifecycleTest {

    @Test
    void startRunsEnabledCustomEventsOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameCustomEventsLifecycle lifecycle = newLifecycle(events, true, true);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "ffa", "ladder", "battleground", "bandit"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void startSkipsDisabledOptionalCustomEventsButAlwaysStartsBandit() {
        List<String> events = new ArrayList<>();
        GameCustomEventsLifecycle lifecycle = newLifecycle(events, false, false);

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "bandit"), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("custom event failed");
        GameCustomEventsLifecycle lifecycle = new GameCustomEventsLifecycle(
            () -> events.add("section"),
            () -> true,
            () -> events.add("ffa"),
            () -> true,
            () -> events.add("ladder"),
            () -> {
                events.add("battleground");
                if (events.size() == 4) {
                    throw failure;
                }
            },
            () -> events.add("bandit")
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(
            "section",
            "ffa",
            "ladder",
            "battleground",
            "section",
            "ffa",
            "ladder",
            "battleground",
            "bandit"
        ), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static GameCustomEventsLifecycle newLifecycle(
        List<String> events,
        boolean ffaEnabled,
        boolean battlegroundEnabled
    ) {
        return new GameCustomEventsLifecycle(
            () -> events.add("section"),
            () -> ffaEnabled,
            () -> events.add("ffa"),
            () -> battlegroundEnabled,
            () -> events.add("ladder"),
            () -> events.add("battleground"),
            () -> events.add("bandit")
        );
    }
}
