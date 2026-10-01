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
import com.aionemu.gameserver.questEngine.tablelane.SimpleCollectItemHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleHuntHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;
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

	/** typed 车道（未切换家族）：一个 quest 域全局 AUTO_REWARD 事件 + 每个交付 NPC 一条对话路由。 /
	 * Typed lane (unswitched families): one quest-scoped global AUTO_REWARD event plus one dialog route per
	 * hand-in NPC. */
	@TestFactory
	Stream<DynamicTest> typedFamilyKeepsOneGlobalAutoRewardAndEveryNpcRoute() {
		return List.of(new FamilyCase("CombineTask", 5000, 2, Set.of()))
			.stream().map(row -> DynamicTest.dynamicTest(row.family() + " " + row.questId(), () -> {
				QuestDefinition definition = ProductionQuestDefinitions.definitionInOverlay(row.questId())
					.definition();
				assertRoutes(definition.transitions(), AUTO_REWARD, row.npcCount(), row.knownNpcs());
			}));
	}

	/**
	 * 已切到 native 车道的多交付 NPC 行：交付集由客户端任务书登记
	 * （{@code quest_client_reward_npcs.tsv}），处理器按集合逐个 NPC 受理奖励动作；native 车道不产生
	 * quest 域全局 AUTO_REWARD 路由，结构上即不存在 typed 车道那条 {@code AMBIGUOUS_TRANSITION} 风险。
	 * <p>
	 * Native multi-NPC rows: the client quest-letter registry declares the hand-in set, the handler accepts
	 * the reward action on each declared NPC, and no quest-scoped global route is created — the typed
	 * lane's {@code AMBIGUOUS_TRANSITION} hazard cannot exist here.
	 */
	@TestFactory
	Stream<DynamicTest> nativeFamiliesRouteEveryClientDeclaredHandInNpc() {
		return List.of(
			new FamilyCase("SimpleCollectItem", 35021, 2, Set.of(799800, 799801)),
			new FamilyCase("SimpleHunt", 35023, 2, Set.of(799800, 799801)),
			new FamilyCase("SimpleTalk", 35045, 2, Set.of(799800, 799801)))
			.stream().map(row -> DynamicTest.dynamicTest(row.family() + " " + row.questId(), () -> {
				assertTrue(nativeLaneOwns(row.family(), row.questId()),
					() -> row.family() + " " + row.questId() + " must be routed by the native lane");
				List<Integer> declared = RetailClientRewardNpcs.defaultRewardNpcs().rewardNpcs(row.questId());
				assertEquals(row.npcCount(), declared.size(),
					"client-declared hand-in NPCs for " + row.questId());
				assertEquals(row.knownNpcs(), new java.util.TreeSet<>(declared),
					"hand-in NPC set for " + row.questId());
				if (row.family().equals("SimpleCollectItem")) {
					// 采集族：native 交付面必须逐元素等于客户端登记（多交付 NPC 展开的落地断言）。
					// Collect family: the native hand-in face must equal the client registry element-wise.
					assertEquals(row.knownNpcs(),
						new java.util.TreeSet<>(SimpleCollectItemHandler.instance().rewardNpcs(row.questId())),
						"native hand-in NPC set for " + row.questId());
				}
			}));
	}

	private static boolean nativeLaneOwns(String family, int questId) {
		return switch (family) {
			case "SimpleCollectItem" -> SimpleCollectItemHandler.instance().routes(questId);
			case "SimpleHunt" -> SimpleHuntHandler.instance().routes(questId);
			case "SimpleTalk" -> SimpleTalkHandler.instance().routes(questId);
			default -> false;
		};
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
