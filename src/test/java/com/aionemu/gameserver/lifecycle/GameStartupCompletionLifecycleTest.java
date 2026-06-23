package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameStartupCompletionLifecycleTest {

    @Test
    void startLogsStartupCompletionOnce() {
        List<String> messages = new ArrayList<>();
        GameStartupCompletionLifecycle lifecycle = new GameStartupCompletionLifecycle(
            (message, value) -> messages.add(message + ":" + value)
        );

        lifecycle.start(42);
        lifecycle.start(99);

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(
            "=== Server initialization COMPLETE ===:null",
            "Total initialization time: {} seconds:42",
            "Server is now ready to accept connections:null"
        ), messages);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> messages = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("completion log failed");
        GameStartupCompletionLifecycle lifecycle = new GameStartupCompletionLifecycle(
            (message, value) -> {
                messages.add(message);
                if (messages.size() == 1) {
                    throw failure;
                }
            }
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> lifecycle.start(7));

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start(8);

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(
            "=== Server initialization COMPLETE ===",
            "=== Server initialization COMPLETE ===",
            "Total initialization time: {} seconds",
            "Server is now ready to accept connections"
        ), messages);
        assertEquals(null, lifecycle.getLastFailure());
    }
}
