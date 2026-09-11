package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the legacy and Aion 5.8 client contracts for the master crafting-potential quest family.
 */
class CraftingPotentialQuestFamilyTest {

	private static final List<SimpleCase> SIMPLE = List.of(
		new SimpleCase(19014, 203790, 203791, 182206765, List.of(152201806), List.of(152201807)),
		new SimpleCase(19020, 203793, 203794, 182206766, List.of(152201962), List.of(152201963)),
		new SimpleCase(19026, 203792, 798013, 182206767,
			List.of(152202049, 152020249), List.of(152202050, 152020249)),
		new SimpleCase(19032, 203786, 203787, 182206768,
			List.of(152202147, 152020248), List.of(152202148, 152020248)),
		new SimpleCase(29014, 204106, 204107, 182207899, List.of(152206808), List.of(152206809)),
		new SimpleCase(29020, 204110, 204111, 182207900, List.of(152206964), List.of(152206965)),
		new SimpleCase(29026, 204108, 798060, 182207901,
			List.of(152207051, 152029250), List.of(152207052, 152029250)),
		new SimpleCase(29032, 204102, 204103, 182207902,
			List.of(152207149, 152029249), List.of(152207150, 152029249)));

	private static final List<ConstructorCase> CONSTRUCTORS = List.of(
		new ConstructorCase(19057, 798450, 798451, 182206927, 152203543, 152203544,
			167500, 223000, 155003543, 155003544),
		new ConstructorCase(29057, 798452, 798453, 182207980, 152208541, 152208542,
			167500, 223000, 155008541, 155008542));

	@Test
	void simpleCraftingPotentialQuestsMatchLegacyAndClientContracts() throws Exception {
		for (SimpleCase contract : SIMPLE) {
			QuestDefinition definition = definition(contract.questId());
			assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
			assertNode(definition, "started0", QuestStatus.START, Map.of("var0", 0));
			assertNode(definition, "started1", QuestStatus.START, Map.of("var0", 1));
			assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1));
			assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0));
			assertStartContract(definition, contract.startNpc(), "started0");

			assertPage(definition, "started0", contract.stageNpc(), QuestDialogAction.QUEST_SELECT,
				QuestDialogPage.SELECT1);
			assertPage(definition, "started0", contract.stageNpc(), QuestDialogAction.SELECT1_1,
				QuestDialogPage.SELECT1_1);
			assertPage(definition, "started0", contract.stageNpc(), QuestDialogAction.SELECT1_2,
				QuestDialogPage.SELECT1_2);
			assertPage(definition, "started0", contract.stageNpc(), QuestDialogAction.SELECT1_3,
				QuestDialogPage.SELECT1_3);
			assertPurchase(definition, contract.stageNpc(), QuestDialogAction.SETPRO10,
				contract.firstWorkItems());
			assertPurchase(definition, contract.stageNpc(), QuestDialogAction.SETPRO20,
				contract.secondWorkItems());

			assertPage(definition, "started0", contract.startNpc(), QuestDialogAction.QUEST_SELECT,
				QuestDialogPage.CHECK_USER_ITEM_FAIL);
			assertTalk(definition, "started1", "reward", contract.startNpc(),
				QuestDialogAction.QUEST_SELECT,
				List.of(new QuestCondition.HasItem(contract.returnItem(), 1, true)),
				List.of(new QuestAction.RemoveItem(contract.returnItem(), 1)),
				List.of(new AfterCommitAction.SyncQuestState(
						QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())));
			assertTalk(definition, "started1", "started1", contract.startNpc(),
				QuestDialogAction.QUEST_SELECT,
				List.of(new QuestCondition.HasItem(contract.returnItem(), 1, false)),
				List.of(), List.of(new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.CHECK_USER_ITEM_FAIL.id())));

			QuestTransition preview = talk(definition, "reward", contract.startNpc(),
				QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM, List.of());
			assertEquals("reward", preview.targetNode());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());
			assertCompletion(definition, "reward", contract.startNpc());
		}
	}

	@Test
	void constructorPotentialQuestsMatchLegacyCostRecipeAndRetryContracts() throws Exception {
		for (ConstructorCase contract : CONSTRUCTORS) {
			QuestDefinition definition = definition(contract.questId());
			assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
			assertNode(definition, "started0", QuestStatus.START, Map.of("var0", 0));
			assertNode(definition, "started1", QuestStatus.START, Map.of("var0", 1));
			assertNode(definition, "started2", QuestStatus.START, Map.of("var0", 2));
			assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1));
			assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0));
			assertStartContract(definition, contract.startNpc(), "started0");

			assertPage(definition, "started0", contract.stageNpc(), QuestDialogAction.QUEST_SELECT,
				QuestDialogPage.SELECT1);
			assertRetryPurchase(definition, "started0", contract);
			assertPage(definition, "started2", contract.stageNpc(), QuestDialogAction.QUEST_SELECT,
				QuestDialogPage.SELECT10);
			assertRetryPurchase(definition, "started2", contract);

			for (String source : List.of("started0", "started1", "started2")) {
				assertReport(definition, source, contract);
			}

			QuestTransition preview = talk(definition, "reward", contract.startNpc(),
				QuestDialogAction.USE_OBJECT, List.of());
			assertEquals("reward", preview.targetNode());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());
			assertCompletion(definition, "reward", contract.startNpc());
		}
	}

	private static void assertRetryPurchase(QuestDefinition definition, String source,
			ConstructorCase contract) {
		assertPage(definition, source, contract.stageNpc(), QuestDialogAction.SELECT1_1,
			QuestDialogPage.SELECT1_1);
		assertPage(definition, source, contract.stageNpc(), QuestDialogAction.SELECT1_2,
			QuestDialogPage.SELECT1_2);
		assertPage(definition, source, contract.stageNpc(), QuestDialogAction.SELECT1_3,
			QuestDialogPage.SELECT1_3);
		assertCostPurchase(definition, source, contract.stageNpc(), QuestDialogAction.SETPRO10,
			contract.firstCost(), contract.firstWorkItem());
		assertCostPurchase(definition, source, contract.stageNpc(), QuestDialogAction.SETPRO20,
			contract.secondCost(), contract.secondWorkItem());
		assertTalk(definition, source, source, contract.stageNpc(), QuestDialogAction.FINISH_DIALOG,
			List.of(), List.of(), List.of(new AfterCommitAction.CloseDialog()));
	}

	private static void assertReport(QuestDefinition definition, String source,
			ConstructorCase contract) {
		assertPage(definition, source, contract.startNpc(), QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SELECT2);
		assertTalk(definition, source, "reward", contract.startNpc(),
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM,
			List.of(new QuestCondition.HasItem(contract.returnItem(), 1, true)),
			List.of(new QuestAction.RemoveItem(contract.returnItem(), 1)),
			List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())));

		assertTalk(definition, source, source, contract.startNpc(),
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM,
			List.of(new QuestCondition.RecipeKnown(contract.firstRecipe(), true)),
			List.of(), List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT6.id())));
		assertTalk(definition, source, source, contract.startNpc(),
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM,
			List.of(new QuestCondition.RecipeKnown(contract.secondRecipe(), true)),
			List.of(), List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT6.id())));

		List<QuestCondition> firstMaterial = List.of(
			new QuestCondition.RecipeKnown(contract.firstRecipe(), false),
			new QuestCondition.RecipeKnown(contract.secondRecipe(), false),
			new QuestCondition.HasItem(contract.firstWorkItem(), 1, true));
		assertTalk(definition, source, source, contract.startNpc(),
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM, firstMaterial, List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT7.id())));
		List<QuestCondition> secondMaterial = List.of(
			new QuestCondition.RecipeKnown(contract.firstRecipe(), false),
			new QuestCondition.RecipeKnown(contract.secondRecipe(), false),
			new QuestCondition.HasItem(contract.secondWorkItem(), 1, true));
		assertTalk(definition, source, source, contract.startNpc(),
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM, secondMaterial, List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT7.id())));

		List<QuestCondition> missing = List.of(
			new QuestCondition.HasItem(contract.returnItem(), 1, false),
			new QuestCondition.RecipeKnown(contract.firstRecipe(), false),
			new QuestCondition.RecipeKnown(contract.secondRecipe(), false),
			new QuestCondition.HasItem(contract.firstWorkItem(), 1, false),
			new QuestCondition.HasItem(contract.secondWorkItem(), 1, false));
		assertTalk(definition, source, "started2", contract.startNpc(),
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM, missing,
			List.of(new QuestAction.SetVariable("var0", 2)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT8.id())));
	}

	private static void assertCostPurchase(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, int cost, int workItem) {
		assertTalk(definition, source, "started1", npcId, action,
			List.of(new QuestCondition.CurrencyAtLeast(QuestRewardKind.KINAH, cost)),
			List.of(
				new QuestAction.DecreaseCurrency(QuestRewardKind.KINAH, cost),
				new QuestAction.GiveItem(workItem, 1),
				new QuestAction.SetVariable("var0", 1)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()));
		assertTalk(definition, source, source, npcId, action,
			List.of(new QuestCondition.CurrencyBelow(QuestRewardKind.KINAH, cost)),
			List.of(), List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SELECT10_4_4.id())));
	}

	private static void assertPurchase(QuestDefinition definition, int npcId,
			QuestDialogAction action, List<Integer> workItems) {
		List<QuestAction> actions = new java.util.ArrayList<>();
		for (int itemId : workItems) {
			actions.add(new QuestAction.GiveItem(itemId, 1));
		}
		actions.add(new QuestAction.SetVariable("var0", 1));
		assertTalk(definition, "started0", "started1", npcId, action, List.of(), actions,
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()));
	}

	private static void assertStartContract(QuestDefinition definition, int npcId,
			String targetNode) {
		assertPageTransition(definition, "unaccepted", "unaccepted", npcId,
			QuestDialogAction.QUEST_SELECT, List.of(), QuestDialogPage.SELECT_NONE);
		assertPageTransition(definition, "unaccepted", "unaccepted", npcId,
			QuestDialogAction.ASK_QUEST_ACCEPT, List.of(), QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW);
		assertTalk(definition, "unaccepted", targetNode, npcId, QuestDialogAction.QUEST_ACCEPT_1,
			List.of(new QuestCondition.StartEligible()), List.of(),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())));
	}

	private static void assertCompletion(QuestDefinition definition, String source, int npcId) {
		QuestTransition completion = talk(definition, source, npcId,
			QuestDialogAction.SELECTED_QUEST_REWARD1, List.of());
		assertEquals("complete", completion.targetNode());
		assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(0)));
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		assertPageTransition(definition, source, source, npcId, action, List.of(), page);
	}

	private static void assertPageTransition(QuestDefinition definition, String source, String target,
			int npcId, QuestDialogAction action, List<QuestCondition> conditions, QuestDialogPage page) {
		QuestTransition transition = talk(definition, source, npcId, action, conditions);
		assertEquals(target, transition.targetNode());
		assertEquals(List.of(), transition.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
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

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst()
			.orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static QuestDefinition definition(int questId) throws Exception {
		try (InputStream input = CraftingPotentialQuestFamilyTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}

	private record SimpleCase(int questId, int startNpc, int stageNpc, int returnItem,
			List<Integer> firstWorkItems, List<Integer> secondWorkItems) {
	}

	private record ConstructorCase(int questId, int startNpc, int stageNpc, int returnItem,
			int firstWorkItem, int secondWorkItem, int firstCost, int secondCost,
			int firstRecipe, int secondRecipe) {
	}
}
