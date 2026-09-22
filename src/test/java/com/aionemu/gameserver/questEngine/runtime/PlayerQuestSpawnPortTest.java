package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.stats.container.NpcLifeStats;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestSpawnLocation;
import com.aionemu.gameserver.questEngine.definition.QuestSpawnVariant;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.world.WorldPosition;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real {@link QuestSpawnPort}: after commit spawns a quest NPC under a slot,
 * tracks the authoritative handle, and despawns only that handle. Slot 幂等,
 * 不凭 templateId 删任意同类,handle 不编码进 quest_vars。
 */
class PlayerQuestSpawnPortTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 1001;
	private static final int PLAYER_WORLD = 110010000;
	private static final int SPAWN_WORLD = 310040000;
	private static final int TEMPLATE = 204830;

	@Test
	void spawnsUnderSlotAndDespawnsOnlyTheAuthoritativeHandle() {
		QuestSpawnRegistry registry = new QuestSpawnRegistry();
		Npc[] handles = {npc(SPAWN_WORLD), npc(SPAWN_WORLD)};
		int[] next = {0};
		Player player = player();
		PlayerQuestSpawnPort port = new PlayerQuestSpawnPort(playerId -> player, registry,
			(worldId, instanceId, templateId, x, y, z, heading) -> handles[next[0]++]);

		assertTrue(port.spawnNpc(snapshot(), plan(), "guardian", SPAWN_WORLD, TEMPLATE, 1f, 2f, 3f, (byte) 95));
		// 第二个 slot 得到独立 handle,必须独立管理。
		assertTrue(port.spawnNpc(snapshot(), plan(), "escort", SPAWN_WORLD, TEMPLATE, 1f, 2f, 3f, (byte) 95));

		// despawn 只作用 "guardian" slot 的 handle;escort 不受影响。
		assertTrue(port.despawnNpc(snapshot(), plan(), "guardian"));
		assertFalse(registry.contains(snapshot(), "guardian"));
		assertTrue(registry.contains(snapshot(), "escort"));
	}

	@Test
	void spawnIsIdempotentPerSlotAndDoesNotRespawn() {
		QuestSpawnRegistry registry = new QuestSpawnRegistry();
		Player player = player();
		int[] spawnCalls = {0};
		PlayerQuestSpawnPort port = new PlayerQuestSpawnPort(playerId -> player, registry,
			(worldId, instanceId, templateId, x, y, z, heading) -> {
				spawnCalls[0]++;
				return npc(worldId);
			});

		assertTrue(port.spawnNpc(snapshot(), plan(), "guardian", SPAWN_WORLD, TEMPLATE, 1f, 2f, 3f, (byte) 0));
		// 重复事件再次触发:同 slot 已存在 → 跳过,不无限刷怪。
		assertTrue(port.spawnNpc(snapshot(), plan(), "guardian", SPAWN_WORLD, TEMPLATE, 1f, 2f, 3f, (byte) 0));
		assertEquals(1, spawnCalls[0]);
	}

	@Test
	void questNpcDestroyedOutsideTheRegistryIsRebuiltForItsOwner() {
		QuestSpawnRegistry registry = new QuestSpawnRegistry();
		Player player = player();
		// 已离开世界的任务 NPC:注册表仍有登记,但它已不代表世界状态。
		// A quest NPC that already left the world: still registered, yet no longer
		// representing the world state.
		Npc[] handles = {destroyedNpc(SPAWN_WORLD), destroyedNpc(SPAWN_WORLD)};
		int[] next = {0};
		PlayerQuestSpawnPort port = new PlayerQuestSpawnPort(playerId -> player, registry,
			(worldId, instanceId, templateId, x, y, z, heading) -> handles[next[0]++]);

		assertTrue(port.spawnNpc(snapshot(), plan(), "guardian", SPAWN_WORLD, TEMPLATE, 1f, 2f, 3f, (byte) 0));
		// 世界级清理/他人击杀销毁了它:同 slot 必须能重建,而不是静默报告期望状态已满足。
		// World-wide cleanup or another player's kill destroyed it: the same slot must be
		// rebuildable instead of silently reporting the desired state as satisfied.
		assertTrue(port.spawnNpc(snapshot(), plan(), "guardian", SPAWN_WORLD, TEMPLATE, 1f, 2f, 3f, (byte) 0));
		assertEquals(2, next[0]);
		assertSame(handles[1], registry.get(snapshot(), "guardian"));
	}

	@Test
	void spawnIsBestEffortWhenPlayerLoggedOut() {
		QuestSpawnRegistry registry = new QuestSpawnRegistry();
		PlayerQuestSpawnPort port = new PlayerQuestSpawnPort(playerId -> null, registry,
			(worldId, instanceId, templateId, x, y, z, heading) -> npc(worldId));

		assertFalse(port.spawnNpc(snapshot(), plan(), "guardian", SPAWN_WORLD, TEMPLATE, 1f, 2f, 3f, (byte) 0));
		assertFalse(registry.contains(snapshot(), "guardian"));
	}

	@Test
	void instanceStrategyPassesThroughToSpawnCall() {
		QuestSpawnRegistry registry = new QuestSpawnRegistry();
		Player player = player();
		int[] captured = {0};
		PlayerQuestSpawnPort port = new PlayerQuestSpawnPort(playerId -> player, registry,
			(worldId, instanceId, templateId, x, y, z, heading) -> {
				captured[0] = instanceId;
				return npc(worldId);
			});

		port.spawnNpc(snapshot(), plan(), "guardian", SPAWN_WORLD, TEMPLATE, 1f, 2f, 3f, (byte) 0);
		assertEquals(1, captured[0]); // 跨世界 → 默认实例 1

		port.spawnNpc(snapshot(), plan(), "same-world", PLAYER_WORLD, TEMPLATE, 1f, 2f, 3f, (byte) 0);
		assertEquals(5, captured[0]); // 同世界 → 复用玩家实例
	}

	@Test
	void playerPositionSpawnUsesTheEventSnapshot() {
		QuestSpawnRegistry registry = new QuestSpawnRegistry();
		Player player = player();
		player.getPosition().setXYZH(99f, 98f, 97f, (byte) 96);
		String[] captured = {null};
		PlayerQuestSpawnPort port = new PlayerQuestSpawnPort(playerId -> player, registry,
			(worldId, instanceId, templateId, x, y, z, heading) -> {
				captured[0] = worldId + ":" + instanceId + ":" + x + ":" + y + ":" + z + ":" + heading;
				return npc(worldId);
			});

		assertTrue(port.spawnNpc(snapshot(), plan(), "escort", TEMPLATE,
			new QuestSpawnLocation.PlayerPosition((byte) 8)));
		assertEquals(PLAYER_WORLD + ":5:10.0:20.0:30.0:8", captured[0]);
	}

	@Test
	void randomSpawnSelectsVariantAndCanReplaceTheExistingSlot() {
		QuestSpawnRegistry registry = new QuestSpawnRegistry();
		Player player = player();
		Npc first = npc(SPAWN_WORLD);
		Npc second = npc(SPAWN_WORLD);
		Npc[] handles = {first, second};
		int[] next = {0};
		PlayerQuestSpawnPort port = new PlayerQuestSpawnPort(playerId -> player, registry,
			(worldId, instanceId, templateId, x, y, z, heading) -> handles[next[0]++], bound -> 1);
		List<QuestSpawnVariant> variants = List.of(
			new QuestSpawnVariant(213576, new QuestSpawnLocation.Fixed(SPAWN_WORLD,
				com.aionemu.gameserver.questEngine.definition.QuestInstanceTarget.currentOrDefault(),
				1f, 2f, 3f, (byte) 95)),
			new QuestSpawnVariant(213577, new QuestSpawnLocation.Fixed(SPAWN_WORLD,
				com.aionemu.gameserver.questEngine.definition.QuestInstanceTarget.currentOrDefault(),
				4f, 5f, 6f, (byte) 95)));

		assertTrue(port.spawnNpcRandom(snapshot(), plan(), "defense-mob", variants, false));
		assertSame(first, registry.get(snapshot(), "defense-mob"));
		assertTrue(port.spawnNpcRandom(snapshot(), plan(), "defense-mob", variants, true));
		assertSame(second, registry.get(snapshot(), "defense-mob"));
		assertEquals(2, next[0]);
	}

	@Test
	void despawnWithoutHandleIsIdempotentSuccess() {
		QuestSpawnRegistry registry = new QuestSpawnRegistry();
		PlayerQuestSpawnPort port = new PlayerQuestSpawnPort(playerId -> player(), registry,
			(worldId, instanceId, templateId, x, y, z, heading) -> npc(worldId));

		assertTrue(port.despawnNpc(snapshot(), plan(), "never-spawned"));
	}

	@Test
	void portFailsClosedOnInvalidArguments() {
		PlayerQuestSpawnPort port = new PlayerQuestSpawnPort(playerId -> player(), new QuestSpawnRegistry(),
			(worldId, instanceId, templateId, x, y, z, heading) -> npc(worldId));

		assertThrows(IllegalArgumentException.class,
			() -> port.spawnNpc(snapshot(), plan(), " ", SPAWN_WORLD, TEMPLATE, 1f, 2f, 3f, (byte) 0));
		assertThrows(IllegalArgumentException.class,
			() -> port.spawnNpc(snapshot(), plan(), "guardian", 0, TEMPLATE, 1f, 2f, 3f, (byte) 0));
		assertThrows(IllegalArgumentException.class,
			() -> port.spawnNpc(snapshot(), plan(), "guardian", SPAWN_WORLD, 0, 1f, 2f, 3f, (byte) 0));
		assertThrows(IllegalArgumentException.class,
			() -> port.despawnNpc(snapshot(), plan(), ""));
	}

	private static QuestSnapshot snapshot() {
		return new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 0, Map.of(), Map.of(),
			true, true, 0, 0, PLAYER_WORLD, 5, 10f, 20f, 30f, (byte) 0);
	}

	private static QuestMutationPlan plan() {
		return new QuestMutationPlan(QUEST_ID, QuestStatus.COMPLETE, 0, List.of(), List.of());
	}

	private static Player player() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		player.setPosition(new WorldPosition(PLAYER_WORLD));
		return player;
	}

	/**
	 * 构造“已离开世界”的任务 NPC 替身:生命属性存在且未死亡,但位置未生成。
	 * Builds a quest-NPC double that already left the world: life stats exist and are alive,
	 * yet the position is not spawned.
	 */
	private static Npc destroyedNpc(int worldId) {
		Npc npc = npc(worldId);
		npc.setLifeStats(new ObjenesisStd().newInstance(NpcLifeStats.class));
		return npc;
	}

	private static Npc npc(int worldId) {
		Npc npc = new ObjenesisStd().newInstance(Npc.class);
		npc.setPosition(new WorldPosition(worldId));
		return npc;
	}
}
