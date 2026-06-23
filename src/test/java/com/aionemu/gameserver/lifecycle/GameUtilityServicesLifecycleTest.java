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
            () -> events.add("exceptionHandler"),
            () -> events.add("callbackSupport"),
            () -> events.add("cron"),
            () -> events.add("config:section"),
            () -> events.add("config:load"),
            () -> events.add("dateTime"),
            () -> events.add("database:section"),
            () -> events.add("databaseFactory"),
            () -> events.add("dao"),
            () -> events.add("threadConfig")
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
            () -> events.add("exceptionHandler"),
            () -> events.add("callbackSupport"),
            () -> events.add("cron"),
            () -> events.add("config:section"),
            () -> {
                events.add("config:load");
                if (events.size() == 5) {
                    throw failure;
                }
            },
            () -> events.add("dateTime"),
            () -> events.add("database:section"),
            () -> events.add("databaseFactory"),
            () -> events.add("dao"),
            () -> events.add("threadConfig")
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
