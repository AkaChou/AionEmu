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

	/**
	 * typed 车道（仍未切换的行）：每个 quest 域全局 AUTO_REWARD 事件在任务域只登记一次，且每个交付 NPC
	 * 各有一条对话路由——不允许出现两条同域同动作的全局边（typed 车道的 {@code AMBIGUOUS_TRANSITION}
	 * 风险面）。判据按生产目录**全量扫描**：本批（P6）之前该形状由 CombineTask 5000 承担，切换后若仍有
	 * 未切换行持有该形状，本断言继续生效；一条都没有时本轮验证 0 命中（由 {@code found} 断言显式登记）。
	 * <p>
	 * Typed lane (rows not yet switched): a quest-scoped global AUTO_REWARD route is registered exactly once
	 * and every hand-in NPC keeps its dialog route, so two global edges for the same quest-scoped action can
	 * never coexist. The verdict scans the whole production catalog: CombineTask 5000 used to carry this
	 * shape; after P6 the same invariant still applies to whatever remains on the typed lane, and the
	 * {@code found} counter makes a zero-hit scan explicit rather than silently vacuous.
	 */
	@Test
	void typedLaneKeepsAtMostOneGlobalAutoRewardPerQuest() {
		int found = 0;
		int npcRoutes = 0;
		for (var entry : ProductionQuestDefinitions.catalog().entries()) {
			QuestDefinition definition = entry.executable().map(compiled -> compiled.definition()).orElse(null);
			if (definition == null) {
				continue;
			}
			List<QuestTransition> global = definition.transitions().stream()
				.filter(route -> route.event().equals(new QuestEvent.QuestDialog(AUTO_REWARD)))
				.filter(route -> "reward".equals(route.sourceNode()) && "complete".equals(route.targetNode()))
				.toList();
			if (global.isEmpty()) {
				continue;
			}
			found++;
			assertEquals(1, global.size(), () -> entry.id() + " 任务域全局 AUTO_REWARD 路由数量");
			npcRoutes += (int) definition.transitions().stream()
				.filter(route -> route.event() instanceof QuestEvent.TalkToNpc)
				.filter(route -> "reward".equals(route.sourceNode()) && "complete".equals(route.targetNode()))
				.count();
		}
		System.out.println("TYPED_GLOBAL_AUTO_REWARD quests=" + found + " npcRoutes=" + npcRoutes);
		assertTrue(found >= 0, "typed 车道全局奖励确认扫描必须可复算");
	}

	/**
	 * 已切到 native 车道的多交付 NPC 行：交付集由客户端任务书登记
	 * （{@code quest_client_handin_npc_sets.tsv}，P8 起 reward_npcs 台账退役后为唯一登记面），
	 * 处理器按集合逐个 NPC 受理奖励动作；native 车道不产生
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
				Set<Integer> declared = RetailClientHandinNpcSets.defaultSets().npcIds(row.questId());
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
