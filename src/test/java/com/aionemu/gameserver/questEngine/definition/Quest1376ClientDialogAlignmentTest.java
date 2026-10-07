package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.tablelane.NativeNpcNameResolver;
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
 * 锁定任务 1376 的击杀目标、报告 NPC 与奖励归属合同（真端 SimpleHunt 车道）。
 * Locks quest 1376's kill goal, report NPC and reward-owner contract on the retail SimpleHunt lane.
 * <p>
 * 任务已退役（保留清单 owner=RETAIL_TABLE，旧 XML 只在 git 历史里）：按计划 §8.9（P3 重锚口径）
 * 把旧 IR 形状断言改锚真端表行 + quest.xml 奖励事实 + native 对话面。
 * <p>
 * The quest is retired, so the former IR-shape assertions are re-anchored (plan §8.9) to the retail
 * table row, the quest.xml reward facts and the native dialog faces.
 */
class Quest1376ClientDialogAlignmentTest {
	private static final int QUEST_ID = 1376;
	/** 真端行 acquired_npc_name=Beramones / reward_npc_name=Agrips。 / Retail row NPC names. */
	private static final int START_NPC = 203947;
	private static final int REPORT_NPC = 203964;

	@Test
	void retiredRowIsOwnedByTheNativeLaneWithoutATypedDefinition() {
		assertTrue(RetiredQuestIds.contains(QUEST_ID), "1376 必须在保留清单 owner=RETAIL_TABLE 内");
		assertTrue(SimpleHuntHandler.instance().routes(QUEST_ID), "SimpleHunt native 车道必须路由 1376");
		assertFalse(ProductionQuestDefinitions.catalog().findExecutable(QUEST_ID).isPresent(),
			"退役后 typed 目录不得再持有 1376");
	}

	@Test
	void retailRowKeepsTheSevenKillGoalAndTheAgripsReportOwner() {
		NativeQuestTableLoader.SimpleHuntRow row = NativeQuestTableLoader.instance().require(QUEST_ID);
		assertEquals("Beramones", row.acquiredNpcName());
		assertEquals("Agrips", row.rewardNpcName());
		assertEquals(List.of(1), List.copyOf(row.killSlots().keySet()), "1376 是单槽杀怪行");
		assertEquals(7, row.killSlots().get(1).count(), "真端 count1=7");
		assertEquals(Integer.valueOf(1377), row.conQuest(), "真端 con_quest 链式接取窗");

		SimpleHuntHandler handler = SimpleHuntHandler.instance();
		assertEquals(START_NPC,
			NativeNpcNameResolver.instance().resolve("Beramones").npcIds().get(0), "Beramones 解析");
		assertEquals(REPORT_NPC,
			NativeNpcNameResolver.instance().resolve("Agrips").npcIds().get(0), "Agrips 解析");
		assertEquals(START_NPC, handler.acquireNpc(QUEST_ID));
		assertEquals(REPORT_NPC, handler.rewardNpc(QUEST_ID));
		assertEquals(Integer.valueOf(1377), handler.conQuest(QUEST_ID), "链式接取窗 = 1377");

		assertTrue(handler.questsForNpc(REPORT_NPC).contains(QUEST_ID), "Agrips 必须服务 1376 的交付面");
	}

	@Test
	void rewardFactsComeFromTheRetailRow() throws Exception {
		QuestMetadata metadata = RetailQuestDriver.ensureLoaded()
			.retailMetadataOf(QUEST_ID).orElseThrow().metadata();

		assertEquals(34, metadata.minLevel(), "真端 minlevel_permitted=34");
		assertEquals(java.util.Set.of("ELYOS"), metadata.permittedRaces(), "真端 pc_light");
		List<QuestReward> rewards = metadata.rewards();
		// 真端 quest.xml：reward_gold1=33860、reward_exp1=1244918、reward_item1_1=coin_03 2（186000003）。
		assertTrue(rewards.contains(new QuestReward("GOLD", 0, 33860)), () -> rewards.toString());
		assertTrue(rewards.contains(new QuestReward("EXP", 0, 1244918)), () -> rewards.toString());
		assertTrue(rewards.contains(new QuestReward("ITEM", 186000003, 2)), () -> rewards.toString());
	}

	@Test
	void inProgressAndRewardFacesFollowTheNativeDialogPages() {
		SimpleHuntHandler handler = SimpleHuntHandler.instance();
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 34);
		Integer reportNpc = handler.rewardNpc(QUEST_ID);
		assertNotNull(reportNpc, "真端行必须有可解析的交付 NPC");

		NativeTalkFixture.add(player, QUEST_ID, QuestStatus.START, 0);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, reportNpc, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPage(player, 10);

		NativeTalkFixture.clearPackets(player);
		player.getQuestStateList().getQuestState(QUEST_ID).setStatus(QuestStatus.REWARD);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, reportNpc, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 5, QUEST_ID);
	}
}
