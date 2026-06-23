package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameScheduledServicesLifecycleTest {

    @Test
    void startRunsEnabledSchedulersOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameScheduledServicesLifecycle lifecycle = newLifecycle(events, true, true, true);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "pigPoppy", "abyss", "imperialTomb"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void startSkipsDisabledSchedulers() {
        List<String> events = new ArrayList<>();
        GameScheduledServicesLifecycle lifecycle = newLifecycle(events, false, false, false);

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section"), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("scheduled service failed");
        GameScheduledServicesLifecycle lifecycle = new GameScheduledServicesLifecycle(
            () -> events.add("section"),
            () -> true,
            () -> events.add("pigPoppy"),
            () -> true,
            () -> {
                events.add("abyss");
                if (events.size() == 3) {
                    throw failure;
                }
            },
            () -> true,
            () -> events.add("imperialTomb")
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(
            "section",
            "pigPoppy",
            "abyss",
            "section",
            "pigPoppy",
            "abyss",
            "imperialTomb"
        ), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static GameScheduledServicesLifecycle newLifecycle(
        List<String> events,
        boolean pigPoppyEventEnabled,
        boolean abyssEventEnabled,
        boolean imperialTombEnabled
    ) {
        return new GameScheduledServicesLifecycle(
            () -> events.add("section"),
            () -> pigPoppyEventEnabled,
            () -> events.add("pigPoppy"),
            () -> abyssEventEnabled,
            () -> events.add("abyss"),
            () -> imperialTombEnabled,
            () -> events.add("imperialTomb")
        );
    }
}
