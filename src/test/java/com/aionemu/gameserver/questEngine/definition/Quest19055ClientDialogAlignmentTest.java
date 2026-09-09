package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 去除不可达的 999 级任务 19054 前置后锁定任务 19055 的接受合同。
 * Locks quest 19055's acceptance contract without the unreachable level-999 quest 19054 prerequisite.
 */
class Quest19055ClientDialogAlignmentTest {
	private static final int START_NPC = 798450;

	@Test
	void acceptsWithoutTheUnavailableLevel999Prerequisite() throws Exception {
		QuestDefinition definition = load().definition();

		assertEquals(List.of(), definition.metadata().startConditions());

		QuestTransition accept = route(definition, QuestDialogAction.QUEST_ACCEPT_1);
		assertEquals("unaccepted", accept.sourceNode());
		assertEquals("started", accept.targetNode());
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())), accept.afterCommit());
	}

	private static QuestTransition route(QuestDefinition definition, QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals("unaccepted"))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == START_NPC && Integer.valueOf(action.id()).equals(talk.dialogId()))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load() throws Exception {
		try (InputStream input = Quest19055ClientDialogAlignmentTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/19055.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 19055.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
