package com.aionemu.gameserver.lifecycle;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;

class GameNetworkStartupLifecycleTest {

    @Test
    void startRunsServerStarterAndRegistersShutdownHookOutsideBootEmbeddedMode() {
        List<String> events = new ArrayList<>();
        Thread hook = new Thread();
        GameNetworkStartupLifecycle lifecycle = new RecordingGameNetworkStartupLifecycle(events, false, hook);

        lifecycle.start(() -> events.add("startServers"));
        lifecycle.start(() -> events.add("startServersAgain"));

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "startServers", "misc", "shutdownHook"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void startSkipsShutdownHookInBootEmbeddedMode() {
        List<String> events = new ArrayList<>();
        GameNetworkStartupLifecycle lifecycle = new RecordingGameNetworkStartupLifecycle(events, true, new Thread());

        lifecycle.start(() -> events.add("startServers"));

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "startServers", "misc"), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("network failed");
        GameNetworkStartupLifecycle lifecycle = new RecordingGameNetworkStartupLifecycle(events, true, new Thread());

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
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void networkStartupLifecycleBridgesShutdownHookThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameNetworkStartupLifecycle.class, "shutdownHookProvider"));
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            return type.getDeclaredField(name).getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameNetworkStartupLifecycle extends GameNetworkStartupLifecycle {

        private final List<String> events;
        private final boolean bootEmbedded;
        private final Thread shutdownHook;

        private RecordingGameNetworkStartupLifecycle(List<String> events, boolean bootEmbedded, Thread shutdownHook) {
            this.events = events;
            this.bootEmbedded = bootEmbedded;
            this.shutdownHook = shutdownHook;
        }

        @Override
        protected void printNetworkSection() {
            events.add("section");
        }

        @Override
        protected void printMiscSection() {
            events.add("misc");
        }

        @Override
        protected boolean isBootEmbedded() {
            return bootEmbedded;
        }

        @Override
        protected Thread shutdownHook() {
            return shutdownHook;
        }

        @Override
        protected void registerShutdownHook(Thread shutdownHook) {
            events.add(shutdownHook == this.shutdownHook ? "shutdownHook" : "wrongHook");
        }
    }
}
