package com.aionemu.gameserver.world.geo.nav;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.WorldMapsData;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NavDataTest {

	@TempDir
	Path dataDir;

	private String oldDataDir;
	private WorldMapsData oldWorldMapsData;
	private final NavData navData = NavData.getInstance();

	@BeforeEach
	void setUp() throws Exception {
		oldDataDir = System.getProperty("aion.game.data.dir");
		oldWorldMapsData = DataManager.WORLD_MAPS_DATA;
		System.setProperty("aion.game.data.dir", dataDir.toString());
		DataManager.WORLD_MAPS_DATA = worldMaps(256, 1001, 1002);
		resetNavData();
		GeoDataConfig.GEO_NAV_ENABLE = true;
		setGeoConfig("GEO_NAV_SOFT_CACHE", false);
		setGeoConfig("GEO_NAV_CACHE_SIZE", 50);
	}

	@AfterEach
	void tearDown() throws Exception {
		resetNavData();
		DataManager.WORLD_MAPS_DATA = oldWorldMapsData;
		if (oldDataDir == null) {
			System.clearProperty("aion.game.data.dir");
		} else {
			System.setProperty("aion.game.data.dir", oldDataDir);
		}
		GeoDataConfig.GEO_NAV_ENABLE = false;
	}

	@Test
	void lazyLoadIndexesNavFilesWithoutLoadingMeshes() throws Exception {
		setGeoConfig("GEO_NAV_LAZY_LOAD", true);
		writeNavFile(1001);

		navData.loadNavMaps();

		assertEquals(1, navData.getAvailableMapCount());
		assertEquals(0, navData.getLoadedMapCount());
	}

	@Test
	void preloadModeLoadsMeshesDuringNavScan() throws Exception {
		setGeoConfig("GEO_NAV_LAZY_LOAD", false);
		writeNavFile(1001);

		navData.loadNavMaps();

		assertEquals(1, navData.getAvailableMapCount());
		assertEquals(1, navData.getLoadedMapCount());
	}

	@Test
	void strongCacheHonorsConfiguredMaximumSize() throws Exception {
		setGeoConfig("GEO_NAV_LAZY_LOAD", true);
		setGeoConfig("GEO_NAV_CACHE_SIZE", 1);
		writeNavFile(1001);
		writeNavFile(1002);
		navData.loadNavMaps();

		navData.getNavMap(1001);
		navData.getNavMap(1002);

		assertEquals(2, navData.getAvailableMapCount());
		assertEquals(1, navData.getLoadedMapCount());
	}

	@Test
	void softCacheDoesNotKeepLoadedMeshesInStrongCache() throws Exception {
		setGeoConfig("GEO_NAV_LAZY_LOAD", true);
		setGeoConfig("GEO_NAV_SOFT_CACHE", true);
		writeNavFile(1001);
		navData.loadNavMaps();

		navData.getNavMap(1001);

		assertEquals(1, navData.getLoadedMapCount());
		assertEquals(0, strongCacheSize());
	}

	private void writeNavFile(int mapId) throws IOException {
		Path navDir = dataDir.resolve("nav");
		Files.createDirectories(navDir);
		ByteBuffer buffer = ByteBuffer.allocate(4 + 9 * 4 + 4 + 6 * 4).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(9);
		buffer.putFloat(0F).putFloat(0F).putFloat(0F);
		buffer.putFloat(10F).putFloat(0F).putFloat(0F);
		buffer.putFloat(0F).putFloat(10F).putFloat(0F);
		buffer.putInt(1);
		buffer.putInt(0).putInt(1).putInt(2);
		buffer.putInt(-1).putInt(-1).putInt(-1);
		Files.write(navDir.resolve(mapId + ".nav"), buffer.array());
	}

	private static WorldMapsData worldMaps(int worldSize, int... mapIds) throws Exception {
		List<WorldMapTemplate> templates = new java.util.ArrayList<>();
		TIntObjectHashMap<WorldMapTemplate> index = new TIntObjectHashMap<>();
		for (int mapId : mapIds) {
			WorldMapTemplate template = new WorldMapTemplate();
			setField(template, "mapId", mapId);
			setField(template, "worldSize", worldSize);
			templates.add(template);
			index.put(mapId, template);
		}

		WorldMapsData data = new WorldMapsData();
		setField(data, "worldMaps", templates);
		setField(data, "worldIdMap", index);
		return data;
	}

	private int strongCacheSize() throws Exception {
		return ((Map<Integer, ?>) getField(navData, "navMaps")).size();
	}

	@SuppressWarnings("unchecked")
	private void resetNavData() throws Exception {
		((Map<Integer, ?>) getField(navData, "navMaps")).clear();
		((Map<Integer, ?>) getField(navData, "navFiles")).clear();
		Object softNavMaps = getField(navData, "softNavMaps");
		if (softNavMaps instanceof Map<?, ?> map) {
			map.clear();
		}
		((Map<Integer, ?>) getField(navData, "mapLocks")).clear();
	}

	private static Object getField(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static void setGeoConfig(String name, Object value) throws Exception {
		Field field = GeoDataConfig.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(null, value);
	}
}
