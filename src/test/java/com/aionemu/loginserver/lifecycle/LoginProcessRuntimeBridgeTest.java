package com.aionemu.loginserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aionemu.loginserver.Shutdown;
import java.lang.reflect.Proxy;
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

    private static final class ProviderUsedException extends RuntimeException {
    }
}
