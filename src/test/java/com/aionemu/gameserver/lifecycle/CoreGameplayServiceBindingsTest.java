package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * R1 冻结审计：GameCoreGameplayServices 静态定位器已退役，绑定职责由 CoreGameplayServiceBindings 承担。
 * R1 freeze audit: the GameCoreGameplayServices static locator is retired; the binding duty moved to
 * CoreGameplayServiceBindings. Consumers use each domain service's fail-fast getInstance() channel or
 * constructor injection instead.
 */
class CoreGameplayServiceBindingsTest {

    @Test
    void retiredLocatorClassDoesNotComeBack() {
        assertFalse(
            Files.exists(Path.of("src/main/java/com/aionemu/gameserver/lifecycle/GameCoreGameplayServices.java")),
            "GameCoreGameplayServices 定位器已退役，不得重新引入（R1，绑定职责见 CoreGameplayServiceBindings）");
    }

    @Test
    void productionCodeNeverTouchesTheRetiredLocator() throws IOException {
        try (var stream = Files.walk(Path.of("src/main/java"))) {
            for (Path source : stream.filter(path -> path.toString().endsWith(".java")).toList()) {
                String content = Files.readString(source);
                assertFalse(content.contains("GameCoreGameplayServices"), source.toString());
            }
        }
    }

    @Test
    void bindingsComponentFeedsEveryDomainServiceChannel() throws IOException {
        String source = Files.readString(
            Path.of("src/main/java/com/aionemu/gameserver/lifecycle/CoreGameplayServiceBindings.java"));

        for (String service : new String[] {"DropService", "MailService", "PvpService", "AutoGroupService",
            "AbyssRankingCache", "LegionService", "BalaurAssaultService", "BattlefieldUnionService"}) {
            assertTrue(source.contains(service + ".setInstanceProvider("),
                "绑定组件必须喂入 " + service + " 的 fail-fast 单例通道");
        }
    }
}
