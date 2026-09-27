package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest1163ClientDialogAlignmentTest {
	private static final int START_NPC = 203096;
	private static final int ILLOS = 203151;
	private static final int REWARD_NPC = 203155;
	private static final int WORK_ITEM = 182200564;

	@Test
	void followsTheRetailPotionHandoffAndRewardOwner() throws Exception {
		QuestDefinition definition = load();

		assertEquals(List.of(new QuestItemRequirement(WORK_ITEM, 1)),
			definition.metadata().questWorkItems());
		assertTrue(routes(definition, "unaccepted", ILLOS).isEmpty());
		assertTrue(routes(definition, "unaccepted", REWARD_NPC).isEmpty());

		QuestTransition accept = route(definition, "unaccepted", START_NPC, QuestDialogAction.QUEST_ACCEPT_1);
		assertEquals("started", accept.targetNode());
		assertTrue(accept.actions().contains(new QuestAction.GiveItem(WORK_ITEM, 1)));

		assertPage(definition, "started", ILLOS, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
		assertPage(definition, "started", ILLOS, QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);

		QuestTransition handoff = route(definition, "started", ILLOS, QuestDialogAction.SETPRO1);
		assertEquals("reward", handoff.targetNode());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), handoff.afterCommit());

		// S3c-D（quest-native-dispatch）：交付 NPC 侧下发报告页的记录按 R-REP **页下发判据**退场（本行有
		// reward 入边翻面记录且无报告块）——奖励面由领奖态奖励窗载体承担（下一条断言）。
		// R-REP retires the reward-side report page; the reward-window carrier takes over.
		assertTrue(routes(definition, "reward", REWARD_NPC, QuestDialogAction.QUEST_SELECT).isEmpty());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			route(definition, "reward", REWARD_NPC, QuestDialogAction.SELECT_QUEST_REWARD).afterCommit());
		assertTrue(route(definition, "reward", REWARD_NPC, QuestDialogAction.SELECTED_QUEST_REWARD1)
			.actions().contains(new QuestAction.CompleteQuest(0)));
		assertTrue(routes(definition, "reward", ILLOS).isEmpty());
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
		QuestDialogAction action, QuestDialogPage page) {
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())),
			route(definition, source, npcId, action).afterCommit());
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
		QuestDialogAction action) {
		List<QuestTransition> routes = routes(definition, source, npcId, action);
		assertEquals(1, routes.size(), "quest 1163 " + source + " " + npcId + " " + action);
		return routes.getFirst();
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

	private static QuestDefinition load() throws Exception {
		return ProductionQuestDefinitions.definitionInOverlay(1163).definition();
	}
}
