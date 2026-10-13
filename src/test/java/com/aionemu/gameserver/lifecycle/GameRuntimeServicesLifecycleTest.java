package com.aionemu.gameserver.lifecycle;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;

class GameRuntimeServicesLifecycleTest {

    @Test
    void runtimeServicesLifecycleBridgesLegacyServicesThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesLifecycle.class, "adminServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesLifecycle.class, "playerTransferServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesLifecycle.class, "periodicSaveServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesLifecycle.class, "territoryServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesLifecycle.class, "gameTimeServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesLifecycle.class, "serverVariableDaoProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesLifecycle.class, "announcementServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesLifecycle.class, "debugServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesLifecycle.class, "weatherServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesLifecycle.class, "brokerServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesLifecycle.class, "legionServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesLifecycle.class, "influenceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesLifecycle.class, "exchangeServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesLifecycle.class, "petitionServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesLifecycle.class, "flyRingServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesLifecycle.class, "curingZoneServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesLifecycle.class, "springZoneServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesLifecycle.class, "boostEventServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesLifecycle.class, "taskManagerFromDBProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesLifecycle.class, "limitedItemTradeServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesLifecycle.class, "webshopServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesLifecycle.class, "surveyServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesLifecycle.class, "findGroupServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameRuntimeServicesLifecycle.class, "inGameShopEnProvider"));
    }

    @Test
    void startRunsInitializersOnceInLegacyOrderAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameRuntimeServicesLifecycle lifecycle = new RecordingGameRuntimeServicesLifecycle(events, null, eventNames());

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
            "legion",
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
            "webshop",
            "survey",
            "findGroup",
            "inGameShop",
            "gameTimeClock"
        ), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("runtime services failed");
        GameRuntimeServicesLifecycle lifecycle = new RecordingGameRuntimeServicesLifecycle(events, failure, List.of("periodicSave", "admin"));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "periodicSave", "admin", "section", "periodicSave", "admin"), events);
		assertNull(lifecycle.getLastFailure());
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
            "legion",
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
            "webshop",
            "survey",
            "findGroup",
            "inGameShop",
            "gameTimeClock"
        );
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameRuntimeServicesLifecycle extends GameRuntimeServicesLifecycle {

        private final List<String> events;
        private final RuntimeException firstFailure;
        private final List<String> eventNames;

        private RecordingGameRuntimeServicesLifecycle(
            List<String> events,
            RuntimeException firstFailure,
            List<String> eventNames
        ) {
            this.events = events;
            this.firstFailure = firstFailure;
            this.eventNames = List.copyOf(eventNames);
        }

        @Override
        protected void runStage() {
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
