package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 锁定任务 2841 的正式怪物狩猎 NPC、计数、报告状态和客户端响应合同。
 * Locks quest 2841 to the retail monster-hunt NPC, counter, report-state, and client-response contract.
 */
class Quest2841RetailAlignmentTest {
	private static final Path XML = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests/2841.xml");
	private static final Set<Integer> TARGET_NPCS = Set.of(
		214752, 214753, 214754, 214755, 214756, 214757, 214758, 214759, 214760,
		214761, 214762, 214763, 214764, 214765, 214766, 214767, 214768, 214769,
		214770, 215439, 215440, 215441, 215442, 215443, 215444);

	@Test
	void usesRetailStartAndReportNpcsWithASeparateStartedCompletionNode() throws Exception {
		QuestDefinition definition = definition();

		assertEquals(QuestStatus.START, node(definition, "hunting-complete").projection().status());
		assertEquals(44, node(definition, "hunting-complete").projection().variables().get("var0"));
		assertEquals(44, node(definition, "reward").projection().variables().get("var0"));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1011)),
			route(definition, "unaccepted", "unaccepted", 805433, 31).afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(4)),
			route(definition, "unaccepted", "unaccepted", 805433, 1007).afterCommit());

		QuestTransition accept = route(definition, "unaccepted", "hunting", 805433, 1002);
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(1003)), accept.afterCommit());

		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1352)),
			route(definition, "hunting-complete", "hunting-complete", 278003, 31).afterCommit());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestDialog(5)),
			route(definition, "hunting-complete", "reward", 278003, 1009).afterCommit());
		assertEquals(16, definition.transitions().stream()
			.filter(transition -> "reward".equals(transition.sourceNode())
				&& "complete".equals(transition.targetNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 278003 && talk.dialogId() >= 8 && talk.dialogId() <= 23)
			.count());
		assertFalse(definition.transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == 271068));
	}

	@Test
	void fortyFourthKillKeepsStartUntilRetailReportAction() throws Exception {
		QuestDefinition definition = definition();
		List<QuestTransition> counters = definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpcSet kill
				&& kill.npcIds().equals(TARGET_NPCS))
			.toList();
		assertEquals(2, counters.size());

		QuestTransition continuing = counters.stream()
			.filter(transition -> "hunting".equals(transition.targetNode()))
			.findFirst().orElseThrow();
		assertEquals(Integer.valueOf(1), continuing.priority());
		assertEquals(List.of(new QuestCondition.VariableBelow("var0", 43)), continuing.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var0", 1)), continuing.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			continuing.afterCommit());

		QuestTransition completing = counters.stream()
			.filter(transition -> "hunting-complete".equals(transition.targetNode()))
			.findFirst().orElseThrow();
		assertEquals(Integer.valueOf(0), completing.priority());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 43)), completing.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var0", 1)), completing.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			completing.afterCommit());
		assertFalse(counters.stream().flatMap(transition -> transition.conditions().stream())
			.anyMatch(QuestCondition.WorldIs.class::isInstance));
	}

	private static QuestDefinition definition() throws Exception {
		try (InputStream input = Files.newInputStream(XML)) {
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
	}

	private static QuestTransition route(QuestDefinition definition, String source, String target,
			int npcId, int dialogId) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals(source)
				&& transition.targetNode().equals(target))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && talk.dialogId() == dialogId)
			.toList();
		assertEquals(1, routes.size());
		return routes.getFirst();
	}
}
