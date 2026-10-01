package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定哈拉梅尔物品采集任务的交互物门控与交付 NPC。
 * Locks interaction-object gates and turn-in NPCs for Haramel item-collecting quests.
 */
class QuestHaramelItemCollectingRegressionTest {
	private static final List<QuestCase> CASES = List.of(
		new QuestCase(18501, 799522, 799523,
			List.of(ObjectDrop.questGate(700833, 182212001), ObjectDrop.questGate(700951, 182212002)), true),
		new QuestCase(28501, 799522, 799523,
			List.of(ObjectDrop.questGate(700833, 182212013), ObjectDrop.questGate(700951, 182212014)), true),
		new QuestCase(18503, 799523, 203166,
			List.of(ObjectDrop.questGate(700834, 182212004)), true),
		new QuestCase(28503, 799523, 804605,
			List.of(ObjectDrop.questGate(700834, 182212016)), true));

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
			if (questCase.canonicalTurnIn()) {
				// P0-2/P0-3 规范形交付：QUEST_SELECT 带真端整组 HasItem 门控直翻 REWARD 并下发档位奖励窗；
				// 39/20002 检查对与 SELECT5 报告页随页链删除（SimpleCollectItem 与 SimpleTalk 族同形）。
				// Canonical since P0-2/P0-3 (the SimpleCollectItem and SimpleTalk families share one
				// shape): QUEST_SELECT gated by the whole retail hand-in set flips REWARD and shows the
				// tiered window; the 39/20002 check pairs and the SELECT5 report page are gone.
				// route() 只匹配 started→started；规范形交付边是 started→reward，单独查。
				// route() only matches started->started; the canonical delivery edge is
				// started->reward, so look it up directly.
				QuestTransition deliver = definition.definition().transitions().stream()
					.filter(transition -> "started".equals(transition.sourceNode())
						&& transition.event() instanceof QuestEvent.TalkToNpc talk
						&& talk.npcId() == questCase.turnInNpcId()
						&& QuestEvent.matches(transition.event(), new QuestEvent.TalkToNpc(questCase.turnInNpcId(),
							QuestDialogAction.QUEST_SELECT.id())))
					.findFirst().orElseThrow();
				assertEquals("reward", deliver.targetNode(),
					"canonical delivery target for quest " + questCase.questId());
				assertNull(deliver.priority(), "canonical delivery priority for quest " + questCase.questId());
				// P0-3 S1：门必须落在交付边的 conditions() 上，且是真端 collect_item* 整组（同序同数量）；
				// SELECT5 报告页与 SELECT6 失败页一律不得残留（未集齐零路由，关窗兜底交 DialogService）。
				// P0-3 S1: the gate must sit on the delivery edge's conditions() and be the whole retail
				// collect_item* group (same order and counts); the SELECT5 report page and the SELECT6
				// failure page must not remain (an incomplete hand-in has no route).
				List<QuestCondition> collectGate = definition.definition().metadata().itemRequirements()
					.stream()
					.map(item -> (QuestCondition) new QuestCondition.HasItem(item.itemId(), item.count()))
					.toList();
				assertFalse(collectGate.isEmpty(), "retail collect gate for quest " + questCase.questId());
				assertEquals(collectGate, deliver.conditions(),
					"canonical delivery gates on the whole retail collect group for quest "
						+ questCase.questId());
				assertTrue(deliver.conditions().stream()
						.allMatch(condition -> condition instanceof QuestCondition.HasItem),
					"canonical delivery gates on hand-ins for quest " + questCase.questId());
				assertFalse(definition.definition().transitions().stream().anyMatch(transition ->
						transition.afterCommit().stream().anyMatch(action ->
							action instanceof AfterCommitAction.ShowQuestDialog page
								&& (page.dialogId() == QuestDialogPage.SELECT5.id()
									|| page.dialogId() == QuestDialogPage.SELECT6.id()))),
					"retired SELECT5/SELECT6 report pages for quest " + questCase.questId());
				assertFalse(hasDialog(definition, "started", questCase.turnInNpcId(),
					QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()),
					"canonical removed the turn-in item-check route for quest " + questCase.questId());
				assertFalse(hasDialog(definition, "started", questCase.turnInNpcId(),
					QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE.id()),
					"canonical removed the turn-in simple item-check route for quest " + questCase.questId());
			} else {
				assertTrue(hasDialog(definition, "started", questCase.turnInNpcId(),
					QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()),
					"turn-in item-check route for quest " + questCase.questId());
				assertTrue(hasDialog(definition, "started", questCase.turnInNpcId(),
					QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE.id()),
					"turn-in simple item-check route for quest " + questCase.questId());
			}
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
				// P0c-22：门对断言仅适用于 ai=quest_use_item 的交互对象（validator 合同域）；
				// IDNovice_WoodenBox(700853) ai=chest 开箱走 chest AI 流，不需要任务路由门。
				// P0c-22: the gate-pair asserts apply only to ai=quest_use_item objects (the
				// validator's contract scope); the chest-ai wooden box opens via the chest AI
				// flow and needs no quest-engine gate.
				if (objectDrop.needsGate()) {
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
				}

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

	/**
	 * 哈拉梅尔 native 行的真端表事实（P3 重锚，计划 §8.9）：18505/18509/28509 自 SimpleTalk 切换批起由
	 * native 车道直驱，typed 定义退出生产视图，交付面改锚「真端表行 + quest.xml collect_item 整组门」；
	 * 接取/进行中/奖励窗的对话页阶梯（4/10/5/1008）由族级门禁 {@code SimpleTalkNativeFamilyGateTest} 逐行守。
	 * <p>
	 * Native-lane retail facts for the Haramel rows (P3 re-anchor, plan §8.9): 18505/18509/28509 moved to the
	 * native lane, so their hand-in contract is anchored on the retail row plus the whole quest.xml
	 * collect_item group; the accept/in-progress/reward dialog ladder is guarded per row by the family gate.
	 */
	@Test
	void nativeHaramelRowsExposeTheirTurnInNpcAndCollectGate() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		// 18505：接取 Zephyros(203166) / 交付 Alaus(203106)，交付门 = collect_item1..3 整组。
		// 18505: acquired Zephyros(203166) / reward Alaus(203106), gate = the whole collect_item1..3 group.
		assertTrue(handler.routes(18505), "18505 必须由 native 车道路由");
		assertEquals(203166, handler.acquireNpc(18505), "18505 接取 NPC（Zephyros）");
		assertEquals(203106, handler.rewardNpc(18505), "18505 交付 NPC（Alaus）");
		assertEquals(0, handler.relayCount(18505), "18505 是单步行");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(182212005, 1),
			new SimpleTalkHandler.ItemStack(182212006, 1), new SimpleTalkHandler.ItemStack(182212007, 1)),
			handler.workItems(18505), "18505 交付门 = quest.xml collect_item 整组（同序同数量）");
		assertFalse(handler.unresolvedGate(18505), "18505 门必须可解");
		// 18509 / 28509：接取 Shugo_IDNovice_2(799523) / 交付 Shugo_IDNovice_3(799524)，单物门。
		// 18509 / 28509: acquired Shugo_IDNovice_2(799523) / reward Shugo_IDNovice_3(799524), one-item gate.
		for (int[] row : new int[][] {{18509, 182212008}, {28509, 182212020}}) {
			int questId = row[0];
			int gateItem = row[1];
			assertTrue(handler.routes(questId), "quest " + questId + " 必须由 native 车道路由");
			assertEquals(799523, handler.acquireNpc(questId), "quest " + questId + " 接取 NPC");
			assertEquals(799524, handler.rewardNpc(questId), "quest " + questId + " 交付 NPC");
			assertEquals(0, handler.relayCount(questId), "quest " + questId + " 是单步行");
			assertTrue(handler.requireRow(questId).itemCheck(), "quest " + questId + " 必须声明 item_check");
			assertEquals(List.of(new SimpleTalkHandler.ItemStack(gateItem, 1)), handler.workItems(questId),
				"quest " + questId + " 交付门物品（quest.xml collect_item1）");
			assertFalse(handler.unresolvedGate(questId), "quest " + questId + " 门必须可解");
		}
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
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		return ProductionQuestDefinitions.definition(questId);
	}

	/** canonicalTurnIn = P0-2/P0-3 规范形交付：QUEST_SELECT+真端整组 HasItem 直翻 REWARD
	 * （SimpleCollectItem 与 SimpleTalk 族同形）。 */
	/** canonicalTurnIn = the canonical delivery since P0-2/P0-3 (SimpleCollectItem and SimpleTalk). */
	private record QuestCase(int questId, int startNpcId, int turnInNpcId, List<ObjectDrop> objectDrops,
			boolean canonicalTurnIn) {
	}

	private record ObjectDrop(int objectId, int itemId, boolean needsGate) {

		/** ai=quest_use_item 的交互对象（validator 合同域）。 / An ai=quest_use_item object. */
		static ObjectDrop questGate(int objectId, int itemId) {
			return new ObjectDrop(objectId, itemId, true);
		}
	}
}
