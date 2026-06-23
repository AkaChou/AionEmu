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

class GameLocationBootstrapLifecycleTest {

    @Test
    void usesLocationBootstrapGatewayCollaborator() {
        assertEquals(GameLocationBootstrapGateway.class, fieldType("locationBootstrapGateway"));
    }

    @Test
    void startRunsLocationBootstrappersOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameLocationBootstrapLifecycle lifecycle = new GameLocationBootstrapLifecycle(
            new RecordingGameLocationBootstrapGateway(events, null));

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("siege", "base", "vortex", "abyss"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("location failed");
        GameLocationBootstrapLifecycle lifecycle = new GameLocationBootstrapLifecycle(
            new RecordingGameLocationBootstrapGateway(events, failure));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("siege", "base", "siege", "base", "vortex", "abyss"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static Class<?> fieldType(String name) {
        try {
            Field field = GameLocationBootstrapLifecycle.class.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameLocationBootstrapGateway extends GameLocationBootstrapGateway {

        private final List<String> events;
        private final RuntimeException firstFailure;

        private RecordingGameLocationBootstrapGateway(List<String> events, RuntimeException firstFailure) {
            this.events = events;
            this.firstFailure = firstFailure;
        }

        @Override
        public void bootstrap() {
            events.add("siege");
            events.add("base");
            if (events.size() == 2 && firstFailure != null) {
                throw firstFailure;
            }
            events.add("vortex");
            events.add("abyss");
        }
    }
}
