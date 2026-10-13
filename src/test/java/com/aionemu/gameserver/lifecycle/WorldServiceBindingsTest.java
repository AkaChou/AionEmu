package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * R4 冻结审计：GameWorldServices 静态定位器已退役，绑定职责由 WorldServiceBindings 承担。
 * R4 freeze audit: the GameWorldServices static locator is retired; the binding duty moved to
 * WorldServiceBindings. Consumers use each domain service's fail-fast getInstance() channel instead.
 */
class WorldServiceBindingsTest {

    @Test
    void retiredLocatorClassDoesNotComeBack() {
        assertFalse(
            Files.exists(Path.of("src/main/java/com/aionemu/gameserver/lifecycle/GameWorldServices.java")),
            "GameWorldServices 定位器已退役，不得重新引入（R4，绑定职责见 WorldServiceBindings）");
    }

    @Test
    void productionCodeNeverTouchesTheRetiredLocator() throws IOException {
        try (var stream = Files.walk(Path.of("src/main/java"))) {
            for (Path source : stream.filter(path -> path.toString().endsWith(".java")).toList()) {
                String content = Files.readString(source);
                assertFalse(content.contains("GameWorldServices"), source.toString());
            }
        }
    }

    @Test
    void bindingsComponentFeedsEveryDomainServiceChannel() throws IOException {
        String source = Files.readString(
            Path.of("src/main/java/com/aionemu/gameserver/lifecycle/WorldServiceBindings.java"));

        for (String service : new String[] {"GeoService", "PathService", "DropRegistrationService"}) {
            assertTrue(source.contains(service + ".setInstanceProvider("),
                "绑定组件必须喂入 " + service + " 的 fail-fast 单例通道");
        }
    }
}
