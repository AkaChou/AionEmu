package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.aionemu.gameserver.questEngine.definition.QuestProjectionNodes.node;
import static com.aionemu.gameserver.questEngine.definition.QuestProjectionNodes.assertTarget;
import static com.aionemu.gameserver.questEngine.definition.QuestProjectionNodes.singleTalkRoute;
import static com.aionemu.gameserver.questEngine.definition.QuestProjectionNodes.talkRoutes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务 1376 的接取、最终击杀报告和奖励归属合同。
 * Locks quest 1376's acceptance, final-kill report, and reward-owner contracts.
 * <p>任务已退役：定义取生产视图，节点按 (状态, 打包投影) 定位（真端网格标签与旧 XML 不同）。
 * The quest is retired, so the definition comes from the production view and nodes are located by
 * (status, packed projection) instead of the legacy XML labels.
 */
class Quest1376ClientDialogAlignmentTest {
	private static final int QUEST_ID = 1376;
	private static final int START_NPC = 203947;
	private static final int REPORT_NPC = 203964;
	/** 旧 XML 的 unaccepted / started / 最终击杀（k7）/ reward / complete 投影。 / Legacy projections. */
	private static final Map<String, Integer> UNACCEPTED = Map.of("var0", 0);
	private static final Map<String, Integer> STARTED = Map.of("var0", 0);
	private static final Map<String, Integer> FINAL_KILL = Map.of("var0", 7);
	private static final Map<String, Integer> REWARDED = Map.of("var0", 7);
	private static final Map<String, Integer> COMPLETED = Map.of("var0", 0);

	@Test
	void followsTheRetailReportOwnerFromTheFinalKillNode() {
		QuestDefinition definition = load();

		node(definition, QuestStatus.START, FINAL_KILL);
		assertTrue(talkRoutes(definition, QuestStatus.NONE, UNACCEPTED, REPORT_NPC).isEmpty());
		assertTrue(talkRoutes(definition, QuestStatus.START, STARTED, REPORT_NPC).isEmpty());

		/* 规范形交付（quest-native-dispatch P0-2）：满段 QUEST_SELECT 直翻 REWARD 并按档位下发奖励窗；
		   报告页（SELECT2）与 1009 中转不再由服务端驱动。
		   Canonical delivery (quest-native-dispatch P0-2): the full node's QUEST_SELECT flips REWARD and
		   shows the tiered reward window; the SELECT2 report page and the 1009 hop are no longer
		   server-driven. */
		QuestTransition report = singleTalkRoute(definition, QuestStatus.START, FINAL_KILL, REPORT_NPC,
			QuestDialogAction.QUEST_SELECT);
		assertTarget(definition, report, QuestStatus.REWARD, REWARDED);
		assertTrue(report.conditions().isEmpty());
		assertTrue(report.actions().isEmpty());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());
		assertTrue(talkRoutes(definition, QuestStatus.START, FINAL_KILL, REPORT_NPC,
			QuestDialogAction.SELECT_QUEST_REWARD).isEmpty(),
			"canonical removed the full-node 1009 hand-in / 规范形删除满段 1009 上交");

		assertTrue(talkRoutes(definition, QuestStatus.START, FINAL_KILL, START_NPC).isEmpty());
		assertTrue(talkRoutes(definition, QuestStatus.REWARD, REWARDED, START_NPC).isEmpty());

		QuestTransition completion = singleTalkRoute(definition, QuestStatus.REWARD, REWARDED, REPORT_NPC,
			QuestDialogAction.SELECTED_QUEST_REWARD1);
		assertTarget(definition, completion, QuestStatus.COMPLETE, COMPLETED);
		assertEquals(List.of(
			new QuestAction.GrantReward("GOLD", 0, 33860, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 1244918, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 186000003, 2, QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());
	}

	private static QuestDefinition load() {
		return ProductionQuestDefinitions.definition(QUEST_ID).definition();
	}
}
