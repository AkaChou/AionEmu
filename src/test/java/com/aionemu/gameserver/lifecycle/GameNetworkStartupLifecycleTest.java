package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class GameNetworkStartupLifecycleTest {

    @Test
    void startRunsServerStarterAndRegistersShutdownHookOutsideBootEmbeddedMode() {
        List<String> events = new ArrayList<>();
        Thread hook = new Thread();
        GameNetworkStartupLifecycle lifecycle = new GameNetworkStartupLifecycle(
            new RecordingGameNetworkStartupGateway(events, false, hook)
        );

        lifecycle.start(() -> events.add("startServers"));
        lifecycle.start(() -> events.add("startServersAgain"));

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "startServers", "misc", "shutdownHook"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void startSkipsShutdownHookInBootEmbeddedMode() {
        List<String> events = new ArrayList<>();
        GameNetworkStartupLifecycle lifecycle = new GameNetworkStartupLifecycle(
            new RecordingGameNetworkStartupGateway(events, true, new Thread())
        );

        lifecycle.start(() -> events.add("startServers"));

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "startServers", "misc"), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("network failed");
        GameNetworkStartupLifecycle lifecycle = new GameNetworkStartupLifecycle(
            new RecordingGameNetworkStartupGateway(events, true, new Thread())
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
        assertEquals(List.of("section", "startServers", "section", "startServers", "misc"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void usesNetworkStartupGatewayCollaborator() {
        assertEquals(GameNetworkStartupGateway.class, fieldType("networkStartupGateway"));
    }

    @Test
    void networkStartupGatewayBridgesShutdownHookThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameNetworkStartupGateway.class, "shutdownHookProvider"));
    }

    private static Class<?> fieldType(String name) {
        try {
            return GameNetworkStartupLifecycle.class.getDeclaredField(name).getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            return type.getDeclaredField(name).getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameNetworkStartupGateway extends GameNetworkStartupGateway {

        private final List<String> events;
        private final boolean bootEmbedded;
        private final Thread shutdownHook;
        private long currentTimeMillis;

        private RecordingGameNetworkStartupGateway(List<String> events, boolean bootEmbedded, Thread shutdownHook) {
            this.events = events;
            this.bootEmbedded = bootEmbedded;
            this.shutdownHook = shutdownHook;
        }

        @Override
        public void printNetworkSection() {
            events.add("section");
        }

        @Override
        public void printMiscSection() {
            events.add("misc");
        }

        @Override
        public boolean isBootEmbedded() {
            return bootEmbedded;
        }

        @Override
        public Thread shutdownHook() {
            return shutdownHook;
        }

        @Override
        public void registerShutdownHook(Thread shutdownHook) {
            events.add(shutdownHook == this.shutdownHook ? "shutdownHook" : "wrongHook");
        }

        @Override
        public long currentTimeMillis() {
            return currentTimeMillis++;
        }
    }
}
