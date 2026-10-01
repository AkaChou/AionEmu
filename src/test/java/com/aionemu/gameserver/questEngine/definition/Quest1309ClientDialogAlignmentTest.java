package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.questEngine.tablelane.SimpleUseItemHandler;

/**
 * 任务 1309（真端 SimpleUseItem 行）的接取页契约：物品接取只下发客户端声明的问询窗，无主 1002 接受
 * 只刷新状态并**关窗**，绝不回发不存在的接取页。
 * <p>
 * P5 重锚（计划 §8.9）：该行已由 {@link SimpleUseItemHandler} 原生直驱，typed 目录里不再有它的定义
 * （旧族编译器的 {@code SyncQuestState + CloseDialog} 形状由 native 接取口的关窗包承接）。
 * <p>
 * Quest 1309's accept-page contract on the native lane: the item-use accept only sends the client-declared
 * ask window, and the targetless 1002 accept refreshes state by closing the dialog — it never echoes an
 * undeclared page id.
 */
class Quest1309ClientDialogAlignmentTest {

	private static final int QUEST_ID = 1309;

	@Test
	void theRowIsDrivenNativelyAndKeepsTheCloseDialogContract() {
		SimpleUseItemHandler handler = SimpleUseItemHandler.instance();
		assertTrue(handler.routes(QUEST_ID), "1309 已由 P5 SimpleUseItem native 车道直驱");
		assertTrue(ProductionQuestDefinitions.catalog().findExecutable(QUEST_ID).isEmpty(),
			"已切换行不得再出现在 typed 目录（单一 owner）");

		// 真端 minlevel_permitted = 22，故用 30 级玩家过 CanAcquireQuest 轴。
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 30);
		Integer useItem = handler.useItemId(QUEST_ID);
		assertNotNull(useItem, "真端 use_item_name 必须解析");

		// 用物只下发接取问询窗（页 4，客户端任务页声明），不得回发其他页。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onItemUse(player, useItem));
		NativeTalkFixture.assertOnlyDialogPage(player, QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id());
		assertTrue(NativeTalkFixture.clientDeclares(QUEST_ID, QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id()),
			"接取问询窗必须是客户端声明的可渲染页");

		// 无主 1002 接受：建档并只发关窗包（页 0），不发送任何任务页。
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(new QuestEnv(null, player, QUEST_ID, 1002)));
		NativeTalkFixture.assertOnlyDialogPage(player, 0);
		QuestState state = player.getQuestStateList().getQuestState(QUEST_ID);
		assertNotNull(state, "接受后必须建档");
		assertEquals(QuestStatus.START, state.getStatus());
	}

	@Test
	void theRelayStepAndHandInUseClientDeclaredPages() {
		SimpleUseItemHandler handler = SimpleUseItemHandler.instance();
		List<Integer> relays = handler.relayNpcs(QUEST_ID);
		assertEquals(1, relays.size(), "真端 talk_npc1 = 单步中继");
		assertTrue(NativeTalkFixture.clientDeclares(QUEST_ID, QuestDialogPage.SELECT2.id()),
			"中继步页必须是客户端声明的 SELECT2");

		// 真端 minlevel_permitted = 22，故用 30 级玩家过 CanAcquireQuest 轴。
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 30);
		NativeTalkFixture.start(player, QUEST_ID);
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, relays.getFirst(), QUEST_ID, 26)),
			"中继 NPC 对话必须推进链条");
		NativeTalkFixture.assertOnlyDialogPage(player, QuestDialogPage.SELECT2.id());

		int rewardNpc = handler.rewardNpcs(QUEST_ID).getFirst();
		assertFalse(relays.contains(rewardNpc), "交付 NPC 独立于中继链");
		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, rewardNpc, QUEST_ID, 26)),
			"中继走完后交付 NPC 翻 REWARD 并开奖励窗");
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleUseItemHandler.PAGE_REWARD_WINDOW);
		assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(QUEST_ID).getStatus());
	}
}
