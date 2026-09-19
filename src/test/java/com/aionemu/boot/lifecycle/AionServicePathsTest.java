package com.aionemu.boot.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertEquals(aionHome.resolve("log/logback-spring.xml").toString(), System.getProperty("aion.logging.config"));

        assertTrue(aionHome.resolve("log/logback-spring.xml").toFile().isFile());
        assertTrue(aionHome.resolve("config/login/database.properties").toFile().isFile());
        assertTrue(aionHome.resolve("config/chat/chatserver.properties").toFile().isFile());
        assertTrue(aionHome.resolve("config/main/gameserver.properties").toFile().isFile());
        assertTrue(aionHome.resolve("data/static_data/items/item/item_template_100000001_100601382.xml").toFile().isFile());
        assertFalse(java.nio.file.Files.exists(aionHome.resolve("game/cache")),
            "no derived cache directory should be created");
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

    /**
     * 本地 IDE/检出运行（未设置 {@code aion.home}）时，配置目录必须和 data/definitions/geo 一样取源码树。
     * An IDE/checkout run (no {@code aion.home}) must resolve configuration from the checkout source
     * tree, exactly like data, definitions and geo, so one process never mixes the two trees.
     */
    @Test
    void prefersCheckoutSourceConfigDirectoryWhenAionHomeIsDefault() {
        System.setProperty("aion.logging.config", aionHome.resolve("log/logback-spring.xml").toString());

        AionServicePaths.configureGame();

        assertEquals(Path.of("src/main/resources/aion/config").normalize().toString(),
            System.getProperty("aion.config.dir"));
        assertEquals(Path.of("src/main/resources/aion/data").normalize().toString(),
            System.getProperty("aion.game.data.dir"));
        assertEquals(Path.of("src/main/resources/aion/geo").normalize().toString(),
            System.getProperty("aion.game.geo.dir"));
    }

    /**
     * 显式路径永远优先：即使 {@code aion.home} 与源码树都存在，也不得被源码树覆盖。
     * An explicit path always wins: neither the home directory nor the source tree may override it.
     */
    @Test
    void keepsExplicitDirectoriesEvenWhenHomeAndSourceTreeExist() throws Exception {
        java.nio.file.Files.createDirectories(aionHome.resolve("src/main/resources/aion/data"));
        Path explicitConfig = aionHome.resolve("custom/config");
        Path explicitGameData = aionHome.resolve("custom/game-data");
        System.setProperty("aion.home", aionHome.toString());
        System.setProperty("aion.logging.config", aionHome.resolve("custom/logback-spring.xml").toString());
        System.setProperty("aion.config.dir", explicitConfig.toString());
        System.setProperty("aion.game.data.dir", explicitGameData.toString());

        AionServicePaths.configureGame();

        assertEquals(explicitConfig.toString(), System.getProperty("aion.config.dir"));
        assertEquals(explicitGameData.toString(), System.getProperty("aion.game.data.dir"));
    }

    @Test
    void prefersRuntimeGameConfigDirectoryOverProjectResources() throws Exception {
        Path sourceGameConfig = aionHome.resolve("src/main/resources/aion/config");
        java.nio.file.Files.createDirectories(sourceGameConfig.resolve("main"));
        java.nio.file.Files.writeString(sourceGameConfig.resolve("main/geodata.properties"), "gameserver.geo.path.enable = true");

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
