package com.aionemu.gameserver.lifecycle;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameSpawnLifecycleTest {

    @Test
    void startSpawnsOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameSpawnLifecycle lifecycle = new RecordingGameSpawnLifecycle(events, null);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "spawn"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("spawn failed");
        GameSpawnLifecycle lifecycle = new RecordingGameSpawnLifecycle(events, failure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "spawn", "section", "spawn"), events);
		assertNull(lifecycle.getLastFailure());
    }

    private static final class RecordingGameSpawnLifecycle extends GameSpawnLifecycle {

        private final List<String> events;
        private final RuntimeException firstFailure;

        private RecordingGameSpawnLifecycle(List<String> events, RuntimeException firstFailure) {
            this.events = events;
            this.firstFailure = firstFailure;
        }

        @Override
        protected void runStage() {
            events.add("section");
            events.add("spawn");
            if (events.size() == 2 && firstFailure != null) {
                throw firstFailure;
            }
        }
    }
}
