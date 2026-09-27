package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest80875RetailAlignmentTest {
	private static final int QUEST_ID = 80875;
	private static final int NPC_ID = 834166;
	private static final int ITEM_ID = 182216117;

	@Test
	void itemTurnInFollowsTheLegacyHandlerAndClientPageChain() {
		QuestDefinition definition = compile();

		assertPage(definition, "started", 31, 1011);
		assertPage(definition, "reward", 31, 10002);
		assertPage(definition, "reward", 1009, 5);

		List<QuestTransition> checks = talkRoutes(definition, "started", 39);
		assertEquals(2, checks.size());
		QuestTransition success = checks.stream()
			.filter(transition -> Integer.valueOf(0).equals(transition.priority()))
			.findFirst().orElseThrow();
		QuestTransition failure = checks.stream()
			.filter(transition -> Integer.valueOf(1).equals(transition.priority()))
			.findFirst().orElseThrow();

		assertEquals("reward", success.targetNode());
		assertEquals(List.of(new QuestCondition.HasItem(ITEM_ID, 7)), success.conditions());
		assertEquals(List.of(new QuestAction.RemoveItem(ITEM_ID, 7)), success.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(10000)), success.afterCommit());
		assertEquals("started", failure.targetNode());
		assertTrue(failure.conditions().isEmpty());
		assertTrue(failure.actions().isEmpty());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(10001)), failure.afterCommit());

		assertTrue(talkRoutes(definition, "started", 10255).isEmpty());
		assertTrue(talkRoutes(definition, "started", 1009).isEmpty());
	}

	/** 生产驱动定义（已退役的 XML 只在 git 历史）。 / The production-driver definition (the retired XML lives only in git history). */
	private static QuestDefinition compile() {
		return ProductionQuestDefinitions.definition(QUEST_ID).definition();
	}

	private static void assertPage(QuestDefinition definition, String source, int dialogId, int pageId) {
		List<QuestTransition> routes = talkRoutes(definition, source, dialogId);
		assertEquals(1, routes.size(), source + " dialog " + dialogId);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(pageId)), routes.getFirst().afterCommit());
	}

	private static List<QuestTransition> talkRoutes(QuestDefinition definition, String source, int dialogId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == NPC_ID && Integer.valueOf(dialogId).equals(talk.dialogId()))
			.toList();
	}
}
