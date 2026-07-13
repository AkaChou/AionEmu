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
import org.springframework.beans.factory.ObjectProvider;

class GameGeoPathLifecycleTest {

    @Test
    void usesGeoPathGatewayCollaborator() {
        assertEquals(GameGeoPathGateway.class, fieldType("geoPathGateway"));
    }

    @Test
    void geoPathGatewayBridgesLegacyServicesThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameGeoPathGateway.class, "geoServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameGeoPathGateway.class, "pathServiceProvider"));
    }

    @Test
    void geoPathGatewayBridgesLegacyFallbackThroughRuntimeBridgeProvider() {
        assertEquals(ObjectProvider.class, fieldType(GameGeoPathGateway.class, "runtimeBridgeProvider"));
    }

    @Test
    void startInitializesGeoThenPathOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameGeoPathLifecycle lifecycle = new GameGeoPathLifecycle(
            new RecordingGameGeoPathGateway(events, null));

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "geo", "path"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("path failed");
        GameGeoPathLifecycle lifecycle = new GameGeoPathLifecycle(
            new RecordingGameGeoPathGateway(events, failure));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "geo", "path", "section", "geo", "path"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static Class<?> fieldType(String name) {
        return fieldType(GameGeoPathLifecycle.class, name);
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameGeoPathGateway extends GameGeoPathGateway {

        private final List<String> events;
        private final RuntimeException firstFailure;

        private RecordingGameGeoPathGateway(List<String> events, RuntimeException firstFailure) {
            this.events = events;
            this.firstFailure = firstFailure;
        }

        @Override
        public void initialize() {
            events.add("section");
            events.add("geo");
            events.add("path");
            if (events.size() == 3 && firstFailure != null) {
                throw firstFailure;
            }
        }
    }
}
