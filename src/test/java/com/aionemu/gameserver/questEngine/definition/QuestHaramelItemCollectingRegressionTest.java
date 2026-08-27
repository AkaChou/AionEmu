package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定哈拉梅尔物品采集任务的交互物门控与交付 NPC。
 * Locks interaction-object gates and turn-in NPCs for Haramel item-collecting quests.
 */
class QuestHaramelItemCollectingRegressionTest {
	private static final List<QuestCase> CASES = List.of(
		new QuestCase(18501, 799522, 799523,
			List.of(new ObjectDrop(700833, 182212001), new ObjectDrop(700951, 182212002))),
		new QuestCase(28501, 799522, 799523,
			List.of(new ObjectDrop(700833, 182212013), new ObjectDrop(700951, 182212014))),
		new QuestCase(18503, 799523, 203166,
			List.of(new ObjectDrop(700834, 182212004))),
		new QuestCase(18509, 799523, 799524,
			List.of(new ObjectDrop(700853, 182212008))),
		new QuestCase(28503, 799523, 804605,
			List.of(new ObjectDrop(700834, 182212016))),
		new QuestCase(28509, 799523, 799524,
			List.of(new ObjectDrop(700853, 182212020))));

	@Test
	void haramelItemCollectingQuestsExposeObjectGatesAndUseTheirTurnInNpc() {
		for (QuestCase questCase : CASES) {
			CompiledQuestDefinition definition = load(questCase.questId());
			assertEquals(QuestStatus.START, node(definition, "started").projection().status(),
				"started status for quest " + questCase.questId());
			assertEquals(Map.of("var0", 0), node(definition, "started").projection().variables(),
				"started variables for quest " + questCase.questId());

			assertTrue(hasDialog(definition, "unaccepted", questCase.startNpcId(), QuestDialogAction.QUEST_SELECT.id()),
				"start NPC route for quest " + questCase.questId());
			assertFalse(hasDialog(definition, "unaccepted", questCase.turnInNpcId(), QuestDialogAction.QUEST_SELECT.id()),
				"stale turn-in NPC start route for quest " + questCase.questId());
			assertTrue(hasDialog(definition, "started", questCase.turnInNpcId(), QuestDialogAction.QUEST_SELECT.id()),
				"turn-in selection route for quest " + questCase.questId());
			assertTrue(hasDialog(definition, "started", questCase.turnInNpcId(),
				QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()),
				"turn-in item-check route for quest " + questCase.questId());
			assertTrue(hasDialog(definition, "started", questCase.turnInNpcId(),
				QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE.id()),
				"turn-in simple item-check route for quest " + questCase.questId());
			assertTrue(hasDialog(definition, "started", questCase.startNpcId(), QuestDialogAction.FINISH_DIALOG.id()),
				"accept-confirm finish route for quest " + questCase.questId());
			assertTrue(hasDialog(definition, "started", questCase.turnInNpcId(), QuestDialogAction.FINISH_DIALOG.id()),
				"turn-in finish route for quest " + questCase.questId());
			assertFalse(hasDialog(definition, "started", questCase.startNpcId(),
				QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()),
				"stale start-NPC item-check route for quest " + questCase.questId());
			assertFalse(hasDialog(definition, "started", questCase.startNpcId(),
				QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE.id()),
				"stale start-NPC simple item-check route for quest " + questCase.questId());

			for (ObjectDrop objectDrop : questCase.objectDrops()) {
				QuestTransition gate = route(definition,
					new QuestEvent.CanAct(objectDrop.objectId(), "ACTION_ITEM_USE"));
				assertEquals("started", gate.sourceNode(),
					"object gate source for quest " + questCase.questId());
				assertEquals("started", gate.targetNode(),
					"object gate target for quest " + questCase.questId());
				assertEquals(List.of(), gate.actions(),
					"object gate actions for quest " + questCase.questId());
				assertEquals(List.of(), gate.afterCommit(),
					"object gate after-commit actions for quest " + questCase.questId());

				QuestTransition use = route(definition,
					new QuestEvent.TalkToNpc(objectDrop.objectId(), -1));
				assertEquals("started", use.sourceNode(),
					"object use source for quest " + questCase.questId());
				assertEquals("started", use.targetNode(),
					"object use target for quest " + questCase.questId());
				assertEquals(List.of(), use.actions(),
					"object use actions for quest " + questCase.questId());
				assertEquals(List.of(), use.afterCommit(),
					"object use after-commit actions for quest " + questCase.questId());

				assertTrue(definition.definition().metadata().drops().stream().anyMatch(drop ->
					drop.npcId() == objectDrop.objectId() && drop.itemId() == objectDrop.itemId()
						&& drop.chance() == 100 && drop.eachMember() && drop.collectingStep() == 0),
					"drop metadata for quest " + questCase.questId());
				assertFalse(definition.definition().transitions().stream().anyMatch(transition ->
					"unaccepted".equals(transition.sourceNode())
						&& ((transition.event() instanceof QuestEvent.TalkToNpc talk
							&& talk.npcId() == objectDrop.objectId())
							|| (transition.event() instanceof QuestEvent.CanAct canAct
								&& canAct.templateId() == objectDrop.objectId()))),
					"unaccepted object route for quest " + questCase.questId());
			}
		}
	}

	@Test
	void quest18509AcceptAndEmptyReportDialogsHaveClientOwnedResponses() {
		CompiledQuestDefinition definition = load(18509);

		QuestTransition accept = dialog(definition, "unaccepted", "started", 799523,
			QuestDialogAction.QUEST_ACCEPT_1.id());
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())), accept.afterCommit());

		QuestTransition acceptFinish = dialog(definition, "started", "started", 799523,
			QuestDialogAction.FINISH_DIALOG.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			acceptFinish.afterCommit());

		QuestTransition emptyReport = definition.definition().transitions().stream()
			.filter(transition -> "started".equals(transition.sourceNode())
				&& "started".equals(transition.targetNode())
				&& Integer.valueOf(1).equals(transition.priority())
				&& transition.event().equals(new QuestEvent.TalkToNpc(799524,
					QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id())))
			.findFirst().orElseThrow();
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT6.id())),
			emptyReport.afterCommit());

		QuestTransition emptyReportFinish = dialog(definition, "started", "started", 799524,
			QuestDialogAction.FINISH_DIALOG.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			emptyReportFinish.afterCommit());
	}

	@Test
	void quest28509AcceptAndEmptyReportDialogsHaveClientOwnedResponses() {
		CompiledQuestDefinition definition = load(28509);

		QuestTransition accept = dialog(definition, "unaccepted", "started", 799523,
			QuestDialogAction.QUEST_ACCEPT_1.id());
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())), accept.afterCommit());

		QuestTransition acceptFinish = dialog(definition, "started", "started", 799523,
			QuestDialogAction.FINISH_DIALOG.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			acceptFinish.afterCommit());

		QuestTransition emptyReport = definition.definition().transitions().stream()
			.filter(transition -> "started".equals(transition.sourceNode())
				&& "started".equals(transition.targetNode())
				&& Integer.valueOf(1).equals(transition.priority())
				&& transition.event().equals(new QuestEvent.TalkToNpc(799524,
					QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id())))
			.findFirst().orElseThrow();
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT6.id())),
			emptyReport.afterCommit());

		QuestTransition emptyReportFinish = dialog(definition, "started", "started", 799524,
			QuestDialogAction.FINISH_DIALOG.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			emptyReportFinish.afterCommit());
	}

	@Test
	void quest18505ExposesTurnInNpcGaphyrkWithoutDuplicateStartNpcCompletion() {
		CompiledQuestDefinition definition = load(18505);

		assertTrue(hasDialog(definition, "unaccepted", 203166, QuestDialogAction.QUEST_SELECT.id()));
		assertFalse(hasDialog(definition, "unaccepted", 203106, QuestDialogAction.QUEST_SELECT.id()));

		assertTrue(hasDialog(definition, "started", 203106, QuestDialogAction.QUEST_SELECT.id()));
		assertFalse(hasDialog(definition, "started", 203166, QuestDialogAction.QUEST_SELECT.id()));

		assertTrue(hasDialog(definition, "started", 203166, QuestDialogAction.FINISH_DIALOG.id()));
		assertTrue(hasDialog(definition, "started", 203106, QuestDialogAction.FINISH_DIALOG.id()));

		assertTrue(hasDialog(definition, "started", 203106, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
		assertFalse(hasDialog(definition, "started", 203166, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));

		assertTrue(hasDialog(definition, "started", 203106, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE.id()));
		assertFalse(hasDialog(definition, "started", 203166, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE.id()));

		long completeCount203106 = definition.definition().transitions().stream()
			.filter(t -> "reward".equals(t.sourceNode()) && "complete".equals(t.targetNode())
				&& t.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == 203106)
			.count();
		long completeCount203166 = definition.definition().transitions().stream()
			.filter(t -> "reward".equals(t.sourceNode()) && "complete".equals(t.targetNode())
				&& t.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == 203166)
			.count();
		assertTrue(completeCount203106 > 0, "turn-in NPC 203106 completion routes");
		assertEquals(0, completeCount203166, "no completion routes on start NPC 203166");
	}

	private static boolean hasDialog(CompiledQuestDefinition definition, String sourceNode, int npcId, int dialogId) {
		return definition.definition().transitions().stream().anyMatch(transition ->
			sourceNode.equals(transition.sourceNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId
				&& QuestEvent.matches(transition.event(), new QuestEvent.TalkToNpc(npcId, dialogId)));
	}

	private static QuestTransition dialog(CompiledQuestDefinition definition, String sourceNode, String targetNode,
		int npcId, int dialogId) {
		return definition.definition().transitions().stream()
			.filter(transition -> sourceNode.equals(transition.sourceNode())
				&& targetNode.equals(transition.targetNode())
				&& transition.event().equals(new QuestEvent.TalkToNpc(npcId, dialogId)))
			.findFirst().orElseThrow();
	}

	private static QuestTransition route(CompiledQuestDefinition definition, QuestEvent event) {
		return definition.definition().transitions().stream()
			.filter(transition -> "started".equals(transition.sourceNode())
				&& "started".equals(transition.targetNode())
				&& QuestEvent.matches(transition.event(), event))
			.findFirst().orElseThrow();
	}

	private static QuestNode node(CompiledQuestDefinition definition, String label) {
		return definition.definition().nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load(int questId) {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = QuestHaramelItemCollectingRegressionTest.class.getResourceAsStream(resource)) {
			if (input == null) {
				throw new AssertionError("missing resource " + resource);
			}
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("failed to load " + resource, e);
		}
	}

	private record QuestCase(int questId, int startNpcId, int turnInNpcId, List<ObjectDrop> objectDrops) {
	}

	private record ObjectDrop(int objectId, int itemId) {
	}
}
