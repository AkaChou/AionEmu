package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
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
		new QuestCase(18509, 799523, 799524,
			List.of(new ObjectDrop(700853, 182212008, false)), true),
		new QuestCase(28503, 799523, 804605,
			List.of(ObjectDrop.questGate(700834, 182212016)), true),
		new QuestCase(28509, 799523, 799524,
			List.of(new ObjectDrop(700853, 182212020, false)), true));

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

		// P0-3 S1：SimpleTalk 接取/交付切真端规范形（页 4 / 分档窗）——39/20002 检查对与 SELECT6
		// 空报告页整体退场（未集齐零路由，关窗兜底交 DialogService）；门落在交付边 conditions() 上，
		// 且是真端 collect_item1 整组（quest_18509a×1）。
		// P0-3 S1: the SimpleTalk accept/delivery segments take the retail canonical shape (page 4 /
		// tiered window) — the 39/20002 check pair and the SELECT6 empty-report page are retired (an
		// incomplete hand-in has no route; DialogService closes the window), and the gate sits on the
		// delivery edge's conditions() as the whole retail collect_item1 group (quest_18509a x1).
		QuestTransition deliver = dialog(definition, "started", "reward", 799524,
			QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new QuestCondition.HasItem(182212008, 1)), deliver.conditions());
		assertEquals(List.of(new QuestAction.RemoveItem(182212008, 1)), deliver.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			deliver.afterCommit());
		assertFalse(definition.definition().transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == 799524
				&& talk.dialogId() != null
				&& (talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()
					|| talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE.id())),
			"canonical removed the 39/20002 check pair at the report npc");
		assertFalse(definition.definition().transitions().stream().anyMatch(transition ->
			transition.afterCommit().stream().anyMatch(action ->
				action instanceof AfterCommitAction.ShowQuestDialog page
					&& (page.dialogId() == QuestDialogPage.SELECT5.id()
						|| page.dialogId() == QuestDialogPage.SELECT6.id()))),
			"canonical removed the SELECT5/SELECT6 report pages");

		// P0c-22 裁定（真端/客户端对、XML 错）：客户端 1008 按钮字面 = "结束对话"（page-action-map
		// 全页一致），报告 NPC 关窗按族形 reportNpcExit 发 CloseDialog；遗留 XML 的
		// ShowQuestSelectionDialog(SELECT_QUEST) 是手工推断。真端掉落 IDNovice_WoodenBox(700853)
		// 的 ai=chest 不在 quest_use_item 交互对象合同内（validator 只查 quest_use_item），
		// 开箱走 chest AI 流，无需任务路由门。
		// P0c-22 adjudication (client-literal, XML-wrong): the client 1008 button reads "end
		// conversation" across all pages, so the report NPC close emits CloseDialog per the
		// family reportNpcExit shape; the legacy XML's ShowQuestSelectionDialog was hand-made.
		// The box drop IDNovice_WoodenBox (700853) has ai=chest — outside the quest_use_item
		// interaction contract (the validator scopes quest_use_item only), so opening runs on
		// the chest AI flow and needs no quest-engine gate.
		QuestTransition emptyReportFinish = dialog(definition, "started", "started", 799524,
			QuestDialogAction.FINISH_DIALOG.id());
		assertEquals(List.of(new AfterCommitAction.CloseDialog()), emptyReportFinish.afterCommit());
		assertFalse(definition.definition().transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.CanAct canAct && canAct.templateId() == 700853),
			"chest-ai box must not carry a quest ACTION_ITEM_USE gate");
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

		// P0-3 S1：SimpleTalk 接取/交付切真端规范形（页 4 / 分档窗）——39/20002 检查对与 SELECT6
		// 空报告页整体退场（未集齐零路由，关窗兜底交 DialogService）；门落在交付边 conditions() 上，
		// 且是真端 collect_item1 整组（quest_28509a×1）。
		// P0-3 S1: the SimpleTalk accept/delivery segments take the retail canonical shape (page 4 /
		// tiered window) — the 39/20002 check pair and the SELECT6 empty-report page are retired (an
		// incomplete hand-in has no route; DialogService closes the window), and the gate sits on the
		// delivery edge's conditions() as the whole retail collect_item1 group (quest_28509a x1).
		QuestTransition deliver = dialog(definition, "started", "reward", 799524,
			QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new QuestCondition.HasItem(182212020, 1)), deliver.conditions());
		assertEquals(List.of(new QuestAction.RemoveItem(182212020, 1)), deliver.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			deliver.afterCommit());
		assertFalse(definition.definition().transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == 799524
				&& talk.dialogId() != null
				&& (talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()
					|| talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE.id())),
			"canonical removed the 39/20002 check pair at the report npc");
		assertFalse(definition.definition().transitions().stream().anyMatch(transition ->
			transition.afterCommit().stream().anyMatch(action ->
				action instanceof AfterCommitAction.ShowQuestDialog page
					&& (page.dialogId() == QuestDialogPage.SELECT5.id()
						|| page.dialogId() == QuestDialogPage.SELECT6.id()))),
			"canonical removed the SELECT5/SELECT6 report pages");

		// P0c-22 裁定同 18509：客户端 1008 = "结束对话" → CloseDialog（族形 reportNpcExit）；
		// 遗留 XML 的 ShowQuestSelectionDialog 是手工推断。IDNovice_WoodenBox(700853) 的
		// ai=chest 不在 quest_use_item 合同内，无需门。
		// P0c-22 adjudication same as 18509: the client 1008 button reads "end conversation" ->
		// CloseDialog (family reportNpcExit); the legacy ShowQuestSelectionDialog was hand-made.
		// IDNovice_WoodenBox (700853) has ai=chest — outside the quest_use_item contract, no gate.
		QuestTransition emptyReportFinish = dialog(definition, "started", "started", 799524,
			QuestDialogAction.FINISH_DIALOG.id());
		assertEquals(List.of(new AfterCommitAction.CloseDialog()), emptyReportFinish.afterCommit());
		assertFalse(definition.definition().transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.CanAct canAct && canAct.templateId() == 700853),
			"chest-ai box must not carry a quest ACTION_ITEM_USE gate");
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

		// P0-3 S1：SimpleTalk 接取/交付切真端规范形（页 4 / 分档窗）——交付 NPC 203106 上的 39/20002
		// 检查对随页链退场，交付 = canonicalDelivery：QUEST_SELECT(started→reward) 带真端
		// collect_item1..3 整组门（quest_18505a/b/c×1）直翻领奖并下发单档奖励窗 1。
		// P0-3 S1: the SimpleTalk accept/delivery segments take the retail canonical shape (page 4 /
		// tiered window) — the 39/20002 check pair at the turn-in npc 203106 is retired and the delivery
		// is canonicalDelivery: a gated QUEST_SELECT(started->reward) carrying the whole retail
		// collect_item1..3 group (quest_18505a/b/c x1) that flips REWARD and shows the reward window 1.
		assertFalse(hasDialog(definition, "started", 203106, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
		assertFalse(hasDialog(definition, "started", 203166, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));

		assertFalse(hasDialog(definition, "started", 203106, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE.id()));
		assertFalse(hasDialog(definition, "started", 203166, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE.id()));

		QuestTransition deliver = dialog(definition, "started", "reward", 203106,
			QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(
			new QuestCondition.HasItem(182212005, 1),
			new QuestCondition.HasItem(182212006, 1),
			new QuestCondition.HasItem(182212007, 1)), deliver.conditions());
		assertEquals(List.of(
			new QuestAction.RemoveItem(182212005, 1),
			new QuestAction.RemoveItem(182212006, 1),
			new QuestAction.RemoveItem(182212007, 1)), deliver.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			deliver.afterCommit());
		assertFalse(definition.definition().transitions().stream().anyMatch(transition ->
			transition.afterCommit().stream().anyMatch(action ->
				action instanceof AfterCommitAction.ShowQuestDialog page
					&& (page.dialogId() == QuestDialogPage.SELECT5.id()
						|| page.dialogId() == QuestDialogPage.SELECT6.id()))),
			"canonical removed the SELECT5/SELECT6 report pages");

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
