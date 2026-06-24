package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameUtilityServicesLifecycleTest {

    @Test
    void startInitializesUtilityServicesOnce() {
        List<String> events = new ArrayList<>();
        GameThreadPoolLifecycle threadPoolLifecycle = new GameThreadPoolLifecycle(
            new RecordingGameThreadPoolGateway(events)
        );
        GameUtilityServicesLifecycle lifecycle = new GameUtilityServicesLifecycle(
            new RecordingGameUtilityServicesGateway(events)
        );

        lifecycle.start(threadPoolLifecycle);
        lifecycle.start(threadPoolLifecycle);

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(
            "exceptionHandler",
            "callbackSupport",
            "cron",
            "config:section",
            "config:load",
            "dateTime",
            "database:section",
            "databaseFactory",
            "dao",
            "threadConfig",
            "threadPool:start"
        ), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("utility failed");
        GameThreadPoolLifecycle threadPoolLifecycle = new GameThreadPoolLifecycle(
            new RecordingGameThreadPoolGateway(events)
        );
        GameUtilityServicesLifecycle lifecycle = new GameUtilityServicesLifecycle(
            new RecordingGameUtilityServicesGateway(events, failure)
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> lifecycle.start(threadPoolLifecycle));

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start(threadPoolLifecycle);

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(
            "exceptionHandler",
            "callbackSupport",
            "cron",
            "config:section",
            "config:load",
            "exceptionHandler",
            "callbackSupport",
            "cron",
            "config:section",
            "config:load",
            "dateTime",
            "database:section",
            "databaseFactory",
            "dao",
            "threadConfig",
            "threadPool:start"
        ), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void usesUtilityServicesGatewayCollaborator() {
        assertEquals(GameUtilityServicesGateway.class, fieldType("utilityServicesGateway"));
    }

    private static Class<?> fieldType(String name) {
        try {
            return GameUtilityServicesLifecycle.class.getDeclaredField(name).getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameUtilityServicesGateway extends GameUtilityServicesGateway {

        private final List<String> events;
        private final RuntimeException configFailure;
        private long currentTimeMillis;

        private RecordingGameUtilityServicesGateway(List<String> events) {
            this(events, null);
        }

        private RecordingGameUtilityServicesGateway(List<String> events, RuntimeException configFailure) {
            this.events = events;
            this.configFailure = configFailure;
        }

        @Override
        public void initializeExceptionHandler() {
            events.add("exceptionHandler");
        }

        @Override
        public void reportCallbackSupport() {
            events.add("callbackSupport");
        }

        @Override
        public void initializeCronService() {
            events.add("cron");
        }

        @Override
        public void printConfigSection() {
            events.add("config:section");
        }

        @Override
        public void loadConfig() {
            events.add("config:load");
            if (configFailure != null && events.size() == 5) {
                throw configFailure;
            }
        }

        @Override
        public void initializeDateTime() {
            events.add("dateTime");
        }

        @Override
        public void printDatabaseSection() {
            events.add("database:section");
        }

        @Override
        public void initializeDatabaseFactory() {
            events.add("databaseFactory");
        }

        @Override
        public void initializeDaoManager() {
            events.add("dao");
        }

        @Override
        public void loadThreadConfig() {
            events.add("threadConfig");
        }

        @Override
        public long currentTimeMillis() {
            return currentTimeMillis++;
        }
    }

    private static final class RecordingGameThreadPoolGateway extends GameThreadPoolGateway {

        private final List<String> events;

        private RecordingGameThreadPoolGateway(List<String> events) {
            this.events = events;
        }

        @Override
        public void start() {
            events.add("threadPool:start");
        }

        @Override
        public void stop() {
            events.add("threadPool:stop");
        }
    }
}
