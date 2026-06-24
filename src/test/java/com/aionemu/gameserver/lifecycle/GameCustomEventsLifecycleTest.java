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

class GameCustomEventsLifecycleTest {

    @Test
    void usesCustomEventsGatewayCollaborator() {
        assertEquals(GameCustomEventsGateway.class, fieldType("customEventsGateway"));
    }

    @Test
    void startRunsEnabledCustomEventsOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameCustomEventsLifecycle lifecycle = newLifecycle(events, true, true);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "ffa", "ladder", "battleground", "bandit"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void startSkipsDisabledOptionalCustomEventsButAlwaysStartsBandit() {
        List<String> events = new ArrayList<>();
        GameCustomEventsLifecycle lifecycle = newLifecycle(events, false, false);

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "bandit"), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("custom event failed");
        GameCustomEventsLifecycle lifecycle = new GameCustomEventsLifecycle(
            new RecordingGameCustomEventsGateway(events, true, true, failure));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(
            "section",
            "ffa",
            "ladder",
            "battleground",
            "section",
            "ffa",
            "ladder",
            "battleground",
            "bandit"
        ), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static GameCustomEventsLifecycle newLifecycle(
        List<String> events,
        boolean ffaEnabled,
        boolean battlegroundEnabled
    ) {
        return new GameCustomEventsLifecycle(new RecordingGameCustomEventsGateway(
            events,
            ffaEnabled,
            battlegroundEnabled,
            null
        ));
    }

    private static Class<?> fieldType(String name) {
        try {
            Field field = GameCustomEventsLifecycle.class.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameCustomEventsGateway extends GameCustomEventsGateway {

        private final List<String> events;
        private final boolean ffaEnabled;
        private final boolean battlegroundEnabled;
        private final RuntimeException firstFailure;

        private RecordingGameCustomEventsGateway(
            List<String> events,
            boolean ffaEnabled,
            boolean battlegroundEnabled,
            RuntimeException firstFailure
        ) {
            this.events = events;
            this.ffaEnabled = ffaEnabled;
            this.battlegroundEnabled = battlegroundEnabled;
            this.firstFailure = firstFailure;
        }

        @Override
        public void start() {
            events.add("section");
            if (ffaEnabled) {
                events.add("ffa");
            }
            if (battlegroundEnabled) {
                events.add("ladder");
                events.add("battleground");
                if (events.size() == 4 && firstFailure != null) {
                    throw firstFailure;
                }
            }
            events.add("bandit");
        }
    }
}
