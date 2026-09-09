package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Quest3926ClientDialogAlignmentTest {
	private static final int START_NPC = 203706;

	@Test
	void keepsTheRetailAcceptanceChainAndTreatsTitleAsAReward() throws Exception {
		QuestDefinition definition = load().definition();

		assertEquals(0, definition.metadata().titleId());
		assertEquals(List.of(new QuestReward("EXP", 0, 633457), new QuestReward("TITLE", 38, 1)),
			definition.metadata().rewards());

		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT_NONE.id())),
			route(definition, QuestDialogAction.QUEST_SELECT).afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())),
			route(definition, QuestDialogAction.ASK_QUEST_ACCEPT).afterCommit());

		QuestTransition accept = route(definition, QuestDialogAction.QUEST_ACCEPT_1);
		assertEquals("unaccepted", accept.sourceNode());
		assertEquals("started", accept.targetNode());
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())),
			accept.afterCommit());
	}

	private static QuestTransition route(QuestDefinition definition, QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals("unaccepted"))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == START_NPC && Integer.valueOf(action.id()).equals(talk.dialogId()))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load() throws Exception {
		try (InputStream input = Quest3926ClientDialogAlignmentTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/3926.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 3926.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
