package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerQuestBroadcastPortTest {
	@Test
	void delayedRefreshCoalescesPendingSourceAndUsesLivePlayerAtExecutionTime() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		List<Runnable> tasks = new ArrayList<>();
		List<Long> delays = new ArrayList<>();
		List<String> refreshes = new ArrayList<>();
		PlayerQuestBroadcastPort port = new PlayerQuestBroadcastPort(playerId -> player,
			(p, questIds) -> true,
			(p, questIds) -> {
				refreshes.add(java.util.Arrays.toString(questIds));
				return true;
			},
			(task, delayMillis) -> {
				tasks.add(task);
				delays.add(delayMillis);
			});
		QuestSnapshot snapshot = new QuestSnapshot(7, 80030, QuestStatus.NONE, 0, Map.of());
		QuestMutationPlan plan = new QuestMutationPlan(80030, QuestStatus.NONE, 0, List.of(), List.of());
		int[] targets = {80030, 80034, 80035, 80036};

		assertTrue(port.scheduleEventQuestRefresh(snapshot, plan, 10, targets));
		assertTrue(port.scheduleEventQuestRefresh(snapshot, plan, 10, targets));
		assertEquals(List.of(10_000L), delays);
		assertEquals(1, tasks.size());

		tasks.getFirst().run();
		assertEquals(List.of("[80030, 80034, 80035, 80036]"), refreshes);

		assertTrue(port.scheduleEventQuestRefresh(snapshot, plan, 10, targets));
		assertEquals(2, tasks.size());
	}

	@Test
	void delayedRefreshDoesNotCoalesceDifferentTargetListsWithTheSameArrayHash() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		List<Runnable> tasks = new ArrayList<>();
		PlayerQuestBroadcastPort port = new PlayerQuestBroadcastPort(playerId -> player,
			(p, questIds) -> true,
			(p, questIds) -> true,
			(task, delayMillis) -> tasks.add(task));
		QuestSnapshot snapshot = new QuestSnapshot(7, 80030, QuestStatus.NONE, 0, Map.of());
		QuestMutationPlan plan = new QuestMutationPlan(80030, QuestStatus.NONE, 0, List.of(), List.of());
		int[] firstTargets = {1, 0};
		int[] secondTargets = {0, 31};
		assertEquals(java.util.Arrays.hashCode(firstTargets), java.util.Arrays.hashCode(secondTargets));

		assertTrue(port.scheduleEventQuestRefresh(snapshot, plan, 10, firstTargets));
		assertTrue(port.scheduleEventQuestRefresh(snapshot, plan, 10, secondTargets));

		assertEquals(2, tasks.size());
	}
}
