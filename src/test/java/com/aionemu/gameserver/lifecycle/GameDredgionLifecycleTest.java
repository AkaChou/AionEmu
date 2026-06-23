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
        assertEquals(List.of("section", "dredgion", "asyunatar"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void startSkipsInitializersWhenAutoGroupDisabled() {
        List<String> events = new ArrayList<>();
        GameDredgionLifecycle lifecycle = newLifecycle(events, false);

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section"), events);
    }

    @Test
    void startReadsAutoGroupFlagForEachInitializerLikeLegacyCode() {
        List<String> events = new ArrayList<>();
        AtomicInteger reads = new AtomicInteger();
        GameDredgionLifecycle lifecycle = new GameDredgionLifecycle(
            () -> events.add("section"),
            () -> reads.incrementAndGet() == 1,
            () -> events.add("dredgion"),
            () -> events.add("asyunatar")
        );

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(2, reads.get());
        assertEquals(List.of("section", "dredgion"), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("dredgion failed");
        GameDredgionLifecycle lifecycle = new GameDredgionLifecycle(
            () -> events.add("section"),
            () -> true,
            () -> events.add("dredgion"),
            () -> {
                events.add("asyunatar");
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
        assertEquals(List.of("section", "dredgion", "asyunatar", "section", "dredgion", "asyunatar"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static GameDredgionLifecycle newLifecycle(List<String> events, boolean autoGroupEnabled) {
        return new GameDredgionLifecycle(
            () -> events.add("section"),
            () -> autoGroupEnabled,
            () -> events.add("dredgion"),
            () -> events.add("asyunatar")
        );
    }
}
