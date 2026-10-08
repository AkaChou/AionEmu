package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;

/**
 * 未绑定交付对象的奖励窗确认动作恢复段门禁（2026-10-08 实机 19640）。
 * <p>
 * 实机链路：19640（接取 Elim_DF4_01/799022，交付 Barus/798991）击杀完成进 REWARD 后，玩家在接取对象
 * 799022 处被传送门类 AI 的开门页轴开出奖励窗（页 5）→ 点「选择奖励 1」（动作 8）→ 各族领奖腿只注册在
 * 交付对象上、全程无人认领 → PortalDialogAI2 把动作 id 回显为页 8 → 客户端
 * {@code Quest_Q19640.html (HtmlPageId 8)} load fail。修复：引擎在 native 八族严格绑定全部未命中后，
 * 按 questId + action 结算奖励窗确认动作（无主键协议），收尾回选择对话页（页 10、questId=0）。
 * <p>
 * Gate for the unpinned reward-window recovery (live 19640): the engine settles a reward-window
 * confirmation by quest id after every native family missed on strict NPC binding, and the tail sends
 * the selection dialog page (page 10, no quest context).
 */
class NativeUnpinnedRewardWindowTest {

	/** 实机行：DD 族（接取 799022 / 交付 798991；单槽 6 可选奖励）。 / The live row. */
	private static final int QUEST = 19640;
	/** 接取对象句柄（实机 targetObj=79221 等价物）。 / The acquire npc the action arrived on. */
	private static final int ACQUIRE_NPC = 799022;

	@BeforeAll
	static void loadRetailFixtures() {
		// 与领奖门禁同型：真端驱动是结算口的元数据来源（与生产目录同一条装载路径）。
		// Same shape as the claim gate: the retail driver is the settlement's metadata source.
		if (RetailQuestDriver.current().isEmpty()) {
			RetailQuestDriver.overlay(ImmutableQuestCatalog.fromEntries(List.of()));
		}
	}

	/** 记录结算调用的假结算体。 / A recording settlement sink. */
	private static final class RecordingSink implements NativeReportRewardFlow.CompletionSink {
		private final List<Call> calls = new ArrayList<>();

		@Override
		public boolean complete(QuestEnv env, int rewardTier, QuestTemplate template) {
			calls.add(new Call(env, rewardTier, template));
			return true;
		}

		private record Call(QuestEnv env, int rewardTier, QuestTemplate template) {
		}
	}

	/**
	 * 用户实机场景：引擎在 native 全族未按 NPC 绑定认领时按 questId 收下奖励窗确认动作，
	 * 结算体收到真端行档位与原始动作，收尾下发选择对话页（页 10、questId=0）。
	 * The live scenario: the engine claims the reward-window confirmation by quest id after all
	 * native families miss, the settlement receives the retail tier and the raw action, and the
	 * tail sends the selection dialog page (page 10, no quest context).
	 */
	@Test
	void engineSettlesTheRewardWindowActionOnTheAcquireNpc() {
		RecordingSink sink = new RecordingSink();
		NativeUnpinnedRewardWindow.setFlowSupplierForTest(() -> NativeReportRewardFlow.withSink(sink));
		NpcData original = DataManager.NPC_DATA;
		DataManager.NPC_DATA = new NpcData();
		try {
			Player player = NativeTalkFixture.player();
			NativeTalkFixture.add(player, QUEST, QuestStatus.REWARD, 1);
			QuestEngine engine = new QuestEngine();

			assertTrue(engine.onDialog(NativeTalkFixture.dialog(player, ACQUIRE_NPC, QUEST, 8)),
				"接取对象上的奖励窗确认必须被恢复面认领 / the unpinned confirmation must be claimed");
			assertEquals(1, sink.calls.size(), "结算体必须被调用一次 / the settlement runs once");
			assertEquals(0, sink.calls.getFirst().rewardTier(), "单槽行档位固定首档 / single-slot tier");
			assertEquals(QUEST, sink.calls.getFirst().env().getQuestId(), "结算按 questId 路由");
			assertEquals(8, sink.calls.getFirst().env().getDialogId(), "原始动作透传给结算体");
			// 收尾 = 选择对话页（页 10、questId=0；与各族交付面领奖收尾同形）。
			NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 10, 0);
		} finally {
			NativeUnpinnedRewardWindow.resetFlowSupplierForTest();
			DataManager.NPC_DATA = original;
		}
	}

	/**
	 * 动作门：奖励窗确认段（{@code isRewardWindowAction}）之外的动作不由本段认领——行选 31 与报告 1009
	 * 属各族的报告/打开面，恢复段不得吞包。
	 * The action band gate: 31/1009 belong to the families' report/open faces and must fall through.
	 */
	@Test
	void actionsOutsideTheRewardWindowBandAreNotClaimed() {
		RecordingSink sink = new RecordingSink();
		NativeReportRewardFlow flow = NativeReportRewardFlow.withSink(sink);
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, QUEST, QuestStatus.REWARD, 1);

		assertFalse(NativeUnpinnedRewardWindow.claim(player, QUEST, 31, 79221, flow),
			"行选动作不是奖励窗确认 / row selection is not a reward-window confirmation");
		assertFalse(NativeUnpinnedRewardWindow.claim(player, QUEST, 1009, 79221, flow),
			"报告动作不是奖励窗确认 / the report action is not a reward-window confirmation");
		assertTrue(sink.calls.isEmpty(), "动作门不过 ⇒ 不进结算体 / no settlement on a gate miss");
		assertTrue(NativeTalkFixture.dialogPages(player).isEmpty(), "动作门不过 ⇒ 零下发 / zero packets");
	}

	/**
	 * 状态门：只有 {@code REWARD} 态可结算（START/NONE 一律零副作用）。
	 * The state gate: only {@code REWARD} rows settle; everything else keeps zero side effects.
	 */
	@Test
	void claimRequiresRewardState() {
		RecordingSink sink = new RecordingSink();
		NativeReportRewardFlow flow = NativeReportRewardFlow.withSink(sink);
		Player started = NativeTalkFixture.player();
		NativeTalkFixture.start(started, QUEST);

		assertFalse(NativeUnpinnedRewardWindow.claim(started, QUEST, 8, 79221, flow),
			"START 态不得结算 / a started row never settles");
		assertTrue(sink.calls.isEmpty(), "状态门不过 ⇒ 不进结算体");
		assertTrue(NativeTalkFixture.dialogPages(started).isEmpty(), "状态门不过 ⇒ 零下发");

		Player fresh = NativeTalkFixture.player();
		assertFalse(NativeUnpinnedRewardWindow.claim(fresh, QUEST, 8, 79221, flow),
			"未接取不得结算 / an unaccepted row never settles");
		assertTrue(sink.calls.isEmpty() && NativeTalkFixture.dialogPages(fresh).isEmpty());
	}

	/**
	 * 按钮声明门：19640 声明 6 个可选奖励 ⇒ 下标 0..5（动作 8..13）合法；越界按钮在结算体之前
	 * fail-closed（与各族领奖口同一道门）。
	 * The declaration gate: six selectable options accept indices 0..5; an out-of-range button fails
	 * closed before the settlement, exactly like every family's claim face.
	 */
	@Test
	void undeclaredOptionIndicesFailClosed() {
		RecordingSink sink = new RecordingSink();
		NativeReportRewardFlow flow = NativeReportRewardFlow.withSink(sink);
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, QUEST, QuestStatus.REWARD, 1);

		assertTrue(NativeUnpinnedRewardWindow.claim(player, QUEST, 13, 79221, flow),
			"下标 5（动作 13）仍在 6 项声明内 / index 5 stays inside the declared options");
		assertEquals(1, sink.calls.size());
		NativeTalkFixture.clearPackets(player);

		assertFalse(NativeUnpinnedRewardWindow.claim(player, QUEST, 14, 79221, flow),
			"下标 6（动作 14）越过声明面 ⇒ fail-closed / index 6 is beyond the declared options");
		assertEquals(1, sink.calls.size(), "越界按钮不得再进结算体");
		assertTrue(NativeTalkFixture.dialogPages(player).isEmpty(), "越界按钮 ⇒ 零下发");
	}
}
