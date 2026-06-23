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

class GameOptionalServicesLifecycleTest {

    @Test
    void usesOptionalServicesGatewayCollaborator() {
        assertEquals(GameOptionalServicesGateway.class, fieldType("optionalServicesGateway"));
    }

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
            new RecordingGameOptionalServicesGateway(
                events,
                () -> {
                    limitReads.incrementAndGet();
                    return false;
                },
                () -> {
                    shoutReads.incrementAndGet();
                    return true;
                },
                () -> {
                    shieldReads.incrementAndGet();
                    return false;
                },
                null
            )
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
            new RecordingGameOptionalServicesGateway(
                events,
                () -> true,
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
            new RecordingGameOptionalServicesGateway(
                events,
                () -> limitsEnabled,
                () -> npcShoutsEnabled,
                () -> siegeShieldEnabled,
                null
            )
        );
    }

    private static Class<?> fieldType(String name) {
        try {
            Field field = GameOptionalServicesLifecycle.class.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameOptionalServicesGateway extends GameOptionalServicesGateway {

        private final List<String> events;
        private final BooleanSupplier limitsEnabled;
        private final BooleanSupplier npcShoutsEnabled;
        private final BooleanSupplier siegeShieldEnabled;
        private final RuntimeException firstFailure;

        private RecordingGameOptionalServicesGateway(
            List<String> events,
            BooleanSupplier limitsEnabled,
            BooleanSupplier npcShoutsEnabled,
            BooleanSupplier siegeShieldEnabled,
            RuntimeException firstFailure
        ) {
            this.events = events;
            this.limitsEnabled = limitsEnabled;
            this.npcShoutsEnabled = npcShoutsEnabled;
            this.siegeShieldEnabled = siegeShieldEnabled;
            this.firstFailure = firstFailure;
        }

        @Override
        public void start() {
            if (limitsEnabled.getAsBoolean()) {
                events.add("playerLimit");
            }
            if (npcShoutsEnabled.getAsBoolean()) {
                events.add("npcShouts");
                if (events.size() == 2 && firstFailure != null) {
                    throw firstFailure;
                }
            }
            if (siegeShieldEnabled.getAsBoolean()) {
                events.add("shield");
            }
        }
    }
}
