package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest1152RetailAlignmentTest {
	private static final int START_NPC = 203132;
	private static final int DELIVERY_NPC = 203130;
	private static final int ODELLA = 182200526;
	private static final int PEPPER = 169400112;

	@Test
	void followsTheClientChefDialogAndLegacyTwoStepItemContract() throws Exception {
		QuestDefinition definition = compile();

		assertPage(definition, "unaccepted", START_NPC, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT1);
		QuestTransition accept = route(definition, "unaccepted", START_NPC, QuestDialogAction.QUEST_ACCEPT_1);
		assertEquals("started", accept.targetNode());
		assertTrue(accept.actions().contains(new QuestAction.GiveItem(ODELLA, 1)));
		assertTrue(routes(definition, "unaccepted", DELIVERY_NPC).isEmpty());

		assertPage(definition, "started", DELIVERY_NPC, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
		assertPage(definition, "started", DELIVERY_NPC, QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
		QuestTransition recipe = route(definition, "started", DELIVERY_NPC, QuestDialogAction.SETPRO1);
		assertEquals("pepper", recipe.targetNode());
		assertEquals(List.of(new QuestAction.RemoveItem(ODELLA, 1)), recipe.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), recipe.afterCommit());

		assertPage(definition, "pepper", DELIVERY_NPC, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT5);
		List<QuestTransition> checks = routes(definition, "pepper", DELIVERY_NPC,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM);
		assertEquals(2, checks.size());
		QuestTransition success = priority(checks, 0);
		QuestTransition failure = priority(checks, 1);
		assertEquals("reward", success.targetNode());
		assertEquals(List.of(new QuestCondition.HasItem(PEPPER, 1)), success.conditions());
		assertEquals(List.of(new QuestAction.RemoveItem(PEPPER, 1)), success.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH), new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), success.afterCommit());
		assertEquals("pepper", failure.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT6.id())),
			failure.afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			route(definition, "pepper", DELIVERY_NPC, QuestDialogAction.FINISH_DIALOG).afterCommit());

		QuestTransition completion = route(definition, "reward", DELIVERY_NPC,
			QuestDialogAction.SELECTED_QUEST_REWARD1);
		assertEquals("complete", completion.targetNode());
		assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(0)));
		assertTrue(routes(definition, "reward", START_NPC).isEmpty());
		for (QuestDialogAction action : List.of(QuestDialogAction.QUEST_SELECT, QuestDialogAction.SELECT2_1,
				QuestDialogAction.SETPRO1, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM)) {
			assertTrue(routes(definition, "started", START_NPC, action).isEmpty());
			assertTrue(routes(definition, "pepper", START_NPC, action).isEmpty());
		}
	}

	private static QuestDefinition compile() throws Exception {
		Path path = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/1152.xml");
		try (InputStream input = Files.newInputStream(path)) {
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())),
			route(definition, source, npcId, action).afterCommit());
	}

	private static QuestTransition priority(List<QuestTransition> transitions, int priority) {
		return transitions.stream().filter(transition -> Integer.valueOf(priority).equals(transition.priority()))
			.findFirst().orElseThrow();
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		return routes(definition, source, npcId, action).stream().findFirst().orElseThrow();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		return routes(definition, source, npcId).stream()
			.filter(transition -> Integer.valueOf(action.id()).equals(
				((QuestEvent.TalkToNpc) transition.event()).dialogId()))
			.toList();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId)
			.toList();
	}
}
