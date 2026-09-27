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
 * 锁定任务 25670 的四物品报告、确认页和唯一奖励归属（真端驱动 talk+collectitem 混合链形）。
 * Locks quest 25670's four-item report, confirmation pages, and unique reward owner (the
 * retail-driven talk+collectitem chain shape).
 */
class Quest25670ClientDialogAlignmentTest {
	private static final int START_NPC = 806116;
	private static final int REPORT_NPC = 806105;
	private static final int CONFIRM_OBJECT = 731794;
	private static final List<Integer> ITEMS = List.of(182216194, 182216195, 182216196, 182216197);

	@Test
	void followsTheRetailReportAndRewardOwnerChain() throws Exception {
		QuestDefinition definition = definition().definition();
		assertEquals(4, definition.metadata().drops().size());
		assertTrue(definition.metadata().drops().stream().allMatch(drop ->
			ITEMS.contains(drop.itemId()) && drop.chance() == 100 && drop.collectingStep() == 1));

		// 阶梯推进：SETPRO1 s0 -> s1（段变量由目标投影承担）。 / Ladder advance: SETPRO1 s0 -> s1
		// (the stage variable rides the target projection).
		QuestTransition handoff = route(definition, "started", "s1", REPORT_NPC, QuestDialogAction.SETPRO1);
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), handoff.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			handoff.afterCommit());

		// 采集段：39 检查整组过/扣 s1 -> s2。 / Collect stage: the group check advances s1 -> s2.
		QuestTransition report = route(definition, "s1", "s2", REPORT_NPC,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM);
		assertEquals(ITEMS.stream().map(item -> new QuestAction.RemoveItem(item, 1)).toList(), report.actions());
		assertEquals(ITEMS.stream().map(item -> new QuestCondition.HasItem(item, 1)).toList(), report.conditions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())), report.afterCommit());

		// 收尾调查：末段 SET_SUCCEED 挂在第五处痕迹物体上，s2 -> reward。
		// The closing survey: the final stage's SET_SUCCEED rides the fifth-track object into reward.
		QuestTransition handover = route(definition, "s2", "reward", CONFIRM_OBJECT,
			QuestDialogAction.SET_SUCCEED);
		assertEquals(List.of(), handover.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), handover.afterCommit());

		assertTrue(routes(definition, "unaccepted", REPORT_NPC).isEmpty());
		assertTrue(routes(definition, "reward", REPORT_NPC).isEmpty());
		QuestTransition preview = route(definition, "reward", "reward", START_NPC, QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());
		QuestTransition completion = route(definition, "reward", "complete", START_NPC,
			QuestDialogAction.SELECTED_QUEST_REWARD1);
		assertEquals(List.of(
			new QuestAction.GrantReward("GOLD", 0, 164700, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 15469350, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.CompleteQuest(0)), completion.actions());
	}

	@Test
	void productionJourneyCompletesFromTheProductionOwner() throws Exception {
		CompiledQuestDefinition definition = definition();
		ClientResourceOracle oracle = ClientResourceOracle.load(Path.of("docs/quest/client-dialog-mapping"));
		QuestProductionJourneyPlanner.Result planned = new QuestProductionJourneyPlanner().plan(definition, oracle);
		assertTrue(planned.planned(), () -> String.valueOf(planned.failure()));
		QuestProductionJourneyExecutor.Result executed = new QuestProductionJourneyExecutor()
			.execute(definition, oracle, planned.plan());
		assertTrue(executed.completed(), () -> String.valueOf(executed.failure()));
	}

	private static QuestTransition route(QuestDefinition definition, String source, String target, int npcId,
			QuestDialogAction action) {
		List<QuestTransition> routes = routes(definition, source, npcId).stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& Integer.valueOf(action.id()).equals(talk.dialogId()))
			.filter(transition -> target.equals(transition.targetNode()))
			.toList();
		assertEquals(1, routes.size(), source + " -> " + target + " " + npcId + " " + action);
		return routes.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId)
			.toList();
	}

	private static CompiledQuestDefinition definition() throws Exception {
		// 退役后只有生产视图：真端模板表 + quest.xml 元数据合成（历史 XML 在 git 里）。
		// After retirement only the production view remains: synthesized from the retail table
		// plus quest.xml metadata (the historical XML lives in git).
		return ProductionQuestDefinitions.definition(25670);
	}
}
