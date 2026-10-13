package com.aionemu.gameserver.lifecycle;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameAdminPanelLifecycleTest {

    @Test
    void startRunsAdminPanelWhenEnabled() {
        List<String> events = new ArrayList<>();
        GameAdminPanelLifecycle lifecycle = new RecordingGameAdminPanelLifecycle(events, true);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("adminPanel:start"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void startSkipsAdminPanelWhenDisabled() {
        List<String> events = new ArrayList<>();
        GameAdminPanelLifecycle lifecycle = new RecordingGameAdminPanelLifecycle(events, false);

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("admin panel failed");
        GameAdminPanelLifecycle lifecycle = new RecordingGameAdminPanelLifecycle(events, true, failure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("adminPanel:start", "adminPanel:start"), events);
		assertNull(lifecycle.getLastFailure());
    }

    private static final class RecordingGameAdminPanelLifecycle extends GameAdminPanelLifecycle {

        private final List<String> events;
        private final boolean enabled;
        private final RuntimeException failure;

        private RecordingGameAdminPanelLifecycle(List<String> events, boolean enabled) {
            this(events, enabled, null);
        }

        private RecordingGameAdminPanelLifecycle(List<String> events, boolean enabled, RuntimeException failure) {
            this.events = events;
            this.enabled = enabled;
            this.failure = failure;
        }

        @Override
        protected boolean isAdminPanelEnabled() {
            return enabled;
        }

        @Override
        protected void startAdminPanel() {
            events.add("adminPanel:start");
            if (failure != null && events.size() == 1) {
                throw failure;
            }
        }
    }
}
