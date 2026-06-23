package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class GameSpawnLifecycleTest {

    @Test
    void startSpawnsOnceAndRecordsLoadTime() {
        AtomicInteger spawns = new AtomicInteger();
        GameSpawnLifecycle lifecycle = new GameSpawnLifecycle(spawns::incrementAndGet);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(1, spawns.get());
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        AtomicInteger spawns = new AtomicInteger();
        IllegalStateException failure = new IllegalStateException("spawn failed");
        GameSpawnLifecycle lifecycle = new GameSpawnLifecycle(() -> {
            if (spawns.incrementAndGet() == 1) {
                throw failure;
            }
        });

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(2, spawns.get());
        assertEquals(null, lifecycle.getLastFailure());
    }
}
