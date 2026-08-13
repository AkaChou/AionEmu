package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest25512ClientDialogAlignmentTest {
	private static final int DIALOG_NPC = 806105;

	@Test
	void followsTheClientSimpleAcceptAndRewardPages() throws Exception {
		List<QuestTransition> transitions = definition().definition().transitions();

		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT_NONE.id())),
			talk(transitions, "unaccepted", DIALOG_NPC, QuestDialogAction.QUEST_SELECT.id()).afterCommit());
		assertEquals("started",
			talk(transitions, "unaccepted", DIALOG_NPC, QuestDialogAction.QUEST_ACCEPT_SIMPLE.id()).targetNode());
		assertEquals(List.of(new AfterCommitAction.CloseDialog()),
			talk(transitions, "unaccepted", DIALOG_NPC, QuestDialogAction.QUEST_ACCEPT_SIMPLE.id())
				.afterCommit().stream().filter(AfterCommitAction.CloseDialog.class::isInstance).toList());

		QuestTransition rewardPage = talk(transitions, "reward", DIALOG_NPC,
			QuestDialogAction.QUEST_SELECT.id());
		assertEquals("reward", rewardPage.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			rewardPage.afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			talk(transitions, "reward", DIALOG_NPC, QuestDialogAction.SELECT_QUEST_REWARD.id()).afterCommit());
	}

	@Test
	void retainsOnlyTheLegacyAuthoritativeDialogNpc() throws Exception {
		Set<Integer> dialogNpcs = definition().definition().transitions().stream()
			.map(QuestTransition::event)
			.filter(QuestEvent.TalkToNpc.class::isInstance)
			.map(QuestEvent.TalkToNpc.class::cast)
			.map(QuestEvent.TalkToNpc::npcId)
			.collect(java.util.stream.Collectors.toSet());

		assertEquals(Set.of(DIALOG_NPC), dialogNpcs);
		assertFalse(dialogNpcs.containsAll(Set.of(241547, 241548, 241550, 241551)));
	}

	@Test
	void killProgressStillEntersRewardBeforeTheReportDialog() throws Exception {
		List<QuestTransition> transitions = definition().definition().transitions();

		QuestTransition finalKill = transitions.stream()
			.filter(transition -> transition.sourceNode().equals("started")
				&& transition.targetNode().equals("reward")
				&& transition.event() instanceof QuestEvent.KillNpcSet)
			.findFirst().orElseThrow();
		assertTrue(finalKill.conditions().contains(new QuestCondition.VariableAtLeast("var1", 29)));
		assertTrue(finalKill.actions().contains(new QuestAction.SetVariable("var1", 30)));
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), finalKill.afterCommit());
	}

	private static QuestTransition talk(List<QuestTransition> transitions, String source, int npcId,
			int action) {
		return transitions.stream()
			.filter(transition -> transition.sourceNode().equals(source))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && Integer.valueOf(action).equals(talk.dialogId()))
			.findFirst().orElseThrow();
	}

	private CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = getClass().getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/25512.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 25512.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
