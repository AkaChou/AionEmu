package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest3961To3964RetailAlignmentTest {
	private static final int FLORA = 798384;
	private static final int ERDOS = 203740;
	private static final List<Spec> SPECS = List.of(
		new Spec(3961, 182206108, List.of(new RequiredItem(182400001, 40000))),
		new Spec(3962, 182206109, List.of(new RequiredItem(186000088, 1),
			new RequiredItem(182400001, 50000))),
		new Spec(3963, 182206110, List.of(new RequiredItem(186000089, 1),
			new RequiredItem(182400001, 70000))),
		new Spec(3964, 182206111, List.of(new RequiredItem(186000090, 1),
			new RequiredItem(182400001, 90000)))
	);

	@Test
	void followsTheClientCharmDialogsAndLegacyTwoStepItemContracts() throws Exception {
		for (Spec spec : SPECS) {
			QuestDefinition definition = compile(spec.questId());

			assertPage(definition, "unaccepted", FLORA, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT1);
			QuestTransition accept = route(definition, "unaccepted", FLORA, QuestDialogAction.QUEST_ACCEPT_1);
			assertEquals("started", accept.targetNode(), "quest " + spec.questId() + " accept target");
			assertTrue(accept.actions().contains(new QuestAction.GiveItem(spec.workItemId(), 1)),
				"quest " + spec.questId() + " work item grant");
			assertTrue(routes(definition, "unaccepted", ERDOS).isEmpty(),
				"quest " + spec.questId() + " intermediate NPC must not start the quest");

			assertPage(definition, "started", ERDOS, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
			assertPage(definition, "started", ERDOS, QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
			QuestTransition handoff = route(definition, "started", ERDOS, QuestDialogAction.SETPRO1);
			assertEquals("payment", handoff.targetNode(), "quest " + spec.questId() + " handoff target");
			assertEquals(List.of(new QuestAction.RemoveItem(spec.workItemId(), 1)), handoff.actions(),
				"quest " + spec.questId() + " work item removal");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()), handoff.afterCommit(),
				"quest " + spec.questId() + " handoff response");

			assertPage(definition, "payment", FLORA, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT5);
			assertItemCheck(definition, spec, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM,
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT6.id()));
			assertItemCheck(definition, spec, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE,
				new AfterCommitAction.CloseDialog());
			assertEquals(List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
				route(definition, "payment", FLORA, QuestDialogAction.FINISH_DIALOG).afterCommit(),
				"quest " + spec.questId() + " failed check finish");

			QuestTransition completion = route(definition, "reward", FLORA,
				QuestDialogAction.SELECTED_QUEST_REWARD1);
			assertEquals("complete", completion.targetNode(), "quest " + spec.questId() + " completion target");
			assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(0)),
				"quest " + spec.questId() + " completion action");
			assertTrue(routes(definition, "reward", ERDOS).isEmpty(),
				"quest " + spec.questId() + " intermediate NPC must not complete the quest");
		}
	}

	private static void assertItemCheck(QuestDefinition definition, Spec spec, QuestDialogAction action,
			AfterCommitAction failureResponse) {
		List<QuestTransition> checks = routes(definition, "payment", FLORA, action);
		assertEquals(2, checks.size(), "quest " + spec.questId() + " " + action + " branches");
		QuestTransition success = priority(checks, 0);
		QuestTransition failure = priority(checks, 1);
		List<QuestCondition> conditions = spec.requirements().stream()
			.map(item -> (QuestCondition) new QuestCondition.HasItem(item.itemId(), item.count(), true))
			.toList();
		List<QuestAction> removals = spec.requirements().stream()
			.map(item -> (QuestAction) new QuestAction.RemoveItem(item.itemId(), item.count()))
			.toList();

		assertEquals("reward", success.targetNode(), "quest " + spec.questId() + " check target");
		assertEquals(conditions, success.conditions(), "quest " + spec.questId() + " item conditions");
		assertEquals(removals, success.actions(), "quest " + spec.questId() + " item removals");
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH), new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), success.afterCommit(),
			"quest " + spec.questId() + " success response");
		assertEquals("payment", failure.targetNode(), "quest " + spec.questId() + " failure target");
		assertEquals(List.of(failureResponse), failure.afterCommit(),
			"quest " + spec.questId() + " failure response");
	}

	private static QuestDefinition compile(int questId) throws Exception {
		Path path = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/" + questId + ".xml");
		try (InputStream input = Files.newInputStream(path)) {
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())),
			route(definition, source, npcId, action).afterCommit(),
			"quest " + definition.id() + " " + source + " page");
	}

	private static QuestTransition priority(List<QuestTransition> transitions, int priority) {
		return transitions.stream().filter(transition -> Integer.valueOf(priority).equals(transition.priority()))
			.findFirst().orElseThrow();
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		List<QuestTransition> routes = routes(definition, source, npcId, action);
		assertEquals(1, routes.size(), "quest " + definition.id() + " " + source + " " + action);
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

	private record RequiredItem(int itemId, int count) {
	}

	private record Spec(int questId, int workItemId, List<RequiredItem> requirements) {
	}
}
