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

class GameLoggingLifecycleTest {

    @Test
    void usesLoggingGatewayCollaborator() {
        assertEquals(GameLoggingGateway.class, fieldType("loggingGateway"));
    }

    @Test
    void startInitializesLoggerOnce() {
        List<String> events = new ArrayList<>();
        GameLoggingLifecycle lifecycle = new GameLoggingLifecycle(
            new RecordingGameLoggingGateway(events, null)
        );

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("initializeLogger"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("logger failed");
        GameLoggingLifecycle lifecycle = new GameLoggingLifecycle(
            new RecordingGameLoggingGateway(events, failure)
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("initializeLogger", "initializeLogger"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static Class<?> fieldType(String name) {
        try {
            Field field = GameLoggingLifecycle.class.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameLoggingGateway extends GameLoggingGateway {

        private final List<String> events;
        private final RuntimeException firstFailure;
        private boolean failed;

        private RecordingGameLoggingGateway(List<String> events, RuntimeException firstFailure) {
            this.events = events;
            this.firstFailure = firstFailure;
        }

        @Override
        public void start() {
            events.add("initializeLogger");
            if (firstFailure != null && !failed) {
                failed = true;
                throw firstFailure;
            }
        }
    }
}
