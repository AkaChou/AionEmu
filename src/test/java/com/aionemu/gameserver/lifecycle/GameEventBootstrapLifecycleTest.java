package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameEventBootstrapLifecycleTest {

    @Test
    void startRunsEventBootstrappersOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameEventBootstrapLifecycle lifecycle = new GameEventBootstrapLifecycle(List.of(
            () -> events.add("luna"),
            () -> events.add("minion"),
            () -> events.add("shugoSweep"),
            () -> events.add("passport"),
            () -> events.add("eventWindow")
        ));

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("luna", "minion", "shugoSweep", "passport", "eventWindow"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("event window failed");
        GameEventBootstrapLifecycle lifecycle = new GameEventBootstrapLifecycle(List.of(
            () -> events.add("luna"),
            () -> {
                events.add("eventWindow");
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
        assertEquals(List.of("luna", "eventWindow", "luna", "eventWindow"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }
}
