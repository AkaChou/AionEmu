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
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.configs.main.PlayerTransferConfig;
import org.junit.jupiter.api.Test;

class GameServerTest {

	@Test
	void resolvesConfiguredGameRuntimeDirectories() throws IOException {
		String oldConfigDir = System.getProperty("aion.config.dir");
		String oldDataDir = System.getProperty("aion.game.data.dir");
		String oldCacheDir = System.getProperty("aion.game.cache.dir");
		try {
			System.setProperty("aion.config.dir", "AL-Game/config");
			System.setProperty("aion.game.data.dir", "AL-Game/data");
			System.setProperty("aion.game.cache.dir", "AL-Game/cache");

			assertEquals(new File("AL-Game/config/administration/commands.properties").getCanonicalFile(),
				Config.configFile("administration/commands.properties").getCanonicalFile());
			assertEquals(new File("AL-Game/data/scripts/system/aihandlers.xml").getCanonicalFile(),
				Config.dataFile("./data/scripts/system/aihandlers.xml").getCanonicalFile());
			assertEquals(new File("AL-Game/data/static_data/items/item_templates.xml").getCanonicalFile(),
				Config.dataFile("data/static_data/items/item_templates.xml").getCanonicalFile());
			assertEquals(new File("AL-Game/cache/static_data.xml").getCanonicalFile(),
				Config.cacheFile("./cache/static_data.xml").getCanonicalFile());
		} finally {
			restoreProperty("aion.config.dir", oldConfigDir);
			restoreProperty("aion.game.data.dir", oldDataDir);
			restoreProperty("aion.game.cache.dir", oldCacheDir);
		}
	}

	@Test
	void loadsGeoDataDefaultsWithoutOverrideProperties() {
		assertDoesNotThrow(() -> ConfigurableProcessor.process(GeoDataConfig.class, new Properties()));
		assertEquals(50, readIntConfig("GEO_NAV_CACHE_SIZE"));
		assertTrue(readBooleanConfig("GEO_NAV_PULL_ENABLE"));
		assertEquals(800, readIntConfig("GEO_NAV_MAX_NODES"));
		assertEquals(5F, readFloatConfig("GEO_NAV_TARGET_THRESHOLD"), 0.0001F);
		assertEquals(0.2F, readFloatConfig("GEO_NAV_PATH_WEIGHT"), 0.0001F);
		assertEquals(20F, readFloatConfig("GEO_NAV_TARGET_WEIGHT"), 0.0001F);
		assertEquals(5F, readFloatConfig("GEO_NAV_GROUND_SEARCH_DISTANCE"), 0.0001F);
		assertEquals(0.8F, readFloatConfig("GEO_NAV_BOX_EXTENT_XY"), 0.0001F);
		assertEquals(-1F, readFloatConfig("GEO_NAV_BOX_OFFSET_Z_MIN"), 0.0001F);
		assertEquals(4F, readFloatConfig("GEO_NAV_BOX_OFFSET_Z_MAX"), 0.0001F);
		assertEquals(0.2F, readFloatConfig("GEO_NAV_BOX_CENTER_Z"), 0.0001F);
		assertTrue(readBooleanConfig("GEO_NAV_SMOOTH_PATH"));
		assertEquals(800, readIntConfig("GEO_NAV_CORRIDOR_LENGTH"));
	}

	@Test
	void loadsGeoNavOverrideProperties() {
		Properties properties = new Properties();
		properties.setProperty("gameserver.geo.nav.cache.size", "7");
		properties.setProperty("gameserver.geo.nav.pull.enable", "false");
		properties.setProperty("gameserver.geo.nav.max.nodes", "123");
		properties.setProperty("gameserver.geo.nav.target.threshold", "6.5");
		properties.setProperty("gameserver.geo.nav.path.weight", "0.4");
		properties.setProperty("gameserver.geo.nav.target.weight", "12.5");
		properties.setProperty("gameserver.geo.nav.ground.search.distance", "9.5");
		properties.setProperty("gameserver.geo.nav.box.extent.xy", "1.5");
		properties.setProperty("gameserver.geo.nav.box.offset.z.min", "-2.5");
		properties.setProperty("gameserver.geo.nav.box.offset.z.max", "6.5");
		properties.setProperty("gameserver.geo.nav.box.center.z", "0.6");
		properties.setProperty("gameserver.geo.nav.smooth.path", "false");
		properties.setProperty("gameserver.geo.nav.corridor.length", "321");

		ConfigurableProcessor.process(GeoDataConfig.class, properties);

		assertEquals(7, readIntConfig("GEO_NAV_CACHE_SIZE"));
		assertFalse(readBooleanConfig("GEO_NAV_PULL_ENABLE"));
		assertEquals(123, readIntConfig("GEO_NAV_MAX_NODES"));
		assertEquals(6.5F, readFloatConfig("GEO_NAV_TARGET_THRESHOLD"), 0.0001F);
		assertEquals(0.4F, readFloatConfig("GEO_NAV_PATH_WEIGHT"), 0.0001F);
		assertEquals(12.5F, readFloatConfig("GEO_NAV_TARGET_WEIGHT"), 0.0001F);
		assertEquals(9.5F, readFloatConfig("GEO_NAV_GROUND_SEARCH_DISTANCE"), 0.0001F);
		assertEquals(1.5F, readFloatConfig("GEO_NAV_BOX_EXTENT_XY"), 0.0001F);
		assertEquals(-2.5F, readFloatConfig("GEO_NAV_BOX_OFFSET_Z_MIN"), 0.0001F);
		assertEquals(6.5F, readFloatConfig("GEO_NAV_BOX_OFFSET_Z_MAX"), 0.0001F);
		assertEquals(0.6F, readFloatConfig("GEO_NAV_BOX_CENTER_Z"), 0.0001F);
		assertFalse(readBooleanConfig("GEO_NAV_SMOOTH_PATH"));
		assertEquals(321, readIntConfig("GEO_NAV_CORRIDOR_LENGTH"));
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
