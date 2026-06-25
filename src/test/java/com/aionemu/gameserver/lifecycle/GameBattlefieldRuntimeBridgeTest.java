package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class GameBattlefieldRuntimeBridgeTest {

    @Test
    void usesSpringProvidersBeforeLegacySingletonFallbacks() {
        ProviderUsedException kamarProviderUsed = new ProviderUsedException();
        ProviderUsedException engulfedOphidanProviderUsed = new ProviderUsedException();
        ProviderUsedException suspiciousOphidanProviderUsed = new ProviderUsedException();
        ProviderUsedException ironWallProviderUsed = new ProviderUsedException();
        ProviderUsedException idgelDomeProviderUsed = new ProviderUsedException();
        ProviderUsedException idgelDomeLandmarkProviderUsed = new ProviderUsedException();
        ProviderUsedException hallOfTenacityProviderUsed = new ProviderUsedException();
        ProviderUsedException grandArenaProviderUsed = new ProviderUsedException();
        ProviderUsedException idRunProviderUsed = new ProviderUsedException();
        GameBattlefieldRuntimeBridge runtimeBridge = new GameBattlefieldRuntimeBridge();

        runtimeBridge.setKamarBattlefieldServiceProvider(throwingProvider(kamarProviderUsed));
        runtimeBridge.setEngulfedOphidanBridgeServiceProvider(throwingProvider(engulfedOphidanProviderUsed));
        runtimeBridge.setSuspiciousOphidanBridgeServiceProvider(throwingProvider(suspiciousOphidanProviderUsed));
        runtimeBridge.setIronWallWarfrontServiceProvider(throwingProvider(ironWallProviderUsed));
        runtimeBridge.setIdgelDomeServiceProvider(throwingProvider(idgelDomeProviderUsed));
        runtimeBridge.setIdgelDomeLandmarkServiceProvider(throwingProvider(idgelDomeLandmarkProviderUsed));
        runtimeBridge.setHallOfTenacityServiceProvider(throwingProvider(hallOfTenacityProviderUsed));
        runtimeBridge.setGrandArenaTrainingCampServiceProvider(throwingProvider(grandArenaProviderUsed));
        runtimeBridge.setIdRunServiceProvider(throwingProvider(idRunProviderUsed));

        assertSame(kamarProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::kamarBattlefieldService));
        assertSame(engulfedOphidanProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::engulfedOphidanBridgeService));
        assertSame(suspiciousOphidanProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::suspiciousOphidanBridgeService));
        assertSame(ironWallProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::ironWallWarfrontService));
        assertSame(idgelDomeProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::idgelDomeService));
        assertSame(idgelDomeLandmarkProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::idgelDomeLandmarkService));
        assertSame(hallOfTenacityProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::hallOfTenacityService));
        assertSame(grandArenaProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::grandArenaTrainingCampService));
        assertSame(idRunProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::idRunService));
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
