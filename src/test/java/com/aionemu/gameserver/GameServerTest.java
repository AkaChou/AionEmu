package com.aionemu.gameserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.aionemu.commons.configuration.ConfigurableProcessor;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.configs.main.PlayerTransferConfig;
import org.junit.jupiter.api.Test;

class GameServerTest {

	@Test
	void resolvesConfiguredGameRuntimeDirectories() throws IOException {
		String oldConfigDir = System.getProperty("aion.game.config.dir");
		String oldDataDir = System.getProperty("aion.game.data.dir");
		String oldCacheDir = System.getProperty("aion.game.cache.dir");
		try {
			System.setProperty("aion.game.config.dir", "AL-Game/config");
			System.setProperty("aion.game.data.dir", "AL-Game/data");
			System.setProperty("aion.game.cache.dir", "AL-Game/cache");

			assertEquals(new File("AL-Game/config/logback-spring.xml").getCanonicalFile(),
				Config.configFile("logback-spring.xml").getCanonicalFile());
			assertEquals(new File("AL-Game/config/administration/commands.properties").getCanonicalFile(),
				Config.configFile("administration/commands.properties").getCanonicalFile());
			assertEquals(new File("AL-Game/data/scripts/system/aihandlers.xml").getCanonicalFile(),
				Config.dataFile("./data/scripts/system/aihandlers.xml").getCanonicalFile());
			assertEquals(new File("AL-Game/data/static_data/items/item_templates.xml").getCanonicalFile(),
				Config.dataFile("data/static_data/items/item_templates.xml").getCanonicalFile());
			assertEquals(new File("AL-Game/cache/static_data.xml").getCanonicalFile(),
				Config.cacheFile("./cache/static_data.xml").getCanonicalFile());
		} finally {
			restoreProperty("aion.game.config.dir", oldConfigDir);
			restoreProperty("aion.game.data.dir", oldDataDir);
			restoreProperty("aion.game.cache.dir", oldCacheDir);
		}
	}

	@Test
	void loadsGeoDataDefaultsWithoutOverrideProperties() {
		assertDoesNotThrow(() -> ConfigurableProcessor.process(GeoDataConfig.class, new Properties()));
		assertFalse(GeoDataConfig.GEO_MONONO2_IN_USE);
	}

	@Test
	void loadsPlayerTransferDefaultsWithoutOverrideProperties() {
		assertDoesNotThrow(() -> ConfigurableProcessor.process(PlayerTransferConfig.class, new Properties()));
		assertEquals(0, PlayerTransferConfig.REUSE_HOURS);
	}

	private static void restoreProperty(String key, String value) {
		if (value == null) {
			System.clearProperty(key);
		} else {
			System.setProperty(key, value);
		}
	}
}
