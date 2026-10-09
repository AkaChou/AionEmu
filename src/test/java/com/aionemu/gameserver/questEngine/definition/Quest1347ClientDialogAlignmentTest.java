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
 * 锁定任务 1347 的两槽击杀目标、报告 NPC 与奖励归属合同（原版 SimpleHunt 车道）。
 * Locks quest 1347's two-slot kill goal, report NPC and reward-owner contract on the retail lane.
 * <p>
 * 旧 typed XML 期此处的关键裁定是「报告 owner = 交付 NPC（203966 Trillian），不是接取 NPC
 * （203965 Castor）」；原版表行给出同一事实，故按计划 §8.9（P3 重锚口径）改锚 native 行 + 对话面。
 * <p>
 * The retired IR assertion here was "the report owner is the hand-in NPC (203966), not the accept NPC
 * (203965)"; the retail row states the same fact, so the test is re-anchored (plan §8.9) to the native
 * row and its dialog faces.
 */
class Quest1347ClientDialogAlignmentTest {
	private static final int QUEST_ID = 1347;
	/** 原版行 acquired_npc_name=Castor / reward_npc_name=Trillian。 / Retail row NPC names. */
	private static final int START_NPC = 203965;
	private static final int REPORT_NPC = 203966;

	@Test
	void retiredRowIsOwnedByTheNativeLaneWithoutATypedDefinition() {
		assertTrue(RetiredQuestIds.contains(QUEST_ID), "1347 必须在保留清单 owner=RETAIL_TABLE 内");
		assertTrue(SimpleHuntHandler.instance().routes(QUEST_ID), "SimpleHunt native 车道必须路由 1347");
		assertFalse(ProductionQuestDefinitions.catalog().findExecutable(QUEST_ID).isPresent(),
			"退役后 typed 目录不得再持有 1347");
	}

	@Test
	void retailRowReportsAtTheHandInNpcInsteadOfTheAcceptNpc() {
		NativeQuestTableLoader.SimpleHuntRow row = NativeQuestTableLoader.instance().require(QUEST_ID);
		assertEquals("Castor", row.acquiredNpcName());
		assertEquals("Trillian", row.rewardNpcName());
		assertEquals(List.of(1, 2), List.copyOf(row.killSlots().keySet()), "1347 是双槽杀怪行");
		assertEquals(7, row.killSlots().get(1).count(), "原版 count1=7");
		assertEquals(3, row.killSlots().get(2).count(), "原版 count2=3");
		assertEquals(Integer.valueOf(1348), row.conQuest(), "原版 con_quest 链式接取窗");

		SimpleHuntHandler handler = SimpleHuntHandler.instance();
		assertEquals(START_NPC,
			NativeNpcNameResolver.instance().resolve("Castor").npcIds().get(0), "Castor 解析");
		assertEquals(REPORT_NPC,
			NativeNpcNameResolver.instance().resolve("Trillian").npcIds().get(0), "Trillian 解析");
		assertEquals(START_NPC, handler.acquireNpc(QUEST_ID));
		assertEquals(REPORT_NPC, handler.rewardNpc(QUEST_ID));
		assertEquals(Integer.valueOf(1348), handler.conQuest(QUEST_ID), "链式接取窗 = 1348");

		// 报告 owner = 交付 NPC：只有 Castor 的接取面 + Trillian 的交付面成立。
		// Report owner = the hand-in NPC: the accept face belongs to Castor, the hand-in face to Trillian.
		assertTrue(handler.questsForNpc(REPORT_NPC).contains(QUEST_ID), "Trillian 必须服务 1347");
	}

	@Test
	void rewardFactsComeFromTheRetailRow() throws Exception {
		QuestMetadata metadata = RetailQuestDriver.ensureLoaded()
			.retailMetadataOf(QUEST_ID).orElseThrow().metadata();

		assertEquals(30, metadata.minLevel(), "原版 minlevel_permitted=30");
		assertEquals(java.util.Set.of("ELYOS"), metadata.permittedRaces(), "原版 pc_light");
		List<QuestReward> rewards = metadata.rewards();
		// 原版 quest.xml：reward_exp1=340413、reward_item1_1=coin_03 1（186000003）。
		assertTrue(rewards.contains(new QuestReward("EXP", 0, 340413)), () -> rewards.toString());
		assertTrue(rewards.contains(new QuestReward("ITEM", 186000003, 1)), () -> rewards.toString());
	}

	@Test
	void inProgressAndRewardFacesFollowTheNativeDialogPages() {
		SimpleHuntHandler handler = SimpleHuntHandler.instance();
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 30);
		Integer reportNpc = handler.rewardNpc(QUEST_ID);
		assertNotNull(reportNpc, "原版行必须有可解析的交付 NPC");

		NativeTalkFixture.add(player, QUEST_ID, QuestStatus.START, 0);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, reportNpc, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPage(player, 10);

		NativeTalkFixture.clearPackets(player);
		player.getQuestStateList().getQuestState(QUEST_ID).setStatus(QuestStatus.REWARD);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, reportNpc, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 5, QUEST_ID);
	}
}
