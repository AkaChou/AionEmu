package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestReward;
import com.aionemu.gameserver.questEngine.definition.RetiredQuestIds;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestTableLoader;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.questEngine.tablelane.SimpleHuntHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1112「클리오네 호수의 불청객」的原版车道（native）生产流证明。
 * <p>
 * 1112 已退役（保留清单 owner=RETAIL_TABLE，旧 XML 只在 git 历史里）：typed dispatcher 与
 * {@code ProductionQuestDefinitions.definition(1112)} 不再持有它（那是 2026-09-27 迁移前的车道），
 * 生产执行由 SimpleHunt native 车道接管。本测试按 P3 重锚口径（同 {@code Quest80487ProductionFlowTest}）
 * 断言等价 native 事实：单 owner、杀怪合同、奖励事实、对话面。
 * <p>
 * 原先针对 typed 定义的三条流程断言（REWARD 推进包序 / 完成流 after-commit 顺序 / after-commit
 * 失败审计）已无 typed 主体，其引擎层语义分别由
 * {@code QuestExecutionCoordinatorTest#commitRunsAfterCommitOnlyAndReportsBestEffortFailure}
 * 与 {@code #publishFailureResynchronizesCommittedStateWithoutReplayingRequiredActions}
 * （AFTER_COMMIT 审计、不重放）以及 {@code QuestMinionTutorialProductionFlowTest}（生产任务 typed 流程）
 * 承担；native 对话流由 {@code SimpleHuntNativeFamilyGateTest} 承担。
 * <p>
 * Production-flow proof for quest 1112 on the retail (native) lane: 1112 is retired to the
 * SimpleHunt native handler, so the typed definition no longer exists. The engine-level semantics the
 * old typed-flow assertions covered live in QuestExecutionCoordinatorTest (after-commit audit and
 * no-replay) and QuestMinionTutorialProductionFlowTest; the native dialog flow lives in
 * SimpleHuntNativeFamilyGateTest. This test pins the retired row's own production facts.
 */
class Quest1112ProductionFlowTest {

	private static final int QUEST_ID = 1112;

	@Test
	void retiredRowIsOwnedByTheNativeLaneWithoutATypedDefinition() {
		// 单 owner 不变量：退役行由 native 车道路由，typed 目录不得再持有（双 owner 即双事实来源）。
		// Single-owner invariant: the native lane routes the retired row and the typed catalog must not
		// hold it any more (two owners would mean two sources of truth).
		assertTrue(RetiredQuestIds.contains(QUEST_ID), "1112 必须在保留清单 owner=RETAIL_TABLE 内");
		assertTrue(SimpleHuntHandler.instance().routes(QUEST_ID), "SimpleHunt native 车道必须路由 1112");
		assertFalse(ProductionQuestDefinitions.catalog().findExecutable(QUEST_ID).isPresent(),
			"退役后 typed 目录不得再持有 1112（旧 XML 只在 git 历史里）");
	}

	@Test
	void retailRowKeepsTheTwoSlotHuntGoalAndTheFeiraHandIn() {
		NativeQuestTableLoader.SimpleHuntRow row = NativeQuestTableLoader.instance().require(QUEST_ID);

		assertEquals("Feira", row.acquiredNpcName(), "原版 acquired_npc_name");
		assertEquals("Feira", row.rewardNpcName(), "原版 reward_npc_name");
		assertEquals(2, row.killSlots().size(), "1112 是两槽杀怪行（Brax + lepisma）");
		assertEquals(5, row.killSlots().get(1).count(), "槽 1 required count");
		assertEquals(5, row.killSlots().get(2).count(), "槽 2 required count");
		assertEquals(SimpleHuntHandler.instance().acquireNpc(QUEST_ID),
			SimpleHuntHandler.instance().rewardNpc(QUEST_ID), "接取与交付是同一 NPC（Feira）");
	}

	@Test
	void rewardFactsComeFromTheRetailRow() throws Exception {
		QuestMetadata metadata = RetailQuestDriver.ensureLoaded()
			.retailMetadataOf(QUEST_ID).orElseThrow().metadata();

		// 原版 quest.xml：minlevel_permitted=3、race_permitted=pc_light、reward_gold1=1810、
		// reward_exp1=1375、reward_item1_1=BANDAGE_01 30（物品模板 169300002）。
		// Retail quest.xml row: min level 3, light race, 1810 kinah, 1375 exp and 30x BANDAGE_01.
		assertEquals(3, metadata.minLevel(), "原版 minlevel_permitted");
		assertEquals(java.util.Set.of("ELYOS"), metadata.permittedRaces(), "原版 pc_light");
		List<QuestReward> rewards = metadata.rewards();
		assertTrue(rewards.contains(new QuestReward("GOLD", 0, 1810)), () -> rewards.toString());
		assertTrue(rewards.contains(new QuestReward("EXP", 0, 1375)), () -> rewards.toString());
		assertTrue(rewards.contains(new QuestReward("ITEM", 169300002, 30)), () -> rewards.toString());
	}

	@Test
	void inProgressAndRewardFacesFollowTheNativeDialogPages() {
		SimpleHuntHandler handler = SimpleHuntHandler.instance();
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 4);
		Integer rewardNpc = handler.rewardNpc(QUEST_ID);
		assertNotNull(rewardNpc, "原版行必须有可解析的交付 NPC");

		// START（未打满）：常规未完成提示页 10，两参下发（不带任务上下文）。
		// START with an unfinished kill goal: the plain in-progress page 10 is a two-parameter dialog.
		NativeTalkFixture.add(player, QUEST_ID, QuestStatus.START, 0);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, rewardNpc, QUEST_ID, 31)),
			"START 态点任务行必须由 native 交付面服务");
		NativeTalkFixture.assertOnlyDialogPage(player, 10);

		// REWARD：奖励选择窗口页 5，带任务上下文。
		// REWARD: the reward-selection window (page 5) carries the quest context.
		NativeTalkFixture.clearPackets(player);
		player.getQuestStateList().getQuestState(QUEST_ID).setStatus(QuestStatus.REWARD);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, rewardNpc, QUEST_ID, 31)),
			"REWARD 态点任务行必须弹出领奖窗口");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 5, QUEST_ID);
	}
}
