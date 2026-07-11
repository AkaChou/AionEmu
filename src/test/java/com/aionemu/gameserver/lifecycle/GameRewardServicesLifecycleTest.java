package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class GameRewardServicesLifecycleTest {

    @Test
    void usesRewardServicesGatewayCollaborator() {
        assertEquals(GameRewardServicesGateway.class, fieldType("rewardServicesGateway"));
    }

    @Test
    void rewardGatewayBridgesLegacyServicesThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameRewardServicesGateway.class, "rewardServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRewardServicesGateway.class, "veteranRewardsServiceProvider"));
    }

    @Test
    void rewardGatewayBridgesLegacyFallbackThroughRuntimeBridgeProvider() {
        assertEquals(ObjectProvider.class, fieldType(GameRewardServicesGateway.class, "runtimeBridgeProvider"));
    }

    @Test
    void startRunsEnabledInitializersOnceInLegacyOrderAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameRewardServicesLifecycle lifecycle = newLifecycle(events, true, true);

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
        AtomicInteger veteranReads = new AtomicInteger();
        GameRewardServicesLifecycle lifecycle = new GameRewardServicesLifecycle(
            new RecordingGameRewardServicesGateway(
                events,
                () -> {
                    rewardReads.incrementAndGet();
                    return false;
                },
                () -> {
                    veteranReads.incrementAndGet();
                    return false;
                },
                null
            )
        );

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(1, rewardReads.get());
        assertEquals(1, veteranReads.get());
        assertEquals(List.of(), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("reward services failed");
        GameRewardServicesLifecycle lifecycle = new GameRewardServicesLifecycle(
            new RecordingGameRewardServicesGateway(
                events,
                () -> true,
                () -> true,
                failure
            )
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("reward", "reward", "veteranRewards"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static GameRewardServicesLifecycle newLifecycle(
        List<String> events,
        boolean rewardEnabled,
        boolean veteranRewardsEnabled
    ) {
        return new GameRewardServicesLifecycle(
            new RecordingGameRewardServicesGateway(
                events,
                () -> rewardEnabled,
                () -> veteranRewardsEnabled,
                null
            )
        );
    }

    private static Class<?> fieldType(String name) {
        return fieldType(GameRewardServicesLifecycle.class, name);
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameRewardServicesGateway extends GameRewardServicesGateway {

        private final List<String> events;
        private final BooleanSupplier rewardEnabled;
        private final BooleanSupplier veteranRewardsEnabled;
        private final RuntimeException firstFailure;

        private RecordingGameRewardServicesGateway(
            List<String> events,
            BooleanSupplier rewardEnabled,
            BooleanSupplier veteranRewardsEnabled,
            RuntimeException firstFailure
        ) {
            this.events = events;
            this.rewardEnabled = rewardEnabled;
            this.veteranRewardsEnabled = veteranRewardsEnabled;
            this.firstFailure = firstFailure;
        }

        @Override
        public void start() {
            if (rewardEnabled.getAsBoolean()) {
                events.add("reward");
                if (events.size() == 1 && firstFailure != null) {
                    throw firstFailure;
                }
            }
            if (veteranRewardsEnabled.getAsBoolean()) {
                events.add("veteranRewards");
            }
        }
    }
}
