package com.aionemu.gameserver.lifecycle;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameSystemPropertiesLifecycleTest {

    @Test
    void startInitializesSystemPropertiesOnce() {
        List<String> events = new ArrayList<>();
        GameSystemPropertiesLifecycle lifecycle = new RecordingGameSystemPropertiesLifecycle(events, null);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("properties:start"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("properties failed");
        GameSystemPropertiesLifecycle lifecycle = new RecordingGameSystemPropertiesLifecycle(events, failure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("properties:start", "properties:start"), events);
		assertNull(lifecycle.getLastFailure());
    }

    private static final class RecordingGameSystemPropertiesLifecycle extends GameSystemPropertiesLifecycle {

        private final List<String> events;
        private final RuntimeException firstFailure;
        private boolean failed;

        private RecordingGameSystemPropertiesLifecycle(List<String> events, RuntimeException firstFailure) {
            this.events = events;
            this.firstFailure = firstFailure;
        }

        @Override
        protected void runStage() {
            events.add("properties:start");
            if (firstFailure != null && !failed) {
                failed = true;
                throw firstFailure;
            }
        }
    }
}
