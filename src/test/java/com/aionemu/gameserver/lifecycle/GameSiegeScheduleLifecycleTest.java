package com.aionemu.gameserver.lifecycle;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;

class GameSiegeScheduleLifecycleTest {

    @Test
    void siegeScheduleLifecycleBridgesLegacyServicesThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameSiegeScheduleLifecycle.class, "siegeServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameSiegeScheduleLifecycle.class, "baseServiceProvider"));
    }


    @Test
    void startRunsInitializersOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameSiegeScheduleLifecycle lifecycle = new RecordingGameSiegeScheduleLifecycle(events, null);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "sieges", "bases"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("siege schedule failed");
        GameSiegeScheduleLifecycle lifecycle = new RecordingGameSiegeScheduleLifecycle(events, failure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "sieges", "bases", "section", "sieges", "bases"), events);
		assertNull(lifecycle.getLastFailure());
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameSiegeScheduleLifecycle extends GameSiegeScheduleLifecycle {

        private final List<String> events;
        private final RuntimeException firstFailure;

        private RecordingGameSiegeScheduleLifecycle(List<String> events, RuntimeException firstFailure) {
            this.events = events;
            this.firstFailure = firstFailure;
        }

        @Override
        protected void runStage() {
            events.add("section");
            events.add("sieges");
            events.add("bases");
            if (events.size() == 3 && firstFailure != null) {
                throw firstFailure;
            }
        }
    }
}
