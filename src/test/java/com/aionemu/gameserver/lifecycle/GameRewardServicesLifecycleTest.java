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

class GameRewardServicesLifecycleTest {

    @Test
    void startRunsEnabledInitializersOnceInLegacyOrderAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameRewardServicesLifecycle lifecycle = newLifecycle(events, true, false, true);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("reward", "veteranRewards"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void startReadsEachConditionIndependently() {
        List<String> events = new ArrayList<>();
        AtomicInteger rewardReads = new AtomicInteger();
        AtomicInteger weddingReads = new AtomicInteger();
        AtomicInteger veteranReads = new AtomicInteger();
        GameRewardServicesLifecycle lifecycle = new GameRewardServicesLifecycle(
            () -> {
                rewardReads.incrementAndGet();
                return false;
            },
            () -> events.add("reward"),
            () -> {
                weddingReads.incrementAndGet();
                return true;
            },
            () -> events.add("wedding"),
            () -> {
                veteranReads.incrementAndGet();
                return false;
            },
            () -> events.add("veteranRewards")
        );

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(1, rewardReads.get());
        assertEquals(1, weddingReads.get());
        assertEquals(1, veteranReads.get());
        assertEquals(List.of("wedding"), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("reward services failed");
        GameRewardServicesLifecycle lifecycle = new GameRewardServicesLifecycle(
            () -> true,
            () -> events.add("reward"),
            () -> true,
            () -> {
                events.add("wedding");
                if (events.size() == 2) {
                    throw failure;
                }
            },
            () -> true,
            () -> events.add("veteranRewards")
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("reward", "wedding", "reward", "wedding", "veteranRewards"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static GameRewardServicesLifecycle newLifecycle(
        List<String> events,
        boolean rewardEnabled,
        boolean weddingEnabled,
        boolean veteranRewardsEnabled
    ) {
        return new GameRewardServicesLifecycle(
            () -> rewardEnabled,
            () -> events.add("reward"),
            () -> weddingEnabled,
            () -> events.add("wedding"),
            () -> veteranRewardsEnabled,
            () -> events.add("veteranRewards")
        );
    }
}
