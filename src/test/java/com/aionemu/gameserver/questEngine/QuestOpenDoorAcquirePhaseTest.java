package com.aionemu.gameserver.questEngine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.questEngine.tablelane.SimpleHuntHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;

/**
 * 开门相位：NPC 的可接取相在客户端任务对话树里排在进行中相之前。
 * <p>
 * 2026-10-19 实机（NPC 203155 Roseino）：接取 Talk 行 1174（{@code item_check} 收集型）后，同一 NPC 上
 * 的 Hunt 行 1173 无法再接——1174 的打开（-1）先认领开门并把自身推进到 REWARD，客户端把页 4
 * （{@code ask_quest_accept} 的 {@code HACTION_ASK_QUEST_ACCEPT}）与 1173 的入口页一起撤掉。修复按相位
 * 编排：本 NPC 仍在发放的接取行先开接取入口页；进行中任务只开当前阶段页、不结算；未接取行在页 4/1011
 * 上的动作按原版「领取中任务」门受理；NPC 的入口页动作（1011 → 页 4）是页内导航，不回发。
 * <p>
 * The open phase: the npc's acquirable phase comes before its live phase in the client dialog tree.
 * Live 203155 (Roseino): after 1174 (a check-form talk row) was taken, the hunt row 1173 could no
 * longer be taken — the open claimed by 1174 advanced it to REWARD and the client dropped page 4
 * together with 1173's entry page. The fix orders the phases, keeps live rows un-settled when opened,
 * gates accept-page acts by the acquiring slot, and never echoes entry-page navigation acts.
 */
class QuestOpenDoorAcquirePhaseTest {

	private static final int ROSEINO = 203155;
	private static final int TALK_QUEST = 1174;
	private static final int HUNT_QUEST = 1173;
	/** 独立狩猎行（交付 NPC 与其交付标记刷新互不相干）。 / An unrelated hunt row. */
	private static final int FREE_HUNT_QUEST = 2354;

	@Test
	void liveTalkRowDoesNotHideTheNpcsAcquirePhase() {
		// 1174 进行中（页 4 在客户端可见）：开门先给同 NPC 仍在发放的 1173 入口页。
		// 1174 live (page 4 visible on the client): the open answers 1173's entry page first.
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.start(player, TALK_QUEST);
		Npc roseino = NativeTalkFixture.npc(ROSEINO);
		QuestEngine engine = new QuestEngine();

		NativeTalkFixture.clearPackets(player);
		assertTrue(engine.onDialog(new QuestEnv(roseino, player, 0, -1)), "开门必须由接取相认领");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player,
			NativeTalkFixture.clientEntryPage(HUNT_QUEST), HUNT_QUEST);
		assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(TALK_QUEST).getStatus(),
			"开门不得推进进行中的任务");
	}

	@Test
	void rowSelectionOnTheLiveTalkRowKeepsItInProgress() {
		// 任务行（31）只开当前阶段页，不结算：收集途中点 1174 的行不能被推到 REWARD。
		// The row selection (31) only shows the current stage page; a mid-collection row must not flip.
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.start(player, TALK_QUEST);
		Npc roseino = NativeTalkFixture.npc(ROSEINO);

		NativeTalkFixture.clearPackets(player);
		assertTrue(SimpleTalkHandler.instance().onDialog(new QuestEnv(roseino, player, TALK_QUEST, 31)));
		// 1174 无中继 ⇒ 报告步已到，任务行答客户端声明的报告确认页（select5=2375），但不结算。
		// 1174 has no relay, so its report step is already reached: the row answers the declared
		// report page (select5 = 2375) without settling.
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player,
			NativeTalkFixture.reportConfirmPage(TALK_QUEST), TALK_QUEST);
		assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(TALK_QUEST).getStatus(),
			"任务行不得结算为 REWARD");
	}

	@Test
	void acceptPageActIsClaimedThroughTheAcquiringSlot() {
		// 页 4 的接取按钮回发（动作 = 页 id 4）：打开 1173 的入口页后由「领取中任务」槽位引导到 1173。
		// The page-4 accept button comes back as act 4: the acquiring slot routes it to 1173.
		Player player = NativeTalkFixture.player();
		Npc roseino = NativeTalkFixture.npc(ROSEINO);
		QuestEngine engine = new QuestEngine();

		NativeTalkFixture.clearPackets(player);
		assertTrue(engine.onDialog(new QuestEnv(roseino, player, HUNT_QUEST, 31)), "任务行进接取面");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player,
			NativeTalkFixture.clientEntryPage(HUNT_QUEST), HUNT_QUEST);
		assertEquals(HUNT_QUEST, player.getNpcQuestDialogAcquiringQuestId(roseino.getObjectId()),
			"接取入口页打开时登记领取中任务");

		NativeTalkFixture.clearPackets(player);
		assertTrue(engine.onDialog(new QuestEnv(roseino, player, HUNT_QUEST, 4)), "页 4 接取按钮由领取中槽位受理");
		assertNull(player.getQuestStateList().getQuestState(HUNT_QUEST),
			"页 4 受理 = 进入接取面（入口动作本身不落库）");
	}

	@Test
	void acceptPageActOfAFinishedRowIsNotAccepted() {
		// 已完成的不可重复行仍在客户端队列里：任务行不进接取面、零发页，槽位不登记。
		// A finished, non-repeatable row no longer opens anything: no page, no acquiring slot.
		Player player = NativeTalkFixture.player();
		QuestState done = NativeTalkFixture.add(player, HUNT_QUEST, QuestStatus.COMPLETE, 0);
		done.setCompleteCount(1);
		Npc roseino = NativeTalkFixture.npc(ROSEINO);
		QuestEngine engine = new QuestEngine();

		NativeTalkFixture.clearPackets(player);
		assertFalse(engine.onDialog(new QuestEnv(roseino, player, HUNT_QUEST, 31)), "已完成行不进接取面");
		assertTrue(NativeTalkFixture.dialogPages(player).isEmpty(), "已完成行零发页");
		assertEquals(0, player.getNpcQuestDialogAcquiringQuestId(roseino.getObjectId()));
	}

	@Test
	void entryPageActOnTheNpcIsPageLocalNavigation() {
		// 客户端任务树的入口页动作（1011 → 页 4）是页内导航：未接取行重放不得把它回发成同一页，
		// 也不得把 NPC 打开降级为问候页（多任务 NPC 上的「点了没反应」）。
		// Entry-page acts (1011 → page 4) are page-local navigation: no echo, no hello-page demotion.
		Player player = NativeTalkFixture.player();
		Npc roseino = NativeTalkFixture.npc(ROSEINO);
		QuestEngine engine = new QuestEngine();

		NativeTalkFixture.clearPackets(player);
		assertTrue(engine.onDialog(new QuestEnv(roseino, player, 0, 1011)), "入口页动作必须被认领");
		assertTrue(NativeTalkFixture.dialogPages(player).isEmpty(), "入口页动作零发页（客户端本地翻页）");
	}

	@Test
	void liveHuntRowWithoutProgressNoLongerAnswersTheGenericListPage() {
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.start(player, FREE_HUNT_QUEST);
		SimpleHuntHandler hunt = SimpleHuntHandler.instance();
		Npc rewardNpc = NativeTalkFixture.npc(hunt.rewardNpc(FREE_HUNT_QUEST));

		NativeTalkFixture.clearPackets(player);
		assertFalse(hunt.onDialog(new QuestEnv(rewardNpc, player, FREE_HUNT_QUEST, 31)),
			"进度未满的狩猎行不得认领任务行打开（页 10 会踢出任务对话树）");
		assertTrue(NativeTalkFixture.dialogPages(player).isEmpty());
	}
}
