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
	 * S3a（quest-native-dispatch）重锚：1152 为 SimpleTalk 链式行（retention owner = RETAIL_TABLE，登记块只有
	 * NPC_START/NPC_COMPLETE、无 NPC_REPORT），接取段走 {@code canonicalAcceptFlow}、交付段仍是登记表逐字回放。
	 * 本测试锁定 canonical 接取形（询问窗 / 两条提交边 / 关窗出口）与原两跳物品契约；旧页梯（1007 中转、
	 * select1 续页）在接取 NPC 上已无路由。
	 * S3a re-anchor: the accept segment is canonical while the delivery segment stays the verbatim registry replay;
	 * this test locks both the canonical accept shape and the original two-hop item contract.
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

		assertPage(definition, "started", DELIVERY_NPC, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
		assertPage(definition, "started", DELIVERY_NPC, QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
		QuestTransition recipe = route(definition, "started", DELIVERY_NPC, QuestDialogAction.SETPRO1);
		assertEquals("pepper", recipe.targetNode());
		assertEquals(List.of(new QuestAction.RemoveItem(ODELLA, 1)), recipe.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), recipe.afterCommit());

		assertPage(definition, "pepper", DELIVERY_NPC, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT5);
		List<QuestTransition> checks = routes(definition, "pepper", DELIVERY_NPC,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM);
		assertEquals(2, checks.size());
		QuestTransition success = priority(checks, 0);
		QuestTransition failure = priority(checks, 1);
		assertEquals("reward", success.targetNode());
		assertEquals(List.of(new QuestCondition.HasItem(PEPPER, 1)), success.conditions());
		assertEquals(List.of(new QuestAction.RemoveItem(PEPPER, 1)), success.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH), new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), success.afterCommit());
		assertEquals("pepper", failure.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT6.id())),
			failure.afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			route(definition, "pepper", DELIVERY_NPC, QuestDialogAction.FINISH_DIALOG).afterCommit());

		QuestTransition completion = route(definition, "reward", DELIVERY_NPC,
			QuestDialogAction.SELECTED_QUEST_REWARD1);
		assertEquals("complete", completion.targetNode());
		assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(0)));
		assertTrue(routes(definition, "reward", START_NPC).isEmpty());
		for (QuestDialogAction action : List.of(QuestDialogAction.QUEST_SELECT, QuestDialogAction.SELECT2_1,
				QuestDialogAction.SETPRO1, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM)) {
			assertTrue(routes(definition, "started", START_NPC, action).isEmpty());
			assertTrue(routes(definition, "pepper", START_NPC, action).isEmpty());
		}
	}

	/**
	 * 1152 于 P0c-34 退役（item_check 门通道落地后入台）：定义改由真端文件驱动合成，断言口径不变；
	 * 生产视图 = XML 目录 + 真端 overlay，门路由（39/20002 对）由链登记 I 记录展开。
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

	private static QuestTransition priority(List<QuestTransition> transitions, int priority) {
		return transitions.stream().filter(transition -> Integer.valueOf(priority).equals(transition.priority()))
			.findFirst().orElseThrow();
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
