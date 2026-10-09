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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务 1192「贝尔特伦要塞的支援请求」的原版三段交付链。
 * Locks quest 1192's retail three-step hand-over chain.
 * <p>
 * 任务已退役（保留清单 owner=RETAIL_TABLE）：旧 typed 节点/转换金标随迁移退场，按计划 §8.9（P3 重锚口径）
 * 改锚原版表行（接取 Spatalos → Lavirintos 步 1 → Xenophon 步 2 → Spatalos 交付、步 1 扣书信）、
 * quest.xml 奖励与 native 对话面。中继步按 var0 分轴门控：未轮到的步零响应，跳步领取只回未完成提示页。
 * <p>
 * The retired typed gold standard is re-anchored (plan §8.9) to the retail row (accept Spatalos →
 * Lavirintos step 1 → Xenophon step 2 → Spatalos hand-in, step-1 letter removal), the quest.xml rewards
 * and the native faces. Relay steps gate on var0: an unreached step stays silent and a skipped hand-in
 * only gets the in-progress page.
 */
class Quest1192StepChainContractTest {
	private static final int QUEST_ID = 1192;
	private static final int SPATALOS = 203098;
	private static final int LAVIRINTOS = 203701;
	private static final int XENOPHON = 203833;
	/** 原版 give_item / remove_item1 = ITEM_DOC_QUEST_1192A 1。 / The retail letter item. */
	private static final int REINFORCEMENT_REQUEST = 182200556;

	@Test
	void retailRowKeepsTheThreeStepChainAndBothRelayOwners() throws Exception {
		assertTrue(RetiredQuestIds.contains(QUEST_ID));
		assertTrue(SimpleTalkHandler.instance().routes(QUEST_ID), "SimpleTalk native 车道必须路由 1192");
		assertFalse(ProductionQuestDefinitions.catalog().findExecutable(QUEST_ID).isPresent(),
			"退役后 typed 目录不得再持有 1192");

		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		assertEquals("Spatalos", handler.requireRow(QUEST_ID).acquiredNpcName(), "接取 owner 名");
		assertEquals(List.of("Lavirintos", "Xenophon"), handler.requireRow(QUEST_ID).talkNpcNames(),
			"原版中继链（talk_npc1 → talk_npc2）");
		assertEquals("Spatalos", handler.requireRow(QUEST_ID).rewardNpcName(), "交付 owner 名");
		assertEquals(SPATALOS, NativeNpcNameResolver.instance().resolve("Spatalos").npcIds().get(0));
		assertEquals(LAVIRINTOS, NativeNpcNameResolver.instance().resolve("Lavirintos").npcIds().get(0));
		assertEquals(XENOPHON, NativeNpcNameResolver.instance().resolve("Xenophon").npcIds().get(0));
		assertEquals(SPATALOS, handler.acquireNpc(QUEST_ID), "接取 owner = Spatalos");
		assertEquals(SPATALOS, handler.rewardNpc(QUEST_ID), "交付 owner = Spatalos");
		assertEquals(2, handler.relayCount(QUEST_ID), "1192 是两步中继行");
		assertTrue(handler.relaysForNpc(LAVIRINTOS).contains(
			new SimpleTalkHandler.RelayStep(QUEST_ID, 1, LAVIRINTOS)), "步 1 = Lavirintos");
		assertTrue(handler.relaysForNpc(XENOPHON).contains(
			new SimpleTalkHandler.RelayStep(QUEST_ID, 2, XENOPHON)), "步 2 = Xenophon");
		assertEquals(Integer.valueOf(1193), handler.conQuest(QUEST_ID), "原版 con_quest = 1193");
		assertEquals(new SimpleTalkHandler.ItemStack(REINFORCEMENT_REQUEST, 1),
			handler.acceptGiveItem(QUEST_ID), "接取发放书信");
		assertEquals(new SimpleTalkHandler.ItemStack(REINFORCEMENT_REQUEST, 1),
			handler.stepRemoveItem(QUEST_ID, 1), "步 1 扣除书信");
		assertEquals(List.of(), handler.workItems(QUEST_ID), "无 item_check 行不得带交付门物品");

		QuestMetadata metadata = RetailQuestDriver.ensureLoaded()
			.retailMetadataOf(QUEST_ID).orElseThrow().metadata();
		assertEquals(18, metadata.minLevel(), "原版 minlevel_permitted=18");
		assertEquals(java.util.Set.of("ELYOS"), metadata.permittedRaces(), "原版 pc_light");
		List<QuestReward> rewards = metadata.rewards();
		assertTrue(rewards.contains(new QuestReward("EXP", 0, 73200)), () -> rewards.toString());
		assertTrue(rewards.contains(new QuestReward("ITEM", 188051196, 1)), () -> rewards.toString());
	}

	@Test
	void eachStepAdvancesItsOwnStateAndNeverJumpsStraightToReward() {
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleTalkHandler handler = NativeTalkFixture.handler(inventory);
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 18);

		// 未接取：任务行打开客户端声明的入口页（信页 select1=1011，带任务上下文）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, SPATALOS, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player,
			NativeTalkFixture.clientEntryPage(QUEST_ID), QUEST_ID);

		NativeTalkFixture.add(player, QUEST_ID, QuestStatus.START, 0);

		// 步 1（Lavirintos）：任务行打开该步页（select2=1352）；步 2 未轮到 → 零响应，不跳步。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, LAVIRINTOS, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1352, QUEST_ID);

		NativeTalkFixture.clearPackets(player);
		assertFalse(handler.onDialog(NativeTalkFixture.dialog(player, XENOPHON, QUEST_ID, 31)),
			"未轮到的中继步必须零响应");

		// 跳步领取无效：交付 NPC 在未集齐中继时只回未完成提示页 10，状态保持 START。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, SPATALOS, QUEST_ID, 1009)));
		NativeTalkFixture.assertOnlyDialogPage(player, 10);
		assertEquals(QuestStatus.START,
			player.getQuestStateList().getQuestState(QUEST_ID).getStatus(), "跳步领取不得推进状态");

		// 步 1 推进（SETPRO1=10000）：var0=1 + 关窗 + 扣除书信。
		inventory.clear();
		inventory.hold(REINFORCEMENT_REQUEST, 1);
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, LAVIRINTOS, QUEST_ID, 10000)));
		assertEquals(1, player.getQuestStateList().getQuestState(QUEST_ID).getQuestVars().getQuestVars(),
			"步 1 推进必须写 var0=1");
		NativeTalkFixture.assertCloseDialog(player);
		assertEquals(List.of("remove:" + REINFORCEMENT_REQUEST + ":1"), inventory.calls(),
			"步 1 必须扣除书信");

		// 步 2（Xenophon）：任务行打开该步页（select3=1693）；推进（SETPRO2=10001）= var0=2 + 关窗。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, XENOPHON, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1693, QUEST_ID);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, XENOPHON, QUEST_ID, 10001)));
		assertEquals(2, player.getQuestStateList().getQuestState(QUEST_ID).getQuestVars().getQuestVars(),
			"步 2 推进必须写 var0=2");
		NativeTalkFixture.assertCloseDialog(player);

		// 中继满：交付确认页（select5=2375）→ 1009 推进 REWARD + 奖励窗（页 5）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, SPATALOS, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 2375, QUEST_ID);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, SPATALOS, QUEST_ID, 1009)));
		assertEquals(QuestStatus.REWARD,
			player.getQuestStateList().getQuestState(QUEST_ID).getStatus(), "确认动作必须推进到 REWARD");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 5, QUEST_ID);
	}
}
