package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class GameDredgionLifecycleTest {

    @Test
    void startRunsEnabledInitializersOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameDredgionLifecycle lifecycle = newLifecycle(events, true);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("dredgion", "asyunatar"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void startSkipsInitializersWhenAutoGroupDisabled() {
        List<String> events = new ArrayList<>();
        GameDredgionLifecycle lifecycle = newLifecycle(events, false);

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(), events);
    }

    @Test
    void startReadsAutoGroupFlagForEachInitializerLikeLegacyCode() {
        List<String> events = new ArrayList<>();
        AtomicInteger reads = new AtomicInteger();
        GameDredgionLifecycle lifecycle = new GameDredgionLifecycle(
            () -> reads.incrementAndGet() == 1,
            () -> events.add("dredgion"),
            () -> events.add("asyunatar")
        );

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(2, reads.get());
        assertEquals(List.of("dredgion"), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("dredgion failed");
        GameDredgionLifecycle lifecycle = new GameDredgionLifecycle(
            () -> true,
            () -> events.add("dredgion"),
            () -> {
                events.add("asyunatar");
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
        assertEquals(List.of("dredgion", "asyunatar", "dredgion", "asyunatar"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static GameDredgionLifecycle newLifecycle(List<String> events, boolean autoGroupEnabled) {
        return new GameDredgionLifecycle(
            () -> autoGroupEnabled,
            () -> events.add("dredgion"),
            () -> events.add("asyunatar")
        );
    }
}
