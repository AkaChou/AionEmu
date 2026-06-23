package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class GameSystemLifecycleTest {

    @Test
    void startRunsInitializersLogsSystemInfoAndRecordsStartupTime() {
        List<String> events = new ArrayList<>();
        AtomicLong now = new AtomicLong(10_000);
        GameSystemLifecycle lifecycle = newLifecycle(events, () -> now.getAndAdd(250));

        long startupTime = lifecycle.start(8_000);
        long repeatedStartupTime = lifecycle.start(8_000);

        assertTrue(lifecycle.isLoaded());
        assertEquals(2, startupTime);
        assertEquals(2, repeatedStartupTime);
        assertEquals(2, lifecycle.getStartupTimeSeconds());
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
        assertEquals(List.of(
            "section:system",
            "versions",
            "infos",
            "section:gameServer",
            "banner:line1",
            "banner:line2",
            "memory:512:128:384:1024",
            "startup:2"
        ), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        AtomicLong now = new AtomicLong(10_000);
        IllegalStateException failure = new IllegalStateException("system failed");
        GameSystemLifecycle lifecycle = new GameSystemLifecycle(
            List.of(
                () -> events.add("section:system"),
                () -> {
                    events.add("versions");
                    if (events.size() == 2) {
                        throw failure;
                    }
                }
            ),
            List.of("line1"),
            now::getAndIncrement,
            () -> 512,
            () -> 128,
            () -> 1024,
            line -> events.add("banner:" + line),
            (total, free, used, max) -> events.add("memory:" + total + ":" + free + ":" + used + ":" + max),
            seconds -> events.add("startup:" + seconds)
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> lifecycle.start(8_000));

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());
        assertEquals(-1, lifecycle.getStartupTimeSeconds());

        long startupTime = lifecycle.start(8_000);

        assertTrue(lifecycle.isLoaded());
        assertEquals(2, startupTime);
        assertEquals(List.of(
            "section:system",
            "versions",
            "section:system",
            "versions",
            "banner:line1",
            "memory:512:128:384:1024",
            "startup:2"
        ), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static GameSystemLifecycle newLifecycle(List<String> events, AtomicLongSupplier currentTimeMillis) {
        return new GameSystemLifecycle(
            List.of(
                () -> events.add("section:system"),
                () -> events.add("versions"),
                () -> events.add("infos"),
                () -> events.add("section:gameServer")
            ),
            List.of("line1", "line2"),
            currentTimeMillis::getAsLong,
            () -> 512,
            () -> 128,
            () -> 1024,
            line -> events.add("banner:" + line),
            (total, free, used, max) -> events.add("memory:" + total + ":" + free + ":" + used + ":" + max),
            seconds -> events.add("startup:" + seconds)
        );
    }

    @FunctionalInterface
    private interface AtomicLongSupplier {
        long getAsLong();
    }
}
