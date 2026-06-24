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

class GameSystemLifecycleTest {

    @Test
    void usesSystemGatewayCollaborator() {
        assertEquals(GameSystemGateway.class, fieldType("systemGateway"));
    }

    @Test
    void startRunsInitializersLogsSystemInfoAndRecordsStartupTime() {
        List<String> events = new ArrayList<>();
        GameSystemLifecycle lifecycle = new GameSystemLifecycle(
            new RecordingGameSystemGateway(events, null, legacyEventNames(), 2)
        );

        long startupTime = lifecycle.start(8_000);
        long repeatedStartupTime = lifecycle.start(8_000);

        assertTrue(lifecycle.isLoaded());
        assertEquals(2, startupTime);
        assertEquals(2, repeatedStartupTime);
        assertEquals(2, lifecycle.getStartupTimeSeconds());
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
        assertEquals(List.of(
            "section:system",
            "versions",
            "infos",
            "section:gameServer",
            "banner:line1",
            "banner:line2",
            "memory:512:128:384:1024",
            "startup:2"
        ), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("system failed");
        GameSystemLifecycle lifecycle = new GameSystemLifecycle(
            new RecordingGameSystemGateway(
                events,
                failure,
                List.of(
                    "section:system",
                    "versions",
                    "banner:line1",
                    "memory:512:128:384:1024",
                    "startup:2"
                ),
                2
            )
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> lifecycle.start(8_000));

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());
        assertEquals(-1, lifecycle.getStartupTimeSeconds());

        long startupTime = lifecycle.start(8_000);

        assertTrue(lifecycle.isLoaded());
        assertEquals(2, startupTime);
        assertEquals(List.of(
            "section:system",
            "versions",
            "section:system",
            "versions",
            "banner:line1",
            "memory:512:128:384:1024",
            "startup:2"
        ), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static List<String> legacyEventNames() {
        return List.of(
            "section:system",
            "versions",
            "infos",
            "section:gameServer",
            "banner:line1",
            "banner:line2",
            "memory:512:128:384:1024",
            "startup:2"
        );
    }

    private static Class<?> fieldType(String name) {
        try {
            Field field = GameSystemLifecycle.class.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameSystemGateway extends GameSystemGateway {

        private final List<String> events;
        private final RuntimeException firstFailure;
        private final List<String> eventNames;
        private final long startupTimeSeconds;
        private boolean failed;

        private RecordingGameSystemGateway(
            List<String> events,
            RuntimeException firstFailure,
            List<String> eventNames,
            long startupTimeSeconds
        ) {
            this.events = events;
            this.firstFailure = firstFailure;
            this.eventNames = List.copyOf(eventNames);
            this.startupTimeSeconds = startupTimeSeconds;
        }

        @Override
        public long start(long serverStartTimeMillis) {
            for (String eventName : eventNames) {
                events.add(eventName);
                if (events.size() == 2 && firstFailure != null && !failed) {
                    failed = true;
                    throw firstFailure;
                }
            }
            return startupTimeSeconds;
        }
    }
}
