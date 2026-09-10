package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证任务 14114 的客户端接取介绍链能打开接受确认窗口。
 * Verifies that quest 14114's client acceptance introduction reaches the accept confirmation window.
 */
class Quest14114ClientDialogAlignmentTest {
	private static final int START_NPC_ID = 203098;
	private static final int PROGRESS_NPC_ID = 203183;
	private static final int CAPTAIN_NPC_ID = 210075;
	private static final int INSTRUCTION_ITEM_ID = 182215457;

	@Test
	void acceptDialogChainBridgesToTheAcceptWindowBeforeStartingTheQuest() throws Exception {
		QuestDefinition definition = definition().definition();
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));

		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT_NONE.id())),
			route(definition, "unaccepted", "unaccepted", QuestDialogAction.QUEST_SELECT).afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT_NONE_1.id())),
			route(definition, "unaccepted", "unaccepted", QuestDialogAction.SELECT_NONE_1).afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1_1.id())),
			route(definition, "started", "started", PROGRESS_NPC_ID, QuestDialogAction.SELECT1_1).afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2_1.id())),
			route(definition, "v2", "v2", PROGRESS_NPC_ID, QuestDialogAction.SELECT2_1).afterCommit());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())),
			route(definition, "v3", "v4", PROGRESS_NPC_ID,
				QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM).afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())),
			route(definition, "v4", "v4", PROGRESS_NPC_ID, QuestDialogAction.QUEST_SELECT).afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			route(definition, "reward", "reward", QuestDialogAction.SELECT_QUEST_REWARD).afterCommit());
		QuestTransition askForAccept = route(definition, "unaccepted", "unaccepted",
			QuestDialogAction.ASK_QUEST_ACCEPT);
		assertEquals(List.of(), askForAccept.conditions());
		assertEquals(List.of(), askForAccept.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())), askForAccept.afterCommit());

		QuestTransition accept = route(definition, "unaccepted", "started", QuestDialogAction.QUEST_ACCEPT_1);
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(), accept.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())), accept.afterCommit());
	}

	@Test
	void captainDropsTheInstructionItemForEachEligibleGroupMember() throws Exception {
		QuestMetadata metadata = definition().definition().metadata();

		assertEquals(List.of(new QuestDrop(CAPTAIN_NPC_ID, INSTRUCTION_ITEM_ID, 100, true, 0)),
			metadata.drops());
	}

	private static QuestTransition route(QuestDefinition definition, String source, String target,
		QuestDialogAction action) {
		return route(definition, source, target, START_NPC_ID, action);
	}

	private static QuestTransition route(QuestDefinition definition, String source, String target, int npcId,
		QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode())
				&& target.equals(transition.targetNode())
				&& transition.event().equals(new QuestEvent.TalkToNpc(npcId, action.id())))
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
				"/aion/data/static_data/quest_definition/quests/14114.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 14114.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
