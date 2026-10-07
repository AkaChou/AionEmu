package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.tablelane.NativeNpcNameResolver;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务 1553 的真端中继链（会说话的镜子注入魔力 → 佩兰托军团长询问 → 皮埃拉交付）。
 * Locks quest 1553's retail relay chain (talking mirror infusion → Perento inquiry → Piera hand-in).
 * <p>
 * 任务已退役（保留清单 owner=RETAIL_TABLE）：旧 typed 节点/转换金标随迁移退场，按计划 §8.9（P3 重锚口径）
 * 改锚真端表行（接取 Diana / 中继 镜子→佩兰托 / 交付 Piera、步 1 物换物）、quest.xml 前置/奖励与 native 对话面。
 * <p>
 * The retired typed node/transition gold standard is re-anchored (plan §8.9) to the retail row (accept
 * Diana / relays mirror → Perento / hand-in Piera, step-1 item swap), the quest.xml prerequisite/rewards
 * and the native faces.
 */
class Quest1553ClientDialogAlignmentTest {
	private static final int QUEST_ID = 1553;
	private static final int DIANA_NPC = 203786;
	private static final int TALKING_MIRROR_NPC = 730051;
	private static final int PERENTO_NPC = 204500;
	private static final int PIERA_NPC = 204584;

	/** 真端 give_item = ITEM_QUEST_1553A 1（待注入的镜子）。 / The retail accept grant (plain mirror). */
	private static final int INITIAL_MIRROR_ITEM = 182201794;
	/** 真端 give_item1 = ITEM_QUEST_1553B 1（注入魔力后的镜子）。 / The step-1 grant (infused mirror). */
	private static final int INFUSED_MIRROR_ITEM = 182201795;

	@Test
	void retailRowAnchorsTheMirrorChainAndThePieraHandIn() throws Exception {
		assertTrue(RetiredQuestIds.contains(QUEST_ID));
		assertTrue(SimpleTalkHandler.instance().routes(QUEST_ID), "SimpleTalk native 车道必须路由 1553");
		assertFalse(ProductionQuestDefinitions.catalog().findExecutable(QUEST_ID).isPresent(),
			"退役后 typed 目录不得再持有 1553");

		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		assertEquals("Diana", handler.requireRow(QUEST_ID).acquiredNpcName(), "接取 owner 名");
		assertEquals(List.of("DF2_NPC_TalkingMirror", "Perento"), handler.requireRow(QUEST_ID).talkNpcNames(),
			"真端中继链（talk_npc1 → talk_npc2）");
		assertEquals("Piera", handler.requireRow(QUEST_ID).rewardNpcName(), "交付 owner 名");
		assertEquals(DIANA_NPC, NativeNpcNameResolver.instance().resolve("Diana").npcIds().get(0));
		assertEquals(TALKING_MIRROR_NPC,
			NativeNpcNameResolver.instance().resolve("DF2_NPC_TalkingMirror").npcIds().get(0));
		assertEquals(PERENTO_NPC, NativeNpcNameResolver.instance().resolve("Perento").npcIds().get(0));
		assertEquals(PIERA_NPC, NativeNpcNameResolver.instance().resolve("Piera").npcIds().get(0));
		assertEquals(DIANA_NPC, handler.acquireNpc(QUEST_ID), "接取 owner = Diana");
		assertEquals(PIERA_NPC, handler.rewardNpc(QUEST_ID), "交付 owner = Piera");
		assertEquals(2, handler.relayCount(QUEST_ID), "1553 是两步中继行");
		assertTrue(handler.relaysForNpc(TALKING_MIRROR_NPC).contains(
			new SimpleTalkHandler.RelayStep(QUEST_ID, 1, TALKING_MIRROR_NPC)), "步 1 = 会说话的镜子");
		assertTrue(handler.relaysForNpc(PERENTO_NPC).contains(
			new SimpleTalkHandler.RelayStep(QUEST_ID, 2, PERENTO_NPC)), "步 2 = Perento");
		assertEquals(Integer.valueOf(1554), handler.conQuest(QUEST_ID), "真端 con_quest = 1554");

		assertEquals(new SimpleTalkHandler.ItemStack(INITIAL_MIRROR_ITEM, 1), handler.acceptGiveItem(QUEST_ID),
			"接取发放待注入的镜子");
		assertEquals(new SimpleTalkHandler.ItemStack(INFUSED_MIRROR_ITEM, 1), handler.stepGiveItem(QUEST_ID, 1),
			"步 1 发放注入魔力后的镜子");
		assertEquals(new SimpleTalkHandler.ItemStack(INITIAL_MIRROR_ITEM, 1),
			handler.stepRemoveItem(QUEST_ID, 1), "步 1 扣除待注入的镜子");
		assertNull(handler.stepGiveItem(QUEST_ID, 2), "步 2 无发放");
		assertNull(handler.stepRemoveItem(QUEST_ID, 2), "步 2 无扣除");
		assertFalse(handler.requireRow(QUEST_ID).itemCheck(), "真端行不得声明 item_check");
		assertEquals(List.of(), handler.workItems(QUEST_ID), "无 item_check 行不得带交付门物品");

		QuestMetadata metadata = RetailQuestDriver.ensureLoaded()
			.retailMetadataOf(QUEST_ID).orElseThrow().metadata();
		assertEquals(43, metadata.minLevel(), "真端 minlevel_permitted=43");
		assertEquals(java.util.Set.of("ELYOS"), metadata.permittedRaces(), "真端 pc_light");
		assertTrue(metadata.prerequisites().contains(1550)
				|| metadata.startConditions().contains(new QuestStartCondition("finished", 1550, 0)),
			"前置 = 完成 1550");
		List<QuestReward> rewards = metadata.rewards();
		assertTrue(rewards.contains(new QuestReward("EXP", 0, 2954681)), () -> rewards.toString());
		assertTrue(rewards.contains(new QuestReward("AP", 0, 200)), () -> rewards.toString());
	}

	@Test
	void acceptFaceIsDianasAloneAndTheChainFacesOpenStepPages() {
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleTalkHandler handler = NativeTalkFixture.handler(inventory);
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 43);

		// 未接取 owner 隔离：镜子/佩兰托/皮埃拉都不是接取面（零响应）。
		for (int npcId : List.of(TALKING_MIRROR_NPC, PERENTO_NPC, PIERA_NPC)) {
			NativeTalkFixture.clearPackets(player);
			assertFalse(handler.onDialog(NativeTalkFixture.dialog(player, npcId, QUEST_ID, 31)),
				"非接取 owner 不得开放接取面: " + npcId);
		}

		// 未接取：完成前置 1550 后 Diana 打开客户端声明的入口页（信页 select1=1011）。
		NativeTalkFixture.completePrerequisites(player, 1550);
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, DIANA_NPC, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player,
			NativeTalkFixture.clientEntryPage(QUEST_ID), QUEST_ID);

		// 步 1（会说话的镜子）：任务行打开该步页（select2=1352，带任务上下文）。
		NativeTalkFixture.add(player, QUEST_ID, QuestStatus.START, 0);
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, TALKING_MIRROR_NPC, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1352, QUEST_ID);

		// 步 1 推进（SETPRO1=10000）：var0=1 + 关窗 + 物换物（发放注入镜、扣除原镜）。
		inventory.clear();
		inventory.hold(INITIAL_MIRROR_ITEM, 1);
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, TALKING_MIRROR_NPC, QUEST_ID, 10000)));
		assertEquals(1, player.getQuestStateList().getQuestState(QUEST_ID).getQuestVars().getQuestVars(),
			"步 1 推进必须写 var0=1");
		NativeTalkFixture.assertCloseDialog(player);
		assertEquals(List.of("give:" + INFUSED_MIRROR_ITEM + ":1", "remove:" + INITIAL_MIRROR_ITEM + ":1"),
			inventory.calls(), "步 1 必须完成物换物");

		// 步 2（佩兰托）：任务行打开该步页（select3=1693）；子页 SELECT3_1 回发。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, PERENTO_NPC, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1693, QUEST_ID);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, PERENTO_NPC, QUEST_ID,
			QuestDialogPage.SELECT3_1.id())));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, QuestDialogPage.SELECT3_1.id(), QUEST_ID);

		// 步 2 推进（SETPRO2=10001）：var0=2 + 关窗，无物品通道流量。
		inventory.clear();
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, PERENTO_NPC, QUEST_ID, 10001)));
		assertEquals(2, player.getQuestStateList().getQuestState(QUEST_ID).getQuestVars().getQuestVars(),
			"步 2 推进必须写 var0=2");
		NativeTalkFixture.assertCloseDialog(player);
		assertEquals(List.of(), inventory.calls(), "步 2 不得触碰物品通道");

		// 报告（皮埃拉）：中继满后才发报告确认页（select5=2375），1009 推进 REWARD + 奖励窗（页 5）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, PIERA_NPC, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 2375, QUEST_ID);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, PIERA_NPC, QUEST_ID, 1009)));
		assertEquals(QuestStatus.REWARD,
			player.getQuestStateList().getQuestState(QUEST_ID).getStatus(), "确认动作必须推进到 REWARD");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 5, QUEST_ID);
	}
}
