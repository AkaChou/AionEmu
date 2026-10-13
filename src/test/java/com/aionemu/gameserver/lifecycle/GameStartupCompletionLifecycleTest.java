package com.aionemu.gameserver.lifecycle;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameStartupCompletionLifecycleTest {

    @Test
    void startLogsStartupCompletionOnce() {
        List<String> messages = new ArrayList<>();
        GameStartupCompletionLifecycle lifecycle = new RecordingGameStartupCompletionLifecycle(messages);

        lifecycle.start(42);
        lifecycle.start(99);

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(
            "=== Server initialization COMPLETE ===:null",
            "Total initialization time: {} seconds:42",
            "Server is now ready to accept connections:null"
        ), messages);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> messages = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("completion log failed");
        GameStartupCompletionLifecycle lifecycle = new RecordingGameStartupCompletionLifecycle(messages, failure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> lifecycle.start(7));

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start(8);

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(
            "=== Server initialization COMPLETE ===:null",
            "=== Server initialization COMPLETE ===:null",
            "Total initialization time: {} seconds:8",
            "Server is now ready to accept connections:null"
        ), messages);
		assertNull(lifecycle.getLastFailure());
    }

    private static final class RecordingGameStartupCompletionLifecycle extends GameStartupCompletionLifecycle {

        private final List<String> messages;
        private final RuntimeException failure;

        private RecordingGameStartupCompletionLifecycle(List<String> messages) {
            this(messages, null);
        }

        private RecordingGameStartupCompletionLifecycle(List<String> messages, RuntimeException failure) {
            this.messages = messages;
            this.failure = failure;
        }

        @Override
        protected void logStartupComplete(long startupTime) {
            messages.add("=== Server initialization COMPLETE ===" + ":" + null);
            if (failure != null && messages.size() == 1) {
                throw failure;
            }
            messages.add("Total initialization time: {} seconds" + ":" + startupTime);
            messages.add("Server is now ready to accept connections" + ":" + null);
        }
    }
}
