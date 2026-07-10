package com.aionemu.gameserver.world.geo.nav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.WorldMapsData;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.commons.utils.collections.IntObjectHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NavDataTest {

	@TempDir
	Path dataDir;

	private String oldDataDir;
	private String oldGeoDir;
	private WorldMapsData oldWorldMapsData;
	private PrintStream oldOut;
	private final NavData navData = NavData.getInstance();

	@BeforeEach
	void setUp() throws Exception {
		oldDataDir = System.getProperty("aion.game.data.dir");
		oldGeoDir = System.getProperty("aion.game.geo.dir");
		oldWorldMapsData = DataManager.WORLD_MAPS_DATA;
		oldOut = System.out;
		System.setProperty("aion.game.data.dir", dataDir.toString());
		System.setProperty("aion.game.geo.dir", dataDir.resolve("geo").toString());
		DataManager.WORLD_MAPS_DATA = worldMaps(256, 1001, 1002);
		resetNavData();
		GeoDataConfig.GEO_NAV_ENABLE = true;
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
		if (oldGeoDir == null) {
			System.clearProperty("aion.game.geo.dir");
		} else {
			System.setProperty("aion.game.geo.dir", oldGeoDir);
		}
		System.setOut(oldOut);
		GeoDataConfig.GEO_NAV_ENABLE = false;
	}

	@Test
	void indexesNavFilesWithoutLoadingMeshes() throws Exception {
		writeNavFile(1001);

		navData.loadNavMaps();

		assertEquals(1, navData.getAvailableMapCount());
		assertEquals(0, navData.getLoadedMapCount());
	}

	@Test
	void scanPrintsBlockProgressLine() throws Exception {
		writeNavFile(1001);
		ByteArrayOutputStream bytes = captureSystemOut();

		navData.loadNavMaps();

		String output = bytes.toString(StandardCharsets.UTF_8);
		assertTrue(output.contains("\r████████████████████ | \"NavigationFiles\" | 2/2\n"));
		assertTrue(output.contains("NavigationFiles"));
		assertTrue(output.chars().noneMatch(character -> character == '%'));
	}

	@Test
	void strongCacheHonorsConfiguredMaximumSize() throws Exception {
		setGeoConfig("GEO_NAV_CACHE_SIZE", 1);
		writeNavFile(1001);
		writeNavFile(1002);
		navData.loadNavMaps();

		navData.getNavMap(1001);
		navData.getNavMap(1002);

		assertEquals(2, navData.getAvailableMapCount());
		assertEquals(1, navData.getLoadedMapCount());
	}

	private void writeNavFile(int mapId) throws IOException {
		Path navDir = dataDir.resolve("geo/nav");
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

	private ByteArrayOutputStream captureSystemOut() {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		System.setOut(new PrintStream(bytes, true, StandardCharsets.UTF_8));
		return bytes;
	}

	private static WorldMapsData worldMaps(int worldSize, int... mapIds) throws Exception {
		List<WorldMapTemplate> templates = new java.util.ArrayList<>();
		IntObjectHashMap<WorldMapTemplate> index = new IntObjectHashMap<>();
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

	@SuppressWarnings("unchecked")
	private void resetNavData() throws Exception {
		((Map<Integer, ?>) getField(navData, "navMaps")).clear();
		((Map<Integer, ?>) getField(navData, "navFiles")).clear();
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
