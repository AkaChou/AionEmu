package com.aionemu.gameserver.configs.network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.gameserver.configs.Config;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IPConfigTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        System.clearProperty("aion.config.dir");
        Config.setBootOverrides(null);
        NetworkConfig.PUBLIC_ADDRESS = null;
    }

    @Test
    void loadUsesPublicNetworkAddress() throws Exception {
        NetworkConfig.PUBLIC_ADDRESS = "203.0.113.10";

        IPConfig.load();

        assertArrayEquals(InetAddress.getByName("203.0.113.10").getAddress(), IPConfig.getDefaultAddress());
    }

    @Test
    void loadFallsBackToLoopbackWhenAddressIsMissing() throws Exception {
        IPConfig.load();

        assertArrayEquals(InetAddress.getByName("127.0.0.1").getAddress(), IPConfig.getDefaultAddress());
    }

    @Test
    void configLoadKeepsPublicAndLoginAddressesSeparate() throws Exception {
        Files.createDirectories(tempDir.resolve("administration"));
        Files.createDirectories(tempDir.resolve("main"));
        Path networkDir = tempDir.resolve("network");
        Files.createDirectories(networkDir);
        Files.writeString(networkDir.resolve("network.properties"), """
            gameserver.network.address = 203.0.113.10
            gameserver.network.login.address = 192.168.1.18:9014
            chatserver.network.public.address = 203.0.113.30:10241
            """);
        System.setProperty("aion.config.dir", tempDir.toString());

        Config.load();

        InetSocketAddress loginAddress = NetworkConfig.LOGIN_ADDRESS;
        assertEquals("192.168.1.18", loginAddress.getAddress().getHostAddress());
        assertEquals(9014, loginAddress.getPort());
        assertEquals("203.0.113.30", NetworkConfig.PUBLIC_CHAT_ADDRESS.getAddress().getHostAddress());
        assertArrayEquals(InetAddress.getByName("203.0.113.10").getAddress(), IPConfig.getDefaultAddress());
    }
}
