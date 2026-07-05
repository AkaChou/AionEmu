package com.aionemu.gameserver.world;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.world.exceptions.DuplicateAionObjectException;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class WorldMapInstanceTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();

	@Test
	void objectIteratorReturnsSnapshotSafeForRemovalDuringIteration() throws ReflectiveOperationException {
		TestWorldMapInstance instance = objenesis.newInstance(TestWorldMapInstance.class);
		Map<Integer, VisibleObject> objects = new LinkedHashMap<Integer, VisibleObject>();
		objects.put(1, objenesis.newInstance(Npc.class));
		objects.put(2, objenesis.newInstance(Npc.class));
		objects.put(3, objenesis.newInstance(Npc.class));
		setField(instance, "worldMapObjects", objects);

		Iterator<VisibleObject> iterator = instance.objectIterator();

		assertDoesNotThrow(() -> {
			while (iterator.hasNext()) {
				objects.values().remove(iterator.next());
			}
		});
		assertTrue(objects.isEmpty());
	}

	@Test
	void playerIteratorReturnsSnapshotSafeForRemovalDuringIteration() throws ReflectiveOperationException {
		TestWorldMapInstance instance = objenesis.newInstance(TestWorldMapInstance.class);
		Map<Integer, Player> players = new LinkedHashMap<Integer, Player>();
		players.put(1, objenesis.newInstance(Player.class));
		players.put(2, objenesis.newInstance(Player.class));
		players.put(3, objenesis.newInstance(Player.class));
		setField(instance, "worldMapPlayers", players);

		Iterator<Player> iterator = instance.playerIterator();

		assertDoesNotThrow(() -> {
			while (iterator.hasNext()) {
				players.values().remove(iterator.next());
			}
		});
		assertTrue(players.isEmpty());
	}

	@Test
	void doOnAllPlayersUsesSnapshotSafeForRemovalDuringVisit() throws ReflectiveOperationException {
		TestWorldMapInstance instance = objenesis.newInstance(TestWorldMapInstance.class);
		Map<Integer, Player> players = new LinkedHashMap<Integer, Player>();
		Player first = objenesis.newInstance(Player.class);
		Player second = objenesis.newInstance(Player.class);
		players.put(1, first);
		players.put(2, second);
		setField(instance, "worldMapPlayers", players);
		List<Player> visited = new java.util.ArrayList<Player>();

		instance.doOnAllPlayers(player -> {
			visited.add(player);
			players.values().remove(player);
		});

		assertTrue(players.isEmpty());
		assertTrue(visited.contains(first));
		assertTrue(visited.contains(second));
	}

	@Test
	void getQuestIdsReturnsReadOnlySnapshot() throws ReflectiveOperationException {
		TestWorldMapInstance instance = objenesis.newInstance(TestWorldMapInstance.class);
		List<Integer> questIds = new ArrayList<Integer>();
		questIds.add(1001);
		setField(instance, "questIds", questIds);

		List<Integer> snapshot = instance.getQuestIds();

		assertEquals(List.of(1001), snapshot);
		assertThrows(UnsupportedOperationException.class, () -> snapshot.add(1002));
		questIds.add(1003);
		assertEquals(List.of(1001), snapshot);
	}

	@Test
	void registeredObjectsUseConcurrentSet() throws IOException {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/world/WorldMapInstance.java"));

		assertTrue(source.contains("ConcurrentHashMap.newKeySet()"));
		assertFalse(source.contains("Collections.newSetFromMap(new LinkedHashMap<Integer, Boolean>())"));
	}

	@Test
	void addObjectDuplicateObjectIdDoesNotReplaceExistingObject() throws ReflectiveOperationException {
		TestWorldMapInstance instance = objenesis.newInstance(TestWorldMapInstance.class);
		Map<Integer, VisibleObject> objects = new LinkedHashMap<Integer, VisibleObject>();
		setField(instance, "worldMapObjects", objects);
		VisibleObject existing = new TestVisibleObject(1);
		VisibleObject duplicate = new TestVisibleObject(1);
		instance.addObject(existing);

		assertThrows(DuplicateAionObjectException.class, () -> instance.addObject(duplicate));

		assertSame(existing, objects.get(1));
	}

	private static void setField(WorldMapInstance instance, String fieldName, Object value) throws ReflectiveOperationException {
		Field field = WorldMapInstance.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(instance, value);
	}

	private static final class TestWorldMapInstance extends WorldMapInstance {

		private TestWorldMapInstance() {
			super(null, 0);
		}

		@Override
		public Integer getMapId() {
			return 1;
		}

		@Override
		public MapRegion getRegion(float x, float y, float z) {
			return null;
		}

		@Override
		protected MapRegion createMapRegion(int regionId) {
			return null;
		}

		@Override
		protected void initMapRegions() {
		}

		@Override
		public boolean isPersonal() {
			return false;
		}

		@Override
		public int getOwnerId() {
			return 0;
		}
	}

	private static final class TestVisibleObject extends VisibleObject {

		private TestVisibleObject(int objectId) {
			super(objectId, null, null, new TestVisibleObjectTemplate(objectId), new WorldPosition(1));
		}

		@Override
		public String getName() {
			return "test";
		}
	}

	private static final class TestVisibleObjectTemplate extends VisibleObjectTemplate {

		private final int templateId;

		private TestVisibleObjectTemplate(int templateId) {
			this.templateId = templateId;
		}

		@Override
		public int getTemplateId() {
			return templateId;
		}

		@Override
		public String getName() {
			return "test";
		}

		@Override
		public int getNameId() {
			return templateId;
		}
	}
}
