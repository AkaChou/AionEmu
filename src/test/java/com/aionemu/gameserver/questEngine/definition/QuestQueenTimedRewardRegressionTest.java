package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 锁定女王计时任务的现有可达档位、奖励窗口、交付及提交后顺序，不推导新的剧情分支。
 * Locks existing reachable queen timer tiers, reward windows, grants and after-commit order without inventing branches.
 */
class QuestQueenTimedRewardRegressionTest {

	@Test
	void elyosKeepsFourExistingTimerTiersAndTheirOwnRewards() throws Exception {
		assertQuest(11467, 186000018, List.of(6, 3, 2, 1));
	}

	@Test
	void asmodiansKeepThreeReachableTimerTiersInsteadOfAlwaysGrantingSixCoins() throws Exception {
		assertQuest(21467, 186000019, List.of(6, 3, 2));
	}

	/**
	 * 从工作区 XML 编译完整合同，避免读取共享 target 中的过期资源；覆盖每次超时、击杀和领取。
	 * Compiles the working-tree XML instead of stale shared target resources, covering every timeout, kill and claim.
	 */
	private static void assertQuest(int questId, int coinId, List<Integer> amounts) throws Exception {
		QuestDefinition definition = load(questId);
		boolean elyos = questId == 11467;
		String timerId = elyos ? "11467-queen" : "21467-timer";
		assertEquals(amounts.size(), definition.metadata().rewardGroups().size());
		assertEquals(amounts.size() * 2 + 2, definition.nodes().size());
		assertNode(definition, "unaccepted", QuestStatus.NONE, 0);
		assertNode(definition, "complete", QuestStatus.COMPLETE, 0);
		assertAcceptance(definition, timerId);

		for (int tier = 0; tier < amounts.size(); tier++) {
			String started = tier == 0 ? "started" : "s" + tier;
			String reward = (elyos ? "reward" : "r") + tier;
			assertNode(definition, started, QuestStatus.START, tier);
			assertNode(definition, reward, QuestStatus.REWARD, tier);
			assertEquals(List.of(
				new QuestReward("GOLD", 0, 25400),
				new QuestReward("EXP", 0, 3663347),
				new QuestReward("ITEM", coinId, amounts.get(tier))),
				definition.metadata().rewardGroups().get(tier).rewards());

			List<QuestCondition> conditions = elyos
				? List.of(new QuestCondition.QuestVariableIs("var0", tier)) : List.of();
			boolean lastTier = tier == amounts.size() - 1;
			List<QuestAction> timerActions = elyos && !lastTier
				? List.of(new QuestAction.SetVariable("var0", tier + 1)) : List.of();
			List<AfterCommitAction> timerEffects = lastTier
				? (elyos ? List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)) : List.of())
				: List.of(timer(240, timerId), new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY));
			assertRoute(definition, started, new QuestEvent.QuestTimerEnd(),
				lastTier ? started : "s" + (tier + 1), conditions, timerActions, timerEffects);
			assertRoute(definition, started, new QuestEvent.KillNpc(215480), reward, conditions, List.of(),
				List.of(new AfterCommitAction.CancelQuestTimer(timerIdentity(timerId)),
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)));

			assertReward(definition, reward, tier, coinId, amounts.get(tier));
			List<QuestAction> resetActions = elyos ? List.of() : List.of(
				new QuestAction.SetStatus(QuestStatus.NONE), new QuestAction.SetVariable("var0", 0));
			for (QuestEvent event : List.of(new QuestEvent.Die(), new QuestEvent.LogOut())) {
				assertRoute(definition, started, event, "unaccepted", List.of(), resetActions,
					List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)));
			}
		}

		for (Class<? extends QuestEvent> eventType : List.of(
			QuestEvent.QuestTimerEnd.class, QuestEvent.KillNpc.class, QuestEvent.Die.class, QuestEvent.LogOut.class)) {
			assertEquals((long) amounts.size(), definition.transitions().stream()
				.filter(transition -> eventType.isInstance(transition.event())).count(), eventType.getSimpleName());
		}
	}

	private static void assertAcceptance(QuestDefinition definition, String timerId) {
		assertRoute(definition, "unaccepted", talk(799527, QuestDialogAction.QUEST_ACCEPT_1), "started",
			List.of(new QuestCondition.StartEligible()), List.of(), List.of(
				timer(480, timerId),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())));
		assertRoute(definition, "unaccepted", talk(799527, QuestDialogAction.QUEST_ACCEPT_SIMPLE), "started",
			List.of(new QuestCondition.StartEligible()), List.of(), List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()));
		assertRoute(definition, "started", talk(799527, QuestDialogAction.FINISH_DIALOG), "started",
			List.of(), List.of(), List.of(
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())));
	}

	private static void assertReward(QuestDefinition definition, String source, int tier, int coinId, int amount) {
		assertRoute(definition, source, talk(799503, QuestDialogAction.USE_OBJECT), source,
			List.of(), List.of(), List.of(new AfterCommitAction.ShowQuestDialog(10002)));
		assertRoute(definition, source, talk(799503, QuestDialogAction.SELECT_QUEST_REWARD), source,
			List.of(), List.of(), List.of(new AfterCommitAction.ShowQuestDialog(List.of(5, 6, 7, 8).get(tier))));

		List<QuestAction> grants = List.of(
			new QuestAction.GrantReward("GOLD", 0, 25400, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 3663347, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", coinId, amount),
			new QuestAction.CompleteQuest(tier));
		List<AfterCommitAction> effects = List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(10));
		for (int action = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
			action <= QuestDialogAction.SELECTED_QUEST_NOREWARD.id(); action++) {
			assertRoute(definition, source, new QuestEvent.TalkToNpc(799503, action), "complete",
				List.of(), grants, effects);
		}
		assertEquals(18L, definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode())).count(), source + " reward routes");
	}

	private static QuestEvent.TalkToNpc talk(int npcId, QuestDialogAction action) {
		return new QuestEvent.TalkToNpc(npcId, action.id());
	}

	private static QuestTimerPolicy.Identity timerIdentity(String timerId) {
		return new QuestTimerPolicy.Identity(timerId, QuestTimerPolicy.Scope.PLAYER_QUEST);
	}

	private static AfterCommitAction.StartQuestTimer timer(int seconds, String timerId) {
		return new AfterCommitAction.StartQuestTimer(seconds, new QuestTimerPolicy(timerIdentity(timerId),
			QuestTimerPolicy.Persistence.SESSION, QuestTimerPolicy.OverwritePolicy.REPLACE,
			QuestTimerPolicy.Delivery.AT_MOST_ONCE));
	}

	private static void assertRoute(QuestDefinition definition, String source, QuestEvent event, String target,
		List<QuestCondition> conditions, List<QuestAction> actions, List<AfterCommitAction> afterCommit) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()) && event.equals(transition.event()))
			.toList();
		String context = source + " " + event;
		assertEquals(1, matches.size(), context);
		QuestTransition route = matches.getFirst();
		assertEquals(source, route.sourceNode(), context);
		assertEquals(event, route.event(), context);
		assertEquals(target, route.targetNode(), context);
		assertEquals(conditions, route.conditions(), context);
		assertEquals(actions, route.actions(), context);
		assertEquals(afterCommit, route.afterCommit(), context);
		assertNull(route.priority(), context);
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status, int tier) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label())).findFirst().orElseThrow();
		assertEquals(status, node.projection().status(), label);
		assertEquals(Map.of("var0", tier), node.projection().variables(), label);
	}

	private static QuestDefinition load(int questId) throws Exception {
		Path path = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/" + questId + ".xml");
		try (InputStream input = Files.newInputStream(path)) {
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
