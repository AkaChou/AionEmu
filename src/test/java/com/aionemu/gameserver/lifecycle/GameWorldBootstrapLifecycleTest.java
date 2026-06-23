package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameWorldBootstrapLifecycleTest {

    @Test
    void startRunsWorldBootstrappersOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameWorldBootstrapLifecycle lifecycle = new GameWorldBootstrapLifecycle(List.of(
            () -> events.add("idFactory"),
            () -> events.add("zone"),
            () -> events.add("hotspot"),
            () -> events.add("road"),
            () -> events.add("world")
        ));

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("idFactory", "zone", "hotspot", "road", "world"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("zone failed");
        GameWorldBootstrapLifecycle lifecycle = new GameWorldBootstrapLifecycle(List.of(
            () -> events.add("idFactory"),
            () -> {
                events.add("zone");
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
        assertEquals(List.of("idFactory", "zone", "idFactory", "zone"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }
}
