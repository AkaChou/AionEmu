package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameSiegeScheduleLifecycleTest {

    @Test
    void startRunsInitializersOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameSiegeScheduleLifecycle lifecycle = new GameSiegeScheduleLifecycle(
            () -> events.add("section"),
            List.<Runnable>of(
                () -> events.add("sieges"),
                () -> events.add("bases")
            )
        );

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "sieges", "bases"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("siege schedule failed");
        GameSiegeScheduleLifecycle lifecycle = new GameSiegeScheduleLifecycle(
            () -> events.add("section"),
            List.<Runnable>of(
                () -> events.add("sieges"),
                () -> {
                    events.add("bases");
                    if (events.size() == 3) {
                        throw failure;
                    }
                }
            )
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "sieges", "bases", "section", "sieges", "bases"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }
}
