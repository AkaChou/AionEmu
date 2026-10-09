package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * 中继已完成步不得回放（2026-10-08 实机 13700/13800 死循环回归）。
 * <p>
 * 两行的末位中继与交付是同一名字节点（13700：talk_npc1=reward=LDF4_Advance_Elger_E；
 * 13800：talk_npc2=reward=LDF5_Fortress_Alphion_E）。缺陷形态：中继分支对任务行打开（31）只
 * 静默「未轮到」的步，**已完成的步照样回放步页并接管**，把报告分支永久遮蔽——31 恒回步页 →
 * 10000/10001 重放只关窗，玩家在「步页 → 子页 → 重放关窗」里无限打转，任务永远交不了。
 * 原版投影（退役 XML）：13700 = NPC_REPORT page=SELECT5@802350；13800 = REWARD 态
 * QUEST_SELECT → SELECT5@802431——节点步进后 NPC 即越过该步，31 落到报告页。
 * <p>
 * A finished relay step must never replay (live 13700/13800 loop regression, 2026-10-08). Both
 * rows put the last relay and the reward on the same name node, so replaying the step page
 * permanently shadowed the report branch: the quest could never be turned in. The retired retail
 * projections show the NPC falls through to the report page once past its step.
 */
class SimpleTalkRelayDoneStepFallthroughTest {

	/** 13700「게르하를 향하여」：单步中继，中继 NPC = 交付 NPC = LDF4_Advance_Elger_E。 */
	private static final int QUEST_13700 = 13700;

	/** 13800「새로운 땅의 발견」：两步中继，末位中继 NPC = 交付 NPC = LDF5_Fortress_Alphion_E。 */
	private static final int QUEST_13800 = 13800;

	@Test
	void sharedRelayRewardNpcOpensTheReportPageOnceTheStepIsDone() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		assertTrue(handler.routes(QUEST_13700), "13700 必须由 native SimpleTalk 车道路由");
		assertEquals(1, handler.relayCount(QUEST_13700), "原版 talk_npc1 单步中继");
		int elger = NativeNpcNameResolver.instance().resolveMembers("LDF4_Advance_Elger_E").get(0);
		assertTrue(handler.rewardNpcs(QUEST_13700).contains(elger),
			"交付节点与中继节点同名（死循环前提）");

		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.GLADIATOR, 65);
		QuestState state = NativeTalkFixture.add(player, QUEST_13700, QuestStatus.START, 0);

		// 当前步：31 → select2=1352；子页 1353 回显（实机轨迹前两拍）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, elger, QUEST_13700, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1352, QUEST_13700);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, elger, QUEST_13700, 1353)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1353, QUEST_13700);

		// 推进 10000：var0=1 + 关窗（实机轨迹第三拍；原版 cabb10 零发页）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, elger, QUEST_13700, 10000)));
		assertEquals(1, state.getQuestVars().getQuestVars(), "步 1 推进必须写 var0=1");
		NativeTalkFixture.assertCloseDialog(player);

		// 回归点：已完成的步不再回放 1352——31 直接开报告确认页 select5=2375（缺陷修复前恒回 1352）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, elger, QUEST_13700, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 2375, QUEST_13700);

		// 报告确认 1009 → REWARD + 奖励窗（页 5）——死循环出口。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, elger, QUEST_13700, 1009)));
		assertEquals(QuestStatus.REWARD, state.getStatus(), "报告确认必须推进到 REWARD");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 5, QUEST_13700);
	}

	@Test
	void midChainDoneStepStopsReplayingAndTheLastRelayReportsAtTheSameNpc() {
		// 13800 第 1 步有 remove_item1（扣书信）⇒ 记录式假背包端口（单测无服务栈，生产端口会 NPE）。
		// Step 1 removes the letter, so the recording inventory port stands in (no service stack in
		// unit tests; the live port would NPE).
		SimpleTalkHandler handler = NativeTalkFixture.handler(NativeTalkFixture.RecordingInventory.EMPTY);
		assertTrue(handler.routes(QUEST_13800), "13800 必须由 native SimpleTalk 车道路由");
		assertEquals(2, handler.relayCount(QUEST_13800), "原版 talk_npc1..2 两步中继");
		int teleport = NativeNpcNameResolver.instance().resolveMembers("LF5_OP1_ZoneTeleport_L").get(0);
		int alphion = NativeNpcNameResolver.instance().resolveMembers("LDF5_Fortress_Alphion_E").get(0);
		assertTrue(handler.rewardNpcs(QUEST_13800).contains(alphion),
			"交付节点与末位中继节点同名（死循环前提）");
		assertFalse(handler.rewardNpcs(QUEST_13800).contains(teleport),
			"首站传送点不是交付节点（中链回放形态）");

		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.GLADIATOR, 65);
		QuestState state = NativeTalkFixture.add(player, QUEST_13800, QuestStatus.START, 0);

		// 行 0（传送点，步 1 当前）：31 → 1352；推进 10000 → var0=1 + 关窗。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, teleport, QUEST_13800, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1352, QUEST_13800);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, teleport, QUEST_13800, 10000)));
		assertEquals(1, state.getQuestVars().getQuestVars(), "步 1 推进必须写 var0=1");
		NativeTalkFixture.assertCloseDialog(player);

		// 回归点（中链）：已完成的步不再回放——传送点被原版节点越过，native 车道零接管回默认对话。
		NativeTalkFixture.clearPackets(player);
		assertFalse(handler.onDialog(NativeTalkFixture.dialog(player, teleport, QUEST_13800, 31)),
			"已完成的步不得再由中继分支接管");
		assertEquals(List.of(), NativeTalkFixture.dialogPages(player), "越过节点的 NPC 不得回放步页");

		// 行 1（Alphion，步 2 当前）：31 → select3=1693；子页 1694 回显；推进 10001 → var0=2 + 关窗。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, alphion, QUEST_13800, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1693, QUEST_13800);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, alphion, QUEST_13800, 1694)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1694, QUEST_13800);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, alphion, QUEST_13800, 10001)));
		assertEquals(2, state.getQuestVars().getQuestVars(), "步 2 推进必须写 var0=2");
		NativeTalkFixture.assertCloseDialog(player);

		// 回归点（末位中继 = 交付）：步满后 31 → 报告确认页 select5=2375（缺陷修复前恒回 1693）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, alphion, QUEST_13800, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 2375, QUEST_13800);

		// 报告确认 1009 → REWARD + 奖励窗（页 5）——死循环出口。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, alphion, QUEST_13800, 1009)));
		assertEquals(QuestStatus.REWARD, state.getStatus(), "报告确认必须推进到 REWARD");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 5, QUEST_13800);
	}
}
