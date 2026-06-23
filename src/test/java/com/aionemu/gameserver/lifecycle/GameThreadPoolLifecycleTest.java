package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class GameThreadPoolLifecycleTest {

    @Test
    void startAndStopAreIdempotent() {
        AtomicInteger starts = new AtomicInteger();
        AtomicInteger stops = new AtomicInteger();
        GameThreadPoolLifecycle lifecycle = new GameThreadPoolLifecycle(starts::incrementAndGet, stops::incrementAndGet);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isStarted());
        assertEquals(1, starts.get());

        lifecycle.stop();
        lifecycle.stop();

        assertFalse(lifecycle.isStarted());
        assertEquals(1, stops.get());
    }

    @Test
    void stopBeforeStartDoesNotRunShutdown() {
        AtomicInteger stops = new AtomicInteger();
        GameThreadPoolLifecycle lifecycle = new GameThreadPoolLifecycle(() -> { }, stops::incrementAndGet);

        lifecycle.stop();

        assertFalse(lifecycle.isStarted());
        assertEquals(0, stops.get());
    }
}
