package com.aionemu.gameserver.configs.network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.aionemu.gameserver.configs.Config;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IPConfigTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        System.clearProperty("aion.game.config.dir");
        Config.setBootOverrides(new Properties());
    }

    @Test
    void loadUsesBootOverrideForDefaultAddress() throws Exception {
        Path networkDir = tempDir.resolve("network");
        Files.createDirectories(networkDir);
        Files.writeString(networkDir.resolve("ipconfig.xml"), """
            <ipconfig default="192.168.1.18">
            </ipconfig>
            """);
        System.setProperty("aion.game.config.dir", tempDir.toString());

        Properties overrides = new Properties();
        overrides.setProperty("gameserver.network.ipconfig.default", "203.0.113.10");
        Config.setBootOverrides(overrides);

        IPConfig.load();

        assertArrayEquals(InetAddress.getByName("203.0.113.10").getAddress(), IPConfig.getDefaultAddress());
    }
}
