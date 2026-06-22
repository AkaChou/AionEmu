package com.aionemu.boot.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AionServicePathsTest {

    @TempDir
    Path aionHome;

    @AfterEach
    void restoreProperties() {
        System.clearProperty("aion.home");
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

        assertTrue(aionHome.resolve("login/config/network/database.properties").toFile().isFile());
        assertTrue(aionHome.resolve("chat/config/slf4j-logback.xml").toFile().isFile());
        assertTrue(aionHome.resolve("game/config/main/gameserver.properties").toFile().isFile());
        assertTrue(aionHome.resolve("game/data/static_data/items/item/item_misc_templates.xml").toFile().isFile());
    }

    @Test
    void keepsExplicitRuntimeDirectories() {
        Path explicitLoginConfig = aionHome.resolve("custom/login-config");
        Path explicitGameData = aionHome.resolve("custom/game-data");
        System.setProperty("aion.login.config.dir", explicitLoginConfig.toString());
        System.setProperty("aion.game.data.dir", explicitGameData.toString());

        AionServicePaths.configureLogin();
        AionServicePaths.configureGame();

        assertEquals(explicitLoginConfig.toString(), System.getProperty("aion.login.config.dir"));
        assertEquals(explicitGameData.toString(), System.getProperty("aion.game.data.dir"));
    }
}
