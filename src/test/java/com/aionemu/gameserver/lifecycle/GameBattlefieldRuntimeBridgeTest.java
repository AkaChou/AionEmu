package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Test
    void runtimeBridgeDoesNotCallLegacySingletonsDirectly() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/lifecycle/GameBattlefieldRuntimeBridge.java"));

        assertFalse(source.contains("KamarBattlefieldService.getInstance()"));
        assertFalse(source.contains("EngulfedOphidanBridgeService.getInstance()"));
        assertFalse(source.contains("SuspiciousOphidanBridgeService.getInstance()"));
        assertFalse(source.contains("IronWallWarfrontService.getInstance()"));
        assertFalse(source.contains("IdgelDomeService.getInstance()"));
        assertFalse(source.contains("IdgelDomeLandmarkService.getInstance()"));
        assertFalse(source.contains("HallOfTenacityService.getInstance()"));
        assertFalse(source.contains("GrandArenaTrainingCampService.getInstance()"));
        assertFalse(source.contains("IDRunService.getInstance()"));
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
