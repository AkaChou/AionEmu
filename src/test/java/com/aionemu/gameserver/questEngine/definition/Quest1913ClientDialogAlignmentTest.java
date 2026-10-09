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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务 1913（贝尔特伦派遣）的原版单步中继与交付 owner。
 * Locks quest 1913's retail single-step relay and hand-in owners.
 * <p>
 * 任务已退役（保留清单 owner=RETAIL_TABLE）：旧 typed 转换金标随迁移退场，按计划 §8.9（P3 重锚口径）
 * 改锚原版表行（接取 Macus → Polyidus 步 1 → Hyacinte 交付）、quest.xml 奖励与 native 对话面。
 * 旧 typed 定义在步 1 上的传送（210030000）是退役 XML 的作者效果，原版 talk 行未声明任何传送列，
 * 车道推进语义 = var0=1 + 关窗。
 * <p>
 * The retired typed transition gold standard is re-anchored (plan §8.9) to the retail row (accept
 * Macus → Polyidus step 1 → Hyacinte hand-in), the quest.xml rewards and the native faces. The old
 * typed teleport (210030000) was a retired-XML authoring effect; the retail talk row declares no
 * teleport column, so the lane advance is var0=1 plus the close dialog.
 */
class Quest1913ClientDialogAlignmentTest {
	private static final int QUEST_ID = 1913;
	private static final int START_NPC = 203758;
	private static final int PROGRESS_NPC = 203726;
	private static final int REPORT_NPC = 203097;
	/** 原版 reward_item1_1 = FOOD_dpheal_40A 5。 / The retail item reward. */
	private static final int FOOD_ITEM = 160001273;

	@Test
	void retailRowAnchorsTheSingleStepChainAndTheHyacinteHandIn() throws Exception {
		assertTrue(RetiredQuestIds.contains(QUEST_ID));
		assertTrue(SimpleTalkHandler.instance().routes(QUEST_ID), "SimpleTalk native 车道必须路由 1913");
		assertFalse(ProductionQuestDefinitions.catalog().findExecutable(QUEST_ID).isPresent(),
			"退役后 typed 目录不得再持有 1913");

		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		assertEquals("Macus", handler.requireRow(QUEST_ID).acquiredNpcName(), "接取 owner 名");
		assertEquals(List.of("Polyidus"), handler.requireRow(QUEST_ID).talkNpcNames(), "原版中继链");
		assertEquals("Hyacinte", handler.requireRow(QUEST_ID).rewardNpcName(), "交付 owner 名");
		assertEquals(START_NPC, NativeNpcNameResolver.instance().resolve("Macus").npcIds().get(0));
		assertEquals(PROGRESS_NPC, NativeNpcNameResolver.instance().resolve("Polyidus").npcIds().get(0));
		assertEquals(REPORT_NPC, NativeNpcNameResolver.instance().resolve("Hyacinte").npcIds().get(0));
		assertEquals(START_NPC, handler.acquireNpc(QUEST_ID), "接取 owner = Macus");
		assertEquals(REPORT_NPC, handler.rewardNpc(QUEST_ID), "交付 owner = Hyacinte");
		assertEquals(1, handler.relayCount(QUEST_ID), "1913 是单步中继行");
		assertTrue(handler.relaysForNpc(PROGRESS_NPC).contains(
			new SimpleTalkHandler.RelayStep(QUEST_ID, 1, PROGRESS_NPC)), "步 1 = Polyidus");
		assertNull(handler.acceptGiveItem(QUEST_ID), "原版行无 give_item");
		assertNull(handler.stepGiveItem(QUEST_ID, 1), "步 1 无发放");
		assertNull(handler.stepRemoveItem(QUEST_ID, 1), "步 1 无扣除");

		QuestMetadata metadata = RetailQuestDriver.ensureLoaded()
			.retailMetadataOf(QUEST_ID).orElseThrow().metadata();
		assertEquals(10, metadata.minLevel(), "原版 minlevel_permitted=10");
		assertEquals(java.util.Set.of("ELYOS"), metadata.permittedRaces(), "原版 pc_light");
		assertTrue(metadata.permittedClasses().contains("GLADIATOR")
				&& metadata.permittedClasses().contains("TEMPLAR"), "原版 class_permitted=fighter knight");
		assertTrue(metadata.startConditions().contains(new QuestStartCondition("finished", 1007, 0))
				|| metadata.prerequisites().contains(1007), "前置 = 完成 1007（奖励分支 1）");
		List<QuestReward> rewards = metadata.rewards();
		assertTrue(rewards.contains(new QuestReward("EXP", 0, 14046)), () -> rewards.toString());
		assertTrue(rewards.contains(new QuestReward("ITEM", FOOD_ITEM, 5)), () -> rewards.toString());
	}

	@Test
	void acceptRelayAndReportFacesFollowTheNativeDialogPages() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.GLADIATOR, 10);
		Integer acquireNpc = handler.acquireNpc(QUEST_ID);
		Integer reportNpc = handler.rewardNpc(QUEST_ID);
		assertNotNull(acquireNpc);
		assertNotNull(reportNpc);

		// 未接取：完成前置 1007 后任务行打开客户端声明的入口页（信页 select1=1011）；1007 开接取窗页 4。
		NativeTalkFixture.completePrerequisites(player, 1007);
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, acquireNpc, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player,
			NativeTalkFixture.clientEntryPage(QUEST_ID), QUEST_ID);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, acquireNpc, QUEST_ID,
			QuestDialogAction.ASK_QUEST_ACCEPT.id())));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 4, QUEST_ID);

		// 步 1（Polyidus）：任务行打开该步页（select2=1352，带任务上下文）。
		NativeTalkFixture.add(player, QUEST_ID, QuestStatus.START, 0);
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, PROGRESS_NPC, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1352, QUEST_ID);

		// 步 1 推进（SETPRO1=10000）：var0=1 + 关窗（原版 after-commit 零发页）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, PROGRESS_NPC, QUEST_ID, 10000)));
		assertEquals(1, player.getQuestStateList().getQuestState(QUEST_ID).getQuestVars().getQuestVars(),
			"步 1 推进必须写 var0=1");
		NativeTalkFixture.assertCloseDialog(player);

		// 报告（Hyacinte）：中继满后才发报告确认页（select5=2375），1009 推进 REWARD + 奖励窗（页 5）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, reportNpc, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 2375, QUEST_ID);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, reportNpc, QUEST_ID, 1009)));
		assertEquals(QuestStatus.REWARD,
			player.getQuestStateList().getQuestState(QUEST_ID).getStatus(), "确认动作必须推进到 REWARD");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 5, QUEST_ID);
	}
}
