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
 * 锁定任务 1346 的击杀目标、报告 NPC 与奖励归属合同（真端 SimpleHunt 车道）。
 * Locks quest 1346's kill goal, report NPC and reward-owner contract on the retail SimpleHunt lane.
 * <p>
 * 任务已退役（保留清单 owner=RETAIL_TABLE，旧 XML 只在 git 历史里）：typed IR 断言随迁移退场，
 * 按计划 §8.9（P3 重锚口径）改锚真端表行 + quest.xml 奖励事实 + native 对话面；
 * 引擎层事务/审计语义由 {@code QuestExecutionCoordinatorTest} 等承担。
 * <p>
 * The quest is retired, so the former IR-shape assertions are re-anchored (plan §8.9) to the retail
 * table row, the quest.xml reward facts and the native dialog faces.
 */
class Quest1346ClientDialogAlignmentTest {
	private static final int QUEST_ID = 1346;
	/** 真端行 acquired_npc_name=Trillian / reward_npc_name=Castor。 / Retail row NPC names. */
	private static final int ACQUIRE_NPC = 203966;
	private static final int REPORT_NPC = 203965;

	@Test
	void retiredRowIsOwnedByTheNativeLaneWithoutATypedDefinition() {
		assertTrue(RetiredQuestIds.contains(QUEST_ID), "1346 必须在保留清单 owner=RETAIL_TABLE 内");
		assertTrue(SimpleHuntHandler.instance().routes(QUEST_ID), "SimpleHunt native 车道必须路由 1346");
		assertFalse(ProductionQuestDefinitions.catalog().findExecutable(QUEST_ID).isPresent(),
			"退役后 typed 目录不得再持有 1346");
	}

	@Test
	void retailRowKeepsTheNineKillGoalAndTheCastorReportOwner() {
		NativeQuestTableLoader.SimpleHuntRow row = NativeQuestTableLoader.instance().require(QUEST_ID);
		assertEquals("Trillian", row.acquiredNpcName());
		assertEquals("Castor", row.rewardNpcName());
		assertEquals(List.of(1), List.copyOf(row.killSlots().keySet()), "1346 是单槽杀怪行");
		assertEquals(9, row.killSlots().get(1).count(), "真端 count1=9");
		assertEquals(8, row.killSlots().get(1).monsters().size(), "真端 monster1 八个变体");

		SimpleHuntHandler handler = SimpleHuntHandler.instance();
		assertEquals(ACQUIRE_NPC,
			NativeNpcNameResolver.instance().resolve("Trillian").npcIds().get(0), "Trillian 解析");
		assertEquals(REPORT_NPC,
			NativeNpcNameResolver.instance().resolve("Castor").npcIds().get(0), "Castor 解析");
		assertEquals(ACQUIRE_NPC, handler.acquireNpc(QUEST_ID));
		assertEquals(REPORT_NPC, handler.rewardNpc(QUEST_ID));

		// 报告 owner 唯一：Castor 服务本行，接取 NPC 不在交付面。
		// Single report owner: Castor serves the row and the accept NPC is not on the hand-in face.
		assertTrue(handler.questsForNpc(REPORT_NPC).contains(QUEST_ID), "Castor 必须服务 1346 的交付面");
	}

	@Test
	void rewardOwnerCarriesTheRetailRewardFacts() throws Exception {
		QuestMetadata metadata = RetailQuestDriver.ensureLoaded()
			.retailMetadataOf(QUEST_ID).orElseThrow().metadata();

		assertEquals(29, metadata.minLevel(), "真端 minlevel_permitted=29");
		assertEquals(java.util.Set.of("ELYOS"), metadata.permittedRaces(), "真端 pc_light");
		List<QuestReward> rewards = metadata.rewards();
		// 真端 quest.xml：reward_exp1=476911、reward_item1_1=coin_03 9（186000003）、
		// reward_item1_2=potion_hp_mp_30a 9（162000048）。
		assertTrue(rewards.contains(new QuestReward("EXP", 0, 476911)), () -> rewards.toString());
		assertTrue(rewards.contains(new QuestReward("ITEM", 186000003, 9)), () -> rewards.toString());
		assertTrue(rewards.contains(new QuestReward("ITEM", 162000048, 9)), () -> rewards.toString());
	}

	@Test
	void inProgressAndRewardFacesFollowTheNativeDialogPages() {
		SimpleHuntHandler handler = SimpleHuntHandler.instance();
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 29);
		Integer reportNpc = handler.rewardNpc(QUEST_ID);
		assertNotNull(reportNpc, "真端行必须有可解析的交付 NPC");

		// START（未打满）：常规未完成提示页 10（两参，不带任务上下文）。
		NativeTalkFixture.add(player, QUEST_ID, QuestStatus.START, 0);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, reportNpc, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPage(player, 10);

		// REWARD：奖励选择窗口页 5，带任务上下文。
		NativeTalkFixture.clearPackets(player);
		player.getQuestStateList().getQuestState(QUEST_ID).setStatus(QuestStatus.REWARD);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, reportNpc, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 5, QUEST_ID);
	}
}
