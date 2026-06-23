package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameEventRuntimeLifecycleTest {

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
            () -> events.add("section"),
            () -> true,
            () -> events.add("eventService"),
            () -> true,
            () -> {
                events.add("playerEvent");
                if (events.size() == 3) {
                    throw failure;
                }
            },
            () -> false,
            () -> events.add("crazyEvent"),
            () -> false,
            () -> events.add("rankingHour"),
            () -> events.add("rankingMinute"),
            () -> events.add("rewardWeekly"),
            () -> events.add("packetBroadcaster"),
            () -> events.add("temporarySpawn")
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
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static GameEventRuntimeLifecycle newLifecycle(
        List<String> events,
        boolean eventServiceEnabled,
        boolean playerEventEnabled,
        boolean crazyEventEnabled,
        boolean topRankingUpdateEnabled
    ) {
        return new GameEventRuntimeLifecycle(
            () -> events.add("section"),
            () -> eventServiceEnabled,
            () -> events.add("eventService"),
            () -> playerEventEnabled,
            () -> events.add("playerEvent"),
            () -> crazyEventEnabled,
            () -> events.add("crazyEvent"),
            () -> topRankingUpdateEnabled,
            () -> events.add("rankingHour"),
            () -> events.add("rankingMinute"),
            () -> events.add("rewardWeekly"),
            () -> events.add("packetBroadcaster"),
            () -> events.add("temporarySpawn")
        );
    }
}
