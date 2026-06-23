package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class GameStaticDataLifecycleTest {

    @Test
    void startLoadsStaticDataOnceAndRecordsLoadTime() {
        AtomicInteger loads = new AtomicInteger();
        GameStaticDataLifecycle lifecycle = new GameStaticDataLifecycle(() -> {
            loads.incrementAndGet();
            return null;
        });

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(1, loads.get());
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        AtomicInteger loads = new AtomicInteger();
        IllegalStateException failure = new IllegalStateException("static data failed");
        GameStaticDataLifecycle lifecycle = new GameStaticDataLifecycle(() -> {
            if (loads.incrementAndGet() == 1) {
                throw failure;
            }
            return null;
        });

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(2, loads.get());
        assertEquals(null, lifecycle.getLastFailure());
    }
}
