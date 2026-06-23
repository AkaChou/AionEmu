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

class GameBattlefieldLifecycleTest {

    @Test
    void startRunsEnabledInitializersOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameBattlefieldLifecycle lifecycle = newLifecycle(events, true);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "kamar", "ophidan", "suspicious", "ironWall", "idgel", "landmark", "tenacity", "grandArena", "idRun"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void startSkipsInitializersWhenAutoGroupDisabled() {
        List<String> events = new ArrayList<>();
        GameBattlefieldLifecycle lifecycle = newLifecycle(events, false);

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section"), events);
    }

    @Test
    void startReadsAutoGroupFlagForEachInitializerLikeLegacyCode() {
        List<String> events = new ArrayList<>();
        AtomicInteger reads = new AtomicInteger();
        GameBattlefieldLifecycle lifecycle = new GameBattlefieldLifecycle(
            () -> events.add("section"),
            () -> reads.incrementAndGet() % 2 == 1,
            initializers(events)
        );

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(9, reads.get());
        assertEquals(List.of("section", "kamar", "suspicious", "idgel", "tenacity", "idRun"), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("battlefield failed");
        GameBattlefieldLifecycle lifecycle = new GameBattlefieldLifecycle(
            () -> events.add("section"),
            () -> true,
            List.<Runnable>of(
                () -> events.add("kamar"),
                () -> {
                    events.add("ophidan");
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
        assertEquals(List.of("section", "kamar", "ophidan", "section", "kamar", "ophidan"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static GameBattlefieldLifecycle newLifecycle(List<String> events, boolean autoGroupEnabled) {
        return new GameBattlefieldLifecycle(() -> events.add("section"), () -> autoGroupEnabled, initializers(events));
    }

    private static List<Runnable> initializers(List<String> events) {
        return List.of(
            () -> events.add("kamar"),
            () -> events.add("ophidan"),
            () -> events.add("suspicious"),
            () -> events.add("ironWall"),
            () -> events.add("idgel"),
            () -> events.add("landmark"),
            () -> events.add("tenacity"),
            () -> events.add("grandArena"),
            () -> events.add("idRun")
        );
    }
}
