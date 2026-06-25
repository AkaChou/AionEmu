package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class GameWorldBootstrapLifecycleTest {

    @Test
    void usesWorldBootstrapGatewayCollaborator() {
        assertEquals(GameWorldBootstrapGateway.class, fieldType("worldBootstrapGateway"));
    }

    @Test
    void worldBootstrapGatewayBridgesWorldServicesThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameWorldBootstrapGateway.class, "idFactoryProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameWorldBootstrapGateway.class, "zoneServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameWorldBootstrapGateway.class, "hotspotTeleportServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameWorldBootstrapGateway.class, "roadServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameWorldBootstrapGateway.class, "worldProvider"));
    }

    @Test
    void startRunsWorldBootstrappersOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameWorldBootstrapLifecycle lifecycle = new GameWorldBootstrapLifecycle(new RecordingGameWorldBootstrapGateway(events, null));

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("worldBootstrap"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("zone failed");
        GameWorldBootstrapLifecycle lifecycle = new GameWorldBootstrapLifecycle(new RecordingGameWorldBootstrapGateway(events, failure));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("worldBootstrap", "worldBootstrap"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static final class RecordingGameWorldBootstrapGateway extends GameWorldBootstrapGateway {

        private final List<String> events;
        private final RuntimeException firstFailure;

        private RecordingGameWorldBootstrapGateway(List<String> events, RuntimeException firstFailure) {
            this.events = events;
            this.firstFailure = firstFailure;
        }

        @Override
        public void bootstrap() {
            events.add("worldBootstrap");
            if (events.size() == 1 && firstFailure != null) {
                throw firstFailure;
            }
        }
    }

    private static Class<?> fieldType(String name) {
        return fieldType(GameWorldBootstrapLifecycle.class, name);
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            return type.getDeclaredField(name).getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }
}
