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
            () -> true,
            () -> events.add("adminPanel:start")
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
            () -> false,
            () -> events.add("adminPanel:start")
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
            () -> true,
            () -> {
                events.add("adminPanel:start");
                if (events.size() == 1) {
                    throw failure;
                }
            }
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
}
