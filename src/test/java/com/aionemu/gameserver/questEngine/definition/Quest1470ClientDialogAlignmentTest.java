package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
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
 * 锁定任务 1470 的击杀目标、哈克内报告对话与奖励合同（原版 SimpleHunt 车道）。
 * Locks quest 1470's kill target, Hagne report dialog and reward contract on the retail SimpleHunt lane.
 * <p>
 * 旧 XML 期金标为「击杀两种克罗梅内模板（212846/214621）后打开报告页」；原版表行的击杀目标是
 * 单只副本首领（{@code FireSanctuaryQueenBoss_37_Ae} ×1），报告与交付同为 Hagne（790004）。
 * 按计划 §8.9（P3 重锚口径）改锚原版行 + quest.xml 奖励事实 + native 对话面。
 * <p>
 * The retired IR gold standard was "report after killing either Kromede template (212846/214621)"; the
 * retail row's kill goal is a single instance boss with Hagne as both accept and hand-in NPC. The test
 * is re-anchored (plan §8.9) to the retail row, the quest.xml reward facts and the native faces.
 */
class Quest1470ClientDialogAlignmentTest {
	private static final int QUEST_ID = 1470;
	/** 原版行 acquired_npc_name=Hagne / reward_npc_name=Hagne。 / Retail row NPC. */
	private static final int HAGNE_NPC_ID = 790004;

	@Test
	void retiredRowIsOwnedByTheNativeLaneWithoutATypedDefinition() {
		assertTrue(RetiredQuestIds.contains(QUEST_ID), "1470 必须在保留清单 owner=RETAIL_TABLE 内");
		assertTrue(SimpleHuntHandler.instance().routes(QUEST_ID), "SimpleHunt native 车道必须路由 1470");
		assertFalse(ProductionQuestDefinitions.catalog().findExecutable(QUEST_ID).isPresent(),
			"退役后 typed 目录不得再持有 1470");
	}

	@Test
	void retailRowKeepsTheInstanceBossKillGoalAndTheHagneReportOwner() {
		NativeQuestTableLoader.SimpleHuntRow row = NativeQuestTableLoader.instance().require(QUEST_ID);
		assertEquals("Hagne", row.acquiredNpcName());
		assertEquals("Hagne", row.rewardNpcName());
		assertEquals(List.of(1), List.copyOf(row.killSlots().keySet()), "1470 是单槽击杀行");
		assertEquals(1, row.killSlots().get(1).count(), "原版 count1=1");
		assertEquals(List.of("FireSanctuaryQueenBoss_37_Ae"), row.killSlots().get(1).monsters(),
			"原版 monster1 = 副本首领单只");

		SimpleHuntHandler handler = SimpleHuntHandler.instance();
		assertEquals(HAGNE_NPC_ID, handler.acquireNpc(QUEST_ID));
		assertEquals(HAGNE_NPC_ID, handler.rewardNpc(QUEST_ID), "报告 owner = 交付 NPC Hagne");
		assertTrue(handler.questsForNpc(HAGNE_NPC_ID).contains(QUEST_ID), "Hagne 必须服务 1470");
	}

	@Test
	void rewardFactsComeFromTheRetailRow() throws Exception {
		QuestMetadata metadata = RetailQuestDriver.ensureLoaded()
			.retailMetadataOf(QUEST_ID).orElseThrow().metadata();

		assertEquals(30, metadata.minLevel(), "原版 minlevel_permitted=30");
		assertEquals(java.util.Set.of("ELYOS"), metadata.permittedRaces(), "原版 pc_light");
		List<QuestReward> rewards = metadata.rewards();
		// 原版 quest.xml：reward_exp1=1244918、reward_item1_1=coin_03 40（186000003）、
		// 六个 selectable_reward_item1_N 手套、reward_title1=light_title18。
		assertTrue(rewards.contains(new QuestReward("EXP", 0, 1244918)), () -> rewards.toString());
		assertTrue(rewards.contains(new QuestReward("ITEM", 186000003, 40)), () -> rewards.toString());
		assertEquals(6, rewards.stream().filter(reward -> reward.kind().equals("SELECTABLE_ITEM")).count(),
			"六个可选手套槽");
		assertEquals(1, rewards.stream().filter(reward -> reward.kind().equals("TITLE")).count(),
			"reward_title1 称号档");
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
