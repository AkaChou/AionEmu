package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * R2 冻结审计：GameLocationBootstrapServices 静态定位器已退役，绑定职责由 LocationBootstrapServiceBindings 承担。
 * R2 freeze audit: the GameLocationBootstrapServices static locator is retired; the binding duty moved to
 * LocationBootstrapServiceBindings. Consumers use each domain service's fail-fast getInstance() channel or
 * constructor injection instead.
 */
class LocationBootstrapServiceBindingsTest {

    @Test
    void retiredLocatorClassDoesNotComeBack() {
        assertFalse(
            Files.exists(Path.of("src/main/java/com/aionemu/gameserver/lifecycle/GameLocationBootstrapServices.java")),
            "GameLocationBootstrapServices 定位器已退役，不得重新引入（R2，绑定职责见 LocationBootstrapServiceBindings）");
    }

    @Test
    void productionCodeNeverTouchesTheRetiredLocator() throws IOException {
        try (var stream = Files.walk(Path.of("src/main/java"))) {
            for (Path source : stream.filter(path -> path.toString().endsWith(".java")).toList()) {
                String content = Files.readString(source);
                assertFalse(content.contains("GameLocationBootstrapServices"), source.toString());
            }
        }
    }

    @Test
    void bindingsComponentFeedsEveryDomainServiceChannel() throws IOException {
        String source = Files.readString(
            Path.of("src/main/java/com/aionemu/gameserver/lifecycle/LocationBootstrapServiceBindings.java"));

        for (String service : new String[] {"VortexService", "BeritraService", "AgentService", "AnohaService",
            "SvsService", "RvrService", "IuService", "NightmareCircusService", "DynamicRiftService",
            "InstanceRiftService", "SiegeService", "BaseService", "OutpostService", "ZorshivDredgionService",
            "MoltenusService", "RiftService", "ConquestService", "IdianDepthsService", "TowerOfEternityService",
            "AbyssLandingService", "LandingUpdateService", "AbyssLandingSpecialService"}) {
            assertTrue(source.contains(service + ".setInstanceProvider("),
                "绑定组件必须喂入 " + service + " 的 fail-fast 单例通道");
        }
    }
}
