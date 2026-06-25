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

class GameRuntimeServicesLifecycleTest {

    @Test
    void usesRuntimeServicesGatewayCollaborator() {
        assertEquals(GameRuntimeServicesGateway.class, fieldType("runtimeServicesGateway"));
    }

    @Test
    void runtimeGatewayBridgesLegacyServicesThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesGateway.class, "adminServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesGateway.class, "playerTransferServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesGateway.class, "periodicSaveServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesGateway.class, "territoryServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesGateway.class, "gameTimeServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesGateway.class, "announcementServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesGateway.class, "debugServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesGateway.class, "weatherServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesGateway.class, "brokerServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesGateway.class, "influenceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesGateway.class, "exchangeServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesGateway.class, "petitionServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesGateway.class, "flyRingServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesGateway.class, "curingZoneServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesGateway.class, "springZoneServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesGateway.class, "boostEventServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesGateway.class, "taskManagerFromDBProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesGateway.class, "limitedItemTradeServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesGateway.class, "runtimeServiceBridgeProvider"));
    }

    @Test
    void startRunsInitializersOnceInLegacyOrderAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameRuntimeServicesLifecycle lifecycle = new GameRuntimeServicesLifecycle(
            new RecordingGameRuntimeServicesGateway(events, null, eventNames())
        );

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(
            "section",
            "periodicSave",
            "admin",
            "playerTransfer",
            "territory",
            "gameTime",
            "announcement",
            "debug",
            "weather",
            "broker",
            "influence",
            "exchange",
            "petition",
            "instance",
            "flyRing",
            "curingZone",
            "springZone",
            "boostEvent",
            "taskManager",
            "limitedItemTrade",
            "gameTimeClock"
        ), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("runtime services failed");
        GameRuntimeServicesLifecycle lifecycle = new GameRuntimeServicesLifecycle(
            new RecordingGameRuntimeServicesGateway(events, failure, List.of("periodicSave", "admin"))
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "periodicSave", "admin", "section", "periodicSave", "admin"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static List<String> eventNames() {
        return List.of(
            "periodicSave",
            "admin",
            "playerTransfer",
            "territory",
            "gameTime",
            "announcement",
            "debug",
            "weather",
            "broker",
            "influence",
            "exchange",
            "petition",
            "instance",
            "flyRing",
            "curingZone",
            "springZone",
            "boostEvent",
            "taskManager",
            "limitedItemTrade",
            "gameTimeClock"
        );
    }

    private static Class<?> fieldType(String name) {
        return fieldType(GameRuntimeServicesLifecycle.class, name);
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameRuntimeServicesGateway extends GameRuntimeServicesGateway {

        private final List<String> events;
        private final RuntimeException firstFailure;
        private final List<String> eventNames;

        private RecordingGameRuntimeServicesGateway(
            List<String> events,
            RuntimeException firstFailure,
            List<String> eventNames
        ) {
            this.events = events;
            this.firstFailure = firstFailure;
            this.eventNames = List.copyOf(eventNames);
        }

        @Override
        public void start() {
            events.add("section");
            for (String eventName : eventNames) {
                events.add(eventName);
                if (events.size() == 3 && firstFailure != null) {
                    throw firstFailure;
                }
            }
        }
    }
}
