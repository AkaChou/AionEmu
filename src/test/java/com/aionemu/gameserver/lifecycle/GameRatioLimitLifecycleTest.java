package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameRatioLimitLifecycleTest {

    @Test
    void startRegistersRatioHookWhenRatioLimitationIsEnabled() {
        List<String> events = new ArrayList<>();
        GameRatioLimitLifecycle lifecycle = new GameRatioLimitLifecycle(
            () -> true,
            () -> events.add("registerRatioHook")
        );

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("registerRatioHook"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void startSkipsRatioHookWhenRatioLimitationIsDisabled() {
        List<String> events = new ArrayList<>();
        GameRatioLimitLifecycle lifecycle = new GameRatioLimitLifecycle(
            () -> false,
            () -> events.add("registerRatioHook")
        );

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("ratio hook failed");
        GameRatioLimitLifecycle lifecycle = new GameRatioLimitLifecycle(
            () -> true,
            () -> {
                events.add("registerRatioHook");
                if (events.size() == 1) {
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
        assertEquals(List.of("registerRatioHook", "registerRatioHook"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }
}
