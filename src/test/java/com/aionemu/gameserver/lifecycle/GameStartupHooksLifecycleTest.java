package com.aionemu.gameserver.lifecycle;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameStartupHooksLifecycleTest {

    @Test
    void usesStartupHooksGatewayCollaborator() {
        assertEquals(GameStartupHooksGateway.class, fieldType("startupHooksGateway"));
    }

    @Test
    void startRunsStartupHooksOnce() {
        List<String> events = new ArrayList<>();
        GameStartupHooksLifecycle lifecycle = new GameStartupHooksLifecycle(
            new RecordingGameStartupHooksGateway(events, null)
        );

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("runStartupHooks"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("startup hooks failed");
        GameStartupHooksLifecycle lifecycle = new GameStartupHooksLifecycle(
            new RecordingGameStartupHooksGateway(events, failure)
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("runStartupHooks", "runStartupHooks"), events);
		assertNull(lifecycle.getLastFailure());
    }

    private static Class<?> fieldType(String name) {
        try {
            Field field = GameStartupHooksLifecycle.class.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameStartupHooksGateway extends GameStartupHooksGateway {

        private final List<String> events;
        private final RuntimeException firstFailure;
        private boolean failed;

        private RecordingGameStartupHooksGateway(List<String> events, RuntimeException firstFailure) {
            this.events = events;
            this.firstFailure = firstFailure;
        }

        @Override
        public void start() {
            events.add("runStartupHooks");
            if (firstFailure != null && !failed) {
                failed = true;
                throw firstFailure;
            }
        }
    }
}
