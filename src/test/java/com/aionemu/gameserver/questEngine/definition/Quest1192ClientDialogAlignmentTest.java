package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 验证任务 1192 的接取页链不会把不存在的 1012 页面回显给客户端。
 * Verifies that quest 1192 uses its client acceptance action instead of echoing the missing page 1012.
 */
class Quest1192ClientDialogAlignmentTest {
	private static final int START_NPC_ID = 203098;
	private static final int START_ITEM_ID = 182200556;

	@Test
	void acceptDialogChainUsesTheClientAcceptanceAction() throws Exception {
		QuestDefinition definition = definition().definition();
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));

		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id())),
			route(definition, "unaccepted", "unaccepted", QuestDialogAction.QUEST_SELECT.id()).afterCommit());

		assertFalse(definition.transitions().stream()
			.anyMatch(transition -> transition.event().equals(new QuestEvent.TalkToNpc(START_NPC_ID,
				QuestDialogAction.SELECT1_1.id()))));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())),
			route(definition, "unaccepted", "unaccepted", QuestDialogAction.ASK_QUEST_ACCEPT.id()).afterCommit());

		QuestTransition accept = route(definition, "unaccepted", "started", QuestDialogAction.QUEST_ACCEPT_1.id());
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(new QuestAction.GiveItem(START_ITEM_ID, 1)), accept.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())), accept.afterCommit());
	}

	private static QuestTransition route(QuestDefinition definition, String source, String target, int action) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode())
				&& target.equals(transition.targetNode())
				&& transition.event().equals(new QuestEvent.TalkToNpc(START_NPC_ID, action)))
			.findFirst().orElseThrow();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = getClass().getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/1192.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 1192.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
