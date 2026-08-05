package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.world.WorldPosition;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies that resident escort follow resolves the live target NPC in the current world. */
class PlayerQuestWorldNpcFollowTest {
	private static final int PLAYER_ID = 7;
	private static final int QUEST_ID = 14042;
	private static final int WORLD = 400010000;

	@Test
	void resolvesTargetNpcAndSchedulesTargetBasedWatcher() {
		Npc escort = npc(253623);
		Npc target = npc(253635);
		Player player = player();
		List<String> calls = new ArrayList<>();
		CompletableFuture<Void> watcher = new CompletableFuture<>();

		PlayerQuestAiPort port = new PlayerQuestAiPort(playerId -> player, new QuestSpawnRegistry(),
			(npc, owner, resolvedTarget, command, argument) -> {
				calls.add(command.name());
				return true;
			}, objectId -> objectId == escort.getObjectId() ? escort : null,
			(p, npc, questId, zone) -> CompletableFuture.completedFuture(null),
			(p, npc, questId, x, y, z) -> CompletableFuture.completedFuture(null),
			(p, task) -> calls.add("REGISTER"),
			(p, npc) -> calls.add("NPC_INFO"),
			(p, templateId) -> templateId == 253635 ? target : null,
			(p, npc, resolvedTarget, questId) -> {
				assertSame(target, resolvedTarget);
				calls.add("TARGET:" + questId);
				return watcher;
			});

		QuestSnapshot snapshot = new QuestSnapshot(PLAYER_ID, QUEST_ID, QuestStatus.START, 0,
			Map.of(), Map.of()).withInteractionObjectId(escort.getObjectId());
		assertTrue(port.startFollowCurrentTargetToNpc(snapshot, plan(), 253635));
		assertEquals(List.of("NPC_INFO", "START_FOLLOW", "TARGET:14042", "REGISTER"), calls);
		assertTrue(!watcher.isCancelled());
	}

	private static QuestMutationPlan plan() {
		return new QuestMutationPlan(QUEST_ID, QuestStatus.START, 0, List.of(), List.of());
	}

	private static Player player() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		player.setPosition(new WorldPosition(WORLD));
		return player;
	}

	private static Npc npc(int objectIdValue) {
		Npc npc = new ObjenesisStd().newInstance(Npc.class);
		try {
			java.lang.reflect.Field objectId = com.aionemu.gameserver.model.gameobjects.AionObject.class
				.getDeclaredField("objectId");
			objectId.setAccessible(true);
			objectId.set(npc, Integer.valueOf(objectIdValue));
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
		npc.setPosition(new WorldPosition(WORLD));
		return npc;
	}
}
