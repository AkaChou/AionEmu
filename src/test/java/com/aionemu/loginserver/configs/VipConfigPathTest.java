package com.aionemu.loginserver.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VipConfigPathTest {

    @TempDir
    Path tempDir;
    private final boolean originalAutoEnable = VipConfig.AUTO_ENABLE;
    private final int originalAutoEnableLevel = VipConfig.AUTO_ENABLE_LEVEL;

    @AfterEach
    void resetConfig() {
        System.clearProperty("aion.config.dir");
        Config.setBootOverrides(null);
        VipConfig.AUTO_ENABLE = originalAutoEnable;
        VipConfig.AUTO_ENABLE_LEVEL = originalAutoEnableLevel;
    }

    @Test
    void loadsVipSettingsFromMainConfigDirectory() throws Exception {
        Path configDir = tempDir.resolve("config");
        Files.createDirectories(configDir.resolve("login"));
        Files.createDirectories(configDir.resolve("network"));
        Files.createDirectories(configDir.resolve("main"));
        Files.writeString(configDir.resolve("network/network.properties"), "");
        Files.writeString(configDir.resolve("main/vip.properties"), """
            loginserver.vip.auto-enable = true
            loginserver.vip.auto-enable.level = 4
            """);
        System.setProperty("aion.config.dir", configDir.toString());

        Config.load();

        assertTrue(VipConfig.AUTO_ENABLE);
        assertEquals(4, VipConfig.AUTO_ENABLE_LEVEL);
    }
}
