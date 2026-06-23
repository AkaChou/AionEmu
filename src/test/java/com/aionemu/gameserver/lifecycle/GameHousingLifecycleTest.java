package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameHousingLifecycleTest {

    @Test
    void startRunsInitializersOnceInLegacyOrderAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameHousingLifecycle lifecycle = new GameHousingLifecycle(initializers(events));

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("housingBid", "maintenance", "town", "challengeTask"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("housing failed");
        GameHousingLifecycle lifecycle = new GameHousingLifecycle(List.of(
            () -> events.add("housingBid"),
            () -> {
                events.add("maintenance");
                if (events.size() == 2) {
                    throw failure;
                }
            }
        ));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("housingBid", "maintenance", "housingBid", "maintenance"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static List<Runnable> initializers(List<String> events) {
        return List.of(
            () -> events.add("housingBid"),
            () -> events.add("maintenance"),
            () -> events.add("town"),
            () -> events.add("challengeTask")
        );
    }
}
