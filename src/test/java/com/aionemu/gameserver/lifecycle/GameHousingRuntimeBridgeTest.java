package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class GameHousingRuntimeBridgeTest {

    @Test
    void usesSpringProvidersBeforeLegacySingletonFallbacks() {
        ProviderUsedException housingBidProviderUsed = new ProviderUsedException();
        ProviderUsedException maintenanceTaskProviderUsed = new ProviderUsedException();
        ProviderUsedException townServiceProviderUsed = new ProviderUsedException();
        ProviderUsedException challengeTaskServiceProviderUsed = new ProviderUsedException();
        GameHousingRuntimeBridge runtimeBridge = new GameHousingRuntimeBridge();

        runtimeBridge.setHousingBidServiceProvider(throwingProvider(housingBidProviderUsed));
        runtimeBridge.setMaintenanceTaskProvider(throwingProvider(maintenanceTaskProviderUsed));
        runtimeBridge.setTownServiceProvider(throwingProvider(townServiceProviderUsed));
        runtimeBridge.setChallengeTaskServiceProvider(throwingProvider(challengeTaskServiceProviderUsed));

        assertSame(housingBidProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::housingBidService));
        assertSame(maintenanceTaskProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::maintenanceTask));
        assertSame(townServiceProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::townService));
        assertSame(challengeTaskServiceProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::challengeTaskService));
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
