package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.QuestReward;
import com.aionemu.gameserver.questEngine.definition.RetiredQuestIds;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.tablelane.NativeNpcNameResolver;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1913「베르테론 파견」的原版车道（native）生产流证明。
 * <p>
 * 1913 已退役（保留清单 owner=RETAIL_TABLE，旧 XML 只在 git 历史里）：typed dispatcher 与
 * {@code ProductionQuestDefinitions.definition(1913)} 不再持有它，生产执行由 SimpleTalk native 车道接管。
 * 本测试按 P3 重锚口径（同 {@code Quest80487ProductionFlowTest} / {@code Quest1112ProductionFlowTest}）
 * 断言等价 native 事实：单 owner、单步中继合同、奖励事实、接取/中继/报告对话面。
 * <p>
 * 旧 typed 流程断言（步 1 传送 210030000、交付边锚在 started1、after-commit 序）随 typed 主体退场：
 * 原版 talk 行未声明传送列，车道推进 = var0=1 + 关窗，「传送后才可领奖」由中继满门承担（未满 = 页 10）。
 * <p>
 * Production-flow proof for quest 1913 on the retail (native) lane: 1913 is retired to the SimpleTalk
 * native handler, so the typed definition no longer exists. The old typed assertions (the step-1 teleport,
 * the started1 delivery edge) retire with it: the retail talk row declares no teleport column, the lane
 * advance is var0=1 plus close, and the "report only after the relay" gate is the relay-saturation check
 * (unsaturated shows page 10).
 */
class Quest1913ProductionFlowTest {

	private static final int QUEST_ID = 1913;

	@Test
	void retiredRowIsOwnedByTheNativeTalkLaneWithoutATypedDefinition() {
		assertTrue(RetiredQuestIds.contains(QUEST_ID), "1913 必须在保留清单 owner=RETAIL_TABLE 内");
		assertTrue(SimpleTalkHandler.instance().routes(QUEST_ID), "SimpleTalk native 车道必须路由 1913");
		assertFalse(ProductionQuestDefinitions.catalog().findExecutable(QUEST_ID).isPresent(),
			"退役后 typed 目录不得再持有 1913（旧 XML 只在 git 历史里）");
	}

	@Test
	void retailRowKeepsTheSingleRelayAndTheHyacinteHandIn() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();

		assertEquals("Macus", handler.requireRow(QUEST_ID).acquiredNpcName(), "原版 acquired_npc_name");
		assertEquals(List.of("Polyidus"), handler.requireRow(QUEST_ID).talkNpcNames(), "原版 talk_npc1");
		assertEquals("Hyacinte", handler.requireRow(QUEST_ID).rewardNpcName(), "原版 reward_npc_name");
		assertEquals(1, handler.relayCount(QUEST_ID), "1913 是单步中继行");
		assertNotNull(handler.acquireNpc(QUEST_ID), "接取 NPC 必须可解析");
		assertNotNull(handler.rewardNpc(QUEST_ID), "交付 NPC 必须可解析");
	}

	@Test
	void rewardFactsComeFromTheRetailRow() throws Exception {
		QuestMetadata metadata = RetailQuestDriver.ensureLoaded()
			.retailMetadataOf(QUEST_ID).orElseThrow().metadata();

		// 原版 quest.xml：minlevel_permitted=10、race_permitted=pc_light、class_permitted=fighter knight、
		// reward_exp1=14046、reward_item1_1=FOOD_dpheal_40A 5（物品模板 160001273）。
		// Retail quest.xml row: min level 10, light race, fighter/knight classes, 14046 exp and 5x food.
		assertEquals(10, metadata.minLevel(), "原版 minlevel_permitted");
		assertEquals(java.util.Set.of("ELYOS"), metadata.permittedRaces(), "原版 pc_light");
		assertTrue(metadata.permittedClasses().contains("GLADIATOR")
				&& metadata.permittedClasses().contains("TEMPLAR"), "原版 class_permitted=fighter knight");
		List<QuestReward> rewards = metadata.rewards();
		assertTrue(rewards.contains(new QuestReward("EXP", 0, 14046)), () -> rewards.toString());
		assertTrue(rewards.contains(new QuestReward("ITEM", 160001273, 5)), () -> rewards.toString());
	}

	@Test
	void acceptRelayAndReportFacesFollowTheNativeDialogPages() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.GLADIATOR, 10);
		NativeTalkFixture.completePrerequisites(player, 1007);
		Integer acquireNpc = handler.acquireNpc(QUEST_ID);
		Integer progressNpc = NativeNpcNameResolver.instance().resolveMembers("Polyidus").get(0);
		Integer reportNpc = handler.rewardNpc(QUEST_ID);
		assertNotNull(acquireNpc);
		assertNotNull(progressNpc);
		assertNotNull(reportNpc);

		// 接取收尾（1002）：建档 START + 接取确认页 1003（带任务上下文）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, acquireNpc, QUEST_ID, 1002)));
		QuestState state = player.getQuestStateList().getQuestState(QUEST_ID);
		assertEquals(QuestStatus.START, state.getStatus(), "接取收尾必须把任务建到 START");
		assertEquals(0, state.getQuestVars().getQuestVars(), "接取收尾必须清零变量");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1003, QUEST_ID);

		// 中继未满时报告面只有未完成提示页 10（两参，不带任务上下文）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, reportNpc, QUEST_ID, 31)),
			"中继未满时交付面必须兜底响应");
		NativeTalkFixture.assertOnlyDialogPage(player, 10);

		// 步 1（Polyidus）：任务行打开该步页（select2=1352）；推进（10000）= var0=1 + 关窗。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, progressNpc, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1352, QUEST_ID);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, progressNpc, QUEST_ID, 10000)));
		assertEquals(1, state.getQuestVars().getQuestVars(), "步 1 推进必须写 var0=1");
		NativeTalkFixture.assertCloseDialog(player);

		// 中继满：报告确认页（select5=2375）→ 1009 推进 REWARD + 奖励窗（页 5）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, reportNpc, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 2375, QUEST_ID);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, reportNpc, QUEST_ID, 1009)));
		assertEquals(QuestStatus.REWARD, state.getStatus(), "确认动作必须推进到 REWARD");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 5, QUEST_ID);
	}
}
