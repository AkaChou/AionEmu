package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

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

    @Test
    void networkStartupGatewayBridgesRuntimeCallsThroughSpringProviders() {
        List<String> events = new ArrayList<>();
        Thread hook = new Thread();
        RecordingGameNetworkStartupRuntimeBridge bridge = new RecordingGameNetworkStartupRuntimeBridge(events, hook);
        GameNetworkStartupGateway gateway = new GameNetworkStartupGateway();
        gateway.setRuntimeBridgeProvider(provider(GameNetworkStartupRuntimeBridge.class, bridge));

        assertSame(hook, gateway.shutdownHook());
        assertFalse(gateway.isBootEmbedded());
        gateway.registerShutdownHook(hook);
        assertEquals(42L, gateway.currentTimeMillis());

        assertEquals(List.of("shutdownHook", "isBootEmbedded", "registerShutdownHook", "currentTimeMillis"), events);
        assertSame(hook, bridge.registeredHook);
    }

    @Test
    void networkStartupGatewayBridgesRuntimeBoundaryThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameNetworkStartupGateway.class, "runtimeBridgeProvider"));
    }

    @Test
    void networkStartupRuntimeBridgeUsesShutdownHookProviderBeforeLegacySingletonFallback() {
        ProviderUsedException providerUsed = new ProviderUsedException();
        GameNetworkStartupRuntimeBridge runtimeBridge = new GameNetworkStartupRuntimeBridge();
        runtimeBridge.setShutdownHookProvider(throwingProvider(providerUsed));

        assertSame(providerUsed, assertThrows(ProviderUsedException.class, runtimeBridge::shutdownHook));
    }

    @Test
    void networkStartupRuntimeBridgeDoesNotCallLegacyShutdownHookDirectly() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/lifecycle/GameNetworkStartupRuntimeBridge.java"));

        assertFalse(source.contains("ShutdownHook.getInstance()"));
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

    private static final class RecordingGameNetworkStartupRuntimeBridge extends GameNetworkStartupRuntimeBridge {

        private final List<String> events;
        private final Thread shutdownHook;
        private Thread registeredHook;

        private RecordingGameNetworkStartupRuntimeBridge(List<String> events, Thread shutdownHook) {
            this.events = events;
            this.shutdownHook = shutdownHook;
        }

        @Override
        public boolean isBootEmbedded() {
            events.add("isBootEmbedded");
            return false;
        }

        @Override
        public Thread shutdownHook() {
            events.add("shutdownHook");
            return shutdownHook;
        }

        @Override
        public void registerShutdownHook(Thread shutdownHook) {
            events.add("registerShutdownHook");
            registeredHook = shutdownHook;
        }

        @Override
        public long currentTimeMillis() {
            events.add("currentTimeMillis");
            return 42L;
        }
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }

    private static <T> ObjectProvider<T> throwingProvider(ProviderUsedException exception) {
        return ObjectProvider.class.cast(Proxy.newProxyInstance(
            ObjectProvider.class.getClassLoader(),
            new Class<?>[] { ObjectProvider.class },
            (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "toString" -> "throwingProvider";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> null;
                    };
                }
                throw exception;
            }
        ));
    }

    private static final class ProviderUsedException extends RuntimeException {
    }
}
