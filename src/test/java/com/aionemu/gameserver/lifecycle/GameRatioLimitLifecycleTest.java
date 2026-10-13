package com.aionemu.gameserver.lifecycle;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameRatioLimitLifecycleTest {

    @Test
    void startRegistersRatioHookWhenRatioLimitationIsEnabled() {
        List<String> events = new ArrayList<>();
        GameRatioLimitLifecycle lifecycle = new RecordingGameRatioLimitLifecycle(events, true);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("registerRatioHook"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void startSkipsRatioHookWhenRatioLimitationIsDisabled() {
        List<String> events = new ArrayList<>();
        GameRatioLimitLifecycle lifecycle = new RecordingGameRatioLimitLifecycle(events, false);

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("ratio hook failed");
        GameRatioLimitLifecycle lifecycle = new RecordingGameRatioLimitLifecycle(events, true, failure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("registerRatioHook", "registerRatioHook"), events);
		assertNull(lifecycle.getLastFailure());
    }

    private static final class RecordingGameRatioLimitLifecycle extends GameRatioLimitLifecycle {

        private final List<String> events;
        private final boolean enabled;
        private final RuntimeException failure;

        private RecordingGameRatioLimitLifecycle(List<String> events, boolean enabled) {
            this(events, enabled, null);
        }

        private RecordingGameRatioLimitLifecycle(List<String> events, boolean enabled, RuntimeException failure) {
            this.events = events;
            this.enabled = enabled;
            this.failure = failure;
        }

        @Override
        protected boolean isRatioLimitationEnabled() {
            return enabled;
        }

        @Override
        protected void registerRatioLimitStartupHook() {
            events.add("registerRatioHook");
            if (failure != null && events.size() == 1) {
                throw failure;
            }
        }
    }
}
