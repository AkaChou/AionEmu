package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * R3 冻结审计：GameFeatureServices 静态定位器已退役，绑定职责由 FeatureServiceBindings 承担。
 * R3 freeze audit: the GameFeatureServices static locator is retired; the binding duty moved to
 * FeatureServiceBindings. Consumers use each domain service's fail-fast getInstance() channel instead.
 */
class FeatureServiceBindingsTest {

    @Test
    void retiredLocatorClassDoesNotComeBack() {
        assertFalse(
            Files.exists(Path.of("src/main/java/com/aionemu/gameserver/lifecycle/GameFeatureServices.java")),
            "GameFeatureServices 定位器已退役，不得重新引入（R3，绑定职责见 FeatureServiceBindings）");
    }

    @Test
    void productionCodeNeverTouchesTheRetiredLocator() throws IOException {
        try (var stream = Files.walk(Path.of("src/main/java"))) {
            for (Path source : stream.filter(path -> path.toString().endsWith(".java")).toList()) {
                String content = Files.readString(source);
                assertFalse(content.contains("GameFeatureServices"), source.toString());
            }
        }
    }

    @Test
    void bindingsComponentFeedsEveryDomainServiceChannel() throws IOException {
        String source = Files.readString(
            Path.of("src/main/java/com/aionemu/gameserver/lifecycle/FeatureServiceBindings.java"));

        for (String service : new String[] {"DisputeLandService", "DredgionService2", "AsyunatarService",
            "PlayerLimitService", "NpcShoutsService", "ShieldService", "RewardService", "VeteranRewardsService",
            "ProtectorConquerorService", "FFAService", "LadderService", "BGService", "BanditService", "SiegeService",
            "BaseService", "AStationService", "F2pService", "WindyGorgeService", "MotionLoggingService",
            "StaticDoorService", "KiskService", "RepurchaseService", "DropDistributionService", "SystemMailService",
            "BonusService", "PetService", "ArcadeUpgradeService", "AtreianBestiaryService", "CoalescenceService",
            "GrowthEnergy"}) {
            assertTrue(source.contains(service + ".setInstanceProvider("),
                "绑定组件必须喂入 " + service + " 的 fail-fast 单例通道");
        }
    }
}
