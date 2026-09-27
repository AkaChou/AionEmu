package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定击杀完成后仍处于 START 中间节点的任务报告对话合同。
 * Locks the report-dialog contract for quests that remain in a START intermediate node after kills.
 */
class Quest11110And1548PostKillReportDialogTest {
	@Test
	void quest11110KeepsTheReportEntryAndRewardRoutesAfterTheFinalKill() throws Exception {
		/* P0c-8c（2026-09-24）：11110 已由真端 SimpleHunt 表驱动（10 段击杀网格，count1=10），旧 XML 的
		   k1（var0=1, var1=9）是同一语义的另一种表示；形状定位改用"START 饱和段"，不再写死标签。
		   Since P0c-8c the quest is retail-driven: the legacy k1 labelling is replaced by the grid's
		   saturated START step, located semantically rather than by label. */
		CompiledQuestDefinition compiled = load(11110);
		QuestDefinition definition = compiled.definition();
		List<QuestNode> steps = startNodesByPack(definition);
		QuestNode postKill = steps.getLast();
		QuestNode beforeFinalKill = steps.get(steps.size() - 2);
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 10)), postKill.projection());

		for (int npcId : List.of(217039, 217040)) {
			QuestTransition finalKill = transition(definition, beforeFinalKill.label(), postKill.label(),
				new QuestEvent.KillNpc(npcId));
			assertEquals(List.of(), finalKill.conditions());
			assertEquals(List.of(), finalKill.actions());
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				finalKill.afterCommit());
		}

		/* P0-2 规范形：11110 满段 QUEST_SELECT 直翻 REWARD（报告页与 1009 中转删除）。
		   Canonical since P0-2: the full node's QUEST_SELECT flips REWARD directly. */
		assertPostKillReportContract(compiled, definition, postKill.label(), 799075, Map.of("var0", 10), true);
	}

	@Test
	void quest1548KeepsTheReportEntryAndRewardRoutesAfterTheFinalKillChainStep() throws Exception {
		CompiledQuestDefinition compiled = load(1548);
		QuestDefinition definition = compiled.definition();
		List<QuestNode> steps = startNodesByPack(definition);
		QuestNode postKill = steps.getLast();
		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 5)), postKill.projection());
		QuestNode beforeFinalKill = steps.get(steps.size() - 2);
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			transition(definition, beforeFinalKill.label(), postKill.label(),
				new QuestEvent.KillNpc(700209)).afterCommit());

		assertPostKillReportContract(compiled, definition, postKill.label(), 204583, Map.of("var0", 5), false);
	}

	/** START 节点按打包值升序：末位即"饱和段"（真端网格与旧 XML 阶梯通用）。 */
	private static List<QuestNode> startNodesByPack(QuestDefinition definition) {
		return definition.nodes().stream()
			.filter(node -> node.projection().status() == QuestStatus.START)
			.sorted(Comparator.comparingInt(node ->
				definition.progressLayout().pack(node.projection().variables())))
			.toList();
	}

	/**
	 * 击杀饱和后的报告合同。canonical=true（P0-2 规范形：QUEST_SELECT 直翻 REWARD，1009 删除）；
	 * canonical=false（1548 仍为 XML 保留行）：QUEST_SELECT 只开报告页、1009 中转进领奖。
	 * The post-kill report contract. canonical=true (P0-2: QUEST_SELECT flips REWARD, no 1009);
	 * canonical=false (1548 is XML-retained): QUEST_SELECT opens the report page and the 1009 hop
	 * enters the reward state.
	 */
	private static void assertPostKillReportContract(CompiledQuestDefinition compiled,
		QuestDefinition definition, String source, int npcId, Map<String, Integer> variables,
		boolean canonical) {
		if (canonical) {
			QuestTransition deliver = transition(definition, source, "reward",
				new QuestEvent.TalkToNpc(npcId, QuestDialogAction.QUEST_SELECT.id()));
			assertEquals(List.of(), deliver.conditions());
			assertEquals(List.of(), deliver.actions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
				deliver.afterCommit());
			assertTrue(definition.transitions().stream().noneMatch(candidate ->
					source.equals(candidate.sourceNode())
					&& candidate.event().equals(new QuestEvent.TalkToNpc(npcId, QuestDialogAction.SELECT_QUEST_REWARD.id()))),
				"canonical removed the full-node 1009 hand-in / 规范形删除满段 1009 上交");

			int packed = definition.progressLayout().pack(variables);
			QuestSnapshot snapshot = new QuestSnapshot(7, compiled.id(), QuestStatus.START, packed, Map.of());
			QuestMutationPlan deliverPlan = QuestMutationPlanner.plan(compiled, snapshot,
				new QuestEvent.TalkToNpc(npcId, QuestDialogAction.QUEST_SELECT.id()), deliver).orElseThrow();
			assertEquals(QuestStatus.REWARD, deliverPlan.nextStatus());
			assertEquals(variables, definition.progressLayout().unpack(deliverPlan.nextPackedVariables()));
			return;
		}

		QuestTransition reportPage = transition(definition, source, source,
			new QuestEvent.TalkToNpc(npcId, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(), reportPage.conditions());
		assertEquals(List.of(), reportPage.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			reportPage.afterCommit());

		int packed = definition.progressLayout().pack(variables);
		QuestSnapshot snapshot = new QuestSnapshot(7, compiled.id(), QuestStatus.START, packed, Map.of());
		QuestMutationPlan reportPagePlan = QuestMutationPlanner.plan(compiled, snapshot,
			new QuestEvent.TalkToNpc(npcId, QuestDialogAction.QUEST_SELECT.id()), reportPage).orElseThrow();
		assertEquals(QuestStatus.START, reportPagePlan.nextStatus());
		assertEquals(variables, definition.progressLayout().unpack(reportPagePlan.nextPackedVariables()));

		QuestTransition report = transition(definition, source, "reward",
			new QuestEvent.TalkToNpc(npcId, QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(List.of(), report.conditions());
		assertEquals(List.of(), report.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());

		QuestMutationPlan reportPlan = QuestMutationPlanner.plan(compiled, snapshot,
			new QuestEvent.TalkToNpc(npcId, QuestDialogAction.SELECT_QUEST_REWARD.id()), report).orElseThrow();
		assertEquals(QuestStatus.REWARD, reportPlan.nextStatus());
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
		QuestEvent event) {
		return transition(definition, source, target, event, null);
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
		QuestEvent event, Integer priority) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode())
				&& target.equals(candidate.targetNode())
				&& candidate.event().equals(event)
				&& (priority == null || priority.equals(candidate.priority())))
			.findFirst().orElseThrow();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
		Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
		assertEquals(new NodeProjection(status, variables), node.projection());
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		// Retired quests live in git history only: use the production view (XML dir + retail overlay).
		return ProductionQuestDefinitions.definition(questId);
	}
}
