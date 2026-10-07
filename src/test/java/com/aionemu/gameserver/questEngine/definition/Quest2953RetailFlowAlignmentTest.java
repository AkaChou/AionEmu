package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
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
 * 锁定任务 2953「配送申请书传递」（可重复）的真端单步物品链与唯一 NPC 归属。
 * Locks quest 2953's retail single-step item chain and its exclusive NPC owners.
 * <p>
 * 任务已退役（保留清单 owner=RETAIL_TABLE）：旧 typed 节点/转换金标随迁移退场，按计划 §8.9（P3 重锚口径）
 * 改锚真端表行（接取 Doman → Veldina 步 1 → Doman 交付、步 1 扣申请书）、quest.xml 奖励与 native 对话面。
 * <p>
 * The retired typed gold standard is re-anchored (plan §8.9) to the retail row (accept Doman →
 * Veldina step 1 → Doman hand-in, step-1 request removal), the quest.xml rewards and the native faces.
 */
class Quest2953RetailFlowAlignmentTest {
	private static final int QUEST_ID = 2953;
	private static final int START_AND_REWARD_NPC = 204191;
	private static final int DELIVERY_NPC = 204071;
	/** 真端 give_item / remove_item1 = ITEM_QUEST_2953A 1。 / The retail supply-request item. */
	private static final int SUPPLY_REQUEST = 182207039;

	@Test
	void retailRowKeepsTheSingleStepItemChainAndExclusiveOwners() throws Exception {
		assertTrue(RetiredQuestIds.contains(QUEST_ID));
		assertTrue(SimpleTalkHandler.instance().routes(QUEST_ID), "SimpleTalk native 车道必须路由 2953");
		assertFalse(ProductionQuestDefinitions.catalog().findExecutable(QUEST_ID).isPresent(),
			"退役后 typed 目录不得再持有 2953");

		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		assertEquals("Doman", handler.requireRow(QUEST_ID).acquiredNpcName(), "接取 owner 名");
		assertEquals(List.of("Veldina"), handler.requireRow(QUEST_ID).talkNpcNames(), "真端中继链");
		assertEquals("Doman", handler.requireRow(QUEST_ID).rewardNpcName(), "交付 owner 名");
		assertEquals(START_AND_REWARD_NPC, NativeNpcNameResolver.instance().resolve("Doman").npcIds().get(0));
		assertEquals(DELIVERY_NPC, NativeNpcNameResolver.instance().resolve("Veldina").npcIds().get(0));
		assertEquals(START_AND_REWARD_NPC, handler.acquireNpc(QUEST_ID), "接取 owner = Doman");
		assertEquals(START_AND_REWARD_NPC, handler.rewardNpc(QUEST_ID), "交付 owner = Doman");
		assertEquals(1, handler.relayCount(QUEST_ID), "2953 是单步中继行");
		assertTrue(handler.relaysForNpc(DELIVERY_NPC).contains(
			new SimpleTalkHandler.RelayStep(QUEST_ID, 1, DELIVERY_NPC)), "步 1 = Veldina");
		assertEquals(new SimpleTalkHandler.ItemStack(SUPPLY_REQUEST, 1), handler.acceptGiveItem(QUEST_ID),
			"接取发放申请书");
		assertEquals(new SimpleTalkHandler.ItemStack(SUPPLY_REQUEST, 1),
			handler.stepRemoveItem(QUEST_ID, 1), "步 1 扣除申请书");
		assertEquals(List.of(), handler.workItems(QUEST_ID), "无 item_check 行不得带交付门物品");

		QuestMetadata metadata = RetailQuestDriver.ensureLoaded()
			.retailMetadataOf(QUEST_ID).orElseThrow().metadata();
		assertEquals(10, metadata.minLevel(), "真端 minlevel_permitted=10");
		assertEquals(java.util.Set.of("ASMODIANS"), metadata.permittedRaces(), "真端 pc_dark");
		assertEquals(100, metadata.repeatPolicy().maxRepeatCount(), "真端 max_repeat_count=100 ⇒ 可重复");
		List<QuestReward> rewards = metadata.rewards();
		assertTrue(rewards.contains(new QuestReward("EXP", 0, 150)), () -> rewards.toString());
		assertTrue(rewards.contains(new QuestReward("GOLD", 0, 500)), () -> rewards.toString());
	}

	@Test
	void followsTheClientDeliveryPagesAndItemLifecycle() {
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleTalkHandler handler = NativeTalkFixture.handler(inventory);
		Player player = NativeTalkFixture.player(Race.ASMODIANS, PlayerClass.WARRIOR, 10);

		// 未接取：交付 NPC 不是接取面（零响应）；任务行在接取 NPC 打开声明的入口页。
		NativeTalkFixture.clearPackets(player);
		assertFalse(handler.onDialog(NativeTalkFixture.dialog(player, DELIVERY_NPC, QUEST_ID, 31)),
			"交付 NPC 不得开放接取面");
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, START_AND_REWARD_NPC, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player,
			NativeTalkFixture.clientEntryPage(QUEST_ID), QUEST_ID);

		// 接取收尾（1002）：建档 START + 接取确认页 1003 + 发放申请书。
		inventory.clear();
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, START_AND_REWARD_NPC, QUEST_ID, 1002)));
		QuestState state = player.getQuestStateList().getQuestState(QUEST_ID);
		assertEquals(QuestStatus.START, state.getStatus(), "接取收尾必须把任务建到 START");
		assertEquals(List.of("give:" + SUPPLY_REQUEST + ":1"), inventory.calls(), "接取必须发放申请书");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1003, QUEST_ID);

		// 步 1（Veldina）：任务行打开该步页（select2=1352）；推进（10000）= var0=1 + 关窗 + 扣申请书。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, DELIVERY_NPC, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1352, QUEST_ID);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, DELIVERY_NPC, QUEST_ID, 10000)));
		assertEquals(1, state.getQuestVars().getQuestVars(), "步 1 推进必须写 var0=1");
		NativeTalkFixture.assertCloseDialog(player);
		assertEquals(List.of("give:" + SUPPLY_REQUEST + ":1", "remove:" + SUPPLY_REQUEST + ":1"),
			inventory.calls(), "物品通道 = 接取发放 + 步 1 扣除");

		// 中继满：交付确认页（select5=2375）→ 1009 推进 REWARD + 奖励窗（页 5）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, START_AND_REWARD_NPC, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 2375, QUEST_ID);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, START_AND_REWARD_NPC, QUEST_ID, 1009)));
		assertEquals(QuestStatus.REWARD, state.getStatus(), "确认动作必须推进到 REWARD");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 5, QUEST_ID);
	}
}
