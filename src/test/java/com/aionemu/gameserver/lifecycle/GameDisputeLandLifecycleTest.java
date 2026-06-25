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

class GameDisputeLandLifecycleTest {

    @Test
    void usesDisputeLandGatewayCollaborator() {
        assertEquals(GameDisputeLandGateway.class, fieldType("disputeLandGateway"));
    }

    @Test
    void disputeLandGatewayBridgesLegacyServicesThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameDisputeLandGateway.class, "disputeLandServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameDisputeLandGateway.class, "outpostServiceProvider"));
    }

    @Test
    void disputeLandGatewayBridgesLegacyFallbackThroughRuntimeBridgeProvider() {
        assertEquals(ObjectProvider.class, fieldType(GameDisputeLandGateway.class, "runtimeBridgeProvider"));
    }

    @Test
    void startRunsInitializersOnceInLegacyOrderAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameDisputeLandLifecycle lifecycle = new GameDisputeLandLifecycle(
            new RecordingGameDisputeLandGateway(events, null)
        );

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "disputeLand", "outposts"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("dispute land failed");
        GameDisputeLandLifecycle lifecycle = new GameDisputeLandLifecycle(
            new RecordingGameDisputeLandGateway(events, failure)
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "disputeLand", "outposts", "section", "disputeLand", "outposts"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static Class<?> fieldType(String name) {
        return fieldType(GameDisputeLandLifecycle.class, name);
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameDisputeLandGateway extends GameDisputeLandGateway {

        private final List<String> events;
        private final RuntimeException firstFailure;

        private RecordingGameDisputeLandGateway(List<String> events, RuntimeException firstFailure) {
            this.events = events;
            this.firstFailure = firstFailure;
        }

        @Override
        public void start() {
            events.add("section");
            events.add("disputeLand");
            events.add("outposts");
            if (events.size() == 3 && firstFailure != null) {
                throw firstFailure;
            }
        }
    }
}
