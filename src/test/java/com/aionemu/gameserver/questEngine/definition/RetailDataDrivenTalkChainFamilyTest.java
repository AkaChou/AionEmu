package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the retail data-driven five-step talk chains for quests 1888 and 2888.
 */
class RetailDataDrivenTalkChainFamilyTest {

	private static final List<Case> CASES = List.of(
		new Case(1888, 278651, 278591, 278578, 278592),
		new Case(2888, 278151, 278086, 278085, 278087));

	@Test
	void retailTalkChainsMatchTheLegacyDataDrivenContracts() throws Exception {
		for (Case contract : CASES) {
			QuestDefinition definition = definition(contract.questId());
			for (int step = 0; step <= 4; step++) {
				assertNode(definition, "step" + step, QuestStatus.START, step);
			}
			assertNode(definition, "reward", QuestStatus.REWARD, 5);
			assertNode(definition, "complete", QuestStatus.COMPLETE, 0);

			assertPage(definition, "unaccepted", contract.startNpc(),
				QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT_NONE);
			assertPage(definition, "unaccepted", contract.startNpc(),
				QuestDialogAction.SELECT_NONE_1, QuestDialogPage.SELECT_NONE_1);
			assertTalk(definition, "unaccepted", "step0", contract.startNpc(),
				QuestDialogAction.QUEST_ACCEPT_SIMPLE,
				List.of(new QuestCondition.StartEligible()), List.of(),
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
					new AfterCommitAction.CloseDialog()));

			assertTalk(definition, "step0", "step0", contract.step0Npc(),
				QuestDialogAction.QUEST_SELECT, List.of(), List.of(),
				List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1.id())));
			assertTalk(definition, "step0", "step1", contract.step0Npc(),
				QuestDialogAction.SETPRO1, List.of(), List.of(),
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
					new AfterCommitAction.CloseDialog()));

			assertTalk(definition, "step1", "step1", contract.startNpc(),
				QuestDialogAction.QUEST_SELECT, List.of(), List.of(),
				List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())));
			assertTalk(definition, "step1", "step2", contract.startNpc(),
				QuestDialogAction.SETPRO2, List.of(), List.of(),
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
					new AfterCommitAction.CloseDialog()));

			assertTalk(definition, "step2", "step2", contract.step2Npc(),
				QuestDialogAction.QUEST_SELECT, List.of(), List.of(),
				List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3.id())));
			assertTalk(definition, "step2", "step2", contract.step2Npc(),
				QuestDialogAction.SELECT3_1, List.of(), List.of(),
				List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3_1.id())));
			assertTalk(definition, "step2", "step3", contract.step2Npc(),
				QuestDialogAction.SETPRO3, List.of(), List.of(),
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
					new AfterCommitAction.CloseDialog()));

			assertTalk(definition, "step3", "step3", contract.startNpc(),
				QuestDialogAction.QUEST_SELECT, List.of(), List.of(),
				List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT4.id())));
			assertTalk(definition, "step3", "step4", contract.startNpc(),
				QuestDialogAction.SETPRO4, List.of(), List.of(),
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
					new AfterCommitAction.CloseDialog()));

			assertTalk(definition, "step4", "step4", contract.step4Npc(),
				QuestDialogAction.QUEST_SELECT, List.of(), List.of(),
				List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5.id())));
			assertTalk(definition, "step4", "reward", contract.step4Npc(),
				QuestDialogAction.SET_SUCCEED, List.of(), List.of(),
				List.of(new AfterCommitAction.SyncQuestState(
						QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.CloseDialog()));

			assertTalk(definition, "reward", "reward", contract.startNpc(),
				QuestDialogAction.QUEST_SELECT, List.of(), List.of(),
				List.of(new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.DEFAULT_SUCCESS.id())));
			assertTalk(definition, "reward", "reward", contract.startNpc(),
				QuestDialogAction.SELECT_QUEST_REWARD, List.of(), List.of(),
				List.of(new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())));
			assertCompletion(definition, contract.startNpc());
		}
	}

	private static void assertCompletion(QuestDefinition definition, int startNpc) {
		QuestTransition completion = talk(definition, "reward", startNpc,
			QuestDialogAction.SELECTED_QUEST_REWARD1, List.of());
		assertEquals("complete", completion.targetNode());
		assertTrue(completion.actions().stream()
			.anyMatch(QuestAction.GrantReward.class::isInstance));
		assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(0)));
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		assertTalk(definition, source, source, npcId, action, List.of(), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(page.id())));
	}

	private static void assertTalk(QuestDefinition definition, String source, String target,
			int npcId, QuestDialogAction action, List<QuestCondition> conditions,
			List<QuestAction> actions, List<AfterCommitAction> afterCommit) {
		QuestTransition transition = talk(definition, source, npcId, action, conditions);
		assertEquals(target, transition.targetNode());
		assertEquals(conditions, transition.conditions());
		assertEquals(actions, transition.actions());
		assertEquals(afterCommit, transition.afterCommit());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, List<QuestCondition> conditions) {
		QuestEvent.TalkToNpc event = new QuestEvent.TalkToNpc(npcId, action.id());
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source))
			.filter(candidate -> candidate.event().equals(event))
			.filter(candidate -> candidate.conditions().equals(conditions))
			.findFirst()
			.orElseThrow(() -> new AssertionError(
				"missing route " + source + " + NPC " + npcId + " + action " + action.id()));
	}

	private static void assertNode(QuestDefinition definition, String label,
			QuestStatus status, int var0) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst()
			.orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(Map.of("var0", var0), node.projection().variables());
	}

	private static QuestDefinition definition(int questId) throws Exception {
		try (InputStream input = RetailDataDrivenTalkChainFamilyTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}

	private record Case(int questId, int startNpc, int step0Npc, int step2Npc, int step4Npc) {
	}
}
