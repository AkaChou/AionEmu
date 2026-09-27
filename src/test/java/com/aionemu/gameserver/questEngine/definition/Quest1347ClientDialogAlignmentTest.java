package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.aionemu.gameserver.questEngine.definition.QuestProjectionNodes.node;
import static com.aionemu.gameserver.questEngine.definition.QuestProjectionNodes.assertTarget;
import static com.aionemu.gameserver.questEngine.definition.QuestProjectionNodes.singleTalkRoute;
import static com.aionemu.gameserver.questEngine.definition.QuestProjectionNodes.talkRoutes;
import static com.aionemu.gameserver.questEngine.definition.QuestProjectionNodes.targetNpcs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务 1347 的报告 NPC（203966）与奖励归属合同（P0-2 规范形：报告页 1352 与 1009 中转不再由服务端驱动）。
 * Locks quest 1347's report NPC (203966) and reward-owner contract (canonical since P0-2: the 1352
 * report page and the 1009 hop are no longer server-driven).
 * <p>legacy 合同（origin/history zz_retail_simple_quests.xml）写明：start_npc=203965、
 * end_npc=203966、report_owner=203966、reward_page=5。客户端 QUEST_Q1347.html 只在
 * 203966 这条链上提供 1352 页（按钮 1009「报告结果。」）；规范形后该页链不再下发，交付收敛为
 * 满段 QUEST_SELECT → 分档奖励窗。此前 XML 把 NPC_REPORT 挂在 203965，
 * 导致在 203966 点击任务行（31）没有路由、客户端卡在任务列表页。</p>
 * <p>The legacy contract (origin/history zz_retail_simple_quests.xml) states start_npc=203965,
 * end_npc=203966, report_owner=203966, reward_page=5. The client html for 1347 only provides page
 * 1352 on that chain (button 1009); canonical stopped driving that page chain and delivery collapsed
 * to the full node's QUEST_SELECT → tiered reward window. The XML used to hang NPC_REPORT on 203965,
 * so the quest-row click (31) at 203966 had no route and the client stayed on the quest-list page.</p>
 * <p>任务已退役：定义取生产视图，节点按 (状态, 打包投影) 定位。
 * The quest is retired, so the definition comes from the production view and nodes are located by
 * (status, packed projection).</p>
 */
class Quest1347ClientDialogAlignmentTest {
	private static final int QUEST_ID = 1347;
	private static final int START_NPC = 203965;
	private static final int REPORT_NPC = 203966;
	/** 旧 XML 的 unaccepted / 最终击杀（a7b3）/ reward / complete 投影。 / Legacy node projections. */
	private static final Map<String, Integer> UNACCEPTED = Map.of("var0", 0, "var1", 0);
	private static final Map<String, Integer> FINAL_KILL = Map.of("var0", 7, "var1", 3);
	private static final Map<String, Integer> REWARDED = Map.of("var0", 7, "var1", 3);
	private static final Map<String, Integer> COMPLETED = Map.of("var0", 0, "var1", 0);

	@Test
	void reportsAtTheLegacyEndNpcInsteadOfTheStartNpc() {
		QuestDefinition definition = load();

		QuestTransition startPage = singleTalkRoute(definition, QuestStatus.NONE, UNACCEPTED, START_NPC,
			QuestDialogAction.QUEST_SELECT);
		assertTarget(definition, startPage, QuestStatus.NONE, UNACCEPTED);
		/* 规范形接取（quest-native-dispatch P0-2）：入口页不再是 SELECT1(1011)，QUEST_SELECT 直发询问窗（页 4）。
		   Canonical acquire (quest-native-dispatch P0-2): the entry page is no longer SELECT1(1011);
		   QUEST_SELECT shows the ask-accept window (page 4) directly. */
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())), startPage.afterCommit());
		assertTrue(talkRoutes(definition, QuestStatus.START, FINAL_KILL, START_NPC).isEmpty(),
			"the start NPC must not own the report chain");

		/* 规范形交付：满段 QUEST_SELECT 直翻 REWARD 并按档位下发奖励窗；报告页（1352/SELECT2）与
		   1009 中转不再由服务端驱动。
		   Canonical delivery: the full node's QUEST_SELECT flips REWARD and shows the tiered window;
		   the 1352/SELECT2 report page and the 1009 hop are no longer server-driven. */
		QuestTransition report = singleTalkRoute(definition, QuestStatus.START, FINAL_KILL, REPORT_NPC,
			QuestDialogAction.QUEST_SELECT);
		assertTarget(definition, report, QuestStatus.REWARD, REWARDED);
		assertTrue(report.actions().isEmpty());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());
		assertTrue(talkRoutes(definition, QuestStatus.START, FINAL_KILL, REPORT_NPC,
			QuestDialogAction.SELECT_QUEST_REWARD).isEmpty(),
			"canonical removed the full-node 1009 hand-in / 规范形删除满段 1009 上交");

		assertEquals(List.of(REPORT_NPC),
			targetNpcs(definition, QuestStatus.REWARD, REWARDED, QuestStatus.COMPLETE));
		node(definition, QuestStatus.REWARD, REWARDED);
	}

	private static QuestDefinition load() {
		return ProductionQuestDefinitions.definition(QUEST_ID).definition();
	}
}
