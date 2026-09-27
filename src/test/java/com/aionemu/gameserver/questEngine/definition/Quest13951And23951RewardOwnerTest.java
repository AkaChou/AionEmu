package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestProductionJourneyExecutor;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestProductionJourneyPlanner;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 锁定两条投递信件任务的唯一接取、领奖归属和原生奖励协议。
 * Locks unique acceptance, reward ownership, and native reward protocol for two letter-delivery quests.
 */
class Quest13951And23951RewardOwnerTest {
	@Test
	void keepsTheStartNpcAndRewardNpcAsSeparateOwners() throws Exception {
		assertContract(13951, 731784, 806582, 182216201);
		assertContract(23951, 731784, 806591, 182216202);
	}

	@Test
	void productionJourneysReachTheRetailRewardOwners() throws Exception {
		ClientResourceOracle oracle = ClientResourceOracle.load(Path.of("docs/quest/client-dialog-mapping"));
		for (int questId : List.of(13951, 23951)) {
			CompiledQuestDefinition definition = definition(questId);
			QuestProductionJourneyPlanner.Result planned = new QuestProductionJourneyPlanner().plan(definition, oracle);
			assertTrue(planned.planned(), () -> questId + " " + planned.failure());
			assertTrue(planned.plan().steps().stream().anyMatch(step ->
				step.kind() == QuestProductionJourneyPlanner.StepKind.USE_OBJECT));
			assertTrue(planned.plan().steps().stream().anyMatch(step ->
				step.kind() == QuestProductionJourneyPlanner.StepKind.NATIVE_REWARD_ACTION));

			QuestProductionJourneyExecutor.Result executed = new QuestProductionJourneyExecutor()
				.execute(definition, oracle, planned.plan());
			assertTrue(executed.completed(), () -> questId + " " + executed.failure());
		}
	}

	private static void assertContract(int questId, int startNpc, int rewardNpc, int itemId) throws Exception {
		QuestDefinition definition = definition(questId).definition();
		assertTrue(routes(definition, "unaccepted", rewardNpc).isEmpty());
		assertTrue(routes(definition, "reward", startNpc).isEmpty());

		QuestTransition accept = talk(definition, "unaccepted", "started", startNpc, QuestDialogAction.QUEST_ACCEPT_SIMPLE);
		assertEquals(List.of(new QuestAction.GiveItem(itemId, 1)), accept.actions());

		// DD 形：使用道具演出推进（ItemPlay 边，时长取遗留证据标准 3000ms；无条件行推进，
		// PACKET_ONLY 同步由领奖行投影与自愈边收口）。
		// DD shape: the item play advances (an ItemPlay edge at the legacy-evidence standard 3000ms)
		// — unconditional row advance with PACKET_ONLY sync, closed out by the reward-row projection
		// and the heal edges.
		QuestTransition itemUse = transition(definition, "started", "reward",
			new QuestEvent.ItemPlay(itemId, 3000));
		assertEquals(List.of(), itemUse.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), itemUse.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.PACKET_ONLY)), itemUse.afterCommit());

		// DD 交付词汇：USE_OBJECT 续接入口直接开领奖窗（显示 ok 页=死端，handin 规则）。
		// DD hand-in vocabulary: the USE_OBJECT entry opens the native reward window directly
		// (rendering the ok page would be a dead end, per the handin rule).
		QuestTransition useObject = talk(definition, "reward", "reward", rewardNpc, QuestDialogAction.USE_OBJECT);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), useObject.afterCommit());
		QuestTransition report = talk(definition, "reward", "reward", rewardNpc, QuestDialogAction.SELECT_QUEST_REWARD);
		assertTrue(report.actions().isEmpty());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), report.afterCommit());

		QuestTransition completion = talk(definition, "reward", "complete", rewardNpc,
			QuestDialogAction.SELECTED_QUEST_REWARD1);
		assertEquals(List.of(
			new QuestAction.GrantReward("GOLD", 0, 405720, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 35561094, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.CompleteQuest(0)), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(10)), completion.afterCommit());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int npcId,
			QuestDialogAction action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(npcId, action.id()));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> event.equals(candidate.event()))
			.toList();
		assertEquals(1, routes.size(), source + " -> " + target + " " + event);
		return routes.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId)
			.toList();
	}

	// 退役任务统一走生产视图（真端 overlay 合成；旧 XML 只在 git 历史里）。
	// Retired quests resolve through the production view (retail overlay; the old XML lives in
	// git history only).
	private static CompiledQuestDefinition definition(int questId) throws Exception {
		return ProductionQuestDefinitions.definition(questId);
	}
}
