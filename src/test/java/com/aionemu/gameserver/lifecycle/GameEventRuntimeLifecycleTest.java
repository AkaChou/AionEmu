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

class GameEventRuntimeLifecycleTest {

    @Test
    void usesEventRuntimeGatewayCollaborator() {
        assertEquals(GameEventRuntimeGateway.class, fieldType("eventRuntimeGateway"));
    }

    @Test
    void eventRuntimeGatewayBridgesLegacyServicesThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameEventRuntimeGateway.class, "eventServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameEventRuntimeGateway.class, "playerEventServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameEventRuntimeGateway.class, "crazyDaevaServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameEventRuntimeGateway.class, "abyssRankUpdateServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameEventRuntimeGateway.class, "packetBroadcasterProvider"));
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
        assertEquals(null, lifecycle.getLastFailure());
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
        GameEventRuntimeLifecycle lifecycle = new GameEventRuntimeLifecycle(
            new RecordingGameEventRuntimeGateway(
                events,
                true,
                true,
                false,
                false,
                failure
            ));

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
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static GameEventRuntimeLifecycle newLifecycle(
        List<String> events,
        boolean eventServiceEnabled,
        boolean playerEventEnabled,
        boolean crazyEventEnabled,
        boolean topRankingUpdateEnabled
    ) {
        return new GameEventRuntimeLifecycle(new RecordingGameEventRuntimeGateway(
            events,
            eventServiceEnabled,
            playerEventEnabled,
            crazyEventEnabled,
            topRankingUpdateEnabled,
            null
        ));
    }

    private static Class<?> fieldType(String name) {
        return fieldType(GameEventRuntimeLifecycle.class, name);
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameEventRuntimeGateway extends GameEventRuntimeGateway {

        private final List<String> events;
        private final boolean eventServiceEnabled;
        private final boolean playerEventEnabled;
        private final boolean crazyEventEnabled;
        private final boolean topRankingUpdateEnabled;
        private final RuntimeException firstFailure;

        private RecordingGameEventRuntimeGateway(
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
        public void start() {
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
