package com.aionemu.gameserver.lifecycle;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;

class GameEventRuntimeLifecycleTest {

    @Test
    void eventRuntimeLifecycleBridgesLegacyServicesThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameEventRuntimeLifecycle.class, "eventServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameEventRuntimeLifecycle.class, "playerEventServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameEventRuntimeLifecycle.class, "crazyDaevaServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameEventRuntimeLifecycle.class, "abyssRankUpdateServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameEventRuntimeLifecycle.class, "packetBroadcasterProvider"));
    }

    @Test
    void startRunsEnabledEventBootstrappersOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameEventRuntimeLifecycle lifecycle = newLifecycle(
            events,
            true,
            true,
            true,
            true
        );

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(
            "section",
            "eventService",
            "playerEvent",
            "crazyEvent",
            "rankingHour",
            "rewardWeekly",
            "packetBroadcaster",
            "temporarySpawn"
        ), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void startSkipsDisabledOptionalEventsAndUsesMinuteRankingSchedule() {
        List<String> events = new ArrayList<>();
        GameEventRuntimeLifecycle lifecycle = newLifecycle(
            events,
            false,
            false,
            false,
            false
        );

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(
            "section",
            "rankingMinute",
            "rewardWeekly",
            "packetBroadcaster",
            "temporarySpawn"
        ), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("event runtime failed");
        GameEventRuntimeLifecycle lifecycle = new RecordingGameEventRuntimeLifecycle(
            events,
            true,
            true,
            false,
            false,
            failure
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(
            "section",
            "eventService",
            "playerEvent",
            "section",
            "eventService",
            "playerEvent",
            "rankingMinute",
            "rewardWeekly",
            "packetBroadcaster",
            "temporarySpawn"
        ), events);
		assertNull(lifecycle.getLastFailure());
    }

    private static GameEventRuntimeLifecycle newLifecycle(
        List<String> events,
        boolean eventServiceEnabled,
        boolean playerEventEnabled,
        boolean crazyEventEnabled,
        boolean topRankingUpdateEnabled
    ) {
        return new RecordingGameEventRuntimeLifecycle(
            events,
            eventServiceEnabled,
            playerEventEnabled,
            crazyEventEnabled,
            topRankingUpdateEnabled,
            null
        );
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameEventRuntimeLifecycle extends GameEventRuntimeLifecycle {

        private final List<String> events;
        private final boolean eventServiceEnabled;
        private final boolean playerEventEnabled;
        private final boolean crazyEventEnabled;
        private final boolean topRankingUpdateEnabled;
        private final RuntimeException firstFailure;

        private RecordingGameEventRuntimeLifecycle(
            List<String> events,
            boolean eventServiceEnabled,
            boolean playerEventEnabled,
            boolean crazyEventEnabled,
            boolean topRankingUpdateEnabled,
            RuntimeException firstFailure
        ) {
            this.events = events;
            this.eventServiceEnabled = eventServiceEnabled;
            this.playerEventEnabled = playerEventEnabled;
            this.crazyEventEnabled = crazyEventEnabled;
            this.topRankingUpdateEnabled = topRankingUpdateEnabled;
            this.firstFailure = firstFailure;
        }

        @Override
        protected void runStage() {
            events.add("section");
            if (eventServiceEnabled) {
                events.add("eventService");
            }
            if (playerEventEnabled) {
                events.add("playerEvent");
                if (events.size() == 3 && firstFailure != null) {
                    throw firstFailure;
                }
            }
            if (crazyEventEnabled) {
                events.add("crazyEvent");
            }
            events.add(topRankingUpdateEnabled ? "rankingHour" : "rankingMinute");
            events.add("rewardWeekly");
            events.add("packetBroadcaster");
            events.add("temporarySpawn");
        }
    }
}
