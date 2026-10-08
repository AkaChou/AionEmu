package com.aionemu.gameserver.spawnengine;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import com.aionemu.gameserver.controllers.VisibleObjectController;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMap;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * 刷怪统一入口在副本实例注销后的护栏：目标实例已不在 WorldMap 时必须放弃生成，而不是继续走到 NPE。
 * Guard for the unified spawn entry after instance teardown: when the target instance is no longer registered
 * in WorldMap the spawn must be abandoned instead of continuing into an NPE.
 * <p>背景：{@code InstanceService.destroyInstance} 先 {@code removeWorldMapInstance} 再逐个 {@code onDelete}；
 * 删除期间 AI 的 despawn 链仍会执行 spawn 动作（实机：帕休曼迪尔寺院 Temadaro despawn → DespawnLich 281697），
 * {@code World.setPosition} 查不到实例时静默返回、mapRegion 保持 null，{@code World.spawn} 继续执行并在
 * onBeforeSpawn（InstanceScaler）处 NPE，同时在 World 注册表残留未生成对象。
 * Background: destroyInstance removes the instance from WorldMap before deleting its objects; AI despawn
 * chains still execute spawn actions during that window (live: Beshmundir Temple Temadaro despawn → DespawnLich
 * 281697); World.setPosition silently leaves mapRegion null, and World.spawn then NPEs in onBeforeSpawn
 * (InstanceScaler) while leaving an unborn object registered in World.</p>
 */
class SpawnEngineTeardownSpawnGuardTest {

	private static final int WORLD_ID = 300_170_000;
	private static final int INSTANCE_ID = 2;
	private static final ObjenesisStd OBJENESIS = new ObjenesisStd();

	private GameWorldBootstrapServices worldServices;

	@AfterEach
	void releaseWorldServices() {
		if (worldServices != null) {
			worldServices.destroy();
		}
	}

	@Test
	void skipsSpawnWhenTheTargetInstanceWasAlreadyUnregistered() {
		TestWorld world = installWorld(worldMap(null));
		TestVisibleObject npc = new TestVisibleObject();

		assertDoesNotThrow(() -> SpawnEngine.bringIntoWorld(npc, WORLD_ID, INSTANCE_ID, 979.0f, 130.0f, 242.0f, (byte) 0));

		assertEquals(0, world.storeCount, "an unregistered instance must not receive a stored object");
		assertEquals(0, world.spawnCount, "an unregistered instance must not receive a spawn");
		assertFalse(npc.isSpawned(), "the object must stay unborn after the guarded spawn");
	}

	@Test
	void keepsSpawningWhenTheTargetInstanceStillExists() {
		TestWorld world = installWorld(worldMap(OBJENESIS.newInstance(TestWorldMapInstance.class)));
		TestVisibleObject npc = new TestVisibleObject();

		SpawnEngine.bringIntoWorld(npc, WORLD_ID, INSTANCE_ID, 979.0f, 130.0f, 242.0f, (byte) 0);

		assertEquals(1, world.storeCount, "a live instance must still receive the object");
		assertEquals(1, world.spawnCount, "a live instance must still receive the spawn");
	}

	private TestWorld installWorld(WorldMap map) {
		TestWorld world = OBJENESIS.newInstance(TestWorld.class);
		world.map = map;
		worldServices = new GameWorldBootstrapServices(
			provider(IDFactory.class, OBJENESIS.newInstance(TestIdFactory.class)), null, null, null,
			provider(World.class, world));
		return world;
	}

	private static WorldMap worldMap(WorldMapInstance instance) {
		WorldMap map = OBJENESIS.newInstance(WorldMap.class);
		setField(map, "worldMapTemplate", OBJENESIS.newInstance(WorldMapTemplate.class));
		Map<Integer, WorldMapInstance> instances = new LinkedHashMap<>();
		if (instance != null) {
			instances.put(INSTANCE_ID, instance);
		}
		setField(map, "instances", instances);
		return map;
	}

	private static void setField(Object target, String name, Object value) {
		try {
			Field field = WorldMap.class.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton(type.getName(), instance);
		return beanFactory.getBeanProvider(type);
	}

	private static final class TestWorld extends World {
		private WorldMap map;
		private int storeCount;
		private int spawnCount;

		@Override
		public WorldMap getWorldMap(int id) {
			return map;
		}

		@Override
		public void storeObject(VisibleObject object) {
			storeCount++;
		}

		@Override
		public void spawn(VisibleObject object) {
			spawnCount++;
		}
	}

	private static final class TestWorldMapInstance extends WorldMapInstance {
		private TestWorldMapInstance() {
			super(null, 0);
		}

		@Override
		public Integer getMapId() {
			return WORLD_ID;
		}

		@Override
		public int getInstanceId() {
			return INSTANCE_ID;
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

	private static final class TestIdFactory extends IDFactory {
		@Override
		public int nextId() {
			return 1;
		}
	}

	private static final class TestVisibleObject extends VisibleObject {
		private TestVisibleObject() {
			super(1, new VisibleObjectController<VisibleObject>() {}, null, null, new WorldPosition(WORLD_ID));
		}

		@Override
		public String getName() {
			return "teardown-spawn-guard";
		}
	}
}
