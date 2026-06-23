package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameUtilityServicesLifecycleTest {

    @Test
    void startInitializesUtilityServicesOnce() {
        List<String> events = new ArrayList<>();
        GameThreadPoolLifecycle threadPoolLifecycle = new GameThreadPoolLifecycle(
            () -> events.add("threadPool:start"),
            () -> events.add("threadPool:stop")
        );
        GameUtilityServicesLifecycle lifecycle = new GameUtilityServicesLifecycle(
            threadPool -> {
                events.add("utility:start");
                threadPool.start();
            }
        );

        lifecycle.start(threadPoolLifecycle);
        lifecycle.start(threadPoolLifecycle);

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("utility:start", "threadPool:start"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("utility failed");
        GameThreadPoolLifecycle threadPoolLifecycle = new GameThreadPoolLifecycle(
            () -> events.add("threadPool:start"),
            () -> events.add("threadPool:stop")
        );
        GameUtilityServicesLifecycle lifecycle = new GameUtilityServicesLifecycle(
            threadPool -> {
                events.add("utility:start");
                if (events.size() == 1) {
                    throw failure;
                }
                threadPool.start();
            }
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> lifecycle.start(threadPoolLifecycle));

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start(threadPoolLifecycle);

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("utility:start", "utility:start", "threadPool:start"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }
}
