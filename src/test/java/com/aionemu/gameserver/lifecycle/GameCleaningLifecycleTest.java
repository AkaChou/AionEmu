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

class GameCleaningLifecycleTest {

    @Test
    void usesCleaningGatewayCollaborator() {
        assertEquals(GameCleaningGateway.class, fieldType("cleaningGateway"));
    }

    @Test
    void cleaningGatewayBridgesLegacyServicesThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameCleaningGateway.class, "databaseCleaningServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameCleaningGateway.class, "abyssRankCleaningServiceProvider"));
    }

    @Test
    void cleaningGatewayBridgesLegacyFallbackThroughRuntimeBridgeProvider() {
        assertEquals(ObjectProvider.class, fieldType(GameCleaningGateway.class, "runtimeBridgeProvider"));
    }

    @Test
    void startRunsCleanersOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameCleaningLifecycle lifecycle = new GameCleaningLifecycle(
            new RecordingGameCleaningGateway(events, null));

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("database", "abyssRank"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("cleaning failed");
        GameCleaningLifecycle lifecycle = new GameCleaningLifecycle(
            new RecordingGameCleaningGateway(events, failure));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("database", "abyssRank", "database", "abyssRank"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static Class<?> fieldType(String name) {
        return fieldType(GameCleaningLifecycle.class, name);
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameCleaningGateway extends GameCleaningGateway {

        private final List<String> events;
        private final RuntimeException firstFailure;

        private RecordingGameCleaningGateway(List<String> events, RuntimeException firstFailure) {
            this.events = events;
            this.firstFailure = firstFailure;
        }

        @Override
        public void clean() {
            events.add("database");
            events.add("abyssRank");
            if (events.size() == 2 && firstFailure != null) {
                throw firstFailure;
            }
        }
    }
}
