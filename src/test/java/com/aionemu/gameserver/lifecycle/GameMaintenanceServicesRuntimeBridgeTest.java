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

class GameMaintenanceServicesRuntimeBridgeTest {

    @Test
    void usesSpringProvidersBeforeLegacySingletonFallbacks() {
        ProviderUsedException databaseCleaningProviderUsed = new ProviderUsedException();
        ProviderUsedException abyssRankCleaningProviderUsed = new ProviderUsedException();
        ProviderUsedException shugoImperialTombProviderUsed = new ProviderUsedException();
        ProviderUsedException seasonRankingProviderUsed = new ProviderUsedException();
        GameMaintenanceServicesRuntimeBridge runtimeBridge = new GameMaintenanceServicesRuntimeBridge();

        runtimeBridge.setDatabaseCleaningServiceProvider(throwingProvider(databaseCleaningProviderUsed));
        runtimeBridge.setAbyssRankCleaningServiceProvider(throwingProvider(abyssRankCleaningProviderUsed));
        runtimeBridge.setShugoImperialTombSpawnManagerProvider(throwingProvider(shugoImperialTombProviderUsed));
        runtimeBridge.setSeasonRankingUpdateServiceProvider(throwingProvider(seasonRankingProviderUsed));

        assertSame(databaseCleaningProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::databaseCleaningService));
        assertSame(abyssRankCleaningProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::abyssRankCleaningService));
        assertSame(shugoImperialTombProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::shugoImperialTombSpawnManager));
        assertSame(seasonRankingProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::seasonRankingUpdateService));
    }

    @Test
    void runtimeBridgeDoesNotCallLegacySingletonsDirectly() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/lifecycle/GameMaintenanceServicesRuntimeBridge.java"));

        assertFalse(source.contains("DatabaseCleaningService.getInstance()"));
        assertFalse(source.contains("AbyssRankCleaningService.getInstance()"));
        assertFalse(source.contains("ShugoImperialTombSpawnManager.getInstance()"));
        assertFalse(source.contains("SeasonRankingUpdateService.getInstance()"));
    }

    @Test
    void gameServerCodeUsesMaintenanceBridgeInsteadOfDirectSingleton() throws IOException {
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            for (Path source : stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("services/ranking/SeasonRankingUpdateService.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameMaintenanceServiceFallbacks.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameMaintenanceServices.java")))
                .toList()) {
                String content = Files.readString(source);

                assertFalse(content.contains("SeasonRankingUpdateService.getInstance()"), source.toString());
            }
        }
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
