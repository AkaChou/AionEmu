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

class GameSiegeScheduleLifecycleTest {

    @Test
    void usesSiegeScheduleGatewayCollaborator() {
        assertEquals(GameSiegeScheduleGateway.class, fieldType("siegeScheduleGateway"));
    }

    @Test
    void siegeScheduleGatewayBridgesLegacyServicesThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameSiegeScheduleGateway.class, "siegeServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameSiegeScheduleGateway.class, "baseServiceProvider"));
    }

    @Test
    void startRunsInitializersOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameSiegeScheduleLifecycle lifecycle = new GameSiegeScheduleLifecycle(
            new RecordingGameSiegeScheduleGateway(events, null));

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "sieges", "bases"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("siege schedule failed");
        GameSiegeScheduleLifecycle lifecycle = new GameSiegeScheduleLifecycle(
            new RecordingGameSiegeScheduleGateway(events, failure));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "sieges", "bases", "section", "sieges", "bases"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static Class<?> fieldType(String name) {
        try {
            Field field = GameSiegeScheduleLifecycle.class.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameSiegeScheduleGateway extends GameSiegeScheduleGateway {

        private final List<String> events;
        private final RuntimeException firstFailure;

        private RecordingGameSiegeScheduleGateway(List<String> events, RuntimeException firstFailure) {
            this.events = events;
            this.firstFailure = firstFailure;
        }

        @Override
        public void start() {
            events.add("section");
            events.add("sieges");
            events.add("bases");
            if (events.size() == 3 && firstFailure != null) {
                throw firstFailure;
            }
        }
    }
}
