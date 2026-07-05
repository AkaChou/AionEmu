package com.aionemu.gameserver.spawnengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.controllers.VisibleObjectController;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.TemporarySpawn;

class TemporarySpawnEngineTest {

	@BeforeEach
	@AfterEach
	void resetTemporarySpawns() throws ReflectiveOperationException {
		field("temporarySpawns", List.class).clear();
		field("tempSpawnInstanceMap", Map.class).clear();
	}

	@Test
	void hourChangeDespawnsEveryVisibleObjectTrackedByTemporaryTemplate() throws Exception {
		SpawnGroup2 spawn = new SpawnGroup2(1, 1);
		TestSpawnTemplate template = new TestSpawnTemplate(spawn);
		TestVisibleObject first = new TestVisibleObject(1);
		TestVisibleObject second = new TestVisibleObject(2);
		template.addVisibleObject(first);
		template.addVisibleObject(second);
		template.setVisibleObject(second);
		spawn.addSpawnTemplate(template);
		TemporarySpawnEngine.addSpawnGroup(spawn, 1);

		despawn();

		assertTrue(first.deleted);
		assertTrue(second.deleted);
	}

	@Test
	void addSpawnGroupTracksEachTemporarySpawnOnlyOnceAcrossInstances() throws Exception {
		SpawnGroup2 spawn = new SpawnGroup2(1, 1);

		TemporarySpawnEngine.addSpawnGroup(spawn, 1);
		TemporarySpawnEngine.addSpawnGroup(spawn, 2);

		assertEquals(1, field("temporarySpawns", List.class).size());
		assertEquals(2, ((Set<?>) field("tempSpawnInstanceMap", Map.class).get(spawn)).size());
	}

	@Test
	void despawnUsesSnapshotWhenTemporarySpawnsAreRegisteredDuringCallbacks() throws Exception {
		SpawnGroup2 spawn = new SpawnGroup2(1, 1);
		SpawnGroup2 lateSpawn = new SpawnGroup2(2, 1);
		TestSpawnTemplate template = new TestSpawnTemplate(spawn, new RegisteringTemporarySpawn(lateSpawn));
		spawn.addSpawnTemplate(template);
		TemporarySpawnEngine.addSpawnGroup(spawn, 1);

		assertDoesNotThrow(TemporarySpawnEngineTest::despawn);
		assertEquals(2, field("temporarySpawns", List.class).size());
	}

	private static void despawn() throws ReflectiveOperationException {
		Method method = TemporarySpawnEngine.class.getDeclaredMethod("despawn");
		method.setAccessible(true);
		try {
			method.invoke(null);
		} catch (ReflectiveOperationException e) {
			throw e;
		}
	}

	private static <T> T field(String name, Class<T> type) throws ReflectiveOperationException {
		Field field = TemporarySpawnEngine.class.getDeclaredField(name);
		field.setAccessible(true);
		return type.cast(field.get(null));
	}

	private static final class TestSpawnTemplate extends SpawnTemplate {
		private final TemporarySpawn temporarySpawn;

		private TestSpawnTemplate(SpawnGroup2 spawnGroup) {
			this(spawnGroup, new TestTemporarySpawn());
		}

		private TestSpawnTemplate(SpawnGroup2 spawnGroup, TemporarySpawn temporarySpawn) {
			super(spawnGroup, 0, 0, 0, (byte) 0, 0, null, 0, 0);
			this.temporarySpawn = temporarySpawn;
		}

		@Override
		public TemporarySpawn getTemporarySpawn() {
			return temporarySpawn;
		}
	}

	private static final class TestTemporarySpawn extends TemporarySpawn {
		@Override
		public boolean canDespawn() {
			return true;
		}

		@Override
		public boolean canSpawn() {
			return false;
		}
	}

	private static final class RegisteringTemporarySpawn extends TemporarySpawn {
		private final SpawnGroup2 lateSpawn;

		private RegisteringTemporarySpawn(SpawnGroup2 lateSpawn) {
			this.lateSpawn = lateSpawn;
		}

		@Override
		public boolean canDespawn() {
			TemporarySpawnEngine.addSpawnGroup(lateSpawn, 1);
			return true;
		}

		@Override
		public boolean canSpawn() {
			return false;
		}
	}

	private static final class TestVisibleObject extends VisibleObject {
		private boolean deleted;

		private TestVisibleObject(int objectId) {
			super(objectId, new TestVisibleObjectController(), null, null, null);
			controller().setOwner(this);
		}

		@SuppressWarnings("unchecked")
		private VisibleObjectController<VisibleObject> controller() {
			return (VisibleObjectController<VisibleObject>) getController();
		}

		@Override
		public boolean isSpawned() {
			return true;
		}

		@Override
		public String getName() {
			return "test-" + getObjectId();
		}
	}

	private static final class TestVisibleObjectController extends VisibleObjectController<VisibleObject> {
		@Override
		public void onDelete() {
			((TestVisibleObject) getOwner()).deleted = true;
		}
	}
}
