package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.e2e.client.ClientResourceOracle;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestProductionJourneyExecutor;
import com.aionemu.gameserver.questEngine.e2e.journey.QuestProductionJourneyPlanner;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 28931 的两次击杀、补给品清理和 Aion 5.8 客户端奖励链。
 * Verifies quest 28931's two-kill counter, supply cleanup, and Aion 5.8 client reward flow.
 */
class Quest28931ClientDialogAlignmentTest {
	private static final int CONVOY_OFFICER_ID = 243797;
	private static final int REWARD_NPC_ID = 806260;
	private static final int SUPPLY_ITEM_ID = 182213556;
	private static final int FOBJ_NPC_ID = 703344;

	@Test
	void restoresLegacyTwoKillAndSingleSupplyRemovalContract() {
		CompiledQuestDefinition compiled = definition();
		QuestDefinition definition = compiled.definition();
		// supply 182213556 = 本任务 work item（真端 quest.xml `quest_work_item1 quest_28931a 1`）。
		// The supply 182213556 is this quest's work item (retail quest.xml declares
		// `quest_work_item1 quest_28931a 1`).
		assertEquals(List.of(new QuestItemRequirement(SUPPLY_ITEM_ID, 1)),
			definition.metadata().questWorkItems());
		// 真端形（P0c-49 采纳）：FOBJ 物体交互步占行不占对话段 ⇒ started → s1；领奖投影携带末段
		// 满击杀数（QE-051；客户端任务书 3 行 = FOBJ 行 / 击杀行 / 报告行 ⇒ 末行 2）。
		// The retail shape (adopted in P0c-49): the TalkFOBJ interaction step occupies a journal row
		// without a dialog stage, so started -> s1; the reward projection carries the final block's
		// full kill count (QE-051; three client journal rows give the last row 2).
		assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 2, "var1", 2));

		// FOBJ 步：装备补给箱（703344）的 USE_OBJECT 交互推进到 s1；凭证由真端 work item 接取即发
		// （planner 负责完成/放弃回收，链定义不再显式发/扣物）。
		// The fobj step: the supply crate's (703344) USE_OBJECT interaction advances to s1; the
		// credential is granted on acceptance (the planner owns completion/abandon cleanup, so the
		// chain definition carries no explicit give/remove edge).
		QuestTransition fobjAdvance = transition(definition, "started", "s1",
			new QuestEvent.TalkToNpc(FOBJ_NPC_ID, QuestDialogAction.USE_OBJECT.id()));
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), fobjAdvance.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			fobjAdvance.afterCommit());

		QuestEvent kill = new QuestEvent.KillNpc(CONVOY_OFFICER_ID);
		QuestTransition firstKill = transition(definition, "s1", "s1", kill);
		assertEquals(Integer.valueOf(1), firstKill.priority());
		// 行门（var0=1）由源节点投影承载（引擎 matchesSourceNode 逐字段比对），条件只剩计数门。
		// The row gate (var0=1) lives in the source node's projection (the engine compares it field by
		// field in matchesSourceNode), so the condition list carries only the counter gate.
		assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
		assertEquals(List.of(new QuestCondition.VariableBelow("var1", 1)), firstKill.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var1", 1)), firstKill.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			firstKill.afterCommit());

		QuestTransition secondKill = transition(definition, "s1", "reward", kill);
		assertEquals(Integer.valueOf(0), secondKill.priority());
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var1", 1)), secondKill.conditions());
		// 末段击杀落领奖：行推进到 2，计数保留（领奖投影即满值）——不重置段计数。
		// The final block's kill lands on reward: the row advances to 2 and the counter is kept (the
		// reward projection is the full value) — no section reset.
		assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), secondKill.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			secondKill.afterCommit());

		for (int action : List.of(QuestDialogAction.USE_OBJECT.id(),
				QuestDialogAction.SELECT_QUEST_REWARD.id())) {
			QuestTransition preview = talk(definition, "reward", "reward", action);
			// 真端形不含显式扣物：supply 182213556 = 本任务 work item（quest_28931a），清理归 planner。
			// The retail shape carries no explicit removal: supply 182213556 is this quest's work item
			// (quest_28931a) and its cleanup belongs to the planner.
			assertEquals(List.of(), preview.actions());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());
		}

		QuestTransition completion = talk(definition, "reward", "complete",
			QuestDialogAction.SELECTED_QUEST_REWARD1.id());
		assertFalse(completion.actions().stream().anyMatch(QuestAction.RemoveItem.class::isInstance));
		assertTrue(completion.actions().stream().anyMatch(QuestAction.CompleteQuest.class::isInstance));
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());
	}

	@Test
	void completesThroughTheProductionHeadlessJourney() throws Exception {
		CompiledQuestDefinition definition = definition();
		ClientResourceOracle oracle = ClientResourceOracle.load(Path.of("docs/quest/client-dialog-mapping"));
		QuestProductionJourneyPlanner.Result planned = new QuestProductionJourneyPlanner().plan(definition, oracle);
		assertTrue(planned.planned(), () -> String.valueOf(planned.failure()));

		QuestProductionJourneyExecutor.Result executed = new QuestProductionJourneyExecutor()
			.execute(definition, oracle, planned.plan());
		assertTrue(executed.completed(), () -> String.valueOf(executed.failure()));
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(REWARD_NPC_ID, action));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source)
				&& candidate.targetNode().equals(target) && candidate.event().equals(event))
			.findFirst().orElseThrow();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	/**
	 * 生产视图加载（P0c-49 采纳后 28931 由真端表驱动，XML 只存 git 历史）。
	 * Loads through the production view (28931 became retail-driven in P0c-49; its XML lives only in
	 * git history).
	 */
	private CompiledQuestDefinition definition() {
		return ProductionQuestDefinitions.definition(28931);
	}
}
