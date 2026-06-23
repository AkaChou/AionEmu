package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameStartupLogLifecycleTest {

    @Test
    void usesStartupLogGatewayCollaborator() {
        assertEquals(GameStartupLogGateway.class, fieldType("startupLogGateway"));
    }

    @Test
    void startLogsStartupAndReturnsStartTimeOnce() {
        List<String> events = new ArrayList<>();
        GameStartupLogLifecycle lifecycle = new GameStartupLogLifecycle(
            new RecordingGameStartupLogGateway(events, null, 123L)
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
            new RecordingGameStartupLogGateway(events, failure, 456L)
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

    private static Class<?> fieldType(String name) {
        try {
            Field field = GameStartupLogLifecycle.class.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameStartupLogGateway extends GameStartupLogGateway {

        private final List<String> events;
        private final RuntimeException firstFailure;
        private final long startupTimeMillis;
        private boolean failed;

        private RecordingGameStartupLogGateway(
            List<String> events,
            RuntimeException firstFailure,
            long startupTimeMillis
        ) {
            this.events = events;
            this.firstFailure = firstFailure;
            this.startupTimeMillis = startupTimeMillis;
        }

        @Override
        public long start() {
            events.add("GameServer starting...");
            if (firstFailure != null && !failed) {
                failed = true;
                throw firstFailure;
            }
            return startupTimeMillis;
        }
    }
}
