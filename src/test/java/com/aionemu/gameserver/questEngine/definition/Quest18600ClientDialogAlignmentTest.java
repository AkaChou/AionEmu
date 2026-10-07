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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务 18600（假烙印之石流通）的真端 SimpleTalk 行、中继页链与交付领奖面。
 * Locks quest 18600's retail SimpleTalk row, relay page chain, and hand-in/reward faces.
 * <p>
 * 任务已退役（保留清单 owner=RETAIL_TABLE，SimpleTalk 车道）：旧 typed XML 合同随迁移退场，按计划
 * §8.9（P3 重锚口径）改锚真端表行（Perento 接取/交付、LF3_Kurochin_E → Herthia 两步中继、真端
 * give_item/give_item2/remove_item2 列）+ 客户端声明页（select1/select2/select2_1/select3/select3_1/
 * select5）。历史缺陷 {@code REWARD_PREVIEW_OPTIONAL_WORK_ITEM} 的物品轴（Paper Bill 182213000 的
 * 接取发放与步 2 回收、Fake Stigma 182213001 的步 2 发放）由真端物品列承载。
 * <p>
 * The retired typed XML contract is re-anchored (plan §8.9) to the retail SimpleTalk row, the client-declared
 * pages, and the native face behaviour.
 */
class Quest18600ClientDialogAlignmentTest {
	private static final int QUEST_ID = 18600;
	private static final int PERENTO = 204500;
	private static final int KUROCHINERK = 804601;
	private static final int HERTHIA = 205228;
	private static final int PAPER_BILL = 182213000;
	private static final int FAKE_STIGMA = 182213001;

	@Test
	void acceptDialogUsesTheClientConfirmationWindow() throws Exception {
		assertTrue(RetiredQuestIds.contains(QUEST_ID));
		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		assertTrue(handler.routes(QUEST_ID), "SimpleTalk native 车道必须路由 18600");
		assertFalse(ProductionQuestDefinitions.catalog().findExecutable(QUEST_ID).isPresent(),
			"退役后 typed 目录不得再持有 18600");
		assertEquals(PERENTO, handler.acquireNpc(QUEST_ID), "真端 acquired_npc_name = Perento");
		assertEquals(PERENTO, handler.rewardNpc(QUEST_ID), "真端 reward_npc_name = Perento");
		assertEquals(2, handler.relayCount(QUEST_ID), "真端 talk_npc1/2 两步中继");
		assertEquals(new SimpleTalkHandler.ItemStack(PAPER_BILL, 1), handler.acceptGiveItem(QUEST_ID),
			"接取发放 quest_18600a（Paper Bill）");
		assertNull(handler.stepGiveItem(QUEST_ID, 1), "步 1（Kurochin）无发放列");
		assertEquals(new SimpleTalkHandler.ItemStack(FAKE_STIGMA, 1), handler.stepGiveItem(QUEST_ID, 2),
			"步 2（Herthia）发放 quest_18600b（Fake Stigma）");
		assertEquals(new SimpleTalkHandler.ItemStack(PAPER_BILL, 1), handler.stepRemoveItem(QUEST_ID, 2),
			"步 2 回收 quest_18600a");
		assertEquals(new SimpleTalkHandler.Cutscene(189, 10001), handler.cutscene(QUEST_ID),
			"真端 cutsceneid1=189 / cs1_haction=10001");
		assertEquals(18601, handler.conQuest(QUEST_ID), "真端 con_quest=18601");

		// 接取面：任务行（31）下发客户端入口页（真端信页 select1=1011；select_none 未声明），
		// 1002 确认建档 + 真端 give_item + 页 1003。
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 37);
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleTalkHandler itemHandler = NativeTalkFixture.handler(inventory);
		NativeTalkFixture.clearPackets(player);
		assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, PERENTO, QUEST_ID, 31)), "接取问询");
		assertEquals(QuestDialogPage.SELECT1.id(), NativeTalkFixture.clientEntryPage(QUEST_ID),
			"入口页 = 客户端声明的 select1(1011)");
		NativeTalkFixture.assertOnlyDialogPage(player, NativeTalkFixture.clientEntryPage(QUEST_ID));
		assertNull(player.getQuestStateList().getQuestState(QUEST_ID), "问询页不得落库");

		NativeTalkFixture.clearPackets(player);
		assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, PERENTO, QUEST_ID, 1002)), "接取确认");
		assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(QUEST_ID).getStatus());
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_ACCEPTED);
		assertEquals(List.of("give:" + PAPER_BILL + ":1"), inventory.calls(), "接取发放按真端 give_item 列");

		QuestMetadata metadata = RetailQuestDriver.ensureLoaded()
			.retailMetadataOf(QUEST_ID).orElseThrow().metadata();
		assertEquals(37, metadata.minLevel(), "真端 minlevel_permitted=37");
		assertEquals(java.util.Set.of("ELYOS"), metadata.permittedRaces(), "真端 pc_light");
	}

	@Test
	void intermediateNpcPagesAndFinalRewardPagesFollowTheLegacyChain() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 37);
		NativeTalkFixture.add(player, QUEST_ID, QuestStatus.START, 0);
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleTalkHandler itemHandler = NativeTalkFixture.handler(inventory);

		// 步 1（Kurochin）：任务行打开该步页（select2=1352），子页 select2_1 原样回发。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, KUROCHINERK, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, QuestDialogPage.SELECT2.id(), QUEST_ID);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, KUROCHINERK, QUEST_ID,
			QuestDialogPage.SELECT2_1.id())));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, QuestDialogPage.SELECT2_1.id(), QUEST_ID);

		// 步 1 推进（SETPRO1=10000）：var0=1 + 关窗（真端 after-commit 零发页），无物品列。
		NativeTalkFixture.clearPackets(player);
		assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, KUROCHINERK, QUEST_ID, 10000)));
		assertEquals(1, player.getQuestStateList().getQuestState(QUEST_ID).getQuestVars().getQuestVars(),
			"步 1 推进必须写 var0=1");
		NativeTalkFixture.assertCloseDialog(player);
		assertEquals(List.of(), inventory.calls(), "步 1 无 give/remove 列");

		// 步 2（Herthia）：页 select3=1693，子页 select3_1 回发。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, HERTHIA, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, QuestDialogPage.SELECT3.id(), QUEST_ID);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, HERTHIA, QUEST_ID,
			QuestDialogPage.SELECT3_1.id())));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, QuestDialogPage.SELECT3_1.id(), QUEST_ID);

		// 步 2 推进（SETPRO2=10001）：var0=2 + 真端 give_item2/remove_item2 + 关窗。
		NativeTalkFixture.clearPackets(player);
		assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, HERTHIA, QUEST_ID, 10001)));
		assertEquals(2, player.getQuestStateList().getQuestState(QUEST_ID).getQuestVars().getQuestVars(),
			"步 2 推进必须写 var0=2");
		NativeTalkFixture.assertCloseDialog(player);
		assertEquals(List.of("give:" + FAKE_STIGMA + ":1", "remove:" + PAPER_BILL + ":1"), inventory.calls(),
			"步 2 发放/回收按真端列");

		// 交付（Perento）：中继满后 31 只发报告确认页（select2 已被中继占用 ⇒ 取 select5=2375），
		// 1009 确认才翻 REWARD + 奖励窗（页 5）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, PERENTO, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, QuestDialogPage.SELECT5.id(), QUEST_ID);
		assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(QUEST_ID).getStatus(),
			"31 只发确认页，不推进");

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, PERENTO, QUEST_ID, 1009)));
		assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(QUEST_ID).getStatus(),
			"确认动作必须推进到 REWARD");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, SimpleTalkHandler.PAGE_REWARD_WINDOW, QUEST_ID);
	}
}
