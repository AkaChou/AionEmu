package com.aionemu.boot.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LegacyServerConfigOverridesTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void resetConfig() {
        System.clearProperty("aion.login.config.dir");
        System.clearProperty("aion.chat.config.dir");
        com.aionemu.loginserver.configs.Config.setBootOverrides(null);
        com.aionemu.chatserver.configs.Config.setBootOverrides(null);
    }

    @Test
    void loginBootOverridesWinAfterLegacyFilesAndMylsOverrides() throws Exception {
        Path configDir = tempDir.resolve("login");
        Files.createDirectories(configDir.resolve("network"));
        Files.writeString(configDir.resolve("network/network.properties"), """
            loginserver.network.client.port=2107
            """);
        Files.writeString(configDir.resolve("myls.properties"), """
            loginserver.network.client.port=2207
            """);
        System.setProperty("aion.login.config.dir", configDir.toString());

        LegacyLoginProperties properties = new LegacyLoginProperties();
        properties.getProperty().put("loginserver.network.client.port", "2307");
        new LegacyLoginConfigOverrides(properties).applyToLoginConfig();

        com.aionemu.loginserver.configs.Config.load();

        assertEquals(2307, com.aionemu.loginserver.configs.Config.LOGIN_PORT);
    }

    @Test
    void chatBootOverridesWinAfterLegacyFilesAndMycsOverrides() throws Exception {
        Path configDir = tempDir.resolve("chat");
        Files.createDirectories(configDir);
        Files.writeString(configDir.resolve("chatserver.properties"), """
            chatserver.chat.lang=1
            """);
        Files.writeString(configDir.resolve("mycs.properties"), """
            chatserver.chat.lang=2
            """);
        System.setProperty("aion.chat.config.dir", configDir.toString());

        LegacyChatProperties properties = new LegacyChatProperties();
        properties.getProperty().put("chatserver.chat.lang", "3");
        new LegacyChatConfigOverrides(properties).applyToChatConfig();

        com.aionemu.chatserver.configs.Config.load();

        assertEquals(3, com.aionemu.chatserver.configs.Config.LANG_CHAT);
    }
}
