package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameLocationBootstrapLifecycleTest {

    @Test
    void startRunsLocationBootstrappersOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameLocationBootstrapLifecycle lifecycle = new GameLocationBootstrapLifecycle(List.of(
            () -> events.add("siege"),
            () -> events.add("base"),
            () -> events.add("vortex"),
            () -> events.add("abyss")
        ));

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("siege", "base", "vortex", "abyss"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("location failed");
        GameLocationBootstrapLifecycle lifecycle = new GameLocationBootstrapLifecycle(List.of(
            () -> events.add("siege"),
            () -> {
                events.add("base");
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
        assertEquals(List.of("siege", "base", "siege", "base"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }
}
