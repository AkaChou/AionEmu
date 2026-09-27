package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestReward;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 多交付 NPC 的奖励窗事件只在任务域登记一次，对话路由仍归各 NPC。
 * Multi-NPC reward windows register one quest-scoped event and retain every NPC-scoped route.
 */
class RetailRewardWindowRouteTest {
	private static final int AUTO_REWARD = QuestDialogAction.SELECTED_QUEST_AUTO_REWARD.id();
	private static final List<AfterCommitAction> CLOSE_WINDOW = List.of(
		new AfterCommitAction.RefreshPlayerStats(),
		new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
		new AfterCommitAction.CloseDialog());

	private record FamilyCase(String family, int questId, int npcCount, Set<Integer> knownNpcs) {
	}

	@TestFactory
	Stream<DynamicTest> multiNpcFamiliesKeepOneGlobalAutoRewardAndEveryNpcRoute() {
		return List.of(
			new FamilyCase("CombineTask", 5000, 2, Set.of()),
			new FamilyCase("SimpleCollectItem", 35021, 2, Set.of(799800, 799801)),
			new FamilyCase("SimpleHunt", 35023, 2, Set.of(799800, 799801)),
			new FamilyCase("SimpleTalk", 35045, 2, Set.of(799800, 799801)))
			.stream().map(row -> DynamicTest.dynamicTest(row.family() + " " + row.questId(), () -> {
				QuestDefinition definition = ProductionQuestDefinitions.definitionInOverlay(row.questId())
					.definition();
				assertRoutes(definition.transitions(), AUTO_REWARD, row.npcCount(), row.knownNpcs());
			}));
	}

	@Test
	void selectableWindowSlotsAlsoKeepOneGlobalRoutePerAction() {
		List<QuestTransition> routes = RetailSimpleHuntDefinitionCompiler.rewardWindowAutoFlow(
			List.of(799800, 799801), List.of(),
			List.of(new QuestReward("SELECTABLE_ITEM", 1001, 1),
				new QuestReward("SELECTABLE_ITEM", 1002, 1)),
			Map.of(), "reward", "complete");
		for (int dialogId = QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id();
				dialogId < QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id() + 2; dialogId++) {
			assertRoutes(routes, dialogId, 2, Set.of(799800, 799801));
		}
	}

	private static void assertRoutes(List<QuestTransition> transitions, int dialogId, int npcCount,
			Set<Integer> knownNpcs) {
		List<QuestTransition> global = transitions.stream()
			.filter(route -> route.event().equals(new QuestEvent.QuestDialog(dialogId)))
			.filter(route -> "reward".equals(route.sourceNode()) && "complete".equals(route.targetNode()))
			.toList();
		assertEquals(1, global.size(), "quest-scoped reward action " + dialogId);
		QuestTransition expected = global.getFirst();
		assertEquals(List.of(), expected.conditions());
		assertNull(expected.priority());
		assertEquals(CLOSE_WINDOW, expected.afterCommit());
		assertTrue(expected.actions().getLast() instanceof QuestAction.CompleteQuest);

		List<QuestTransition> npcRoutes = transitions.stream()
			.filter(route -> route.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() != null && talk.dialogId() == dialogId)
			.filter(route -> "reward".equals(route.sourceNode()) && "complete".equals(route.targetNode()))
			.toList();
		assertEquals(npcCount, npcRoutes.size(), "NPC-scoped reward action " + dialogId);
		Set<Integer> actualNpcs = npcRoutes.stream()
			.map(route -> ((QuestEvent.TalkToNpc) route.event()).npcId())
			.collect(java.util.stream.Collectors.toUnmodifiableSet());
		assertEquals(npcCount, actualNpcs.size(), "distinct completion NPCs");
		if (!knownNpcs.isEmpty()) {
			assertEquals(knownNpcs, actualNpcs);
		}
		for (QuestTransition route : npcRoutes) {
			assertEquals(expected.conditions(), route.conditions());
			assertEquals(expected.actions(), route.actions());
			assertEquals(expected.afterCommit(), route.afterCommit());
			assertEquals(expected.priority(), route.priority());
		}
	}
}
