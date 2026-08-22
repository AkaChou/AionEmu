package com.aionemu.gameserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;

import com.aionemu.commons.configuration.ConfigurableProcessor;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.configs.main.AIConfig;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.configs.main.PlayerTransferConfig;
import org.junit.jupiter.api.Test;

class GameServerTest {

	@Test
	void enhancedHomeReturnIsEnabledByDefaultAndCanBeDisabled() {
		boolean original = AIConfig.ENHANCED_HOME_RETURN;
		try {
			ConfigurableProcessor.process(AIConfig.class, new Properties());
			assertTrue(AIConfig.ENHANCED_HOME_RETURN);

			Properties properties = new Properties();
			properties.setProperty("gameserver.ai.home.return.enhanced.enable", "false");
			ConfigurableProcessor.process(AIConfig.class, properties);
			assertFalse(AIConfig.ENHANCED_HOME_RETURN);
		} finally {
			AIConfig.ENHANCED_HOME_RETURN = original;
		}
	}

	@Test
	void resolvesConfiguredGameRuntimeDirectories() throws IOException {
		String oldConfigDir = System.getProperty("aion.config.dir");
		String oldDataDir = System.getProperty("aion.game.data.dir");
		String oldDefinitionsDir = System.getProperty("aion.game.definitions.dir");
		String oldCacheDir = System.getProperty("aion.game.cache.dir");
		try {
			System.setProperty("aion.config.dir", "AL-Game/config");
			System.setProperty("aion.game.data.dir", "AL-Game/data");
			System.setProperty("aion.game.definitions.dir", "AL-Game/definitions");
			System.setProperty("aion.game.cache.dir", "AL-Game/cache");

			assertEquals(new File("AL-Game/config/administration/commands.properties").getCanonicalFile(),
				Config.configFile("administration/commands.properties").getCanonicalFile());
			assertEquals(new File("AL-Game/data/scripts/system/aihandlers.xml").getCanonicalFile(),
				Config.dataFile("./data/scripts/system/aihandlers.xml").getCanonicalFile());
			assertEquals(new File("AL-Game/data/static_data/items/item_templates.xml").getCanonicalFile(),
				Config.dataFile("data/static_data/items/item_templates.xml").getCanonicalFile());
			assertEquals(new File("AL-Game/definitions/compact/world/fly_path.xml").getCanonicalFile(),
				Config.definitionFile("definitions/compact/world/fly_path.xml").getCanonicalFile());
			assertEquals(new File("AL-Game/cache/runtime-index.xml").getCanonicalFile(),
				Config.cacheFile("./cache/runtime-index.xml").getCanonicalFile());
		} finally {
			restoreProperty("aion.config.dir", oldConfigDir);
			restoreProperty("aion.game.data.dir", oldDataDir);
			restoreProperty("aion.game.definitions.dir", oldDefinitionsDir);
			restoreProperty("aion.game.cache.dir", oldCacheDir);
		}
	}

	@Test
	void loadsGeoDataDefaultsWithoutOverrideProperties() {
		assertDoesNotThrow(() -> ConfigurableProcessor.process(GeoDataConfig.class, new Properties()));
		assertFalse(readBooleanConfig("GEO_PATH_ENABLE"));
		assertFalse(readBooleanConfig("GEO_PATH_DISTANCE_TIERS_ENABLE"));
		assertTrue(readBooleanConfig("GEO_PATH_RECOVERY_ENABLE"));
		assertFalse(readBooleanConfig("GEO_PATH_HIERARCHICAL_ENABLE"));
		assertEquals(3, readIntConfig("GEO_PATH_WAYPOINT_LOOKAHEAD"));
		assertEquals(32, readIntConfig("GEO_PATH_CACHE_SIZE"));
		assertEquals(50000, readIntConfig("GEO_PATH_MAX_NODES"));
		assertEquals(250, readIntConfig("GEO_PATH_TIMEOUT_MS"));
		assertEquals(2, readFloatConfig("GEO_PATH_SPATIAL_STEP"), 0.001f);
	}

	@Test
	void loadsGeoPathOverrideProperties() {
		Properties properties = new Properties();
		properties.setProperty("gameserver.geo.path.enable", "true");
		properties.setProperty("gameserver.geo.path.distance.tiers.enable", "true");
		properties.setProperty("gameserver.geo.path.recovery.enable", "false");
		properties.setProperty("gameserver.geo.path.hierarchical.enable", "true");
		properties.setProperty("gameserver.geo.path.waypoint.lookahead", "5");
		properties.setProperty("gameserver.geo.path.cache.size", "7");
		properties.setProperty("gameserver.geo.path.max.nodes", "123");
		properties.setProperty("gameserver.geo.path.timeout.ms", "400");
		properties.setProperty("gameserver.geo.path.spatial.step", "1.5");

		ConfigurableProcessor.process(GeoDataConfig.class, properties);

		assertTrue(readBooleanConfig("GEO_PATH_ENABLE"));
		assertTrue(readBooleanConfig("GEO_PATH_DISTANCE_TIERS_ENABLE"));
		assertFalse(readBooleanConfig("GEO_PATH_RECOVERY_ENABLE"));
		assertTrue(readBooleanConfig("GEO_PATH_HIERARCHICAL_ENABLE"));
		assertEquals(5, readIntConfig("GEO_PATH_WAYPOINT_LOOKAHEAD"));
		assertEquals(7, readIntConfig("GEO_PATH_CACHE_SIZE"));
		assertEquals(123, readIntConfig("GEO_PATH_MAX_NODES"));
		assertEquals(400, readIntConfig("GEO_PATH_TIMEOUT_MS"));
		assertEquals(1.5f, readFloatConfig("GEO_PATH_SPATIAL_STEP"), 0.001f);
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

	private static boolean readBooleanConfig(String fieldName) {
		return (boolean) readConfig(fieldName);
	}

	private static int readIntConfig(String fieldName) {
		return (int) readConfig(fieldName);
	}

	private static float readFloatConfig(String fieldName) {
		return (float) readConfig(fieldName);
	}

	private static Object readConfig(String fieldName) {
		try {
			Field field = GeoDataConfig.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			return field.get(null);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Missing GeoDataConfig field " + fieldName, e);
		}
	}
}
