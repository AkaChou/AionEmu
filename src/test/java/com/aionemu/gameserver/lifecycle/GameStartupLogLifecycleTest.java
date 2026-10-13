package com.aionemu.gameserver.lifecycle;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameStartupLogLifecycleTest {

    @Test
    void startLogsStartupAndReturnsStartTimeOnce() {
        List<String> events = new ArrayList<>();
        GameStartupLogLifecycle lifecycle = new RecordingGameStartupLogLifecycle(events, null, 123L);

        long firstStartTime = lifecycle.start();
        long secondStartTime = lifecycle.start();

        assertEquals(123L, firstStartTime);
        assertEquals(123L, secondStartTime);
        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("GameServer starting..."), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("startup log failed");
        GameStartupLogLifecycle lifecycle = new RecordingGameStartupLogLifecycle(events, failure, 456L);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        long startTime = lifecycle.start();

        assertEquals(456L, startTime);
        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("GameServer starting...", "GameServer starting..."), events);
		assertNull(lifecycle.getLastFailure());
    }

    private static final class RecordingGameStartupLogLifecycle extends GameStartupLogLifecycle {

        private final List<String> events;
        private final RuntimeException firstFailure;
        private final long startupTimeMillis;
        private boolean failed;

        private RecordingGameStartupLogLifecycle(
            List<String> events,
            RuntimeException firstFailure,
            long startupTimeMillis
        ) {
            this.events = events;
            this.firstFailure = firstFailure;
            this.startupTimeMillis = startupTimeMillis;
        }

        @Override
        protected long captureStartupLog() {
            events.add("GameServer starting...");
            if (firstFailure != null && !failed) {
                failed = true;
                throw firstFailure;
            }
            return startupTimeMillis;
        }
    }
}
