package com.aionemu.gameserver.services.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.configs.main.InstanceConfig;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.WorldMap;
import com.aionemu.gameserver.world.WorldMapInstance;

class InstanceServiceTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();

	@Test
	void emptyInstanceCanResetEvenWhenPlayerRegistrationRemains() {
		TestWorldMapInstance instance = instanceWithPlayerCount(0);
		instance.register(1001);

		assertTrue(instance.isRegistered(1001));
		assertTrue(InstanceService.isEmptyForResetAfterLeave(instance));
	}

	@Test
	void instanceWithPlayersInsideCannotResetAfterOnePlayerLeaves() {
		TestWorldMapInstance instance = instanceWithPlayerCount(1);

		assertFalse(InstanceService.isEmptyForResetAfterLeave(instance));
	}

	@Test
	void usesSeparateRegularAndSoloDestroyDelays() {
		int regularDelay = InstanceConfig.DESTROY_DELAY_SECONDS;
		int soloDelay = InstanceConfig.SOLO_DESTROY_DELAY_SECONDS;
		try {
			InstanceConfig.DESTROY_DELAY_SECONDS = 90;
			InstanceConfig.SOLO_DESTROY_DELAY_SECONDS = 30;
			assertEquals(90_000L, InstanceService.getDestroyDelayMillis(false));
			assertEquals(30_000L, InstanceService.getDestroyDelayMillis(true));
		} finally {
			InstanceConfig.DESTROY_DELAY_SECONDS = regularDelay;
			InstanceConfig.SOLO_DESTROY_DELAY_SECONDS = soloDelay;
		}
	}

	@Test
	void protectsPlayerTransitionsWhenDestroyDelayIsZero() {
		int regularDelay = InstanceConfig.DESTROY_DELAY_SECONDS;
		try {
			InstanceConfig.DESTROY_DELAY_SECONDS = 0;
			assertEquals(1000L, InstanceService.getScheduledDestroyDelayMillis(false));
		} finally {
			InstanceConfig.DESTROY_DELAY_SECONDS = regularDelay;
		}
	}

	@Test
	void failedSpawnRemovesPublishedInstanceAndPreservesOriginalFailure() {
		TestWorldMap map = emptyMap();
		TestWorldMapInstance instance = instanceWithPlayerCount(0);
		instance.instanceId = 7;
		IllegalStateException failure = new IllegalStateException("spawn failed");

		IllegalStateException thrown = assertThrows(IllegalStateException.class,
				() -> InstanceService.initializeInstance(map, instance, () -> {
					throw failure;
				}, () -> {
				}));

		assertSame(failure, thrown);
		assertFalse(map.contains(7));
	}

	@Test
	void failedInstanceCreateRemovesSpawnedInstanceAndPreservesOriginalFailure() {
		TestWorldMap map = emptyMap();
		TestWorldMapInstance instance = instanceWithPlayerCount(0);
		instance.instanceId = 8;
		IllegalStateException failure = new IllegalStateException("instance create failed");

		IllegalStateException thrown = assertThrows(IllegalStateException.class,
				() -> InstanceService.initializeInstance(map, instance, () -> {
				}, () -> {
					throw failure;
				}));

		assertSame(failure, thrown);
		assertFalse(map.contains(8));
	}

	@Test
	void rollbackCleanupFailureIsSuppressedWithoutReplacingOriginalFailure() {
		TestWorldMap map = emptyMap();
		TestWorldMapInstance instance = instanceWithPlayerCount(0);
		instance.instanceId = 12;
		IllegalStateException failure = new IllegalStateException("spawn failed");
		IllegalStateException cleanupFailure = new IllegalStateException("remove failed");
		map.removeFailure = cleanupFailure;

		IllegalStateException thrown = assertThrows(IllegalStateException.class,
				() -> InstanceService.initializeInstance(map, instance, () -> {
					throw failure;
				}, () -> {
				}));

		assertSame(failure, thrown);
		assertTrue(Arrays.asList(thrown.getSuppressed()).contains(cleanupFailure));
	}

	@Test
	void registeredOrCreateReturnsExistingRegistrationWithoutCreating() throws ReflectiveOperationException {
		TestWorldMap map = emptyMap();
		Player player = player(1001);
		TestWorldMapInstance registered = instanceWithPlayerCount(0);
		registered.instanceId = 9;
		registered.register(player.getObjectId());
		map.addInstance(registered.getInstanceId(), registered);

		WorldMapInstance result = InstanceService.getRegisteredOrCreateAndRegister(map, player, () -> {
			throw new AssertionError("creator must not run");
		});

		assertSame(registered, result);
	}

	@Test
	void registeredOrCreateRegistersNewInstance() throws ReflectiveOperationException {
		TestWorldMap map = emptyMap();
		Player player = player(1002);
		TestWorldMapInstance created = instanceWithPlayerCount(0);
		created.instanceId = 10;

		WorldMapInstance result = InstanceService.getRegisteredOrCreateAndRegister(map, player, () -> {
			map.addInstance(created.getInstanceId(), created);
			return created;
		});

		assertSame(created, result);
		assertTrue(created.isRegistered(player.getObjectId()));
		assertEquals(player.getObjectId(), created.getSoloPlayerObj());
		assertTrue(map.contains(10));
	}

	@Test
	void registrationFailureRollsBackNewInstance() throws ReflectiveOperationException {
		TestWorldMap map = emptyMap();
		Player player = player(1003);
		TestWorldMapInstance created = instanceWithPlayerCount(0);
		created.instanceId = 11;
		created.registrationFailure = new IllegalStateException("registration failed");

		IllegalStateException thrown = assertThrows(IllegalStateException.class,
				() -> InstanceService.getRegisteredOrCreateAndRegister(map, player, () -> {
					map.addInstance(created.getInstanceId(), created);
					return created;
				}));

		assertSame(created.registrationFailure, thrown);
		assertFalse(map.contains(11));
	}

	@Test
	void publicRegisteredOrCreateApiSharesStaticSynchronizationMonitor() throws ReflectiveOperationException {
		Method method = InstanceService.class.getDeclaredMethod("getRegisteredOrCreateAndRegister", int.class,
				Player.class);

		assertTrue(Modifier.isPublic(method.getModifiers()));
		assertTrue(Modifier.isStatic(method.getModifiers()));
		assertTrue(Modifier.isSynchronized(method.getModifiers()));
	}

	@Test
	void concurrentRegisteredOrCreateCallsCreateOneInstance() throws Exception {
		TestWorldMap map = emptyMap();
		Player player = player(1004);
		TestWorldMapInstance created = instanceWithPlayerCount(0);
		created.instanceId = 13;
		AtomicInteger creates = new AtomicInteger();
		CountDownLatch creatorStarted = new CountDownLatch(1);
		CountDownLatch allowCreator = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<WorldMapInstance> first = executor.submit(() -> InstanceService.getRegisteredOrCreateAndRegister(
				map, player, () -> {
					creates.incrementAndGet();
					creatorStarted.countDown();
					try {
						if (!allowCreator.await(5, TimeUnit.SECONDS)) {
							throw new IllegalStateException("Timed out waiting to finish instance creation");
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new IllegalStateException(e);
					}
					map.addInstance(created.getInstanceId(), created);
					return created;
				}));
			assertTrue(creatorStarted.await(5, TimeUnit.SECONDS));
			Future<WorldMapInstance> second = executor.submit(() -> InstanceService.getRegisteredOrCreateAndRegister(
				map, player, () -> {
					creates.incrementAndGet();
					throw new AssertionError("Second caller must reuse the registered instance");
				}));
			allowCreator.countDown();

			assertSame(created, first.get(5, TimeUnit.SECONDS));
			assertSame(created, second.get(5, TimeUnit.SECONDS));
			assertEquals(1, creates.get());
		} finally {
			allowCreator.countDown();
			executor.shutdownNow();
		}
	}

	private TestWorldMapInstance instanceWithPlayerCount(int playerCount) {
		TestWorldMapInstance instance = objenesis.newInstance(TestWorldMapInstance.class);
		instance.playerCount = playerCount;
		return instance;
	}

	private TestWorldMap emptyMap() {
		TestWorldMap map = objenesis.newInstance(TestWorldMap.class);
		map.instances = new LinkedHashMap<>();
		return map;
	}

	private Player player(int objectId) throws ReflectiveOperationException {
		Player player = objenesis.newInstance(Player.class);
		Field objectIdField = AionObject.class.getDeclaredField("objectId");
		objectIdField.setAccessible(true);
		objectIdField.set(player, objectId);
		return player;
	}

	private static final class TestWorldMap extends WorldMap {
		private Map<Integer, WorldMapInstance> instances;
		private RuntimeException removeFailure;

		private TestWorldMap() {
			super(null, null);
		}

		@Override
		public void addInstance(int instanceId, WorldMapInstance instance) {
			instances.put(instanceId, instance);
		}

		@Override
		public void removeWorldMapInstance(int instanceId) {
			if (removeFailure != null) {
				throw removeFailure;
			}
			instances.remove(instanceId);
		}

		@Override
		public Iterator<WorldMapInstance> iterator() {
			return instances.values().iterator();
		}

		@Override
		public boolean isInstanceType() {
			return true;
		}

		private boolean contains(int instanceId) {
			return instances.containsKey(instanceId);
		}
	}

	private static final class TestWorldMapInstance extends WorldMapInstance {
		private int playerCount;
		private int instanceId;
		private Integer registeredObjectId;
		private RuntimeException registrationFailure;

		private TestWorldMapInstance() {
			super(null, 0);
		}

		@Override
		public void register(int objectId) {
			if (registrationFailure != null) {
				throw registrationFailure;
			}
			registeredObjectId = objectId;
		}

		@Override
		public boolean isRegistered(int objectId) {
			return registeredObjectId != null && registeredObjectId == objectId;
		}

		@Override
		public int playersCount() {
			return playerCount;
		}

		@Override
		public int getInstanceId() {
			return instanceId;
		}

		@Override
		public Integer getMapId() {
			return 301580000;
		}

		@Override
		public Iterator<VisibleObject> objectIterator() {
			return Collections.emptyIterator();
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
