package com.aionemu.gameserver.world;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class WorldMapTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();

	@Test
	void iteratorReturnsSnapshotSafeForInstanceRemovalDuringIteration() throws ReflectiveOperationException {
		WorldMap map = worldMapWithInstances();
		Iterator<WorldMapInstance> iterator = map.iterator();

		assertDoesNotThrow(() -> {
			while (iterator.hasNext()) {
				map.removeWorldMapInstance(iterator.next().getInstanceId());
			}
		});
		assertTrue(map.getInstances().isEmpty());
	}

	@Test
	void getAvailableInstanceIdsReturnsSnapshotSafeForInstanceRemovalDuringIteration() throws ReflectiveOperationException {
		WorldMap map = worldMapWithInstances();
		Collection<Integer> instanceIds = map.getAvailableInstanceIds();

		assertDoesNotThrow(() -> {
			for (Integer instanceId : instanceIds) {
				map.removeWorldMapInstance(instanceId);
			}
		});
		assertTrue(map.getInstances().isEmpty());
	}

	@Test
	void getInstancesReturnsSnapshotSafeForInstanceRemovalDuringIteration() throws ReflectiveOperationException {
		WorldMap map = worldMapWithInstances();
		Collection<WorldMapInstance> instances = map.getInstances();

		assertDoesNotThrow(() -> {
			for (WorldMapInstance instance : instances) {
				map.removeWorldMapInstance(instance.getInstanceId());
			}
		});
		assertTrue(map.getInstances().isEmpty());
	}

	private WorldMap worldMapWithInstances() throws ReflectiveOperationException {
		WorldMap map = objenesis.newInstance(WorldMap.class);
		Map<Integer, WorldMapInstance> instances = new LinkedHashMap<Integer, WorldMapInstance>();
		instances.put(1, instance(1));
		instances.put(2, instance(2));
		instances.put(3, instance(3));
		setField(map, "instances", instances);
		return map;
	}

	private WorldMapInstance instance(int instanceId) throws ReflectiveOperationException {
		WorldMapInstance instance = objenesis.newInstance(TestWorldMapInstance.class);
		setField(instance, "instanceId", instanceId);
		return instance;
	}

	private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
		Field field = target instanceof WorldMap ? WorldMap.class.getDeclaredField(fieldName)
				: WorldMapInstance.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static final class TestWorldMapInstance extends WorldMapInstance {

		private TestWorldMapInstance() {
			super(null, 0);
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
}
