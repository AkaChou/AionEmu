package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.dataholders.NpcSkillData;
import com.aionemu.gameserver.dataholders.RetailAiData;
import com.aionemu.gameserver.dataholders.RetailAiData.ConditionSpawn;
import com.aionemu.gameserver.dataholders.RetailAiData.ConditionSpawnChoice;
import com.aionemu.gameserver.dataholders.RetailAiData.ConditionSpawnGroup;
import com.aionemu.gameserver.dataholders.RetailAiData.ConditionSpawnNpc;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.NpcType;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.npc.NpcRating;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.stats.NpcStatsTemplate;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldPosition;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetailConditionSpawnEngineTest {
	private static final int WORLD_ID = 1;
	private static final int NPC_ID = 999_999;
	private static final ObjenesisStd OBJENESIS = new ObjenesisStd();

	@Test
	void spawnsOnInitializeAndDespawnsWhenConditionStopsMatching() throws ReflectiveOperationException {
		RetailAiData previousRetailAiData = DataManager.RETAIL_AI_DATA;
		NpcData previousNpcData = DataManager.NPC_DATA;
		NpcSkillData previousNpcSkillData = DataManager.NPC_SKILL_DATA;
		TestWorld world = OBJENESIS.newInstance(TestWorld.class);
		WorldMapInstance instance = OBJENESIS.newInstance(TestWorldMapInstance.class);
		AI2Engine ai2Engine = new AI2Engine();
		ai2Engine.registerAI(DummyAI2.class);
		GameEngineServices engineServices = new GameEngineServices(null, null, null,
			provider(AI2Engine.class, ai2Engine), null);
		GameWorldBootstrapServices worldServices = new GameWorldBootstrapServices(
			provider(IDFactory.class, OBJENESIS.newInstance(TestIdFactory.class)), null, null, null,
			provider(World.class, world));
		try {
			DataManager.NPC_DATA = npcData();
			DataManager.NPC_SKILL_DATA = new NpcSkillData(List.of());
			DataManager.RETAIL_AI_DATA = retailAiData();

			RetailConditionSpawnEngine.initialize(instance);

			assertEquals(1, world.spawnCount);
			assertNotNull(world.object);
			VisibleObject spawned = world.object;
			assertTrue(spawned.isSpawned());

			assertTrue(RetailConditionSpawnEngine.setVariable(instance, "wave", 1, 0));

			assertEquals(1, world.despawnCount);
			assertFalse(spawned.isSpawned());
			assertNull(world.findVisibleObject(spawned.getObjectId()));
		} finally {
			RetailConditionSpawnEngine.clear(instance);
			DataManager.RETAIL_AI_DATA = previousRetailAiData;
			DataManager.NPC_DATA = previousNpcData;
			DataManager.NPC_SKILL_DATA = previousNpcSkillData;
			worldServices.destroy();
			engineServices.destroy();
		}
	}

	@Test
	void appliesRetailVariablesAndCompoundExpressions() {
		assertEquals(7, RetailConditionSpawnEngine.nextValue(3, 7, 0));
		assertEquals(5, RetailConditionSpawnEngine.nextValue(3, 0, 2));
		assertEquals(-2, RetailConditionSpawnEngine.nextValue(3, 0, -5));
		assertTrue(RetailConditionSpawnEngine.evaluate("wave >= 3 && (race == 1 || race == 2)",
			Map.of("wave", 3, "race", 2)));
		assertTrue(RetailConditionSpawnEngine.evaluate("SpecialServer_Cond == 0", Map.of()));
		assertFalse(RetailConditionSpawnEngine.evaluate("wave >= 3 && race != 2",
			Map.of("wave", 3, "race", 2)));
	}

	@Test
	void filtersRetailConditionsBySpawnPage() {
		ConditionSpawn normal = condition(1, 1);
		ConditionSpawn hard = condition(2, 2);

		assertEquals(List.of(normal), RetailConditionSpawnEngine.conditionsForPage(List.of(normal, hard), 1));
		assertEquals(List.of(hard), RetailConditionSpawnEngine.conditionsForPage(List.of(normal, hard), 2));
	}

	@Test
	void appliesRetailRespawnRandomWindow() {
		ConditionSpawnNpc npc = conditionNpc(0, 120, 30);
		for (int i = 0; i < 100; i++) {
			int delay = RetailConditionSpawnEngine.respawnDelaySeconds(npc);
			assertTrue(delay >= 120 && delay <= 150);
		}
	}

	@Test
	void appliesCaseInsensitiveWorldFlagGates() {
		HashSet<String> flags = new HashSet<>();

		assertTrue(RetailConditionSpawnEngine.updateFlag(flags, "FLAGVARI_ALPHA_1", true));
		assertFalse(RetailConditionSpawnEngine.updateFlag(flags, "flagvari_alpha_1", true));
		assertTrue(RetailConditionSpawnEngine.updateFlag(flags, "FLAGVARI_ALPHA_1", false));
		assertFalse(RetailConditionSpawnEngine.updateFlag(flags, "FLAGVARI_ALPHA_1", false));
		assertTrue(RetailConditionSpawnEngine.updateFlag(flags, "FLAGVARI_ALPHA_1", true));
		assertTrue(RetailConditionSpawnEngine.consumeFlag(flags, "flagvari_alpha_1"));
		assertFalse(RetailConditionSpawnEngine.consumeFlag(flags, "FLAGVARI_ALPHA_1"));
	}

	@Test
	void persistsConditionVariablesAndFlags() {
		RetailAiData previous = DataManager.RETAIL_AI_DATA;
		WorldMapInstance instance = OBJENESIS.newInstance(TestWorldMapInstance.class);
		try {
			DataManager.RETAIL_AI_DATA = retailAiData();
			assertTrue(RetailConditionSpawnEngine.setVariable(instance, "wave", 4, 0));
			assertTrue(RetailConditionSpawnEngine.setFlag(instance, "gate", true));
			var restored = com.aionemu.gameserver.model.instance.InstanceRuntimeState.decode(
				instance.getRuntimeState().encode());

			assertEquals(4, restored.getInt("retail.condition.variable.wave", 0));
			assertTrue(restored.getBoolean("retail.condition.flag.gate", false));
		} finally {
			RetailConditionSpawnEngine.clear(instance);
			DataManager.RETAIL_AI_DATA = previous;
		}
	}

	@Test
	void doesNotRestoreDeadSingleUseSpawn() throws ReflectiveOperationException {
		try (TestContext context = new TestContext(conditionNpc(0, 0))) {
			RetailConditionSpawnEngine.initialize(context.instance);
			RetailConditionSpawnEngine.onDie(context.instance, (Npc) context.world.object);
			var restoredState = com.aionemu.gameserver.model.instance.InstanceRuntimeState.decode(
				context.instance.getRuntimeState().encode());
			WorldMapInstance restored = OBJENESIS.newInstance(TestWorldMapInstance.class);
			restored.setDynamicInstance(null, restoredState);

			RetailConditionSpawnEngine.initialize(restored);

			assertEquals(1, context.world.spawnCount);
			assertTrue(restoredState.getBoolean("retail.condition.spawn.1.object.0.0.0.dead", false));
			RetailConditionSpawnEngine.clear(restored);
		}
	}

	@Test
	void doesNotRestoreExpiredTimedSpawn() throws ReflectiveOperationException {
		try (TestContext context = new TestContext(conditionNpc(1, 0))) {
			context.instance.getRuntimeState().put("retail.condition.spawn.1.object.0.0.0.life_deadline",
				System.currentTimeMillis() - 1);

			RetailConditionSpawnEngine.initialize(context.instance);

			assertEquals(0, context.world.spawnCount);
		}
	}

	@Test
	void schedulesFixedRespawnOnlyOnce() throws Exception {
		try (TestContext context = new TestContext(conditionNpc(0, 1))) {
			RetailConditionSpawnEngine.initialize(context.instance);
			Npc npc = (Npc) context.world.object;

			RetailConditionSpawnEngine.onDie(context.instance, npc);
			long deadline = context.instance.getRuntimeState()
				.getLong("retail.condition.spawn.1.object.0.0.0.respawn_deadline", 0);
			assertTrue(deadline > 0);
			TimeUnit.MILLISECONDS.sleep(10);
			RetailConditionSpawnEngine.onDie(context.instance, npc);

			assertEquals(deadline, context.instance.getRuntimeState()
				.getLong("retail.condition.spawn.1.object.0.0.0.respawn_deadline", 0));
			long timeout = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
			while (context.world.spawnCount < 2 && System.nanoTime() < timeout) {
				TimeUnit.MILLISECONDS.sleep(10);
			}
			TimeUnit.MILLISECONDS.sleep(100);
			assertEquals(2, context.world.spawnCount);
		}
	}

	private static RetailAiData retailAiData() {
		return retailAiData(conditionNpc(0, 0));
	}

	private static ConditionSpawnNpc conditionNpc(int life, int respawnTime) {
		return conditionNpc(life, respawnTime, 0);
	}

	private static ConditionSpawnNpc conditionNpc(int life, int respawnTime, int respawnTimeExtra) {
		return new ConditionSpawnNpc(NPC_ID, 10, 20, 30, 0, 0, 0, null, null, life, respawnTime,
			respawnTimeExtra);
	}

	private static RetailAiData retailAiData(ConditionSpawnNpc npc) {
		ConditionSpawn condition = condition(0, 255, npc);
		return new RetailAiData(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
			Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(WORLD_ID, List.of(condition)),
			Map.of(WORLD_ID, Set.of("wave")), Map.of(), Map.of());
	}

	private static ConditionSpawn condition(int pageStart, int pageEnd) {
		return condition(pageStart, pageEnd, conditionNpc(0, 0));
	}

	private static ConditionSpawn condition(int pageStart, int pageEnd, ConditionSpawnNpc npc) {
		ConditionSpawnChoice choice = new ConditionSpawnChoice(10_000, null, List.of(npc));
		ConditionSpawnGroup group = new ConditionSpawnGroup(1_000, List.of(List.of(choice)));
		return new ConditionSpawn(1, "wave == 0", pageStart, pageEnd, true, "all", List.of(group));
	}

	private static NpcData npcData() throws ReflectiveOperationException {
		NpcTemplate template = new NpcTemplate();
		setField(template, NpcTemplate.class, "npcId", NPC_ID);
		setField(template, NpcTemplate.class, "level", (byte) 1);
		setField(template, NpcTemplate.class, "name", "condition-spawn-test");
		setField(template, NpcTemplate.class, "rating", NpcRating.JUNK);
		template.setNpcType(NpcType.ATTACKABLE);
		template.setStatsTemplate(new NpcStatsTemplate());
		NpcData data = new NpcData();
		data.getNpcData().put(NPC_ID, template);
		return data;
	}

	private static void setField(Object target, Class<?> owner, String name, Object value)
			throws ReflectiveOperationException {
		Field field = owner.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static void setSpawned(VisibleObject object, boolean spawned) {
		try {
			setField(object.getPosition(), WorldPosition.class, "isSpawned", spawned);
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
		private VisibleObject object;
		private volatile int spawnCount;
		private int despawnCount;

		@Override
		public void storeObject(VisibleObject object) {
			this.object = object;
		}

		@Override
		public void setPosition(VisibleObject object, int mapId, int instance, float x, float y, float z, byte heading) {
			object.getPosition().setXYZH(x, y, z, heading);
		}

		@Override
		public void spawn(VisibleObject object) {
			setSpawned(object, true);
			spawnCount++;
		}

		@Override
		public void despawn(VisibleObject object) {
			setSpawned(object, false);
			despawnCount++;
		}

		@Override
		public void removeObject(VisibleObject object) {
			this.object = null;
		}

		@Override
		public VisibleObject findVisibleObject(int objectId) {
			return object != null && object.getObjectId() == objectId ? object : null;
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

	private static final class TestIdFactory extends IDFactory {
		@Override
		public int nextId() {
			return 1;
		}
	}

	private static final class TestContext implements AutoCloseable {
		private final RetailAiData previousRetailAiData = DataManager.RETAIL_AI_DATA;
		private final NpcData previousNpcData = DataManager.NPC_DATA;
		private final NpcSkillData previousNpcSkillData = DataManager.NPC_SKILL_DATA;
		private final TestWorld world = OBJENESIS.newInstance(TestWorld.class);
		private final WorldMapInstance instance = OBJENESIS.newInstance(TestWorldMapInstance.class);
		private final ThreadPoolManager threadPoolManager = new ThreadPoolManager();
		private final GameThreadPoolServices threadPoolServices;
		private final GameEngineServices engineServices;
		private final GameWorldBootstrapServices worldServices;

		private TestContext(ConditionSpawnNpc npc) throws ReflectiveOperationException {
			DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
			beans.registerSingleton("threadPoolManager", threadPoolManager);
			threadPoolServices = new GameThreadPoolServices(beans.getBeanProvider(ThreadPoolManager.class));
			AI2Engine ai2Engine = new AI2Engine();
			ai2Engine.registerAI(DummyAI2.class);
			engineServices = new GameEngineServices(null, null, null, provider(AI2Engine.class, ai2Engine), null);
			worldServices = new GameWorldBootstrapServices(
				provider(IDFactory.class, OBJENESIS.newInstance(TestIdFactory.class)), null, null, null,
				provider(World.class, world));
			DataManager.NPC_DATA = npcData();
			DataManager.NPC_SKILL_DATA = new NpcSkillData(List.of());
			DataManager.RETAIL_AI_DATA = retailAiData(npc);
		}

		@Override
		public void close() {
			RetailConditionSpawnEngine.clear(instance);
			DataManager.RETAIL_AI_DATA = previousRetailAiData;
			DataManager.NPC_DATA = previousNpcData;
			DataManager.NPC_SKILL_DATA = previousNpcSkillData;
			worldServices.destroy();
			engineServices.destroy();
			threadPoolServices.destroy();
			threadPoolManager.shutdown();
		}
	}
}
