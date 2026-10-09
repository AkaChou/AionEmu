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
 * 锁定任务 29003 的接取/报告/领奖 owner 与简易接取发放（原版 SimpleTalk 车道）。
 * Locks quest 29003's accept/report/reward owners and the simple-accept grant on the retail SimpleTalk lane.
 * <p>
 * 任务已退役（保留清单 owner=RETAIL_TABLE）：旧 typed 转换金标随迁移退场，按计划 §8.9（P3 重锚口径）
 * 改锚原版表行（接取 Ustarn / 交付 Vidar、单步、接取发放文档）、quest.xml 奖励与 native 接取面。
 * <p>
 * The retired typed transition gold standard is re-anchored (plan §8.9) to the retail row (accept Ustarn /
 * hand-in Vidar, single step, accept grant), the quest.xml rewards and the native accept face.
 */
class Quest29003ClientDialogAlignmentTest {
	private static final int QUEST_ID = 29003;
	private static final int START_NPC = 204257;
	private static final int REPORT_NPC = 204052;
	/** 原版 give_item = ITEM_DOC_QUEST_29003A 1。 / The retail accept grant. */
	private static final int DOC_ITEM = 182207142;

	@Test
	void retailRowConfinesTheStartAndReportOwnersAndGrantsTheDocumentOnAccept() throws Exception {
		assertTrue(RetiredQuestIds.contains(QUEST_ID));
		assertTrue(SimpleTalkHandler.instance().routes(QUEST_ID), "SimpleTalk native 车道必须路由 29003");
		assertFalse(ProductionQuestDefinitions.catalog().findExecutable(QUEST_ID).isPresent(),
			"退役后 typed 目录不得再持有 29003");

		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		assertEquals(START_NPC, NativeNpcNameResolver.instance().resolve("Ustarn").npcIds().get(0));
		assertEquals(REPORT_NPC, NativeNpcNameResolver.instance().resolve("Vidar").npcIds().get(0));
		assertEquals(START_NPC, handler.acquireNpc(QUEST_ID), "接取 owner = Ustarn");
		assertEquals(REPORT_NPC, handler.rewardNpc(QUEST_ID), "报告 owner = Vidar");
		assertEquals(0, handler.relayCount(QUEST_ID), "29003 是单步 talk 行");
		assertEquals(new SimpleTalkHandler.ItemStack(DOC_ITEM, 1), handler.acceptGiveItem(QUEST_ID),
			"接取发放 29003 文档");
		assertFalse(handler.requireRow(QUEST_ID).itemCheck(), "原版行不得声明 item_check");
		assertEquals(List.of(), handler.workItems(QUEST_ID), "无 item_check 行不得带交付门物品");

		QuestMetadata metadata = RetailQuestDriver.ensureLoaded()
			.retailMetadataOf(QUEST_ID).orElseThrow().metadata();
		assertEquals(50, metadata.minLevel(), "原版 minlevel_permitted=50");
		assertEquals(java.util.Set.of("ASMODIANS"), metadata.permittedRaces(), "原版 pc_dark");
		assertTrue(metadata.prerequisites().contains(29002)
				|| metadata.startConditions().contains(new QuestStartCondition("finished", 29002, 0)),
			"前置 = 完成 29002");
		List<QuestReward> rewards = metadata.rewards();
		assertTrue(rewards.contains(new QuestReward("EXP", 0, 1347585)), () -> rewards.toString());
		assertTrue(rewards.contains(new QuestReward("ITEM", 125020014, 1)), () -> rewards.toString());
		assertTrue(rewards.contains(new QuestReward("ITEM", 169405022, 30)), () -> rewards.toString());
	}

	@Test
	void acceptAndReportFacesFollowTheNativeDialogPages() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		Player player = NativeTalkFixture.player(Race.ASMODIANS, PlayerClass.WARRIOR, 50);
		Integer acquireNpc = handler.acquireNpc(QUEST_ID);
		Integer reportNpc = handler.rewardNpc(QUEST_ID);
		assertNotNull(acquireNpc);
		assertNotNull(reportNpc);

		// 未接取：完成前置 29002 后任务行打开客户端声明的入口页（信页 select1=1011，带任务上下文）。
		NativeTalkFixture.completePrerequisites(player, 29002);
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, acquireNpc, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player,
			NativeTalkFixture.clientEntryPage(QUEST_ID), QUEST_ID);

		// 进行中（单步 talk 行，无中继步）：任务行只发契约声明的报告确认页（select5=2375），不推进状态。
		NativeTalkFixture.clearPackets(player);
		NativeTalkFixture.add(player, QUEST_ID, QuestStatus.START, 0);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, reportNpc, QUEST_ID, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 2375, QUEST_ID);

		// 报告确认（直翻型 1009）：推进 REWARD + 奖励窗（页 5，带任务上下文）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, reportNpc, QUEST_ID, 1009)));
		assertEquals(QuestStatus.REWARD,
			player.getQuestStateList().getQuestState(QUEST_ID).getStatus(), "确认动作必须推进到 REWARD");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 5, QUEST_ID);
	}
}
