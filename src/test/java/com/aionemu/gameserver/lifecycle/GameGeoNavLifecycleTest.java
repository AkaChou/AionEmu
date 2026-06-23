package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameGeoNavLifecycleTest {

    @Test
    void startInitializesGeoThenNavOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameGeoNavLifecycle lifecycle = new GameGeoNavLifecycle(
            () -> events.add("section"),
            () -> events.add("geo"),
            () -> events.add("nav")
        );

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "geo", "nav"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("nav failed");
        GameGeoNavLifecycle lifecycle = new GameGeoNavLifecycle(
            () -> events.add("section"),
            () -> events.add("geo"),
            () -> {
                events.add("nav");
                if (events.size() == 3) {
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
        assertEquals(List.of("section", "geo", "nav", "section", "geo", "nav"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }
}
