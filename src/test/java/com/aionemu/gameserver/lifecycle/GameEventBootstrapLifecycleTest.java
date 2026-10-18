package com.aionemu.gameserver.lifecycle;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;

class GameEventBootstrapLifecycleTest {

    @Test
    void eventBootstrapLifecycleBridgesEventServicesThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameEventBootstrapLifecycle.class, "lunaShopServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameEventBootstrapLifecycle.class, "minionServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameEventBootstrapLifecycle.class, "shugoSweepServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameEventBootstrapLifecycle.class, "atreianPassportServiceProvider"));
    }

    @Test
    void startRunsEventBootstrappersOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameEventBootstrapLifecycle lifecycle = new RecordingGameEventBootstrapLifecycle(events, null);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("luna", "minion", "shugoSweep", "passport"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("event window failed");
        GameEventBootstrapLifecycle lifecycle = new RecordingGameEventBootstrapLifecycle(events, failure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(
            "luna",
            "minion",
            "shugoSweep",
            "passport",
            "luna",
            "minion",
            "shugoSweep",
            "passport"
        ), events);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void startReportsProgressForEventStartupSteps() {
        List<String> events = new ArrayList<>();
        GameEventBootstrapLifecycle lifecycle = new ProgressRecordingGameEventBootstrapLifecycle(
            events,
            new RecordingStartupProgressReporter(events)
        );

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(
            "progress:start:game event systems",
            "progress:started:Luna Shop System",
            "load:Luna Shop System",
            "progress:finished:Luna Shop System",
            "progress:started:Minion System",
            "load:Minion System",
            "progress:finished:Minion System",
            "progress:started:Shugo Sweep System",
            "load:Shugo Sweep System",
            "progress:finished:Shugo Sweep System",
            "progress:started:Atreian Passport System",
            "load:Atreian Passport System",
            "progress:finished:Atreian Passport System",
            "progress:finish:game event systems"
        ), events);
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameEventBootstrapLifecycle extends GameEventBootstrapLifecycle {

        private final List<String> events;
        private final RuntimeException firstFailure;

        private RecordingGameEventBootstrapLifecycle(List<String> events, RuntimeException firstFailure) {
            this.events = events;
            this.firstFailure = firstFailure;
        }

        @Override
        protected void runStage() {
            events.add("luna");
            events.add("minion");
            events.add("shugoSweep");
            events.add("passport");
            if (events.size() == 4 && firstFailure != null) {
                throw firstFailure;
            }
        }
    }

    private static final class ProgressRecordingGameEventBootstrapLifecycle extends GameEventBootstrapLifecycle {

        private final List<String> events;

        private ProgressRecordingGameEventBootstrapLifecycle(List<String> events, StartupProgressReporter progressReporter) {
            super(progressReporter);
            this.events = events;
        }

        @Override
        protected void initializeLunaShopSystem() {
            events.add("load:Luna Shop System");
        }

        @Override
        protected void initializeMinionSystem() {
            events.add("load:Minion System");
        }

        @Override
        protected void initializeShugoSweepSystem() {
            events.add("load:Shugo Sweep System");
        }

        @Override
        protected void initializeAtreianPassportSystem() {
            events.add("load:Atreian Passport System");
        }
    }
}
