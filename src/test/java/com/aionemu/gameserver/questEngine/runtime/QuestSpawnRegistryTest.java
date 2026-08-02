package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 领域注册表持有 quest spawn 的权威 handle;slot 幂等,despawn 只作用权威 handle,
 * 绝不凭 templateId 删任意同类,handle 不编码进 quest_vars。
 */
class QuestSpawnRegistryTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 1001;
	private static final int OTHER_QUEST = 2002;

	@Test
	void registerIsIdempotentPerSlot() {
		QuestSpawnRegistry registry = new QuestSpawnRegistry();
		Npc first = npc();
		Npc second = npc();

		assertTrue(registry.register(snapshot(QUEST_ID), "guardian", first));
		// 同一 slot 已存在 → 跳过,不重复刷怪。
		assertFalse(registry.register(snapshot(QUEST_ID), "guardian", second));
		assertSame(first, registry.remove(snapshot(QUEST_ID), "guardian"));
		assertNull(registry.remove(snapshot(QUEST_ID), "guardian"));
	}

	@Test
	void despawnOnlyTouchesTheAuthoritativeSlot() {
		QuestSpawnRegistry registry = new QuestSpawnRegistry();
		Npc guardian = npc();
		Npc escort = npc();
		assertTrue(registry.register(snapshot(QUEST_ID), "guardian", guardian));
		assertTrue(registry.register(snapshot(QUEST_ID), "escort", escort));

		// 删除 slot "guardian" 只作用 guardian handle;escort (即使可能同 templateId) 不受影响。
		assertSame(guardian, registry.remove(snapshot(QUEST_ID), "guardian"));
		assertNull(registry.remove(snapshot(QUEST_ID), "guardian"));
		assertTrue(registry.contains(snapshot(QUEST_ID), "escort"));
	}

	@Test
	void cleanupRemovesEverySlotOfTheQuest() {
		QuestSpawnRegistry registry = new QuestSpawnRegistry();
		Npc a = npc();
		Npc b = npc();
		registry.register(snapshot(QUEST_ID), "a", a);
		registry.register(snapshot(QUEST_ID), "b", b);

		List<Npc> removed = registry.cleanup(PLAYER_ID, QUEST_ID);

		assertEquals(List.of(a, b), removed);
		assertFalse(registry.contains(snapshot(QUEST_ID), "a"));
		assertFalse(registry.contains(snapshot(QUEST_ID), "b"));
	}

	@Test
	void registryIsIsolatedAcrossPlayersAndQuests() {
		QuestSpawnRegistry registry = new QuestSpawnRegistry();
		Npc questOne = npc();
		Npc questTwo = npc();
		assertTrue(registry.register(snapshot(QUEST_ID), "guardian", questOne));
		assertTrue(registry.register(snapshot(OTHER_QUEST), "guardian", questTwo));

		// 清理 quest 1001 不影响 quest 2002 的同名 slot。
		registry.cleanup(PLAYER_ID, QUEST_ID);
		assertTrue(registry.contains(snapshot(OTHER_QUEST), "guardian"));

		// 清理其他玩家的同任务不影响本玩家。
		registry.cleanup(999, OTHER_QUEST);
		assertTrue(registry.contains(snapshot(OTHER_QUEST), "guardian"));
		registry.cleanup(PLAYER_ID, OTHER_QUEST);
		assertFalse(registry.contains(snapshot(OTHER_QUEST), "guardian"));
	}

	@Test
	void registryFailsClosedOnInvalidArguments() {
		QuestSpawnRegistry registry = new QuestSpawnRegistry();
		assertThrows(IllegalArgumentException.class, () -> registry.register(snapshot(QUEST_ID), " ", npc()));
		assertThrows(IllegalArgumentException.class, () -> registry.remove(snapshot(QUEST_ID), ""));
		assertThrows(IllegalArgumentException.class, () -> registry.cleanup(0, QUEST_ID));
		assertThrows(IllegalArgumentException.class, () -> registry.cleanup(PLAYER_ID, 0));
	}

	@Test
	void cleanupAllCancelsWatchersAndRemovesEveryAuthoritativeSpawn() {
		QuestSpawnRegistry registry = new QuestSpawnRegistry();
		CompletableFuture<Void> watcher = new CompletableFuture<>();
		registry.register(snapshot(QUEST_ID), "escort", npc());
		registry.register(snapshot(OTHER_QUEST), "guardian", npc());
		assertTrue(registry.registerFollowTask(snapshot(QUEST_ID), "escort", watcher));

		assertEquals(2, registry.cleanupAll().size());
		assertTrue(watcher.isCancelled());
		assertFalse(registry.contains(snapshot(QUEST_ID), "escort"));
		assertFalse(registry.contains(snapshot(OTHER_QUEST), "guardian"));
	}

	@Test
	void followWatcherRequiresAnAuthoritativeSpawnSlot() {
		QuestSpawnRegistry registry = new QuestSpawnRegistry();
		CompletableFuture<Void> watcher = new CompletableFuture<>();

		assertFalse(registry.registerFollowTask(snapshot(QUEST_ID), "missing", watcher));
		assertTrue(watcher.isCancelled());
	}

	private static QuestSnapshot snapshot(int questId) {
		return new QuestSnapshot(PLAYER_ID, questId, QuestStatus.START, 0, Map.of(), Map.of());
	}

	private static Npc npc() {
		return new ObjenesisStd().newInstance(Npc.class);
	}
}
