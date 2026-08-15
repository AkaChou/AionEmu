package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemCollectingDialogProtocolAlignmentTest {
	private static final Path QUEST_DIRECTORY = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests");
	private static final int[] ITEM_COLLECTING_QUESTS = {
		13968, 15230, 15231, 15232, 15307, 15323, 15403, 15404, 15405, 15540, 15541,
		15665, 15666, 15689, 15691, 18742, 18975, 18976, 18977, 18978, 23968, 25012,
		25020, 25033, 25085, 25091, 25092, 25307, 25323, 25403, 25404, 25405, 25540,
		25541, 25665, 25666, 25689, 25691, 28742, 28975, 28976, 28977, 28978, 50052,
		50053, 50054, 50055, 50056, 50057, 50088, 50089, 50090, 50094, 80723, 80724,
		80725, 80726, 80727, 80728, 80729, 80730, 80735, 80736, 80834, 80835, 80836, 80837,
		80838, 80839, 80840, 80841, 80870, 80871, 80872, 80874, 80877, 80878, 80881,
		80900, 80901, 80902, 80903, 80904, 80905, 80906, 80907,
		80908, 80909, 80910, 80911, 80912, 80913, 80914, 80915, 80916, 80917, 80918,
		80919, 80947, 80948, 80949, 80950, 80951, 80953
	};

	@Test
	void retailItemCollectingQuestsUseTheClientItemCheckProtocol() throws Exception {
		for (int questId : ITEM_COLLECTING_QUESTS) {
			QuestDefinition definition = compile(questId);
			List<Integer> startNpcs = startNpcs(questId);
			List<Integer> endNpcs = endNpcs(questId);

			assertEquals(startNpcs, dialogNpcs(definition, "unaccepted", 31),
				"quest " + questId + " start NPCs");
			for (int npcId : startNpcs) {
				assertDialogPage(definition, "unaccepted", npcId, 31, 4762);
			}

			assertEquals(endNpcs, dialogNpcs(definition, "started", 39),
				"quest " + questId + " turn-in NPCs");
			for (int npcId : endNpcs) {
				assertDialogPage(definition, "started", npcId, 31, 1011);
				assertDialogPage(definition, "reward", npcId, 31, 10002);
				assertItemCheck(definition, npcId);
			}

			Set<Integer> collectedItemIds = definition.metadata().itemRequirements().stream()
				.map(QuestItemRequirement::itemId).collect(java.util.stream.Collectors.toSet());
			definition.transitions().stream()
				.filter(transition -> "reward".equals(transition.sourceNode()))
				.forEach(transition -> {
					assertFalse(transition.conditions().stream().anyMatch(condition -> condition instanceof QuestCondition.HasItem item
						&& collectedItemIds.contains(item.itemId())), "quest " + questId + " reward item condition");
					assertFalse(transition.actions().stream().anyMatch(action -> action instanceof QuestAction.RemoveItem item
						&& collectedItemIds.contains(item.itemId())), "quest " + questId + " repeated item removal");
				});
			assertTrue(definition.transitions().stream().anyMatch(transition -> "reward".equals(transition.sourceNode())
				&& "complete".equals(transition.targetNode())), "quest " + questId + " completion route");
		}
	}

	@Test
	void lunaItemTurnInsKeepStartAndEndNpcOwnershipSeparate() throws Exception {
		for (int questId : List.of(80870, 80871, 80872, 80874)) {
			QuestDefinition definition = compile(questId);
			assertTrue(talkRoutes(definition, "started", 833825, 31).isEmpty(),
				"quest " + questId + " start NPC must not own started selection");
			assertTrue(talkRoutes(definition, "started", 833825, 39).isEmpty(),
				"quest " + questId + " start NPC must not own item check");
			assertTrue(talkRoutes(definition, "reward", 833825).isEmpty(),
				"quest " + questId + " start NPC must not own reward routes");
		}
	}

	@Test
	void simpleItemCheckQuestsKeepBothLegacyCheckRoutes() throws Exception {
		for (SimpleItemQuest quest : List.of(
			new SimpleItemQuest(80745, 833623), new SimpleItemQuest(80748, 833623),
			new SimpleItemQuest(80785, 833659), new SimpleItemQuest(80786, 833659),
			new SimpleItemQuest(80975, 835994), new SimpleItemQuest(80976, 835994),
			new SimpleItemQuest(80977, 835994))) {
			QuestDefinition definition = compile(quest.id());
			assertDialogPage(definition, "unaccepted", quest.npcId(), 31, 4762);
			assertDialogPage(definition, "started", quest.npcId(), 31, 1011);
			assertDialogPage(definition, "reward", quest.npcId(), 31, 10002);
			assertItemCheck(definition, quest.npcId());

			List<QuestTransition> simpleChecks = talkRoutes(definition, "started", quest.npcId(), 20002);
			assertEquals(2, simpleChecks.size(), "quest " + quest.id() + " simple check branches");
			QuestTransition success = simpleChecks.stream()
				.filter(transition -> Integer.valueOf(0).equals(transition.priority()))
				.findFirst().orElseThrow();
			QuestTransition failure = simpleChecks.stream()
				.filter(transition -> Integer.valueOf(1).equals(transition.priority()))
				.findFirst().orElseThrow();
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(5)), success.afterCommit(),
				"quest " + quest.id() + " simple success");
			assertEquals(List.of(new AfterCommitAction.CloseDialog()), failure.afterCommit(),
				"quest " + quest.id() + " simple failure");
		}
	}

	@Test
	void autoStartedMisfortuneQuestsRemoveCollectedItemsOnSuccessfulCheck() throws Exception {
		for (SimpleItemQuest quest : List.of(
			new SimpleItemQuest(80945, 835303), new SimpleItemQuest(80946, 835303))) {
			QuestDefinition definition = compile(quest.id());
			assertTrue(talkRoutes(definition, "unaccepted", quest.npcId(), 31).isEmpty(),
				"quest " + quest.id() + " remains auto-started");
			assertDialogPage(definition, "started", quest.npcId(), 31, 1011);
			assertDialogPage(definition, "reward", quest.npcId(), 31, 10002);
			assertItemCheck(definition, quest.npcId());
		}
	}

	private static void assertItemCheck(QuestDefinition definition, int npcId) {
		List<QuestTransition> checks = talkRoutes(definition, "started", npcId, 39);
		assertEquals(2, checks.size(), "quest " + definition.id() + " item check branches");
		QuestTransition success = checks.stream()
			.filter(transition -> Integer.valueOf(0).equals(transition.priority()))
			.findFirst().orElseThrow();
		QuestTransition failure = checks.stream()
			.filter(transition -> Integer.valueOf(1).equals(transition.priority()))
			.findFirst().orElseThrow();
		List<QuestCondition> expectedConditions = definition.metadata().itemRequirements().stream()
			.map(item -> (QuestCondition) new QuestCondition.HasItem(item.itemId(), item.count(), true))
			.toList();
		List<QuestAction> expectedActions = definition.metadata().itemRequirements().stream()
			.map(item -> (QuestAction) new QuestAction.RemoveItem(item.itemId(), item.count()))
			.toList();

		assertEquals("reward", success.targetNode(), "quest " + definition.id() + " successful target");
		assertEquals(expectedConditions, success.conditions(), "quest " + definition.id() + " item conditions");
		assertEquals(expectedActions, success.actions(), "quest " + definition.id() + " item removals");
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(10000)), success.afterCommit(),
			"quest " + definition.id() + " successful page");
		assertEquals("started", failure.targetNode(), "quest " + definition.id() + " failed target");
		assertTrue(failure.conditions().isEmpty(), "quest " + definition.id() + " failed fallback");
		assertTrue(failure.actions().isEmpty(), "quest " + definition.id() + " failed actions");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(10001)), failure.afterCommit(),
			"quest " + definition.id() + " failed page");
	}

	private static QuestDefinition compile(int questId) throws Exception {
		try (InputStream input = Files.newInputStream(QUEST_DIRECTORY.resolve(questId + ".xml"))) {
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}

	private static void assertDialogPage(QuestDefinition definition, String source, int npcId, int dialogId,
			int pageId) {
		List<QuestTransition> routes = talkRoutes(definition, source, npcId, dialogId);
		assertEquals(1, routes.size(), "quest " + definition.id() + " " + source + " dialog " + dialogId);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(pageId)), routes.getFirst().afterCommit(),
			"quest " + definition.id() + " " + source + " page");
	}

	private static List<Integer> dialogNpcs(QuestDefinition definition, String source, int dialogId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& Integer.valueOf(dialogId).equals(talk.dialogId()))
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
			.distinct().toList();
	}

	private static List<QuestTransition> talkRoutes(QuestDefinition definition, String source, int npcId, int dialogId) {
		return talkRoutes(definition, source, npcId).stream()
			.filter(transition -> Integer.valueOf(dialogId).equals(((QuestEvent.TalkToNpc) transition.event()).dialogId()))
			.toList();
	}

	private static List<QuestTransition> talkRoutes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId)
			.toList();
	}

	private static List<Integer> startNpcs(int questId) {
		return switch (questId) {
			case 18742 -> List.of(206378, 206379, 206380);
			case 28742 -> List.of(206395, 206396, 206397);
			case 50052, 50053, 50054, 50055, 50056, 50057 -> List.of(833982, 833983);
			case 50088 -> List.of(835542, 835543);
			case 50089, 50090, 50094 -> List.of(835680, 835681);
			default -> List.of(singleStartNpc(questId));
		};
	}

	private static List<Integer> endNpcs(int questId) {
		return switch (questId) {
			case 18742 -> List.of(804707);
			case 25012 -> List.of(804905);
			case 25085 -> List.of(804927);
			case 25092 -> List.of(804929);
			case 28742 -> List.of(804732);
			case 18977, 18978 -> List.of(805215);
			case 28977, 28978 -> List.of(805218);
			case 50052, 50053, 50054, 50055, 50056, 50057 -> List.of(833982, 833983);
			case 50088 -> List.of(835542, 835543);
			case 50089, 50090, 50094 -> List.of(835680, 835681);
			case 80870, 80871, 80872, 80874 -> List.of(834167);
			default -> startNpcs(questId);
		};
	}

	private static int singleStartNpc(int questId) {
		return switch (questId) {
			case 13968 -> 835217;
			case 15230, 15231, 15232 -> 805222;
			case 15307 -> 805327;
			case 15323 -> 805330;
			case 15403, 15404, 15405 -> 805378;
			case 15540 -> 806134;
			case 15541 -> 834136;
			case 15665 -> 806089;
			case 15666 -> 806090;
			case 15689, 15691 -> 806696;
			case 18975, 18976 -> 805215;
			case 18977 -> 802350;
			case 18978 -> 802431;
			case 23968 -> 835220;
			case 25012 -> 804906;
			case 25020 -> 804725;
			case 25033 -> 804913;
			case 25085 -> 804922;
			case 25091 -> 804738;
			case 25092 -> 804739;
			case 25307 -> 805339;
			case 25323 -> 805342;
			case 25403, 25404, 25405 -> 805401;
			case 25540 -> 806135;
			case 25541 -> 834138;
			case 25665 -> 806101;
			case 25666 -> 806102;
			case 25689, 25691 -> 806697;
			case 28975, 28976 -> 805218;
			case 28977 -> 802353;
			case 28978 -> 802433;
			case 80723, 80725, 80727, 80729 -> 833543;
			case 80724, 80726, 80728, 80730 -> 833545;
			case 80735 -> 833544;
			case 80736 -> 833546;
			case 80834, 80836 -> 833742;
			case 80835, 80837 -> 833743;
			case 80838, 80839, 80840, 80841 -> 832913;
			case 80870, 80871, 80872, 80874 -> 833825;
			case 80877, 80878, 80881 -> 834463;
			case 80900, 80901, 80902, 80903, 80904, 80905, 80906, 80907, 80908, 80909,
				80910, 80911, 80912, 80913, 80914, 80915, 80916, 80917, 80918, 80919 -> 834418;
			case 80947, 80948 -> 835439;
			case 80949 -> 835551;
			case 80950, 80951 -> 835552;
			case 80953 -> 835553;
			default -> throw new IllegalArgumentException("missing start NPC for quest " + questId);
		};
	}

	private record SimpleItemQuest(int id, int npcId) {
	}
}
