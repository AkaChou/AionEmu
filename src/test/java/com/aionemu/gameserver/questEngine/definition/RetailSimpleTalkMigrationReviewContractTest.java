package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真端交付物与 Aion 5.8 客户端按钮驱动的链式迁移合同。 /
 * Chain migration contracts driven by retail turn-in items and Aion 5.8 client buttons.
 * <p>
 * S2（quest-native-dispatch）：双块行（NPC_START + NPC_REPORT）的接取/交付由规范段接管——交付 =
 * NPC_REPORT 块 source 上的 {@code QUEST_SELECT}(31) 整组门直翻 REWARD，报告页 SELECT5、39/20002
 * 检查按钮对与 SELECT6 失败页随页链退场；接取 = 31 直发接取窗（页 4）。本类断言按该规范形重锚。
 * S2 gives the two canonical segments to double-block rows: the gated {@code QUEST_SELECT}(31) from the
 * report block source delivers, and the report page, its check buttons and the failure page retire.
 */
class RetailSimpleTalkMigrationReviewContractTest {

	private record CheckQuest(int id, int rewardNpc, List<QuestItemRequirement> items) {
	}

	private record DirectHandIn(int id, String source, int rewardNpc, int itemId) {
	}

	private static final List<CheckQuest> CHECK_QUESTS = List.of(
		new CheckQuest(3961, 798384, List.of(new QuestItemRequirement(182400001, 40000))),
		new CheckQuest(3962, 798384, List.of(new QuestItemRequirement(186000088, 1),
			new QuestItemRequirement(182400001, 50000))),
		new CheckQuest(3963, 798384, List.of(new QuestItemRequirement(186000089, 1),
			new QuestItemRequirement(182400001, 70000))),
		new CheckQuest(3964, 798384, List.of(new QuestItemRequirement(186000090, 1),
			new QuestItemRequirement(182400001, 90000))),
		new CheckQuest(4966, 798385, List.of(new QuestItemRequirement(182400001, 40000))),
		new CheckQuest(4967, 798385, List.of(new QuestItemRequirement(186000091, 1),
			new QuestItemRequirement(182400001, 50000))),
		new CheckQuest(4968, 798385, List.of(new QuestItemRequirement(186000092, 1),
			new QuestItemRequirement(182400001, 70000))),
		new CheckQuest(4969, 798385, List.of(new QuestItemRequirement(186000093, 1),
			new QuestItemRequirement(182400001, 90000)))
	);

	@Test
	void clientCheckButtonsConsumeEveryRetailRequirementAndRetireTheFailurePage() {
		for (CheckQuest quest : CHECK_QUESTS) {
			QuestDefinition definition = ProductionQuestDefinitions.definitionInOverlay(quest.id()).definition();
			// S2 规范形：交付 = NPC_REPORT 块 source（s1）上的 QUEST_SELECT(31)，整组 HasItem 门直翻
			// REWARD 并下发档位奖励窗；检查按钮对、SELECT5 报告页与 SELECT6 失败页随页链退场
			// （未集齐零路由，关窗兜底交 DialogService）。
			// S2 canonical delivery: the report block source's QUEST_SELECT(31) gated by the whole
			// hand-in set flips REWARD with the tiered window; the check pairs, the SELECT5 report page
			// and the SELECT6 failure page retire (an incomplete hand-in has no route).
			List<QuestCondition> conditions = quest.items().stream()
				.map(item -> (QuestCondition) new QuestCondition.HasItem(item.itemId(), item.count()))
				.toList();
			List<QuestAction> removals = quest.items().stream()
				.map(item -> (QuestAction) new QuestAction.RemoveItem(item.itemId(), item.count()))
				.toList();
			QuestTransition handIn = route(definition, "s1", quest.rewardNpc(), QuestDialogAction.QUEST_SELECT);
			assertEquals("reward", handIn.targetNode());
			assertEquals(conditions, handIn.conditions());
			assertEquals(removals, handIn.actions());
			assertNull(handIn.priority());
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(deliveryWindow(definition))), handIn.afterCommit());
			// 旧形退场（负控）：39/20002 检查按钮不再有分支，reward 态 SELECT5 入口也不再由服务端驱动。
			for (QuestDialogAction button : List.of(QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM,
				QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE)) {
				assertTrue(routes(definition, "s1", quest.rewardNpc(), button).isEmpty(),
					() -> quest.id() + " " + button + " branches retired");
			}
			assertTrue(routes(definition, "reward", quest.rewardNpc(), QuestDialogAction.QUEST_SELECT).isEmpty(),
				() -> quest.id() + " reward-state SELECT5 page-chain entry retired");
			// 关窗出口随规范接取段锚在 {unaccepted, 接取目标 started}；推进节点上的旧 SELECT6 出口退场。
			for (String source : List.of("unaccepted", "started")) {
				assertEquals(List.of(new AfterCommitAction.ShowQuestSelectionDialog(
					QuestDialogPage.SELECT_QUEST.id())),
					route(definition, source, quest.rewardNpc(), QuestDialogAction.FINISH_DIALOG).afterCommit(),
					() -> quest.id() + " " + source + " close exit");
			}
			assertTrue(routes(definition, "s1", quest.rewardNpc(), QuestDialogAction.FINISH_DIALOG).isEmpty(),
				() -> quest.id() + " select6 close exit retired");
			// 领奖态预览由完成块承担（1009 → 第 1 档奖励窗），页链的 reward 态 31 入口不再存在。
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
				route(definition, "reward", quest.rewardNpc(),
					QuestDialogAction.SELECT_QUEST_REWARD).afterCommit());
		}
	}

	@Test
	void directClientReportButtonsConsumeOnlyWorkItemsStillCarriedAtTheRetailReportStage() {
		for (DirectHandIn quest : List.of(
			new DirectHandIn(1909, "s1", 203099, 182206001),
			new DirectHandIn(3208, "s2", 805836, 182209088),
			new DirectHandIn(3209, "s1", 798331, 182209089),
			new DirectHandIn(4208, "s2", 805843, 182209103),
			new DirectHandIn(11077, "s2", 798903, 182214016))) {
			QuestDefinition definition = ProductionQuestDefinitions.definitionInOverlay(quest.id()).definition();
			// S2 规范形：1009 直报对（成功 priority 0 / 失败 priority 1）退场，物品门搬到 NPC_REPORT
			// 块 source 的 QUEST_SELECT(31)——条件/动作 = carried work-items（段内授予 − 段内移除）
			// 的整组，未集齐零路由。
			// S2 canonical: the 1009 direct-report pair retires and the gate moves onto the report block
			// source's QUEST_SELECT(31) — the whole carried work-item set (grants minus removals), with no
			// route at all while the hand-in is incomplete.
			QuestTransition handIn = route(definition, quest.source(), quest.rewardNpc(),
				QuestDialogAction.QUEST_SELECT);
			assertEquals(List.of(new QuestCondition.HasItem(quest.itemId(), 1)), handIn.conditions());
			assertEquals(List.of(new QuestAction.RemoveItem(quest.itemId(), 1)), handIn.actions());
			assertEquals("reward", handIn.targetNode());
			assertNull(handIn.priority());
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(deliveryWindow(definition))), handIn.afterCommit());
			assertTrue(routes(definition, quest.source(), quest.rewardNpc(),
				QuestDialogAction.SELECT_QUEST_REWARD).isEmpty(),
				() -> quest.id() + " 1009 direct report pair retired");
		}
		// 1971：give_item 已由推进段中间 NPC 的 SETPRO1（HAS_ITEM + REMOVE_ITEM）消耗 ⇒ carried 门为空，
		// 交付 = 空门 QUEST_SELECT(31) 直翻领奖态（与旧 1009 直报同为空门）。
		// 1971: the granted item is consumed by the mid NPC's SETPRO1, so the carried gate is empty and the
		// empty-gated QUEST_SELECT(31) delivers (the legacy 1009 direct report was empty-gated as well).
		QuestDefinition dye = ProductionQuestDefinitions.definitionInOverlay(1971).definition();
		QuestTransition report = route(dye, "k1", 203812, QuestDialogAction.QUEST_SELECT);
		assertEquals(List.of(), report.conditions());
		assertEquals(List.of(), report.actions());
		assertEquals("reward", report.targetNode());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(deliveryWindow(dye))), report.afterCommit());
		assertTrue(routes(dye, "k1", 203812, QuestDialogAction.SELECT_QUEST_REWARD).isEmpty(),
			"canonical removed the 1009 direct report route");
	}

	@Test
	void onlyTheRetailTalkNpcOpensTheClientContinuationFor11008() {
		QuestDefinition definition = ProductionQuestDefinitions.definitionInOverlay(11008).definition();
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			route(definition, "started", 798927, QuestDialogAction.QUEST_SELECT).afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			route(definition, "started", 798934, QuestDialogAction.QUEST_SELECT).afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2_1.id())),
			route(definition, "started", 798934, QuestDialogAction.SELECT2_1).afterCommit());
		assertEquals("s1", route(definition, "started", 798934, QuestDialogAction.SETPRO1).targetNode());
		// S2 规范形：1009 中转退场，交付 = NPC_REPORT 块 source（s1）的 QUEST_SELECT(31) 空门直翻
		// 领奖态 + 档位奖励窗（11008 无 item_check ⇒ 规范门为空）。
		// S2 canonical: the 1009 relay retires; the report block source's empty-gated QUEST_SELECT(31)
		// flips REWARD with the tiered window (11008 carries no item_check, so the gate is empty).
		QuestTransition handIn = route(definition, "s1", 798997, QuestDialogAction.QUEST_SELECT);
		assertEquals("reward", handIn.targetNode());
		assertEquals(List.of(), handIn.conditions());
		assertEquals(List.of(), handIn.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(deliveryWindow(definition))), handIn.afterCommit());
		assertTrue(routes(definition, "s1", 798997, QuestDialogAction.SELECT_QUEST_REWARD).isEmpty(),
			"canonical removed the 1009 relay");
	}

	/**
	 * 规范形交付窗页：按奖励组档位查表（QE-028），零奖励组回落固定窗 1——与合成器
	 * {@code deliveryWindowPage} 同口径。
	 * The canonical delivery window: the tier lookup (QE-028) with the fixed first window as the
	 * zero-group fallback, the same caliber as the compiler's {@code deliveryWindowPage}.
	 */
	private static int deliveryWindow(QuestDefinition definition) {
		return QuestDialogPage.rewardWindowForTier(definition.metadata().rewardGroups().size() - 1)
			.orElse(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1).id();
	}

	@Test
	void oldRewardSavesRecoverToTheClientRewardRow() {
		for (int questId : new int[] {1218, 11294, 11458}) {
			QuestDefinition definition = ProductionQuestDefinitions.definitionInOverlay(questId).definition();
			List<QuestTransition> recoveries = definition.transitions().stream()
				.filter(route -> route.sourceNode() == null && "reward".equals(route.targetNode()))
				.filter(route -> route.event() instanceof QuestEvent.EnterWorld)
				.toList();
			assertEquals(1, recoveries.size(), () -> questId + " reward recovery");
			QuestTransition route = recoveries.getFirst();
			assertEquals(List.of(new QuestCondition.StatusIs(QuestStatus.REWARD),
				new QuestCondition.QuestVariableIs("var0", 0)), route.conditions());
			assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), route.actions());
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), route.afterCommit());
		}
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		List<QuestTransition> matching = routes(definition, source, npcId, action);
		assertEquals(1, matching.size(), () -> definition.id() + " " + source + " " + npcId + " " + action);
		return matching.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(route -> source.equals(route.sourceNode()))
			.filter(route -> route.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && talk.dialogId() != null && talk.dialogId() == action.id())
			.toList();
	}
}
