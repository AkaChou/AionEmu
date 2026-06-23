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

class GameEventBootstrapLifecycleTest {

    @Test
    void usesEventBootstrapGatewayCollaborator() {
        assertEquals(GameEventBootstrapGateway.class, fieldType("eventBootstrapGateway"));
    }

    @Test
    void startRunsEventBootstrappersOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameEventBootstrapLifecycle lifecycle = new GameEventBootstrapLifecycle(
            new RecordingGameEventBootstrapGateway(events, null));

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("luna", "minion", "shugoSweep", "passport", "eventWindow"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("event window failed");
        GameEventBootstrapLifecycle lifecycle = new GameEventBootstrapLifecycle(
            new RecordingGameEventBootstrapGateway(events, failure));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(
            "luna",
            "minion",
            "shugoSweep",
            "passport",
            "eventWindow",
            "luna",
            "minion",
            "shugoSweep",
            "passport",
            "eventWindow"
        ), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static Class<?> fieldType(String name) {
        try {
            Field field = GameEventBootstrapLifecycle.class.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameEventBootstrapGateway extends GameEventBootstrapGateway {

        private final List<String> events;
        private final RuntimeException firstFailure;

        private RecordingGameEventBootstrapGateway(List<String> events, RuntimeException firstFailure) {
            this.events = events;
            this.firstFailure = firstFailure;
        }

        @Override
        public void bootstrap() {
            events.add("luna");
            events.add("minion");
            events.add("shugoSweep");
            events.add("passport");
            events.add("eventWindow");
            if (events.size() == 5 && firstFailure != null) {
                throw firstFailure;
            }
        }
    }
}
