package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestTableLoader;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.questEngine.tablelane.SimpleHuntHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务 1842（深层克罗坦全歼，可重复）的原版重开生命周期：完成后可再次接取并清零两路计数。
 * Locks quest 1842's retail repeat lifecycle: a completed row re-opens and both counters reset.
 * <p>
 * 任务已退役（保留清单 owner=RETAIL_TABLE）：旧断言按 typed 目录的 unaccepted 转换 + planner 投影，
 * 随迁移退场；按计划 §8.9（P3 重锚口径）改锚 native 接取面（可重复行 COMPLETE 态开放接取窗，
 * 与 3733 同形）——原版 80+1 双槽计数在 20000 建档时清零（`NativeQuestStartPort.start`）。
 * <p>
 * The retired typed assertions (unaccepted transition + planner projection) are re-anchored (plan §8.9)
 * to the native accept face: a repeatable row re-opens at COMPLETE and the 80+1 kill counters reset on
 * the 20000 accept tail, the same shape as quest 3733.
 */
class Quest1842RepeatLifecycleTest {

	private static final int QUEST_ID = 1842;

	@Test
	void completedRepeatableRowCanRestartAndResetsBothKillCounters() throws Exception {
		SimpleHuntHandler handler = SimpleHuntHandler.instance();
		NativeQuestTableLoader.SimpleHuntRow row = NativeQuestTableLoader.instance().require(QUEST_ID);
		assertEquals(2, row.killSlots().size(), "1842 是双槽行（80 小怪 + 1 精英）");
		assertEquals(80, row.killSlots().get(1).count());
		assertEquals(1, row.killSlots().get(2).count());
		assertEquals(255, RetailQuestDriver.ensureLoaded().retailMetadataOf(QUEST_ID).orElseThrow()
			.metadata().repeatPolicy().maxRepeatCount(), "原版 max_repeat_count=255 ⇒ 可重复");
		Integer acquireNpc = handler.acquireNpc(QUEST_ID);
		assertNotNull(acquireNpc, "原版行必须有可解析的接取 NPC");

		com.aionemu.gameserver.model.gameobjects.player.Player player = NativeTalkFixture.player(
			com.aionemu.gameserver.model.Race.ELYOS, com.aionemu.gameserver.model.PlayerClass.WARRIOR, 45);
		QuestState state = NativeTalkFixture.add(player, QUEST_ID, QuestStatus.COMPLETE, 0);
		state.setCompleteCount(1);

		// COMPLETE 态点任务行：可重复行重新开放接取面（客户端入口页，带任务上下文）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, acquireNpc, QUEST_ID, 31)),
			"可重复行 COMPLETE 态必须开放接取面");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player,
			NativeTalkFixture.clientEntryPage(QUEST_ID), QUEST_ID);

		// 20000 建档收尾：状态复位 START、两路计数清零、完成次数保留（重复预算依据）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, acquireNpc, QUEST_ID, 20000)));
		assertEquals(QuestStatus.START, state.getStatus(), "重复接取必须复位为 START");
		assertEquals(0, state.getQuestVars().getQuestVars(), "重复接取必须清零双槽计数");
		assertEquals(1, state.getCompleteCount(), "重复接取不得重置完成次数（重复预算依据）");
		assertTrue(List.of(1, 2).containsAll(row.killSlots().keySet()));
	}
}
