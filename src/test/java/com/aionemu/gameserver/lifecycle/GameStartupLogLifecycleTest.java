package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameStartupLogLifecycleTest {

    @Test
    void startLogsStartupAndReturnsStartTimeOnce() {
        List<String> events = new ArrayList<>();
        GameStartupLogLifecycle lifecycle = new GameStartupLogLifecycle(
            () -> 123L,
            () -> events.add("GameServer starting...")
        );

        long firstStartTime = lifecycle.start();
        long secondStartTime = lifecycle.start();

        assertEquals(123L, firstStartTime);
        assertEquals(123L, secondStartTime);
        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("GameServer starting..."), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("startup log failed");
        GameStartupLogLifecycle lifecycle = new GameStartupLogLifecycle(
            () -> 456L,
            () -> {
                events.add("GameServer starting...");
                if (events.size() == 1) {
                    throw failure;
                }
            }
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        long startTime = lifecycle.start();

        assertEquals(456L, startTime);
        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("GameServer starting...", "GameServer starting..."), events);
        assertEquals(null, lifecycle.getLastFailure());
    }
}
