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

class GameHousingLifecycleTest {

    @Test
    void usesHousingGatewayCollaborator() {
        assertEquals(GameHousingGateway.class, fieldType("housingGateway"));
    }

    @Test
    void startRunsInitializersOnceInLegacyOrderAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameHousingLifecycle lifecycle = new GameHousingLifecycle(
            new RecordingGameHousingGateway(events, null, eventNames())
        );

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "housingBid", "maintenance", "town", "challengeTask"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("housing failed");
        GameHousingLifecycle lifecycle = new GameHousingLifecycle(
            new RecordingGameHousingGateway(events, failure, List.of("housingBid", "maintenance"))
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "housingBid", "maintenance", "section", "housingBid", "maintenance"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static List<String> eventNames() {
        return List.of(
            "housingBid",
            "maintenance",
            "town",
            "challengeTask"
        );
    }

    private static Class<?> fieldType(String name) {
        try {
            Field field = GameHousingLifecycle.class.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameHousingGateway extends GameHousingGateway {

        private final List<String> events;
        private final RuntimeException firstFailure;
        private final List<String> eventNames;

        private RecordingGameHousingGateway(
            List<String> events,
            RuntimeException firstFailure,
            List<String> eventNames
        ) {
            this.events = events;
            this.firstFailure = firstFailure;
            this.eventNames = List.copyOf(eventNames);
        }

        @Override
        public void start() {
            events.add("section");
            for (String eventName : eventNames) {
                events.add(eventName);
                if (events.size() == 3 && firstFailure != null) {
                    throw firstFailure;
                }
            }
        }
    }
}
