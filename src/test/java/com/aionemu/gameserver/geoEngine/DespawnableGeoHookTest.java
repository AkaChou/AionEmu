package com.aionemu.gameserver.geoEngine;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.collision.IgnoreProperties;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.geoEngine.models.GeoMap;
import com.aionemu.gameserver.geoEngine.scene.DespawnableNode;
import com.aionemu.gameserver.geoEngine.scene.DespawnableNode.DespawnableType;
import com.aionemu.gameserver.world.geo.DummyGeoMap;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class DespawnableGeoHookTest {

	@Test
	void lifecycleHooksUpdateDespawnableGeoState() throws Exception {
		String visibleObjectController = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/controllers/VisibleObjectController.java"));
		String npcController = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/controllers/NpcController.java"));
		String houseController = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/controllers/HouseController.java"));
		String town = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/model/town/Town.java"));
		String geoService = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/world/geo/GeoService.java"));

		assertTrue(visibleObjectController.contains("spawnPlaceableObject"));
		assertTrue(visibleObjectController.contains("despawnPlaceableObject"));
		assertTrue(npcController.contains("despawnPlaceableObject"));
		assertTrue(houseController.contains("setHouseDoorState"));
		assertTrue(town.contains("updateTownToLevel"));
		assertTrue(geoService.contains("target.getSpawn().getStaticId()"));
		assertTrue(geoService.contains("IgnoreProperties.of(race, staticId)"));
		assertTrue(geoService.contains("PHYSICAL_SEE_THROUGH.getId()"));
	}

	@Test
	void staticDoorsUseSpawnStaticIdForDespawnableGeoState() throws Exception {
		String staticDoor = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/model/gameobjects/StaticDoor.java"));
		String staticDoorSpawnManager = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/spawnengine/StaticDoorSpawnManager.java"));
		String geoService = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/world/geo/GeoService.java"));
		String geoMap = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/geoEngine/models/GeoMap.java"));
		String geoWorldLoader = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/geoEngine/GeoWorldLoader.java"));

		assertTrue(staticDoor.contains("getSpawn().getStaticId()"));
		assertFalse(staticDoor.contains("getObjectTemplate().getDoorId()"));
		assertFalse(staticDoor.contains("getDoorName()"));
		assertFalse(staticDoor.contains("doorName"));
		assertTrue(staticDoorSpawnManager.contains("spawn.setStaticId(data.getDoorId())"));
		assertTrue(staticDoorSpawnManager.contains("data.getDoorId(), staticDoor.isOpen()"));
		assertFalse(staticDoorSpawnManager.contains("staticDoor.getDoorName()"));
		assertFalse(geoService.contains("getDoorName("));
		assertFalse(geoService.contains("setDoorState(int worldId, int instanceId, String"));
		assertFalse(geoMap.contains("getDoorName("));
		assertFalse(geoMap.contains("setDoorState(int instanceId, String"));
		assertFalse(geoWorldLoader.contains("DoorGeometry"));
	}

	@Test
	void canPassChecksNullBeforeReadingObjects() throws Exception {
		String geoService = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/world/geo/GeoService.java"));
		int methodStart = geoService.indexOf("public boolean canPass(VisibleObject object, VisibleObject target)");
		int nullCheck = geoService.indexOf("if (object == null || target == null)", methodStart);
		int firstDistanceRead = geoService.indexOf("MathUtil.getDistance(object, target)", methodStart);
		int firstTemplateRead = geoService.indexOf("target.getObjectTemplate()", methodStart);

		assertTrue(nullCheck > methodStart);
		assertTrue(nullCheck < firstDistanceRead);
		assertTrue(nullCheck < firstTemplateRead);
	}

	@Test
	void missingDoorGeometryLogsOnlyForNonIgnorableDoors() {
		Logger logger = (Logger) LoggerFactory.getLogger(GeoMap.class);
		ListAppender<ILoggingEvent> appender = attachAppender(logger);
		boolean originalGeoEnabled = GeoDataConfig.GEO_ENABLE;

		try {
			GeoDataConfig.GEO_ENABLE = true;
			new GeoMap("300250000", 256).setDoorState(1, 78, true);
			new GeoMap("300250000", 256).setDoorState(1, 999, true);
		} finally {
			GeoDataConfig.GEO_ENABLE = originalGeoEnabled;
			detachAppender(logger, appender);
		}

		assertFalse(hasLog(appender, "No geometry found for door 78"));
		assertTrue(hasLog(appender, "No geometry found for door 999 in world 300250000"));
	}

	@Test
	void missingDoorStateLogsUnavailableState() throws Exception {
		Logger logger = (Logger) LoggerFactory.getLogger(GeoMap.class);
		ListAppender<ILoggingEvent> appender = attachAppender(logger);
		GeoMap map = new GeoMap("300250000", 256);
		DespawnableNode closedState = new DespawnableNode();
		closedState.type = DespawnableType.DOOR_STATE1;
		closedState.id = 77;
		despawnableDoors(map).put(77, new DespawnableNode[] { closedState, null });

		try {
			map.setDoorState(1, 77, true);
		} finally {
			detachAppender(logger, appender);
		}

		assertFalse(closedState.isActive(1));
		assertTrue(hasLog(appender, "Door state 2 not available for door 77 in world 300250000"));
	}

	@Test
	void dummyGeoMapIgnoresDespawnableStateUpdates() {
		Logger logger = (Logger) LoggerFactory.getLogger(GeoMap.class);
		ListAppender<ILoggingEvent> appender = attachAppender(logger);
		boolean originalGeoEnabled = GeoDataConfig.GEO_ENABLE;
		DummyGeoMap map = new DummyGeoMap("300250000", 256);

		try {
			GeoDataConfig.GEO_ENABLE = true;
			map.setDoorState(1, 999, true);
			map.spawnPlaceableObject(1, 123);
			map.despawnPlaceableObject(1, 123);
			map.updateTownToLevel(1, 5);
			map.setHouseDoorState(1, 42, true);
		} finally {
			GeoDataConfig.GEO_ENABLE = originalGeoEnabled;
			detachAppender(logger, appender);
		}

		assertFalse(hasLog(appender, "No geometry found for door"));
	}

	@Test
	void dummyGeoMapSupportsIgnorePropertiesApis() {
		DummyGeoMap map = new DummyGeoMap("", 0);

		assertTrue(map.canSee(0, 0, 0, 1, 1, 1, 10, 1, IgnoreProperties.of(1)));
		Vector3f collision = map.getClosestCollision(0, 0, 0, 1, 2, 3, false, false, 1,
				CollisionIntention.DEFAULT_COLLISIONS.getId(), IgnoreProperties.of(1));
		assertEquals(1F, collision.x);
		assertEquals(2F, collision.y);
		assertEquals(3F, collision.z);
		assertEquals(0, map.getCollisions(0, 0, 0, 1, 1, 1, false, false, 1,
				CollisionIntention.DEFAULT_COLLISIONS.getId(), IgnoreProperties.of(1)).size());
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

	@SuppressWarnings("unchecked")
	private static Map<Integer, DespawnableNode[]> despawnableDoors(GeoMap map) throws Exception {
		Field field = GeoMap.class.getDeclaredField("despawnableDoors");
		field.setAccessible(true);
		return (Map<Integer, DespawnableNode[]>) field.get(map);
	}
}
