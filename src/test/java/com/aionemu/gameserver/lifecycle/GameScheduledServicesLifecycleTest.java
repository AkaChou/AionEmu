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

class GameScheduledServicesLifecycleTest {

    @Test
    void usesScheduledServicesGatewayCollaborator() {
        assertEquals(GameScheduledServicesGateway.class, fieldType("scheduledServicesGateway"));
    }

    @Test
    void scheduledServicesGatewayBridgesImperialTombThroughSpringProvider() {
        assertEquals(ObjectProvider.class, fieldType(GameScheduledServicesGateway.class, "shugoImperialTombSpawnManagerProvider"));
    }

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
            new RecordingGameScheduledServicesGateway(events, true, true, true, failure));

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
        return new GameScheduledServicesLifecycle(new RecordingGameScheduledServicesGateway(
            events,
            pigPoppyEventEnabled,
            abyssEventEnabled,
            imperialTombEnabled,
            null
        ));
    }

    private static Class<?> fieldType(String name) {
        return fieldType(GameScheduledServicesLifecycle.class, name);
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameScheduledServicesGateway extends GameScheduledServicesGateway {

        private final List<String> events;
        private final boolean pigPoppyEventEnabled;
        private final boolean abyssEventEnabled;
        private final boolean imperialTombEnabled;
        private final RuntimeException firstFailure;

        private RecordingGameScheduledServicesGateway(
            List<String> events,
            boolean pigPoppyEventEnabled,
            boolean abyssEventEnabled,
            boolean imperialTombEnabled,
            RuntimeException firstFailure
        ) {
            this.events = events;
            this.pigPoppyEventEnabled = pigPoppyEventEnabled;
            this.abyssEventEnabled = abyssEventEnabled;
            this.imperialTombEnabled = imperialTombEnabled;
            this.firstFailure = firstFailure;
        }

        @Override
        public void start() {
            events.add("section");
            if (pigPoppyEventEnabled) {
                events.add("pigPoppy");
            }
            if (abyssEventEnabled) {
                events.add("abyss");
                if (events.size() == 3 && firstFailure != null) {
                    throw firstFailure;
                }
            }
            if (imperialTombEnabled) {
                events.add("imperialTomb");
            }
        }
    }
}
