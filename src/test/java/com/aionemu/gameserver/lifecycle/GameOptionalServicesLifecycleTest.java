package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class GameOptionalServicesLifecycleTest {

    @Test
    void startRunsEnabledInitializersOnceInLegacyOrderAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameOptionalServicesLifecycle lifecycle = newLifecycle(events, true, false, true);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("playerLimit", "shield"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void startReadsEachConditionIndependently() {
        List<String> events = new ArrayList<>();
        AtomicInteger limitReads = new AtomicInteger();
        AtomicInteger shoutReads = new AtomicInteger();
        AtomicInteger shieldReads = new AtomicInteger();
        GameOptionalServicesLifecycle lifecycle = new GameOptionalServicesLifecycle(
            () -> {
                limitReads.incrementAndGet();
                return false;
            },
            () -> events.add("playerLimit"),
            () -> {
                shoutReads.incrementAndGet();
                return true;
            },
            () -> events.add("npcShouts"),
            () -> {
                shieldReads.incrementAndGet();
                return false;
            },
            () -> events.add("shield")
        );

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(1, limitReads.get());
        assertEquals(1, shoutReads.get());
        assertEquals(1, shieldReads.get());
        assertEquals(List.of("npcShouts"), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("optional services failed");
        GameOptionalServicesLifecycle lifecycle = new GameOptionalServicesLifecycle(
            () -> true,
            () -> events.add("playerLimit"),
            () -> true,
            () -> {
                events.add("npcShouts");
                if (events.size() == 2) {
                    throw failure;
                }
            },
            () -> true,
            () -> events.add("shield")
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("playerLimit", "npcShouts", "playerLimit", "npcShouts", "shield"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static GameOptionalServicesLifecycle newLifecycle(
        List<String> events,
        boolean limitsEnabled,
        boolean npcShoutsEnabled,
        boolean siegeShieldEnabled
    ) {
        return new GameOptionalServicesLifecycle(
            () -> limitsEnabled,
            () -> events.add("playerLimit"),
            () -> npcShoutsEnabled,
            () -> events.add("npcShouts"),
            () -> siegeShieldEnabled,
            () -> events.add("shield")
        );
    }
}
