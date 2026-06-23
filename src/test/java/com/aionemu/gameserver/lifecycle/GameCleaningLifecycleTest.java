package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameCleaningLifecycleTest {

    @Test
    void startRunsCleanersOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameCleaningLifecycle lifecycle = new GameCleaningLifecycle(List.of(
            () -> events.add("database"),
            () -> events.add("abyssRank")
        ));

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("database", "abyssRank"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("cleaning failed");
        GameCleaningLifecycle lifecycle = new GameCleaningLifecycle(List.of(
            () -> events.add("database"),
            () -> {
                events.add("abyssRank");
                if (events.size() == 2) {
                    throw failure;
                }
            }
        ));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("database", "abyssRank", "database", "abyssRank"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }
}
