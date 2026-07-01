package com.aionemu.boot.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AionServicePathsTest {

    @TempDir
    Path aionHome;

    @BeforeEach
    @AfterEach
    void restoreProperties() {
        System.clearProperty("aion.home");
        System.clearProperty("aion.logging.config");
        System.clearProperty("aion.login.config.dir");
        System.clearProperty("aion.login.data.dir");
        System.clearProperty("aion.chat.config.dir");
        System.clearProperty("aion.game.config.dir");
        System.clearProperty("aion.game.data.dir");
        System.clearProperty("aion.game.cache.dir");
    }

    @Test
    void configuresDefaultRuntimeDirectoriesUnderAionHome() {
        System.setProperty("aion.home", aionHome.toString());

        AionServicePaths.configureLogin();
        AionServicePaths.configureChat();
        AionServicePaths.configureGame();

        assertEquals(aionHome.resolve("login/config").toString(), System.getProperty("aion.login.config.dir"));
        assertEquals(aionHome.resolve("login/data").toString(), System.getProperty("aion.login.data.dir"));
        assertEquals(aionHome.resolve("chat/config").toString(), System.getProperty("aion.chat.config.dir"));
        assertEquals(aionHome.resolve("game/config").toString(), System.getProperty("aion.game.config.dir"));
        assertEquals(aionHome.resolve("game/data").toString(), System.getProperty("aion.game.data.dir"));
        assertEquals(aionHome.resolve("game/cache").toString(), System.getProperty("aion.game.cache.dir"));
        assertEquals(aionHome.resolve("log/logback-spring.xml").toString(), System.getProperty("aion.logging.config"));

        assertTrue(aionHome.resolve("log/logback-spring.xml").toFile().isFile());
        assertTrue(aionHome.resolve("login/config/network/database.properties").toFile().isFile());
        assertTrue(aionHome.resolve("chat/config/chatserver.properties").toFile().isFile());
        assertTrue(aionHome.resolve("game/config/main/gameserver.properties").toFile().isFile());
        assertTrue(aionHome.resolve("game/data/static_data/items/item/item_misc_templates.xml").toFile().isFile());
        assertTrue(aionHome.resolve("game/cache").toFile().isDirectory());
    }

    @Test
    void usesProjectResourceGameDataDirectoryWhenAvailable() throws Exception {
        Path sourceGameData = aionHome.resolve("src/main/resources/aion/game/data");
        java.nio.file.Files.createDirectories(sourceGameData.resolve("geo"));
        java.nio.file.Files.writeString(sourceGameData.resolve("geo/100.geo"), "geo");

        System.setProperty("aion.home", aionHome.toString());

        AionServicePaths.configureGame();

        assertEquals(sourceGameData.toString(), System.getProperty("aion.game.data.dir"));
    }

    @Test
    void prefersRuntimeGameConfigDirectoryOverProjectResources() throws Exception {
        Path sourceGameConfig = aionHome.resolve("src/main/resources/aion/game/config");
        java.nio.file.Files.createDirectories(sourceGameConfig.resolve("main"));
        java.nio.file.Files.writeString(sourceGameConfig.resolve("main/geodata.properties"), "gameserver.geo.nav.pathfinding.enable = true");

        System.setProperty("aion.home", aionHome.toString());

        AionServicePaths.configureGame();

        assertEquals(aionHome.resolve("game/config").toString(), System.getProperty("aion.game.config.dir"));
        assertTrue(aionHome.resolve("game/config/main/gameserver.properties").toFile().isFile());
    }

    @Test
    void prefersRuntimeLogbackFileOverProjectResources() throws Exception {
        Path sourceLogback = aionHome.resolve("src/main/resources/logback-spring.xml");
        java.nio.file.Files.createDirectories(sourceLogback.getParent());
        java.nio.file.Files.writeString(sourceLogback, "<configuration/>");

        System.setProperty("aion.home", aionHome.toString());

        AionServicePaths.configureGame();

        assertEquals(aionHome.resolve("log/logback-spring.xml").toString(), System.getProperty("aion.logging.config"));
        assertTrue(aionHome.resolve("log/logback-spring.xml").toFile().isFile());
    }

    @Test
    void keepsExplicitRuntimeDirectories() {
        Path explicitLoginConfig = aionHome.resolve("custom/login-config");
        Path explicitGameData = aionHome.resolve("custom/game-data");
        Path explicitLogback = aionHome.resolve("custom/logback-spring.xml");
        System.setProperty("aion.logging.config", explicitLogback.toString());
        System.setProperty("aion.login.config.dir", explicitLoginConfig.toString());
        System.setProperty("aion.game.data.dir", explicitGameData.toString());

        AionServicePaths.configureLogin();
        AionServicePaths.configureGame();

        assertEquals(explicitLoginConfig.toString(), System.getProperty("aion.login.config.dir"));
        assertEquals(explicitGameData.toString(), System.getProperty("aion.game.data.dir"));
        assertEquals(explicitLogback.toString(), System.getProperty("aion.logging.config"));
    }
}
