package com.aionemu.gameserver.lifecycle;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameUtilityServicesLifecycleTest {

    @Test
    void startInitializesUtilityServicesOnce() {
        List<String> events = new ArrayList<>();
        GameThreadPoolLifecycle threadPoolLifecycle = new RecordingGameThreadPoolLifecycle(events);
        GameUtilityServicesLifecycle lifecycle = new RecordingGameUtilityServicesLifecycle(events);

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
            "threadConfig",
            "threadPool:start"
        ), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("utility failed");
        GameThreadPoolLifecycle threadPoolLifecycle = new RecordingGameThreadPoolLifecycle(events);
        GameUtilityServicesLifecycle lifecycle = new RecordingGameUtilityServicesLifecycle(events, failure);

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
            "threadConfig",
            "threadPool:start"
        ), events);
		assertNull(lifecycle.getLastFailure());
    }

    private static final class RecordingGameUtilityServicesLifecycle extends GameUtilityServicesLifecycle {

        private final List<String> events;
        private final RuntimeException configFailure;

        private RecordingGameUtilityServicesLifecycle(List<String> events) {
            this(events, null);
        }

        private RecordingGameUtilityServicesLifecycle(List<String> events, RuntimeException configFailure) {
            this.events = events;
            this.configFailure = configFailure;
        }

        @Override
        protected void initializeExceptionHandler() {
            events.add("exceptionHandler");
        }

        @Override
        protected void reportCallbackSupport() {
            events.add("callbackSupport");
        }

        @Override
        protected void initializeCronService() {
            events.add("cron");
        }

        @Override
        protected void printConfigSection() {
            events.add("config:section");
        }

        @Override
        protected void loadConfig() {
            events.add("config:load");
            if (configFailure != null && events.size() == 5) {
                throw configFailure;
            }
        }

        @Override
        protected void initializeDateTime() {
            events.add("dateTime");
        }

        @Override
        protected void printDatabaseSection() {
            events.add("database:section");
        }

        @Override
        protected void loadThreadConfig() {
            events.add("threadConfig");
        }
    }

    private static final class RecordingGameThreadPoolLifecycle extends GameThreadPoolLifecycle {

        private final List<String> events;

        private RecordingGameThreadPoolLifecycle(List<String> events) {
            this.events = events;
        }

        @Override
        protected void startThreadPool() {
            events.add("threadPool:start");
        }

        @Override
        protected void stopThreadPool() {
            events.add("threadPool:stop");
        }
    }
}
