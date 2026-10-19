package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TribeClass;
import com.aionemu.gameserver.model.gameobjects.TransformModel;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.skillengine.effect.Effects;
import com.aionemu.gameserver.skillengine.effect.PolymorphEffect;
import com.aionemu.gameserver.skillengine.effect.TransformEffect;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * 术古皇陵生命周期、调度归属与开战门禁回归，不启动世界或线程池。
 * Guards Shugo Imperial Tomb lifecycle, task ownership and battle gating without starting a world or thread pool.
 */
class ShugoImperialTombInstanceTest {
	private static final ObjenesisStd OBJENESIS = new ObjenesisStd();
	private static final List<String> WAVE_TASKS = List.of(
		"tombRaidTaskA1", "tombRaidTaskB1", "tombRaidTaskC1", "tombRaidTaskC2");

	@Test
	void teardownCancelsEveryWaveAndNestedSceneTask() throws ReflectiveOperationException {
		ShugoImperialTombInstance handler = createBoundHandler(List.of(), 1);
		List<Future<?>> allTasks = tasks(handler, "imperialTombTask");
		List<Future<?>> submitted = new ArrayList<>();
		for (String group : WAVE_TASKS) {
			List<Future<?>> waveTasks = tasks(handler, group);
			for (int index = 0; index < 2; index++) {
				CompletableFuture<Void> task = new CompletableFuture<>();
				waveTasks.add(task);
				allTasks.add(task);
				submitted.add(task);
			}
		}

		handler.onInstanceDestroy();

		assertTrue(submitted.stream().allMatch(Future::isCancelled));
		assertTrue(allTasks.isEmpty());
		for (String group : WAVE_TASKS) {
			assertTrue(tasks(handler, group).isEmpty(), group);
		}
		assertDoesNotThrow(handler::onInstanceDestroy);
	}

	@Test
	void stageCompletionCancelsThatStagesTopLevelAndNestedFutures() throws ReflectiveOperationException {
		TestSchedulingHandler handler = (TestSchedulingHandler) createBoundHandler(List.of(), 1);
		List<Future<?>> stageA = tasks(handler, "tombRaidTaskA1");
		List<Future<?>> stageB = tasks(handler, "tombRaidTaskB1");
		CompletableFuture<Void> topLevel = new CompletableFuture<>();
		stageA.add(topLevel);
		invokeRunTask(handler, () -> handler.sp(219508, 0, 0, 0, (byte) 0, 1000, "unused"), stageA, 1);
		Future<?> nested = stageA.get(1);
		CompletableFuture<Void> otherStage = new CompletableFuture<>();
		stageB.add(otherStage);
		Method cancelTasks = ShugoImperialTombInstance.class.getDeclaredMethod("cancelTasks", List.class);
		cancelTasks.setAccessible(true);

		cancelTasks.invoke(handler, stageA);

		assertTrue(topLevel.isCancelled());
		assertTrue(nested == handler.lastScheduled);
		assertTrue(nested.isCancelled());
		assertTrue(stageA.isEmpty());
		assertFalse(otherStage.isCancelled());
	}

	@Test
	void staleCallbacksAreIgnoredAfterTeardownAndInstanceRebind() throws ReflectiveOperationException {
		ShugoImperialTombInstance handler = createBoundHandler(List.of(), 1);
		AtomicInteger calls = new AtomicInteger();

		invokeRunTask(handler, calls::incrementAndGet, null, 1);
		assertEquals(1, calls.get());

		handler.onInstanceDestroy();
		invokeRunTask(handler, calls::incrementAndGet, null, 1);
		createBoundHandler(handler, List.of(), 2);
		invokeRunTask(handler, calls::incrementAndGet, null, 1);

		assertEquals(1, calls.get());
	}

	@Test
	void unregisteredInstanceRejectsAlreadyDequeuedCallbacksAndNewTasks() throws ReflectiveOperationException {
		TestSchedulingHandler handler = (TestSchedulingHandler) createBoundHandler(List.of(), 1);
		AtomicInteger calls = new AtomicInteger();
		handler.currentInstance = false;

		invokeRunTask(handler, calls::incrementAndGet, null, 1);
		handler.scheduleTask(calls::incrementAndGet, 0);

		assertEquals(0, calls.get());
		assertTrue(tasks(handler, "imperialTombTask").isEmpty());
	}

	@Test
	void teardownAndUnboundHandlerIgnoreSceneCallbacksWithoutThrowing() throws ReflectiveOperationException {
		ShugoImperialTombInstance handler = createBoundHandler(List.of(), 1);
		AtomicInteger calls = new AtomicInteger();
		handler.onInstanceDestroy();

		assertDoesNotThrow(() -> handler.scheduleTask(calls::incrementAndGet, 0));
		assertDoesNotThrow(() -> handler.sp(219508, 0, 0, 0, (byte) 0, 1000, "unused"));
		assertDoesNotThrow(() -> handler.sendMsgByRace(1401582, Race.PC_ALL, 1000));
		assertDoesNotThrow(() -> handler.onDie(null));
		assertDoesNotThrow(() -> new ShugoImperialTombInstance().onInstanceDestroy());

		assertEquals(0, calls.get());
		assertTrue(tasks(handler, "imperialTombTask").isEmpty());
	}

	@Test
	void battleGateRequiresActiveEffectPanelTribeAndEveryOnlineParticipant() throws ReflectiveOperationException {
		Player owner = player(1, Race.ELYOS, true, TribeClass.IDEVENT01_POLYMORPHL);
		ShugoImperialTombInstance handler = createBoundHandler(List.of(owner), 1);

		assertTrue(((TestSchedulingHandler) handler).currentInstance);
		assertTrue(handler.hasValidShugoTransform(owner));

		Player persistedPanelOnly = player(2, Race.ELYOS, false, TribeClass.IDEVENT01_POLYMORPHL);
		assertFalse(handler.hasValidShugoTransform(persistedPanelOnly));

		Player wrongPanel = player(3, Race.ELYOS, true, TribeClass.IDEVENT01_POLYMORPHL);
		wrongPanel.getTransformModel().setPanelId(71);
		assertFalse(handler.hasValidShugoTransform(wrongPanel));

		Player wrongTribe = player(4, Race.ELYOS, true, TribeClass.IDEVENT01_POLYMORPHD);
		assertFalse(handler.hasValidShugoTransform(wrongTribe));
		Player darkPlayer = player(5, Race.ASMODIANS, true, TribeClass.IDEVENT01_POLYMORPHD);
		assertTrue(handler.hasValidShugoTransform(darkPlayer));

		ShugoImperialTombInstance solo = createBoundHandler(List.of(owner), 1);
		setOnline(owner, true);
		assertTrue(solo.canStartTombBattle(owner));

		ShugoImperialTombInstance group = createBoundHandler(List.of(owner, persistedPanelOnly), 1);
		setOnline(owner, true);
		setOnline(persistedPanelOnly, true);
		assertFalse(group.canStartTombBattle(owner));
	}

	@SuppressWarnings("unchecked")
	private static List<Future<?>> tasks(ShugoImperialTombInstance handler, String name)
		throws ReflectiveOperationException {
		Field field = ShugoImperialTombInstance.class.getDeclaredField(name);
		field.setAccessible(true);
		return (List<Future<?>>) field.get(handler);
	}

	private static void invokeRunTask(ShugoImperialTombInstance handler, Runnable task,
		List<Future<?>> waveTasks, long generation) throws ReflectiveOperationException {
		Method runTask = ShugoImperialTombInstance.class.getDeclaredMethod(
			"runTask", Runnable.class, List.class, long.class);
		runTask.setAccessible(true);
		runTask.invoke(handler, task, waveTasks, generation);
	}

	private static ShugoImperialTombInstance createBoundHandler(List<Player> players, int instanceId)
		throws ReflectiveOperationException {
		return createBoundHandler(new TestSchedulingHandler(), players, instanceId);
	}

	private static ShugoImperialTombInstance createBoundHandler(ShugoImperialTombInstance handler,
		List<Player> players, int instanceId) throws ReflectiveOperationException {
		TestWorldMapInstance instance = OBJENESIS.newInstance(TestWorldMapInstance.class);
		Map<Integer, Player> playerMap = new LinkedHashMap<>();
		for (Player player : players) {
			playerMap.put(player.getObjectId(), player);
		}
		setField(WorldMapInstance.class, instance, "instanceId", instanceId);
		setField(WorldMapInstance.class, instance, "worldMapPlayers", Collections.synchronizedMap(playerMap));
		setField(WorldMapInstance.class, instance, "instanceHandler", handler);
		setField(ShugoImperialTombInstance.class, handler, "instance", instance);
		setField(ShugoImperialTombInstance.class, handler, "instanceId", instanceId);
		setField(ShugoImperialTombInstance.class, handler, "mapId", 300560000);
		setField(ShugoImperialTombInstance.class, handler, "isInstanceDestroyed", false);
		setField(ShugoImperialTombInstance.class, handler, "lifecycleGeneration", (long) instanceId);
		return handler;
	}

	private static Player player(int objectId, Race race, boolean activeEffect, TribeClass tribe)
		throws ReflectiveOperationException {
		Player player = OBJENESIS.newInstance(Player.class);
		setField(Player.class, player, "objectId", objectId);
		PlayerCommonData commonData = new PlayerCommonData(objectId);
		commonData.setRace(race);
		TransformModel model = OBJENESIS.newInstance(TransformModel.class);
		model.setPanelId(25);
		if (activeEffect) {
			PolymorphEffect polymorph = new PolymorphEffect();
			setField(TransformEffect.class, polymorph, "panelid", 25);
			Effects effects = new Effects();
			effects.getEffects().add(polymorph);
			SkillTemplate template = OBJENESIS.newInstance(SkillTemplate.class);
			setField(SkillTemplate.class, template, "effects", effects);
			Effect effect = OBJENESIS.newInstance(Effect.class);
			setField(Effect.class, effect, "skillTemplate", template);
			model.setActiveTransform(effect, 175, false, 0);
			model.setTribe(tribe, false);
		}
		player.setTransformModel(model);
		setField(Player.class, player, "playerCommonData", commonData);
		return player;
	}

	private static void setOnline(Player player, boolean online) throws ReflectiveOperationException {
		if (online) {
			player.setClientConnection(OBJENESIS.newInstance(
				com.aionemu.gameserver.network.aion.AionConnection.class));
		} else {
			player.setClientConnection(null);
		}
	}

	private static void setField(Class<?> owner, Object target, String name, Object value)
		throws ReflectiveOperationException {
		Class<?> type = owner;
		Field field;
		while (true) {
			try {
				field = type.getDeclaredField(name);
				break;
			} catch (NoSuchFieldException e) {
				type = type.getSuperclass();
				if (type == null) {
					throw e;
				}
			}
		}
		field.setAccessible(true);
		field.set(target, value);
	}

	/**
	 * 仅提供参与者与地图标识的实例，不初始化地图区域。
	 * Instance stub supplying participants and map identity without initializing regions.
	 */
	private static final class TestWorldMapInstance extends WorldMapInstance {
		private TestWorldMapInstance() {
			super(null, 0);
		}

		@Override
		public Integer getMapId() {
			return 300560000;
		}

		@Override
		public com.aionemu.gameserver.world.MapRegion getRegion(float x, float y, float z) {
			return null;
		}

		@Override
		protected com.aionemu.gameserver.world.MapRegion createMapRegion(int regionId) {
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

	/**
	 * 用可取消句柄替代真实线程池，并显式控制注册身份。
	 * Replaces the thread pool with cancellable handles and explicit registration identity.
	 */
	private static final class TestSchedulingHandler extends ShugoImperialTombInstance {
		private Future<?> lastScheduled;
		private boolean currentInstance = true;

		@Override
		protected Future<?> scheduleTaskExecution(Runnable task, int delay) {
			lastScheduled = new CompletableFuture<>();
			return lastScheduled;
		}

		@Override
		protected boolean isCurrentInstance() {
			return currentInstance;
		}
	}
}
