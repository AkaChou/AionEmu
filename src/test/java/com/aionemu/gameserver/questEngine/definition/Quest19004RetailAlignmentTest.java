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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务 19004（圣所对话链）的原版中继链与交付 owner。
 * Locks quest 19004's retail talk chain and hand-in owners.
 * <p>
 * 任务已退役（保留清单 owner=RETAIL_TABLE）：旧 typed 转换金标随迁移退场，按计划 §8.9（P3 重锚口径）
 * 改锚原版表行（Perikles → Jucleas 步 1 → Lavirintos 步 2 → Hilarus 交付）、quest.xml 奖励与 native 对话面。
 * <p>
 * The retired typed transition gold standard is re-anchored (plan §8.9) to the retail row (Perikles →
 * Jucleas step 1 → Lavirintos step 2 → Hilarus hand-in), the quest.xml rewards and the native faces.
 */
class Quest19004RetailAlignmentTest {
	private static final int QUEST_ID = 19004;
	private static final int PERIKLES_NPC = 203757;
	private static final int JUCLEAS_NPC = 203752;
	private static final int LAVIRINTOS_NPC = 203701;
	private static final int HILARUS_NPC = 798500;

	@Test
	void retailRowKeepsTheTalkChainAndTheRewardOwners() throws Exception {
		assertTrue(RetiredQuestIds.contains(QUEST_ID));
		assertTrue(SimpleTalkHandler.instance().routes(QUEST_ID), "SimpleTalk native 车道必须路由 19004");
		assertFalse(ProductionQuestDefinitions.catalog().findExecutable(QUEST_ID).isPresent(),
			"退役后 typed 目录不得再持有 19004");

		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		assertEquals("Perikles", handler.requireRow(QUEST_ID).acquiredNpcName(), "接取 owner 名");
		assertEquals(List.of("Jucleas", "Lavirintos"), handler.requireRow(QUEST_ID).talkNpcNames(),
			"原版中继链（talk_npc1 → talk_npc2）");
		assertEquals("Hilarus", handler.requireRow(QUEST_ID).rewardNpcName(), "交付 owner 名");
		assertEquals(PERIKLES_NPC, handler.acquireNpc(QUEST_ID), "接取 owner = Perikles");
		assertEquals(HILARUS_NPC, handler.rewardNpc(QUEST_ID), "交付 owner = Hilarus");
		assertEquals(2, handler.relayCount(QUEST_ID), "19004 是两步中继行");
		assertTrue(handler.relaysForNpc(JUCLEAS_NPC).contains(
			new SimpleTalkHandler.RelayStep(QUEST_ID, 1, JUCLEAS_NPC)), "步 1 = Jucleas");
		assertTrue(handler.relaysForNpc(LAVIRINTOS_NPC).contains(
			new SimpleTalkHandler.RelayStep(QUEST_ID, 2, LAVIRINTOS_NPC)), "步 2 = Lavirintos");
		assertEquals(PERIKLES_NPC, NativeNpcNameResolver.instance().resolve("Perikles").npcIds().get(0));
		assertEquals(JUCLEAS_NPC, NativeNpcNameResolver.instance().resolve("Jucleas").npcIds().get(0));
		assertEquals(LAVIRINTOS_NPC, NativeNpcNameResolver.instance().resolve("Lavirintos").npcIds().get(0));
		assertEquals(HILARUS_NPC, NativeNpcNameResolver.instance().resolve("Hilarus").npcIds().get(0));

		QuestMetadata metadata = RetailQuestDriver.ensureLoaded()
			.retailMetadataOf(QUEST_ID).orElseThrow().metadata();
		assertEquals(29, metadata.minLevel(), "原版 minlevel_permitted=29");
		assertEquals(java.util.Set.of("ELYOS"), metadata.permittedRaces(), "原版 pc_light");
		List<QuestReward> rewards = metadata.rewards();
		assertTrue(rewards.contains(new QuestReward("GOLD", 0, 9830)), () -> rewards.toString());
		assertTrue(rewards.contains(new QuestReward("EXP", 0, 37405)), () -> rewards.toString());
	}

	@Test
	void talkChainAndReportFacesFollowTheNativeDialogPages() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 29);
		NativeTalkFixture.add(player, QUEST_ID, QuestStatus.START, 0);

		// 步 1（Jucleas）：任务行打开该步页（select2=1352，带任务上下文）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, JUCLEAS_NPC, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1352, QUEST_ID);

		// 尚未轮到的步 2：零响应，不跳步（vars=0 < step-1）。
		NativeTalkFixture.clearPackets(player);
		assertFalse(handler.onDialog(NativeTalkFixture.dialog(player, LAVIRINTOS_NPC, QUEST_ID, 31)),
			"未轮到的中继步必须零响应");

		// 步 1 推进（SETPRO1=10000）：var0=1 + 关窗（原版 after-commit 零发页）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, JUCLEAS_NPC, QUEST_ID, 10000)));
		assertEquals(1, player.getQuestStateList().getQuestState(QUEST_ID).getQuestVars().getQuestVars(),
			"步 1 推进必须写 var0=1");
		NativeTalkFixture.assertCloseDialog(player);

		// 步 2（Lavirintos）：任务行打开该步页（select3=1693）；子页 SELECT3_1 回发。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, LAVIRINTOS_NPC, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1693, QUEST_ID);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, LAVIRINTOS_NPC, QUEST_ID,
			QuestDialogPage.SELECT3_1.id())));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, QuestDialogPage.SELECT3_1.id(), QUEST_ID);

		// 步 2 推进（SETPRO2=10001）：var0=2 + 关窗。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, LAVIRINTOS_NPC, QUEST_ID, 10001)));
		assertEquals(2, player.getQuestStateList().getQuestState(QUEST_ID).getQuestVars().getQuestVars(),
			"步 2 推进必须写 var0=2");
		NativeTalkFixture.assertCloseDialog(player);

		// 报告（Hilarus）：中继满后才发报告确认页（select5=2375），1009 推进 REWARD + 奖励窗（页 5）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, HILARUS_NPC, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 2375, QUEST_ID);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, HILARUS_NPC, QUEST_ID, 1009)));
		assertEquals(QuestStatus.REWARD,
			player.getQuestStateList().getQuestState(QUEST_ID).getStatus(), "确认动作必须推进到 REWARD");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 5, QUEST_ID);
	}
}
