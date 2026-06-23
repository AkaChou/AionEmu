package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameSpawnLifecycleTest {

    @Test
    void startSpawnsOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameSpawnLifecycle lifecycle = new GameSpawnLifecycle(
            () -> events.add("section"),
            () -> events.add("spawn")
        );

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "spawn"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("spawn failed");
        GameSpawnLifecycle lifecycle = new GameSpawnLifecycle(
            () -> events.add("section"),
            () -> {
                events.add("spawn");
                if (events.size() == 2) {
                    throw failure;
                }
            }
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "spawn", "section", "spawn"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }
}
