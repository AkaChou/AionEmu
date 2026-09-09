package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 锁定击杀完成后仍处于 START 中间节点的任务报告对话合同。
 * Locks the report-dialog contract for quests that remain in a START intermediate node after kills.
 */
class Quest11110And1548PostKillReportDialogTest {
	@Test
	void quest11110KeepsTheReportEntryAndRewardRoutesAfterTheFinalKill() throws Exception {
		CompiledQuestDefinition compiled = load(11110);
		QuestDefinition definition = compiled.definition();
		assertNode(definition, "k1", QuestStatus.START, Map.of("var0", 1));

		for (int npcId : List.of(217039, 217040)) {
			QuestTransition finalKill = transition(definition, "started", "k1",
				new QuestEvent.KillNpc(npcId), 0);
			assertEquals(List.of(new QuestCondition.VariableAtLeast("var1", 9)), finalKill.conditions());
			assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), finalKill.actions());
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				finalKill.afterCommit());
		}

		assertPostKillReportContract(compiled, definition, "k1", 799075, Map.of("var0", 1, "var1", 9));
	}

	@Test
	void quest1548KeepsTheReportEntryAndRewardRoutesAfterTheFinalKillChainStep() throws Exception {
		CompiledQuestDefinition compiled = load(1548);
		QuestDefinition definition = compiled.definition();
		assertNode(definition, "k5", QuestStatus.START, Map.of("var0", 5));
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			transition(definition, "k4", "k5", new QuestEvent.KillNpc(700209)).afterCommit());

		assertPostKillReportContract(compiled, definition, "k5", 204583, Map.of("var0", 5));
	}

	private static void assertPostKillReportContract(CompiledQuestDefinition compiled,
		QuestDefinition definition, String source, int npcId, Map<String, Integer> variables) {
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
		Path path = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/" + questId + ".xml");
		try (InputStream input = Files.newInputStream(path)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
