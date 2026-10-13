package com.aionemu.gameserver.lifecycle;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameLoggingLifecycleTest {

    @Test
    void startInitializesLoggerOnce() {
        List<String> events = new ArrayList<>();
        GameLoggingLifecycle lifecycle = new RecordingGameLoggingLifecycle(events, null);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("initializeLogger"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("logger failed");
        GameLoggingLifecycle lifecycle = new RecordingGameLoggingLifecycle(events, failure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("initializeLogger", "initializeLogger"), events);
		assertNull(lifecycle.getLastFailure());
    }

    private static final class RecordingGameLoggingLifecycle extends GameLoggingLifecycle {

        private final List<String> events;
        private final RuntimeException firstFailure;
        private boolean failed;

        private RecordingGameLoggingLifecycle(List<String> events, RuntimeException firstFailure) {
            this.events = events;
            this.firstFailure = firstFailure;
        }

        @Override
        protected void runStage() {
            events.add("initializeLogger");
            if (firstFailure != null && !failed) {
                failed = true;
                throw firstFailure;
            }
        }
    }
}
