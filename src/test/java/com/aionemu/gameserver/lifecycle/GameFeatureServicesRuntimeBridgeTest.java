package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class GameFeatureServicesRuntimeBridgeTest {

    @Test
    void usesSpringProvidersBeforeLegacySingletonFallbacks() {
        ProviderUsedException playerLimitProviderUsed = new ProviderUsedException();
        ProviderUsedException npcShoutsProviderUsed = new ProviderUsedException();
        ProviderUsedException shieldProviderUsed = new ProviderUsedException();
        ProviderUsedException rewardProviderUsed = new ProviderUsedException();
        ProviderUsedException weddingProviderUsed = new ProviderUsedException();
        ProviderUsedException veteranRewardsProviderUsed = new ProviderUsedException();
        ProviderUsedException disputeLandProviderUsed = new ProviderUsedException();
        ProviderUsedException outpostProviderUsed = new ProviderUsedException();
        ProviderUsedException protectorConquerorProviderUsed = new ProviderUsedException();
        ProviderUsedException dredgionProviderUsed = new ProviderUsedException();
        ProviderUsedException asyunatarProviderUsed = new ProviderUsedException();
        ProviderUsedException ffaProviderUsed = new ProviderUsedException();
        ProviderUsedException ladderProviderUsed = new ProviderUsedException();
        ProviderUsedException bgProviderUsed = new ProviderUsedException();
        ProviderUsedException banditProviderUsed = new ProviderUsedException();
        ProviderUsedException siegeProviderUsed = new ProviderUsedException();
        ProviderUsedException baseProviderUsed = new ProviderUsedException();
        GameFeatureServicesRuntimeBridge runtimeBridge = new GameFeatureServicesRuntimeBridge();

        runtimeBridge.setPlayerLimitServiceProvider(throwingProvider(playerLimitProviderUsed));
        runtimeBridge.setNpcShoutsServiceProvider(throwingProvider(npcShoutsProviderUsed));
        runtimeBridge.setShieldServiceProvider(throwingProvider(shieldProviderUsed));
        runtimeBridge.setRewardServiceProvider(throwingProvider(rewardProviderUsed));
        runtimeBridge.setWeddingServiceProvider(throwingProvider(weddingProviderUsed));
        runtimeBridge.setVeteranRewardsServiceProvider(throwingProvider(veteranRewardsProviderUsed));
        runtimeBridge.setDisputeLandServiceProvider(throwingProvider(disputeLandProviderUsed));
        runtimeBridge.setOutpostServiceProvider(throwingProvider(outpostProviderUsed));
        runtimeBridge.setProtectorConquerorServiceProvider(throwingProvider(protectorConquerorProviderUsed));
        runtimeBridge.setDredgionServiceProvider(throwingProvider(dredgionProviderUsed));
        runtimeBridge.setAsyunatarServiceProvider(throwingProvider(asyunatarProviderUsed));
        runtimeBridge.setFfaServiceProvider(throwingProvider(ffaProviderUsed));
        runtimeBridge.setLadderServiceProvider(throwingProvider(ladderProviderUsed));
        runtimeBridge.setBgServiceProvider(throwingProvider(bgProviderUsed));
        runtimeBridge.setBanditServiceProvider(throwingProvider(banditProviderUsed));
        runtimeBridge.setSiegeServiceProvider(throwingProvider(siegeProviderUsed));
        runtimeBridge.setBaseServiceProvider(throwingProvider(baseProviderUsed));

        assertSame(playerLimitProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::playerLimitService));
        assertSame(npcShoutsProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::npcShoutsService));
        assertSame(shieldProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::shieldService));
        assertSame(rewardProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::rewardService));
        assertSame(weddingProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::weddingService));
        assertSame(veteranRewardsProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::veteranRewardsService));
        assertSame(disputeLandProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::disputeLandService));
        assertSame(outpostProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::outpostService));
        assertSame(protectorConquerorProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::protectorConquerorService));
        assertSame(dredgionProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::dredgionService));
        assertSame(asyunatarProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::asyunatarService));
        assertSame(ffaProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::ffaService));
        assertSame(ladderProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::ladderService));
        assertSame(bgProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::bgService));
        assertSame(banditProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::banditService));
        assertSame(siegeProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::siegeService));
        assertSame(baseProviderUsed, assertThrows(ProviderUsedException.class, runtimeBridge::baseService));
    }

    @Test
    void gameServerCodeUsesFeatureNpcShoutsBridgeInsteadOfDirectSingleton() throws IOException {
        List<Path> sources;
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("services/NpcShoutsService.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameFeatureServices.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameFeatureServicesRuntimeBridge.java")))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("NpcShoutsService.getInstance()"), source.toString());
        }
    }

    @Test
    void gameServerCodeUsesFeatureSiegeBridgeInsteadOfDirectSingleton() throws IOException {
        List<Path> sources;
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("services/SiegeService.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameFeatureServices.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameFeatureServicesRuntimeBridge.java")))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("SiegeService.getInstance()"), source.toString());
        }
    }

    @Test
    void gameServerCodeUsesFeatureBaseBridgeInsteadOfDirectSingleton() throws IOException {
        List<Path> sources;
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("services/BaseService.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameFeatureServices.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameFeatureServicesRuntimeBridge.java")))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("BaseService.getInstance()"), source.toString());
        }
    }

    @Test
    void gameServerCodeUsesFeatureSystemMailBridgeInsteadOfDirectSingleton() throws IOException {
        List<Path> sources;
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("services/mail/SystemMailService.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameFeatureServices.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameFeatureServicesRuntimeBridge.java")))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("SystemMailService.getInstance()"), source.toString());
        }
    }

    @Test
    void gameServerCodeUsesFeatureBridgeInsteadOfDirectArenaPetAndKiskSingletons() throws IOException {
        List<Path> sources;
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("services/events/FFAService.java")))
                .filter(path -> !path.endsWith(Path.of("services/events/LadderService.java")))
                .filter(path -> !path.endsWith(Path.of("services/toypet/PetService.java")))
                .filter(path -> !path.endsWith(Path.of("services/KiskService.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameFeatureServices.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameFeatureServicesRuntimeBridge.java")))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("FFAService.getInstance()"), source.toString());
            assertFalse(content.contains("LadderService.getInstance()"), source.toString());
            assertFalse(content.contains("PetService.getInstance()"), source.toString());
            assertFalse(content.contains("KiskService.getInstance()"), source.toString());
        }
    }

    @Test
    void gameServerCodeUsesFeaturePlayerActionBridgeInsteadOfDirectSingletons() throws IOException {
        List<Path> sources;
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("services/instance/DredgionService2.java")))
                .filter(path -> !path.endsWith(Path.of("services/instance/AsyunatarService.java")))
                .filter(path -> !path.endsWith(Path.of("services/ShieldService.java")))
                .filter(path -> !path.endsWith(Path.of("services/WeddingService.java")))
                .filter(path -> !path.endsWith(Path.of("services/ProtectorConquerorService.java")))
                .filter(path -> !path.endsWith(Path.of("services/AStationService.java")))
                .filter(path -> !path.endsWith(Path.of("services/F2pService.java")))
                .filter(path -> !path.endsWith(Path.of("services/WindyGorgeService.java")))
                .filter(path -> !path.endsWith(Path.of("services/MotionLoggingService.java")))
                .filter(path -> !path.endsWith(Path.of("services/RepurchaseService.java")))
                .filter(path -> !path.endsWith(Path.of("services/drop/DropDistributionService.java")))
                .filter(path -> !path.endsWith(Path.of("services/events/ArcadeUpgradeService.java")))
                .filter(path -> !path.endsWith(Path.of("services/player/AtreianBestiaryService.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameFeatureServices.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameFeatureServicesRuntimeBridge.java")))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("DredgionService2.getInstance()"), source.toString());
            assertFalse(content.contains("AsyunatarService.getInstance()"), source.toString());
            assertFalse(content.contains("ShieldService.getInstance()"), source.toString());
            assertFalse(content.contains("WeddingService.getInstance()"), source.toString());
            assertFalse(content.contains("ProtectorConquerorService.getInstance()"), source.toString());
            assertFalse(content.contains("AStationService.getInstance()"), source.toString());
            assertFalse(content.contains("F2pService.getInstance()"), source.toString());
            assertFalse(content.contains("WindyGorgeService.getInstance()"), source.toString());
            assertFalse(content.contains("MotionLoggingService.getInstance()"), source.toString());
            assertFalse(content.contains("RepurchaseService.getInstance()"), source.toString());
            assertFalse(content.contains("DropDistributionService.getInstance()"), source.toString());
            assertFalse(content.contains("ArcadeUpgradeService.getInstance()"), source.toString());
            assertFalse(content.contains("AtreianBestiaryService.getInstance()"), source.toString());
        }
    }

    @Test
    void gameServerCodeUsesFeatureEventBridgeInsteadOfDirectSingletons() throws IOException {
        List<Path> sources;
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("services/DisputeLandService.java")))
                .filter(path -> !path.endsWith(Path.of("services/events/BanditService.java")))
                .filter(path -> !path.endsWith(Path.of("services/StaticDoorService.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameFeatureServices.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameFeatureServicesRuntimeBridge.java")))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("DisputeLandService.getInstance()"), source.toString());
            assertFalse(content.contains("BanditService.getInstance()"), source.toString());
            assertFalse(content.contains("StaticDoorService.getInstance()"), source.toString());
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
