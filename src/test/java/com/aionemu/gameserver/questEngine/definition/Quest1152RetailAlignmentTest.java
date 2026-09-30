package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest1152RetailAlignmentTest {
	private static final int START_NPC = 203132;
	private static final int DELIVERY_NPC = 203130;
	private static final int ODELLA = 182200526;
	private static final int PEPPER = 169400112;

	/**
	 * S3a（quest-native-dispatch）重锚 + 页梯退役重锚：1152 为 SimpleTalk 链式行（retention owner = RETAIL_TABLE），
	 * 接取段走 {@code canonicalAcceptFlow}；交付段现在是**规范交付**——阶段首屏 = 客户端 select2 页，
	 * 行内翻页（select2_1）是客户端本地行为（服务端零路由），推进由 `SETPRO1` ≡ `SET_SUCCEED` 两条同义边承担，
	 * 领奖由带门 `QUEST_SELECT` 直翻领奖态 + 档位奖励窗完成；39 检查中转与 select5 报告页已退场。
	 * <p>
	 * S3a + ladder-retirement re-anchor: the accept segment stays canonical; the delivery segment now derives its
	 * stage head from the client contract, keeps the in-stage flip client-local, advances through SETPRO1 ==
	 * SET_SUCCEED, and opens the reward window through the gated QUEST_SELECT delivery. The 39 relay and the
	 * select5 report page are retired.
	 */
	@Test
	void followsTheClientChefDialogAndLegacyTwoStepItemContract() throws Exception {
		QuestDefinition definition = compile();

		// 接取段（canonical）：QUEST_SELECT 直发接取询问窗（页 4，SHOW_ASK_QUEST_ACCEPT_WINDOW）；物品契约第一跳
		// 落在 1002/20000 两条提交边上——同条件 StartEligible、同发物 ODELLA，仅 after 出口不同（开 1003 页 / 关窗）。
		// Accept segment (canonical): the ask window straight off QUEST_SELECT; the item contract's first hop sits
		// on both commit forms (1002/20000), which differ only in their exit (open page 1003 / close the dialog).
		assertPage(definition, "unaccepted", START_NPC, QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW);
		QuestTransition accept = route(definition, "unaccepted", START_NPC, QuestDialogAction.QUEST_ACCEPT_1);
		assertEquals("started", accept.targetNode());
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertTrue(accept.actions().contains(new QuestAction.GiveItem(ODELLA, 1)));
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())), accept.afterCommit());
		QuestTransition acceptSimple = route(definition, "unaccepted", START_NPC,
			QuestDialogAction.QUEST_ACCEPT_SIMPLE);
		assertEquals("started", acceptSimple.targetNode());
		assertEquals(accept.conditions(), acceptSimple.conditions());
		assertEquals(accept.actions(), acceptSimple.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), acceptSimple.afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			route(definition, "unaccepted", START_NPC, QuestDialogAction.FINISH_DIALOG).afterCommit());
		// 旧页梯退场锁定：1007 中转与 select1 续页在接取 NPC 上不得再有路由（策略 A + 零残留守卫）。
		// Retired-ladder lock: the 1007 relay and the select1 continuation carry no route on the accept npc.
		for (QuestDialogAction retired : List.of(QuestDialogAction.ASK_QUEST_ACCEPT, QuestDialogAction.SELECT1_1)) {
			assertTrue(routes(definition, "unaccepted", START_NPC, retired).isEmpty());
		}
		assertTrue(routes(definition, "unaccepted", DELIVERY_NPC).isEmpty());

		// 交付段：阶段首屏 = 客户端声明的 select2 页；行内翻页 select2_1 是客户端本地行为（服务端零路由）。
		// Delivery segment: the derived select2 head page; the select2_1 flip stays client-local (no route).
		assertPage(definition, "started", DELIVERY_NPC, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
		assertTrue(routes(definition, "started", DELIVERY_NPC, QuestDialogAction.SELECT2_1).isEmpty(),
			"the retired in-stage flip must carry no route on the delivery npc");
		QuestTransition recipe = route(definition, "started", DELIVERY_NPC, QuestDialogAction.SETPRO1);
		QuestTransition succeeded = route(definition, "started", DELIVERY_NPC, QuestDialogAction.SET_SUCCEED);
		assertEquals("step1", recipe.targetNode());
		assertEquals(recipe.actions(), succeeded.actions(), "SETPRO1 and SET_SUCCEED must share the actions");
		assertEquals(recipe.afterCommit(), succeeded.afterCommit(),
			"SETPRO1 and SET_SUCCEED must share the after-commit");
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1), new QuestAction.RemoveItem(ODELLA, 1)),
			recipe.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH), new AfterCommitAction.CloseDialog()),
			recipe.afterCommit());

		// 规范交付：带门 QUEST_SELECT 直翻领奖态 + 档位奖励窗；39 检查中转（CHECK_USER_HAS_QUEST_ITEM）与
		// select5 报告页退场——该阶段在交付 NPC 上只剩这一条交付边与关窗出口。
		// Canonical delivery: the gated QUEST_SELECT into the reward window; the 39 relay and the select5
		// report page are retired (no route at all on this stage).
		List<QuestTransition> delivery = routes(definition, "step1", DELIVERY_NPC, QuestDialogAction.QUEST_SELECT);
		assertEquals(1, delivery.size(), "the reward-stage delivery must be unique");
		assertEquals("reward", delivery.getFirst().targetNode());
		assertEquals(List.of(new QuestCondition.HasItem(PEPPER, 1)), delivery.getFirst().conditions());
		assertEquals(List.of(new QuestAction.RemoveItem(PEPPER, 1)), delivery.getFirst().actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH), new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), delivery.getFirst().afterCommit());
		assertTrue(routes(definition, "step1", DELIVERY_NPC, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM).isEmpty(),
			"the retired 39 check relay must carry no route");
		// 交付阶段的关窗出口：8.8 起该阶段页尾是关窗（不再回任务列表），沿用客户端页面按钮。
		// The close-dialog exit stays on the delivery stage (no quest-list re-open).
		assertEquals(List.of(new AfterCommitAction.CloseDialog()),
			route(definition, "step1", DELIVERY_NPC, QuestDialogAction.FINISH_DIALOG).afterCommit());

		// 领奖态：奖励窗载体（QUEST_SELECT 与 1009 都重发窗 1）与完成边留在交付 NPC。
		// Reward state: the window carrier (QUEST_SELECT and 1009 both reopen window 1) lives on the delivery npc.
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			route(definition, "reward", DELIVERY_NPC, QuestDialogAction.QUEST_SELECT).afterCommit());
		QuestTransition completion = route(definition, "reward", DELIVERY_NPC,
			QuestDialogAction.SELECTED_QUEST_REWARD1);
		assertEquals("complete", completion.targetNode());
		assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(0)));
		assertTrue(routes(definition, "reward", START_NPC).isEmpty());
		for (QuestDialogAction action : List.of(QuestDialogAction.QUEST_SELECT, QuestDialogAction.SETPRO1,
				QuestDialogAction.SET_SUCCEED, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM)) {
			assertTrue(routes(definition, "started", START_NPC, action).isEmpty());
			assertTrue(routes(definition, "step1", START_NPC, action).isEmpty());
		}
	}

	/**
	 * 1152 于 P0c-34 退役（item_check 门通道落地后入台）：定义改由真端文件驱动合成，断言口径不变；
	 * 生产视图 = XML 目录 + 真端 overlay，门路由由链登记展开，推进按钮改为客户端结果页同义集。
	 * Quest 1152 was retired in P0c-34; the same assertions now run against the retail-driven
	 * production overlay view.
	 */
	private static QuestDefinition compile() {
		return RetailQuestDriver.overlay(QuestDefinitionDirectoryLoader.compile(
			Quest1152RetailAlignmentTest.class.getClassLoader()))
			.findExecutable(1152).orElseThrow(() -> new IllegalStateException("missing 1152"))
			.definition();
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())),
			route(definition, source, npcId, action).afterCommit());
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		return routes(definition, source, npcId, action).stream().findFirst().orElseThrow();
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
}
