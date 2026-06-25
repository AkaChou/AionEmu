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

class GameBattlefieldLifecycleTest {

    private static final List<String> EVENT_NAMES = List.of(
        "kamar",
        "ophidan",
        "suspicious",
        "ironWall",
        "idgel",
        "landmark",
        "tenacity",
        "grandArena",
        "idRun"
    );

    @Test
    void usesBattlefieldGatewayCollaborator() {
        assertEquals(GameBattlefieldGateway.class, fieldType("battlefieldGateway"));
    }

    @Test
    void battlefieldGatewayBridgesLegacyServicesThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameBattlefieldGateway.class, "kamarBattlefieldServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameBattlefieldGateway.class, "engulfedOphidanBridgeServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameBattlefieldGateway.class, "suspiciousOphidanBridgeServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameBattlefieldGateway.class, "ironWallWarfrontServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameBattlefieldGateway.class, "idgelDomeServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameBattlefieldGateway.class, "idgelDomeLandmarkServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameBattlefieldGateway.class, "hallOfTenacityServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameBattlefieldGateway.class, "grandArenaTrainingCampServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameBattlefieldGateway.class, "idRunServiceProvider"));
    }

    @Test
    void startRunsEnabledInitializersOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameBattlefieldLifecycle lifecycle = newLifecycle(events, true);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "kamar", "ophidan", "suspicious", "ironWall", "idgel", "landmark", "tenacity", "grandArena", "idRun"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void startSkipsInitializersWhenAutoGroupDisabled() {
        List<String> events = new ArrayList<>();
        GameBattlefieldLifecycle lifecycle = newLifecycle(events, false);

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section"), events);
    }

    @Test
    void startReadsAutoGroupFlagForEachInitializerLikeLegacyCode() {
        List<String> events = new ArrayList<>();
        AtomicInteger reads = new AtomicInteger();
        GameBattlefieldLifecycle lifecycle = new GameBattlefieldLifecycle(
            new RecordingGameBattlefieldGateway(events, () -> reads.incrementAndGet() % 2 == 1, null)
        );

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(9, reads.get());
        assertEquals(List.of("section", "kamar", "suspicious", "idgel", "tenacity", "idRun"), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("battlefield failed");
        GameBattlefieldLifecycle lifecycle = new GameBattlefieldLifecycle(
            new RecordingGameBattlefieldGateway(events, () -> true, failure, List.of("kamar", "ophidan"))
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "kamar", "ophidan", "section", "kamar", "ophidan"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static GameBattlefieldLifecycle newLifecycle(List<String> events, boolean autoGroupEnabled) {
        return new GameBattlefieldLifecycle(new RecordingGameBattlefieldGateway(
            events,
            () -> autoGroupEnabled,
            null,
            EVENT_NAMES
        ));
    }

    private static Class<?> fieldType(String name) {
        try {
            Field field = GameBattlefieldLifecycle.class.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameBattlefieldGateway extends GameBattlefieldGateway {

        private final List<String> events;
        private final BooleanSupplier autoGroupEnabled;
        private final RuntimeException firstFailure;
        private final List<String> eventNames;

        private RecordingGameBattlefieldGateway(
            List<String> events,
            BooleanSupplier autoGroupEnabled,
            RuntimeException firstFailure
        ) {
            this(events, autoGroupEnabled, firstFailure, EVENT_NAMES);
        }

        private RecordingGameBattlefieldGateway(
            List<String> events,
            BooleanSupplier autoGroupEnabled,
            RuntimeException firstFailure,
            List<String> eventNames
        ) {
            this.events = events;
            this.autoGroupEnabled = autoGroupEnabled;
            this.firstFailure = firstFailure;
            this.eventNames = List.copyOf(eventNames);
        }

        @Override
        public void start() {
            events.add("section");
            eventNames.forEach(this::maybeRun);
        }

        private void maybeRun(String event) {
            if (autoGroupEnabled.getAsBoolean()) {
                events.add(event);
                if (events.size() == 3 && firstFailure != null) {
                    throw firstFailure;
                }
            }
        }
    }
}
