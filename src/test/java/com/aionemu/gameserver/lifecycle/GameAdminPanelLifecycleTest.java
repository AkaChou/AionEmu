package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameAdminPanelLifecycleTest {

    @Test
    void startRunsAdminPanelWhenEnabled() {
        List<String> events = new ArrayList<>();
        GameAdminPanelLifecycle lifecycle = new GameAdminPanelLifecycle(
            new RecordingGameAdminPanelGateway(events, true)
        );

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("adminPanel:start"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void startSkipsAdminPanelWhenDisabled() {
        List<String> events = new ArrayList<>();
        GameAdminPanelLifecycle lifecycle = new GameAdminPanelLifecycle(
            new RecordingGameAdminPanelGateway(events, false)
        );

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("admin panel failed");
        GameAdminPanelLifecycle lifecycle = new GameAdminPanelLifecycle(
            new RecordingGameAdminPanelGateway(events, true, failure)
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("adminPanel:start", "adminPanel:start"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void usesAdminPanelGatewayCollaborator() {
        assertEquals(GameAdminPanelGateway.class, fieldType("adminPanelGateway"));
    }

    private static Class<?> fieldType(String name) {
        try {
            return GameAdminPanelLifecycle.class.getDeclaredField(name).getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameAdminPanelGateway extends GameAdminPanelGateway {

        private final List<String> events;
        private final boolean enabled;
        private final RuntimeException failure;
        private long currentTimeMillis;

        private RecordingGameAdminPanelGateway(List<String> events, boolean enabled) {
            this(events, enabled, null);
        }

        private RecordingGameAdminPanelGateway(List<String> events, boolean enabled, RuntimeException failure) {
            this.events = events;
            this.enabled = enabled;
            this.failure = failure;
        }

        @Override
        public boolean isAdminPanelEnabled() {
            return enabled;
        }

        @Override
        public void startAdminPanel() {
            events.add("adminPanel:start");
            if (failure != null && events.size() == 1) {
                throw failure;
            }
        }

        @Override
        public long currentTimeMillis() {
            return currentTimeMillis++;
        }
    }
}
