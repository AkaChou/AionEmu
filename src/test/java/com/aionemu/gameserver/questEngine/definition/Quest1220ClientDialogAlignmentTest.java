package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务 1220「秘密配送」的真端 SimpleTalk 行与宝箱交接链：
 * 乌内 Une(203172) 接取并发放宝箱 182200568 → 努蒙 Shugo3(798004) 换箱 182200569 → 玛平恩恩
 * shugo_Lender_LF2_01(205240) 交付领奖。
 * <p>
 * 任务已退役（保留清单 owner=RETAIL_TABLE，SimpleTalk 车道）：旧 typed XML 合同随迁移退场，按计划
 * §8.9（P3 重锚口径）改锚真端表行（give_item 接取发放、give_item1/remove_item1 步 1 换箱、
 * reward_npc_name 交付）+ 客户端声明页（select1/select2/select2_1/select5）。历史缺陷（接取漏发箱使
 * SETPRO1 无物可交、服务层把按钮动作 ID 当页面 ID 回显）的物品轴由真端 give/remove 列承载。
 * <p>
 * The retired typed XML contract is re-anchored (plan §8.9) to the retail SimpleTalk row, the client-declared
 * pages, and the native face behaviour.
 */
class Quest1220ClientDialogAlignmentTest {
	private static final int QUEST_ID = 1220;
	private static final int START_NPC_ID = 203172;
	private static final int MID_NPC_ID = 798004;
	private static final int END_NPC_ID = 205240;
	private static final int BOX_FOR_NUMONERK = 182200568;
	private static final int BOX_FOR_MAPPINERK = 182200569;

	@Test
	void startGrantsTheTreasureBoxAndOnlyUneOwnsTheQuestStart() throws Exception {
		assertTrue(RetiredQuestIds.contains(QUEST_ID));
		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		assertTrue(handler.routes(QUEST_ID), "SimpleTalk native 车道必须路由 1220");
		assertFalse(ProductionQuestDefinitions.catalog().findExecutable(QUEST_ID).isPresent(),
			"退役后 typed 目录不得再持有 1220");
		assertEquals(START_NPC_ID, handler.acquireNpc(QUEST_ID), "真端 acquired_npc_name = Une");
		assertEquals(END_NPC_ID, handler.rewardNpc(QUEST_ID), "真端 reward_npc_name = shugo_Lender_LF2_01");
		assertEquals(1, handler.relayCount(QUEST_ID), "真端 talk_npc1 = 单步中继");
		assertEquals(new SimpleTalkHandler.ItemStack(BOX_FOR_NUMONERK, 1), handler.acceptGiveItem(QUEST_ID),
			"接取发放 quest_1220a（宝箱）");
		assertEquals(new SimpleTalkHandler.ItemStack(BOX_FOR_MAPPINERK, 1), handler.stepGiveItem(QUEST_ID, 1),
			"步 1 发放 quest_1220b（换箱）");
		assertEquals(new SimpleTalkHandler.ItemStack(BOX_FOR_NUMONERK, 1), handler.stepRemoveItem(QUEST_ID, 1),
			"步 1 回收 quest_1220a");

		// 接取面：前置 Q1219 完成 + 17 级过 CanAcquireQuest 轴；31 只发入口页（select1=1011），
		// 1002 确认建档 + 真端 give_item + 页 1003。
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 17);
		NativeTalkFixture.completePrerequisites(player, 1219);
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleTalkHandler itemHandler = NativeTalkFixture.handler(inventory);
		NativeTalkFixture.clearPackets(player);
		assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, START_NPC_ID, QUEST_ID, 31)), "接取问询");
		assertEquals(QuestDialogPage.SELECT1.id(), NativeTalkFixture.clientEntryPage(QUEST_ID),
			"入口页 = 客户端声明的 select1(1011)");
		NativeTalkFixture.assertOnlyDialogPage(player, NativeTalkFixture.clientEntryPage(QUEST_ID));
		assertEquals(null, player.getQuestStateList().getQuestState(QUEST_ID), "问询页不得落库");

		NativeTalkFixture.clearPackets(player);
		assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, START_NPC_ID, QUEST_ID, 1002)), "接取确认");
		assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(QUEST_ID).getStatus());
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_ACCEPTED);
		assertEquals(List.of("give:" + BOX_FOR_NUMONERK + ":1"), inventory.calls(), "接取发放按真端 give_item 列");

		// 努蒙与玛平恩恩只承接对话，不是接取 NPC（真端 acquired_npc_name 仅 Une）。
		// Numonerk and Mappinerk only own dialogs, not the acquisition (retail acquired_npc_name is Une only).
		for (int npcId : List.of(MID_NPC_ID, END_NPC_ID)) {
			Player fresh = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 17);
			NativeTalkFixture.completePrerequisites(fresh, 1219);
			NativeTalkFixture.clearPackets(fresh);
			assertFalse(SimpleTalkHandler.instance().onDialog(
				NativeTalkFixture.dialog(fresh, npcId, QUEST_ID, 31)),
				"npc " + npcId + " must not open the quest 1220 acquire face");
		}

		QuestMetadata metadata = RetailQuestDriver.ensureLoaded()
			.retailMetadataOf(QUEST_ID).orElseThrow().metadata();
		assertEquals(17, metadata.minLevel(), "真端 minlevel_permitted=17");
		assertEquals(java.util.Set.of("ELYOS"), metadata.permittedRaces(), "真端 pc_light");
	}

	@Test
	void numonerkExchangesTheBoxAndMappinerkSettlesTheReward() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 17);
		NativeTalkFixture.add(player, QUEST_ID, QuestStatus.START, 0);
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleTalkHandler itemHandler = NativeTalkFixture.handler(inventory);

		// 步 1（努蒙 Shugo3）：任务行打开该步页（select2=1352），子页 select2_1 原样回发。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, MID_NPC_ID, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, QuestDialogPage.SELECT2.id(), QUEST_ID);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, MID_NPC_ID, QUEST_ID,
			QuestDialogPage.SELECT2_1.id())));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, QuestDialogPage.SELECT2_1.id(), QUEST_ID);

		// 步 1 推进（SETPRO1=10000）：var0=1 + 真端 give_item1/remove_item1（换箱）+ 关窗。
		NativeTalkFixture.clearPackets(player);
		assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, MID_NPC_ID, QUEST_ID, 10000)));
		assertEquals(1, player.getQuestStateList().getQuestState(QUEST_ID).getQuestVars().getQuestVars(),
			"步 1 推进必须写 var0=1");
		NativeTalkFixture.assertCloseDialog(player);
		assertEquals(List.of("give:" + BOX_FOR_MAPPINERK + ":1", "remove:" + BOX_FOR_NUMONERK + ":1"),
			inventory.calls(), "步 1 换箱按真端列（发新箱、回收旧箱）");

		// 交付（玛平恩恩）：中继满后 31 只发报告确认页（select2 已被中继占用 ⇒ 取 select5=2375），
		// 1009 确认才翻 REWARD + 奖励窗（页 5）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, END_NPC_ID, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, QuestDialogPage.SELECT5.id(), QUEST_ID);
		assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(QUEST_ID).getStatus(),
			"31 只发确认页，不推进");

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, END_NPC_ID, QUEST_ID, 1009)));
		assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(QUEST_ID).getStatus(),
			"确认动作必须推进到 REWARD");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, SimpleTalkHandler.PAGE_REWARD_WINDOW, QUEST_ID);
	}

	@Test
	void treasureBoxesStayRegisteredAsQuestWorkItems() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		// 两个宝箱仍登记在真端行：接取发放 quest_1220a、步 1 发 quest_1220b / 收 quest_1220a；
		// 本族无 item_check 门列（交付由报告确认动作分叉，不设工作物品门）。
		// Both boxes stay registered on the retail row (accept gives 1220a; step 1 gives 1220b and
		// removes 1220a); the row declares no item_check gate.
		assertEquals(List.of(), handler.workItems(QUEST_ID), "真端行无 item_check 工作物品门");
		assertEquals(new SimpleTalkHandler.ItemStack(BOX_FOR_NUMONERK, 1), handler.acceptGiveItem(QUEST_ID));
		assertEquals(new SimpleTalkHandler.ItemStack(BOX_FOR_MAPPINERK, 1), handler.stepGiveItem(QUEST_ID, 1));
		assertEquals(new SimpleTalkHandler.ItemStack(BOX_FOR_NUMONERK, 1), handler.stepRemoveItem(QUEST_ID, 1));
	}
}
