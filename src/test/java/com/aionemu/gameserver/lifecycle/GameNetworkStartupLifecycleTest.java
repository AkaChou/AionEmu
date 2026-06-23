package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameNetworkStartupLifecycleTest {

    @Test
    void startRunsServerStarterAndRegistersShutdownHookOutsideBootEmbeddedMode() {
        List<String> events = new ArrayList<>();
        Thread hook = new Thread();
        GameNetworkStartupLifecycle lifecycle = new GameNetworkStartupLifecycle(
            () -> events.add("section"),
            () -> false,
            () -> hook,
            thread -> events.add(thread == hook ? "shutdownHook" : "wrongHook")
        );

        lifecycle.start(() -> events.add("startServers"));
        lifecycle.start(() -> events.add("startServersAgain"));

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "startServers", "shutdownHook"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void startSkipsShutdownHookInBootEmbeddedMode() {
        List<String> events = new ArrayList<>();
        GameNetworkStartupLifecycle lifecycle = new GameNetworkStartupLifecycle(
            () -> events.add("section"),
            () -> true,
            () -> new Thread(),
            thread -> events.add("shutdownHook")
        );

        lifecycle.start(() -> events.add("startServers"));

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "startServers"), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("network failed");
        GameNetworkStartupLifecycle lifecycle = new GameNetworkStartupLifecycle(
            () -> events.add("section"),
            () -> true,
            () -> new Thread(),
            thread -> events.add("shutdownHook")
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> lifecycle.start(() -> {
            events.add("startServers");
            if (events.size() == 2) {
                throw failure;
            }
        }));

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start(() -> events.add("startServers"));

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "startServers", "section", "startServers"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }
}
