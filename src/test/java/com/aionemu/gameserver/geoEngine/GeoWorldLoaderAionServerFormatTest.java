package com.aionemu.gameserver.geoEngine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.aionemu.gameserver.geoEngine.models.GeoMap;
import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.collision.IgnoreProperties;
import com.aionemu.gameserver.geoEngine.math.Ray;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.geoEngine.scene.DespawnableNode;
import com.aionemu.gameserver.geoEngine.scene.Node;
import com.aionemu.gameserver.geoEngine.scene.Spatial;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

class GeoWorldLoaderAionServerFormatTest {

	@TempDir
	Path dataDir;

	private String oldDataDir;
	private String oldGeoDir;

	@AfterEach
	void tearDown() {
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
	}

	@Test
	void loadsModelsMeshSeparateTerrainPngAndWorldGeo() throws Exception {
		oldDataDir = System.getProperty("aion.game.data.dir");
		oldGeoDir = System.getProperty("aion.game.geo.dir");
		System.setProperty("aion.game.data.dir", dataDir.toString());
		Path geoDir = dataDir.resolve("geo");
		System.setProperty("aion.game.geo.dir", geoDir.toString());
		Files.createDirectories(geoDir);
		Files.write(geoDir.resolve("models.mesh"), modelsMesh());
		Files.write(geoDir.resolve("1001.geo"), worldGeo(0, 0, 0));
		writeTerrainPng(geoDir.resolve("1001.png"), 32);

		Map<String, Spatial> models = GeoWorldLoader.loadMeshs("geo/models.mesh");
		GeoMap map = new GeoMap("1001", 256);

		loadCurrentWorld(1001, models, map);
		assertTrue(models.containsKey("world/1001"));
		assertEquals(1F, map.getZ(2F, 2F), 0.01F);
	}

	@Test
	void loadsDespawnableEntitiesFromWorldGeoTypeIdLevel() throws Exception {
		oldDataDir = System.getProperty("aion.game.data.dir");
		System.setProperty("aion.game.data.dir", dataDir.toString());
		Path geoDir = dataDir.resolve("geo");
		Files.createDirectories(geoDir);
		Files.write(geoDir.resolve("models.mesh"), modelsMesh());
		Files.write(geoDir.resolve("1001.geo"), worldGeo(2, 123, 0));
		writeTerrainPng(geoDir.resolve("1001.png"), 32);

		Map<String, Spatial> models = GeoWorldLoader.loadMeshs("geo/models.mesh");
		GeoMap map = new GeoMap("1001", 256);

		loadCurrentWorld(1001, models, map);
		DespawnableNode node = findDespawnable(map);
		assertTrue(node != null);
		assertEquals(DespawnableNode.DespawnableType.PLACEABLE, node.type);
		assertEquals(123, node.id);
		assertFalse(node.isActive(1));
		map.spawnPlaceableObject(1, 123);
		assertTrue(node.isActive(1));
		map.despawnPlaceableObject(1, 123);
		assertFalse(node.isActive(1));
	}

	@Test
	void treatsTerrainPngSamplesAsUnsignedHeights() throws Exception {
		oldDataDir = System.getProperty("aion.game.data.dir");
		System.setProperty("aion.game.data.dir", dataDir.toString());
		Path geoDir = dataDir.resolve("geo");
		Files.createDirectories(geoDir);
		writeTerrainPng(geoDir.resolve("1001.png"), 40000);
		GeoMap map = new GeoMap("1001", 256);

		loadCurrentWorld(1001, Map.of(), map);

		assertEquals(1250F, map.getZ(2F, 2F), 0.01F);
	}

	@Test
	void readsTerrainPngTransposedLikeClientCoordinates() throws Exception {
		oldDataDir = System.getProperty("aion.game.data.dir");
		System.setProperty("aion.game.data.dir", dataDir.toString());
		Path geoDir = dataDir.resolve("geo");
		Files.createDirectories(geoDir);
		writeTransposedTerrainPng(geoDir.resolve("1001.png"), 32000);
		writeTransposedMaterialPng(geoDir.resolve("1001_materials.png"), 7);
		GeoMap map = new GeoMap("1001", 256);

		loadCurrentWorld(1001, Map.of(), map);

		assertEquals(1000F, map.getZ(2.5F, 4.5F), 0.01F);
		assertEquals(7, map.getTerrainMaterialAt(2.5F, 4.5F, 1000F, 1));
	}

	@Test
	void loadsTerrainMaterialPng() throws Exception {
		oldDataDir = System.getProperty("aion.game.data.dir");
		System.setProperty("aion.game.data.dir", dataDir.toString());
		Path geoDir = dataDir.resolve("geo");
		Files.createDirectories(geoDir);
		writeTerrainPng(geoDir.resolve("1001.png"), 32);
		writeTerrainMaterialPng(geoDir.resolve("1001_materials.png"), 7);
		GeoMap map = new GeoMap("1001", 256);

		loadCurrentWorld(1001, Map.of(), map);

		assertTrue(map.hasTerrainMaterials());
		assertEquals(7, map.getTerrainMaterialAt(2.5F, 2.5F, 1F, 1));
	}

	@Test
	void loadTerrainsSharesCombinedTerrainPngAcrossMaps() throws Exception {
		oldDataDir = System.getProperty("aion.game.data.dir");
		System.setProperty("aion.game.data.dir", dataDir.toString());
		Path geoDir = dataDir.resolve("geo");
		Files.createDirectories(geoDir);
		writeTerrainPng(geoDir.resolve("1001,1002.png"), 64);
		writeTerrainMaterialPng(geoDir.resolve("1001,1002_materials.png"), 9);
		GeoMap first = new GeoMap("1001", 256);
		GeoMap second = new GeoMap("1002", 256);

		GeoWorldLoader.loadTerrains(List.of(first, second));

		assertEquals(2F, first.getZ(2F, 2F), 0.01F);
		assertEquals(2F, second.getZ(2F, 2F), 0.01F);
		assertEquals(9, first.getTerrainMaterialAt(2.5F, 2.5F, 2F, 1));
		assertEquals(9, second.getTerrainMaterialAt(2.5F, 2.5F, 2F, 1));
	}

	@Test
	void loadTerrainsPrefersDirectPngOverCombinedPng() throws Exception {
		oldDataDir = System.getProperty("aion.game.data.dir");
		System.setProperty("aion.game.data.dir", dataDir.toString());
		Path geoDir = dataDir.resolve("geo");
		Files.createDirectories(geoDir);
		writeTerrainPng(geoDir.resolve("1001,1002.png"), 64);
		writeTerrainPng(geoDir.resolve("1001.png"), 96);
		writeTerrainMaterialPng(geoDir.resolve("1001,1002_materials.png"), 7);
		writeTerrainMaterialPng(geoDir.resolve("1001_materials.png"), 9);
		GeoMap first = new GeoMap("1001", 256);
		GeoMap second = new GeoMap("1002", 256);

		GeoWorldLoader.loadTerrains(List.of(first, second));

		assertEquals(3F, first.getZ(2F, 2F), 0.01F);
		assertEquals(2F, second.getZ(2F, 2F), 0.01F);
		assertEquals(9, first.getTerrainMaterialAt(2.5F, 2.5F, 3F, 1));
		assertEquals(7, second.getTerrainMaterialAt(2.5F, 2.5F, 2F, 1));
	}

	@Test
	void despawnableCollisionHonorsInstanceStateAndIgnoredStaticId() throws Exception {
		oldDataDir = System.getProperty("aion.game.data.dir");
		System.setProperty("aion.game.data.dir", dataDir.toString());
		Path geoDir = dataDir.resolve("geo");
		Files.createDirectories(geoDir);
		Files.write(geoDir.resolve("models.mesh"), modelsMesh());
		Files.write(geoDir.resolve("1001.geo"), worldGeo(2, 123, 0));
		writeTerrainPng(geoDir.resolve("1001.png"), 32);

		Map<String, Spatial> models = GeoWorldLoader.loadMeshs("geo/models.mesh");
		GeoMap map = new GeoMap("1001", 256);
		loadCurrentWorld(1001, models, map);
		DespawnableNode node = findDespawnable(map);

		assertEquals(0, collideDown(node, new CollisionResults(CollisionIntention.PHYSICAL.getId(), false, 1)));
		node.setActive(1, true);
		assertTrue(collideDown(node, new CollisionResults(CollisionIntention.PHYSICAL.getId(), false, 1)) > 0);
		assertEquals(0, collideDown(node,
				new CollisionResults(CollisionIntention.PHYSICAL.getId(), false, 1, IgnoreProperties.of(123))));
	}

	@Test
	void eventDespawnableOnlyCollidesForActiveEventTheme() throws Exception {
		oldDataDir = System.getProperty("aion.game.data.dir");
		System.setProperty("aion.game.data.dir", dataDir.toString());
		Path geoDir = dataDir.resolve("geo");
		Files.createDirectories(geoDir);
		Files.write(geoDir.resolve("models.mesh"), modelsMesh());
		Files.write(geoDir.resolve("1001.geo"), worldGeo(1, 1, 0));
		writeTerrainPng(geoDir.resolve("1001.png"), 32);

		Map<String, Spatial> models = GeoWorldLoader.loadMeshs("geo/models.mesh");
		GeoMap map = new GeoMap("1001", 256);
		loadCurrentWorld(1001, models, map);
		DespawnableNode node = findDespawnable(map);

		assertEquals(DespawnableNode.DespawnableType.EVENT, node.type);
		assertEquals(0, collideDown(node, new CollisionResults(CollisionIntention.PHYSICAL.getId(), false, 1)));
	}

	@Test
	void geoMapCollisionApiCarriesIgnorePropertiesToDespawnables() throws Exception {
		oldDataDir = System.getProperty("aion.game.data.dir");
		System.setProperty("aion.game.data.dir", dataDir.toString());
		Path geoDir = dataDir.resolve("geo");
		Files.createDirectories(geoDir);
		Files.write(geoDir.resolve("models.mesh"), modelsMesh());
		Files.write(geoDir.resolve("1001.geo"), worldGeo(2, 123, 0));
		writeTerrainPng(geoDir.resolve("1001.png"), 32);

		Map<String, Spatial> models = GeoWorldLoader.loadMeshs("geo/models.mesh");
		GeoMap map = new GeoMap("1001", 256);
		loadCurrentWorld(1001, models, map);
		map.spawnPlaceableObject(1, 123);

		int normalCollisions = map.getCollisions(1F, 1F, 5F, 1F, 1F, -5F, false, true, 1,
				CollisionIntention.PHYSICAL.getId()).size();
		int ignoredCollisions = map.getCollisions(1F, 1F, 5F, 1F, 1F, -5F, false, true, 1,
				CollisionIntention.PHYSICAL.getId(), IgnoreProperties.of(123)).size();
		assertTrue(normalCollisions > ignoredCollisions);
	}

	@Test
	void splitsModelsMeshAliases() throws Exception {
		oldDataDir = System.getProperty("aion.game.data.dir");
		System.setProperty("aion.game.data.dir", dataDir.toString());
		Path geoDir = dataDir.resolve("geo");
		Files.createDirectories(geoDir);
		Files.write(geoDir.resolve("models.mesh"), modelsMesh("world/alias_a|world/alias_b"));

		Map<String, Spatial> models = GeoWorldLoader.loadMeshs("geo/models.mesh");

		assertTrue(models.containsKey("world/alias_a"));
		assertTrue(models.containsKey("world/alias_b"));
		assertFalse(models.containsKey("world/alias_a|world/alias_b"));
	}

	@Test
	void loadsMoveableMeshes() throws Exception {
		oldDataDir = System.getProperty("aion.game.data.dir");
		System.setProperty("aion.game.data.dir", dataDir.toString());
		Path geoDir = dataDir.resolve("geo");
		Files.createDirectories(geoDir);
		Files.write(geoDir.resolve("models.mesh"), modelsMesh("world/moveable", CollisionIntention.MOVEABLE.getId()));

		Map<String, Spatial> models = GeoWorldLoader.loadMeshs("geo/models.mesh");

		assertTrue(models.containsKey("world/moveable"));
	}

	@Test
	void attachesMultiChildModelsOnlyOnce() throws Exception {
		oldDataDir = System.getProperty("aion.game.data.dir");
		System.setProperty("aion.game.data.dir", dataDir.toString());
		Path geoDir = dataDir.resolve("geo");
		Files.createDirectories(geoDir);
		Files.write(geoDir.resolve("models.mesh"), modelsMesh("world/multi", CollisionIntention.PHYSICAL.getId(), 2));
		Files.write(geoDir.resolve("1001.geo"), worldGeo("world/multi", 0, 0, 0));
		writeTerrainPng(geoDir.resolve("1001.png"), 32);

		Map<String, Spatial> models = GeoWorldLoader.loadMeshs("geo/models.mesh");
		GeoMap map = new GeoMap("1001", 256);
		loadCurrentWorld(1001, models, map);

		assertEquals(2, collideDown(map, new CollisionResults(CollisionIntention.PHYSICAL.getId(), false, 1)));
	}

	@Test
	void logsMissingWorldGeoMeshes() throws Exception {
		oldDataDir = System.getProperty("aion.game.data.dir");
		System.setProperty("aion.game.data.dir", dataDir.toString());
		Path geoDir = dataDir.resolve("geo");
		Files.createDirectories(geoDir);
		Files.write(geoDir.resolve("1001.geo"), worldGeo("world/missing", 0, 0, 0));
		writeTerrainPng(geoDir.resolve("1001.png"), 32);
		Logger logger = (Logger) LoggerFactory.getLogger(GeoWorldLoader.class);
		ListAppender<ILoggingEvent> appender = attachAppender(logger);

		try {
			loadCurrentWorld(1001, Map.of(), new GeoMap("1001", 256));
		} finally {
			detachAppender(logger, appender);
		}

		assertTrue(hasLog(appender, "Missing geo mesh world/missing in world 1001"));
	}

	@Test
	void physicalSeeThroughMeshesBlockMovementButNotSight() throws Exception {
		assertEquals((byte) 0x80, CollisionIntention.valueOf("PHYSICAL_SEE_THROUGH").getId());
		assertEquals((byte) (CollisionIntention.PHYSICAL.getId() | CollisionIntention.DOOR.getId() | 0x80),
				CollisionIntention.valueOf("DEFAULT_COLLISIONS").getId());
		assertEquals((byte) (CollisionIntention.PHYSICAL.getId() | CollisionIntention.DOOR.getId()),
				CollisionIntention.valueOf("CANT_SEE_COLLISIONS").getId());

		oldDataDir = System.getProperty("aion.game.data.dir");
		System.setProperty("aion.game.data.dir", dataDir.toString());
		Path geoDir = dataDir.resolve("geo");
		Files.createDirectories(geoDir);
		Files.write(geoDir.resolve("models.mesh"), modelsMesh("world/see_through", (byte) 0x80));
		Files.write(geoDir.resolve("1001.geo"), worldGeo("world/see_through", 0, 0, 0));
		writeTerrainPng(geoDir.resolve("1001.png"), 32);

		Map<String, Spatial> models = GeoWorldLoader.loadMeshs("geo/models.mesh");
		GeoMap map = new GeoMap("1001", 256);
		loadCurrentWorld(1001, models, map);

		assertTrue(collideDown(map, new CollisionResults(CollisionIntention.valueOf("DEFAULT_COLLISIONS").getId(), false, 1)) > 0);
		assertEquals(0, collideDown(map, new CollisionResults(CollisionIntention.valueOf("CANT_SEE_COLLISIONS").getId(), false, 1)));
		assertFalse(map.canPass(1F, 1F, 5F, 1F, 1F, -5F, 10F, 1));
	}

	@Test
	void geoParitySourceNoLongerUsesLegacyTerrainOrPerNodeMissingMeshHandling() throws Exception {
		String geoMap = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/geoEngine/models/GeoMap.java"));
		String loader = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/geoEngine/GeoWorldLoader.java"));
		String aiQuestion = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/ai2/poll/AIQuestion.java"));
		String terrainActor = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/controllers/observer/TerrainZoneCollisionMaterialActor.java"));
		String siegeShield = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/model/siege/SiegeShield.java"));

		assertFalse(geoMap.contains("terraionCollision"));
		assertFalse(geoMap.contains("calculateTerrainCollision"));
		assertFalse(geoMap.contains("terrainCutoutData"));
		assertTrue(loader.contains("loadTerrains("));
		assertTrue(loader.contains("missingMeshes"));
		assertTrue(aiQuestion.contains("CONSIDER_BOUNDS_IN_CAN_SEE_CHECK_WHEN_ATTACKING"));
		assertTrue(aiQuestion.contains("CONSIDER_BOUNDS_IN_CAN_SEE_CHECK_WHEN_ATTACKED"));
		assertFalse(terrainActor.contains("matches.isEmpty() ? null : matches.get(0)"));
		assertTrue(terrainActor.contains("AtomicReference<List<MaterialSkill>>"));
		assertFalse(siegeShield.contains("shieldService()"));
		assertTrue(siegeShield.contains("new CollisionDieActor(creature, geometry)"));
	}

	private static void loadCurrentWorld(int worldId, Map<String, Spatial> models, GeoMap map) throws Exception {
		GeoWorldLoader.loadTerrains(List.of(map));
		GeoWorldLoader.loadWorldObjects(worldId, models, map, new HashSet<>());
	}

	private static byte[] modelsMesh() {
		return modelsMesh("world/1001");
	}

	private static byte[] modelsMesh(String name) {
		return modelsMesh(name, CollisionIntention.PHYSICAL.getId());
	}

	private static byte[] modelsMesh(String name, byte collisionIntentions) {
		return modelsMesh(name, collisionIntentions, 1);
	}

	private static byte[] modelsMesh(String name, byte collisionIntentions, int modelCount) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] nameBytes = name.getBytes();
		writeShortBE(out, nameBytes.length);
		out.writeBytes(nameBytes);
		out.write(modelCount);
		for (int model = 0; model < modelCount; model++) {
			writeShortBE(out, 3);
			writeFloatBE(out, 0F);
			writeFloatBE(out, 0F);
			writeFloatBE(out, 0F);
			writeFloatBE(out, 10F);
			writeFloatBE(out, 0F);
			writeFloatBE(out, 0F);
			writeFloatBE(out, 0F);
			writeFloatBE(out, 10F);
			writeFloatBE(out, 0F);
			writeShortBE(out, 1);
			out.write(1);
			out.write(0);
			out.write(1);
			out.write(2);
			out.write(0);
			out.write(collisionIntentions);
		}
		return out.toByteArray();
	}

	private static byte[] worldGeo(int type, int id, int level) {
		return worldGeo("world/1001", type, id, level);
	}

	private static byte[] worldGeo(String name, int type, int id, int level) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] nameBytes = name.getBytes();
		writeShortBE(out, nameBytes.length);
		out.writeBytes(nameBytes);
		writeFloatBE(out, 0F);
		writeFloatBE(out, 0F);
		writeFloatBE(out, 0F);
		for (float value : new float[] {1F, 0F, 0F, 0F, 1F, 0F, 0F, 0F, 1F}) {
			writeFloatBE(out, value);
		}
		writeFloatBE(out, 1F);
		writeFloatBE(out, 1F);
		writeFloatBE(out, 1F);
		out.write(type);
		writeShortBE(out, id);
		out.write(level);
		return out.toByteArray();
	}

	private static void writeTerrainPng(Path path, int height) throws Exception {
		BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_USHORT_GRAY);
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				image.getRaster().setSample(x, y, 0, height);
			}
		}
		ImageIO.write(image, "png", path.toFile());
	}

	private static void writeTransposedTerrainPng(Path path, int height) throws Exception {
		BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_USHORT_GRAY);
		for (int x : new int[] { 2, 3 }) {
			for (int y : new int[] { 1, 2 }) {
				image.getRaster().setSample(x, y, 0, height);
			}
		}
		ImageIO.write(image, "png", path.toFile());
	}

	private static void writeTerrainMaterialPng(Path path, int materialId) throws Exception {
		BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_BYTE_GRAY);
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				image.getRaster().setSample(x, y, 0, materialId);
			}
		}
		ImageIO.write(image, "png", path.toFile());
	}

	private static void writeTransposedMaterialPng(Path path, int materialId) throws Exception {
		BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_BYTE_GRAY);
		for (int x : new int[] { 2, 3 }) {
			for (int y : new int[] { 1, 2 }) {
				image.getRaster().setSample(x, y, 0, materialId);
			}
		}
		ImageIO.write(image, "png", path.toFile());
	}

	private static DespawnableNode findDespawnable(Node node) {
		for (Spatial child : node.getChildren()) {
			if (child instanceof DespawnableNode) {
				return (DespawnableNode) child;
			}
			if (child instanceof Node) {
				DespawnableNode found = findDespawnable((Node) child);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	private static int collideDown(Node node, CollisionResults results) {
		Ray ray = new Ray(new Vector3f(1F, 1F, 5F), new Vector3f(0F, 0F, -1F));
		ray.setLimit(10F);
		return node.collideWith(ray, results);
	}

	private static ListAppender<ILoggingEvent> attachAppender(Logger logger) {
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		return appender;
	}

	private static void detachAppender(Logger logger, ListAppender<ILoggingEvent> appender) {
		logger.detachAppender(appender);
		appender.stop();
	}

	private static boolean hasLog(ListAppender<ILoggingEvent> appender, String message) {
		return appender.list.stream().map(ILoggingEvent::getFormattedMessage).anyMatch(log -> log.contains(message));
	}

	private static void writeShortBE(ByteArrayOutputStream out, int value) {
		out.write((value >>> 8) & 0xFF);
		out.write(value & 0xFF);
	}

	private static void writeFloatBE(ByteArrayOutputStream out, float value) {
		out.writeBytes(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putFloat(value).array());
	}
}
