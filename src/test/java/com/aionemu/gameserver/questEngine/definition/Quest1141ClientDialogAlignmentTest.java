package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 1141 将贝尔布亚的接取交接与酒桶报告、领奖 owner 分离。
 * Verifies quest 1141 keeps Belbua's acquisition handoff separate from the barrel report and reward owner.
 */
class Quest1141ClientDialogAlignmentTest {
	private static final int START_NPC = 730001;
	private static final int BARREL_NPC = 700122;

	@Test
	void keepsTheStartNpcSeparateFromTheBarrelReportAndRewardOwner() {
		QuestDefinition definition = load().definition();

		// P0-3 S1：SimpleTalk 接取切真端规范形——QUEST_SELECT 直发询问窗（页 4）；
		// select1 简报页（1011）与 ASK_QUEST_ACCEPT(1007) 中转随页链退场。
		// P0-3 S1: the SimpleTalk accept switches to the retail canonical shape — QUEST_SELECT
		// emits the ask-accept window (page 4); the select1 letter page (1011) and the
		// ASK_QUEST_ACCEPT(1007) hop retire with the page chain.
		assertPage(definition, "unaccepted", START_NPC, QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW);
		assertTrue(routes(definition, "started", START_NPC).stream()
			.allMatch(transition -> transition.event().equals(
				new QuestEvent.TalkToNpc(START_NPC, QuestDialogAction.FINISH_DIALOG.id()))));
		assertTrue(routes(definition, "reward", START_NPC).isEmpty());

		// P0c-24 裁定（真端对、XML 错）：客户端模板索引声明酒桶(700122)的报告开启动作 =
		// QUEST_SELECT(31)（对象也走对话开启），与族编译一致；遗留 XML 的 USE_OBJECT 开启是
		// 手工推断。1141 真端行无掉落——validator 的 ACTION_ITEM_USE 门要求是掉落驱动
		// （quest_use_item 掉落才需要），无掉落无门（P0c-16 铁 KEEP 的再翻案）。
		// P0c-24 adjudication (retail-right, XML-wrong): the client template index declares the
		// barrel's report opener as QUEST_SELECT(31) — objects open via the dialog action too,
		// matching the family compile. The legacy USE_OBJECT opener was inferred. The retail row
		// has no drops; the validator's gate requirement is drop-driven, so no gate applies
		// (reversal of the P0c-16 firm KEEP).
		// P0-3 S1：交付切真端规范形——QUEST_SELECT 带门（本行无 item_check = 空门）直翻 REWARD
		// 并下发档位奖励窗；报告页 SELECT5(2375) 与 SELECT_QUEST_REWARD(1009) 中转随页链退场，
		// 未集齐时零路由（关窗兜底交 DialogService）。
		// P0-3 S1: the delivery switches to the retail canonical shape — the gated QUEST_SELECT
		// (no item_check on this row = empty gate) flips REWARD and shows the tiered reward
		// window; the SELECT5 report page and the SELECT_QUEST_REWARD(1009) hop retire with the
		// page chain, so an incomplete hand-in has no route at all.
		int deliveryWindow = QuestDialogPage.rewardWindowForTier(
			definition.metadata().rewardGroups().size() - 1)
			.orElse(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1).id();
		QuestTransition delivery = route(definition, "started", BARREL_NPC,
			QuestDialogAction.QUEST_SELECT);
		assertEquals("reward", delivery.targetNode());
		assertEquals(List.of(), delivery.conditions());
		assertEquals(List.of(), delivery.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(deliveryWindow)), delivery.afterCommit());
		assertNoLegacyPageChainResidue(definition, BARREL_NPC);

		QuestTransition completion = route(definition, "reward", BARREL_NPC,
			QuestDialogAction.SELECTED_QUEST_REWARD1);
		assertEquals("complete", completion.targetNode());
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
		QuestDialogAction action, QuestDialogPage page) {
		QuestTransition transition = route(definition, source, npcId, action);
		assertEquals(source, transition.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals(source))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId)
			.toList();
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
		QuestDialogAction action) {
		return routes(definition, source, npcId).stream()
			.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(npcId, action.id())))
			.findFirst().orElseThrow();
	}

	/**
	 * 旧页链零残留（P0-3 S1）：交付段（started→reward）不得有带优先级的路由，定义内不得再下发
	 * SELECT5/SELECT6 页，started 态不得残留 1009/39/20002 交付路由（完成流的 reward→complete
	 * 类/槽位路由本来就带优先级，不是残留）。
	 * Zero legacy page-chain residue (P0-3 S1): no priority-carrying delivery route, no SELECT5/SELECT6
	 * page push, and no started-state 1009/39/20002 hand-in route (the completion flow's
	 * reward→complete class/slot routes legitimately carry priorities and are not residue).
	 */
	private static void assertNoLegacyPageChainResidue(QuestDefinition definition, int rewardNpc) {
		assertTrue(definition.transitions().stream().noneMatch(transition ->
			"started".equals(transition.sourceNode()) && "reward".equals(transition.targetNode())
				&& transition.priority() != null),
			"the started->reward delivery segment must be priority-free");
		assertTrue(definition.transitions().stream().noneMatch(transition ->
			transition.afterCommit().stream().anyMatch(action ->
				action instanceof AfterCommitAction.ShowQuestDialog page
					&& (page.dialogId() == QuestDialogPage.SELECT5.id()
						|| page.dialogId() == QuestDialogPage.SELECT6.id()))),
			"the retired SELECT5/SELECT6 pages must not be pushed");
		assertTrue(definition.transitions().stream().noneMatch(transition ->
			"started".equals(transition.sourceNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == rewardNpc
				&& talk.dialogId() != null
				&& (talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id()
					|| talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()
					|| talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE.id())),
			"the started-state 1009/39/20002 hand-in routes must not survive");
	}

	private static CompiledQuestDefinition load() {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		return ProductionQuestDefinitions.definition(1141);
	}
}
