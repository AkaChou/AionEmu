package com.aionemu.boot.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
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
        System.clearProperty("aion.config.dir");
        System.clearProperty("aion.login.data.dir");
        System.clearProperty("aion.game.data.dir");
        System.clearProperty("aion.game.definitions.dir");
        System.clearProperty("aion.game.geo.dir");
        System.clearProperty("aion.game.cache.dir");
    }

    @Test
    void configuresDefaultRuntimeDirectoriesUnderAionHome() {
        System.setProperty("aion.home", aionHome.toString());

        AionServicePaths.configureLogin();
        assertTrue(aionHome.resolve("config/network/network.properties").toFile().isFile());
        AionServicePaths.configureChat();
        AionServicePaths.configureGame();

        assertEquals(aionHome.resolve("config").toString(), System.getProperty("aion.config.dir"));
        assertEquals(aionHome.resolve("login/data").toString(), System.getProperty("aion.login.data.dir"));
        assertEquals(aionHome.resolve("data").toString(), System.getProperty("aion.game.data.dir"));
        assertEquals(aionHome.resolve("definitions").toString(), System.getProperty("aion.game.definitions.dir"));
        assertEquals(aionHome.resolve("geo").toString(), System.getProperty("aion.game.geo.dir"));
        assertEquals(aionHome.resolve("game/cache").toString(), System.getProperty("aion.game.cache.dir"));
        assertEquals(aionHome.resolve("log/logback-spring.xml").toString(), System.getProperty("aion.logging.config"));

        assertTrue(aionHome.resolve("log/logback-spring.xml").toFile().isFile());
        assertTrue(aionHome.resolve("config/login/database.properties").toFile().isFile());
        assertTrue(aionHome.resolve("config/chat/chatserver.properties").toFile().isFile());
        assertTrue(aionHome.resolve("config/main/gameserver.properties").toFile().isFile());
		assertTrue(aionHome.resolve("definitions/items/item/item_misc_templates.xml").toFile().isFile());
        assertTrue(aionHome.resolve("game/cache").toFile().isDirectory());
    }

    @Test
    void usesProjectResourceGameDataDirectoryWhenAvailable() throws Exception {
        Path sourceGameData = aionHome.resolve("src/main/resources/aion/data");
        Path sourceDefinitions = aionHome.resolve("src/main/resources/aion/definitions");
        Path sourceGameGeo = aionHome.resolve("src/main/resources/aion/geo");
        java.nio.file.Files.createDirectories(sourceGameData);
        java.nio.file.Files.createDirectories(sourceDefinitions);
        java.nio.file.Files.createDirectories(sourceGameGeo);
        java.nio.file.Files.writeString(sourceGameGeo.resolve("100.geo"), "geo");

        System.setProperty("aion.home", aionHome.toString());

        AionServicePaths.configureGame();

        assertEquals(sourceGameData.toString(), System.getProperty("aion.game.data.dir"));
        assertEquals(sourceDefinitions.toString(), System.getProperty("aion.game.definitions.dir"));
        assertEquals(sourceGameGeo.toString(), System.getProperty("aion.game.geo.dir"));
    }

    @Test
    void usesCheckoutResourceGameDataDirectoryWhenAionHomeIsDefault() throws Exception {
        Method configureSourceResourceDirectory = AionServicePaths.class.getDeclaredMethod("configureSourceResourceDirectory", String.class,
            String.class);
        configureSourceResourceDirectory.setAccessible(true);

        assertTrue((boolean) configureSourceResourceDirectory.invoke(null, "aion.game.data.dir", "aion/data"));

        assertEquals(Path.of("src/main/resources/aion/data").normalize().toString(), System.getProperty("aion.game.data.dir"));
    }

    @Test
    void prefersRuntimeGameConfigDirectoryOverProjectResources() throws Exception {
        Path sourceGameConfig = aionHome.resolve("src/main/resources/aion/config");
        java.nio.file.Files.createDirectories(sourceGameConfig.resolve("main"));
        java.nio.file.Files.writeString(sourceGameConfig.resolve("main/geodata.properties"), "gameserver.geo.nav.pathfinding.enable = true");

        System.setProperty("aion.home", aionHome.toString());

        AionServicePaths.configureGame();

        assertEquals(aionHome.resolve("config").toString(), System.getProperty("aion.config.dir"));
        assertTrue(aionHome.resolve("config/main/gameserver.properties").toFile().isFile());
    }

    @Test
    void keepsExistingRuntimeGameConfigFiles() throws Exception {
        Path gameServerConfig = aionHome.resolve("config/main/gameserver.properties");
        java.nio.file.Files.createDirectories(gameServerConfig.getParent());
        java.nio.file.Files.writeString(gameServerConfig, "custom = keep");

        System.setProperty("aion.home", aionHome.toString());

        AionServicePaths.configureGame();

        assertEquals("custom = keep", java.nio.file.Files.readString(gameServerConfig));
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
        Path explicitConfig = aionHome.resolve("custom/config");
        Path explicitGameData = aionHome.resolve("custom/game-data");
        Path explicitDefinitions = aionHome.resolve("custom/definitions");
        Path explicitGameGeo = aionHome.resolve("custom/game-geo");
        Path explicitLogback = aionHome.resolve("custom/logback-spring.xml");
        System.setProperty("aion.logging.config", explicitLogback.toString());
        System.setProperty("aion.config.dir", explicitConfig.toString());
        System.setProperty("aion.game.data.dir", explicitGameData.toString());
        System.setProperty("aion.game.definitions.dir", explicitDefinitions.toString());
        System.setProperty("aion.game.geo.dir", explicitGameGeo.toString());

        AionServicePaths.configureLogin();
        AionServicePaths.configureGame();

        assertEquals(explicitConfig.toString(), System.getProperty("aion.config.dir"));
        assertEquals(explicitGameData.toString(), System.getProperty("aion.game.data.dir"));
        assertEquals(explicitDefinitions.toString(), System.getProperty("aion.game.definitions.dir"));
        assertEquals(explicitGameGeo.toString(), System.getProperty("aion.game.geo.dir"));
        assertEquals(explicitLogback.toString(), System.getProperty("aion.logging.config"));
    }
}
