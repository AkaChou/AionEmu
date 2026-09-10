package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest18602ClientDialogAlignmentTest {
	private static final int START_NPC = 205229;
	private static final int POTION_NPC = 730308;
	private static final int RELIC_KEY = 185000109;

	@Test
	void keepsRetailAlternativeStartConditionsAndAcceptancePage() throws Exception {
		QuestDefinition definition = load().definition();

		assertEquals(List.of(
			List.of(new QuestStartCondition("finished", 18601, 0)),
			List.of(new QuestStartCondition("finished", 1527, 0))),
			definition.metadata().startConditionGroups());

		QuestTransition accept = route(definition, QuestDialogAction.QUEST_ACCEPT_1);
		assertEquals("unaccepted", accept.sourceNode());
		assertEquals("started", accept.targetNode());
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())),
			accept.afterCommit());
	}

	@Test
	void routesMagaPotionThroughRetailCrystalAndItemCheckPages() throws Exception {
		QuestDefinition definition = load().definition();

		QuestTransition inspect = route(definition, "s1", POTION_NPC, QuestDialogAction.USE_OBJECT);
		assertEquals("s1", inspect.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())), inspect.afterCommit());

		QuestTransition examine = route(definition, "s1", POTION_NPC, QuestDialogAction.SELECT2_1);
		assertEquals("s1", examine.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2_1.id())), examine.afterCommit());

		List<QuestTransition> usePotionRoutes = definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals("s1"))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == POTION_NPC && Integer.valueOf(QuestDialogAction.SETPRO2.id()).equals(talk.dialogId()))
			.toList();
		assertEquals(2, usePotionRoutes.size());

		QuestTransition success = usePotionRoutes.stream()
			.filter(transition -> transition.targetNode().equals("s2"))
			.findFirst().orElseThrow();
		assertEquals(List.of(new QuestCondition.HasItem(RELIC_KEY, 1, true)), success.conditions());
		assertEquals(List.of(
			new QuestAction.RemoveItem(RELIC_KEY, 1),
			new QuestAction.SetVariable("var0", 2)), success.actions());
		assertTrue(success.afterCommit().contains(new AfterCommitAction.TeleportPlayer(
			300230000, 687.56116f, 681.68225f, 200.28648f, (byte) 30)));
		assertTrue(success.afterCommit().contains(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)));
		assertTrue(success.afterCommit().contains(new AfterCommitAction.CloseDialog()));

		QuestTransition missingItem = usePotionRoutes.stream()
			.filter(transition -> transition.targetNode().equals("s1"))
			.findFirst().orElseThrow();
		assertEquals(List.of(new QuestCondition.HasItem(RELIC_KEY, 1, false)), missingItem.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_FAIL.id())),
			missingItem.afterCommit());

		assertTrue(definition.transitions().stream()
			.noneMatch(transition -> transition.event().equals(new QuestEvent.MovieEnd(454))));
	}

	@Test
	void routesRobstinCorpseThroughInspectAndRescueActions() throws Exception {
		QuestDefinition definition = load().definition();

		QuestTransition inspect = route(definition, "s2", 700939, QuestDialogAction.USE_OBJECT);
		assertEquals("s2", inspect.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3.id())), inspect.afterCommit());

		QuestTransition rescue = route(definition, "s2", 700939, QuestDialogAction.SETPRO3);
		assertEquals("s3", rescue.targetNode());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 3)), rescue.actions());
		assertTrue(rescue.afterCommit().contains(new AfterCommitAction.CloseDialog()));
	}

	private static QuestTransition route(QuestDefinition definition, QuestDialogAction action) {
		return route(definition, "unaccepted", START_NPC, action);
	}

	private static QuestTransition route(QuestDefinition definition, String sourceNode, int npcId,
		QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals(sourceNode))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && Integer.valueOf(action.id()).equals(talk.dialogId()))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load() throws Exception {
		try (InputStream input = Quest18602ClientDialogAlignmentTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/18602.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 18602.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
