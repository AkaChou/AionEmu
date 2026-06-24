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
            new RecordingGameStartupCompletionGateway(messages)
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
            new RecordingGameStartupCompletionGateway(messages, failure)
        );

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
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void usesStartupCompletionGatewayCollaborator() {
        assertEquals(GameStartupCompletionGateway.class, fieldType("startupCompletionGateway"));
    }

    private static Class<?> fieldType(String name) {
        try {
            return GameStartupCompletionLifecycle.class.getDeclaredField(name).getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameStartupCompletionGateway extends GameStartupCompletionGateway {

        private final List<String> messages;
        private final RuntimeException failure;
        private long currentTimeMillis;

        private RecordingGameStartupCompletionGateway(List<String> messages) {
            this(messages, null);
        }

        private RecordingGameStartupCompletionGateway(List<String> messages, RuntimeException failure) {
            this.messages = messages;
            this.failure = failure;
        }

        @Override
        public void logStartupComplete(long startupTime) {
            messages.add("=== Server initialization COMPLETE ===" + ":" + null);
            if (failure != null && messages.size() == 1) {
                throw failure;
            }
            messages.add("Total initialization time: {} seconds" + ":" + startupTime);
            messages.add("Server is now ready to accept connections" + ":" + null);
        }

        @Override
        public long currentTimeMillis() {
            return currentTimeMillis++;
        }
    }
}
