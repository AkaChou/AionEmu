package com.aionemu.loginserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aionemu.loginserver.Shutdown;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class LoginProcessRuntimeBridgeTest {

    @Test
    void usesShutdownProviderBeforeLegacySingletonFallbackForShutdownHook() {
        ProviderUsedException providerUsed = new ProviderUsedException();
        LoginProcessRuntimeBridge runtimeBridge = new LoginProcessRuntimeBridge();
        runtimeBridge.setShutdownProvider(throwingProvider(providerUsed));

        assertSame(providerUsed, assertThrows(ProviderUsedException.class, runtimeBridge::shutdownHook));
    }

    @Test
    void usesShutdownProviderBeforeLegacySingletonFallbackForShutdown() {
        ProviderUsedException providerUsed = new ProviderUsedException();
        LoginProcessRuntimeBridge runtimeBridge = new LoginProcessRuntimeBridge();
        runtimeBridge.setShutdownProvider(throwingProvider(providerUsed));

        assertSame(providerUsed, assertThrows(ProviderUsedException.class, () -> runtimeBridge.shutdown(true)));
    }

    @Test
    void prepareShutdownCachesShutdownBeforeProviderBecomesUnavailable() {
        List<String> events = new ArrayList<>();
        LoginProcessRuntimeBridge runtimeBridge = new LoginProcessRuntimeBridge();
        runtimeBridge.setShutdownProvider(oneShotProvider(new RecordingShutdown(events)));

        runtimeBridge.prepareShutdown();
        runtimeBridge.shutdown(false);

        assertEquals(List.of("shutdown:false"), events);
    }

    private static ObjectProvider<Shutdown> throwingProvider(ProviderUsedException exception) {
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

    private static ObjectProvider<Shutdown> oneShotProvider(Shutdown shutdown) {
        AtomicBoolean used = new AtomicBoolean();
        return ObjectProvider.class.cast(Proxy.newProxyInstance(
            ObjectProvider.class.getClassLoader(),
            new Class<?>[] { ObjectProvider.class },
            (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "toString" -> "oneShotProvider";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> null;
                    };
                }
                if ("getIfAvailable".equals(method.getName())) {
                    if (!used.compareAndSet(false, true)) {
                        throw new ProviderUsedAfterPreparationException();
                    }
                    return shutdown;
                }
                throw new UnsupportedOperationException(method.toString());
            }
        ));
    }

    private static final class RecordingShutdown extends Shutdown {

        private final List<String> events;

        private RecordingShutdown(List<String> events) {
            this.events = events;
        }

        @Override
        public void shutdown(boolean haltJvm) {
            events.add("shutdown:" + haltJvm);
        }
    }

    private static final class ProviderUsedException extends RuntimeException {
    }

    private static final class ProviderUsedAfterPreparationException extends RuntimeException {
    }
}
