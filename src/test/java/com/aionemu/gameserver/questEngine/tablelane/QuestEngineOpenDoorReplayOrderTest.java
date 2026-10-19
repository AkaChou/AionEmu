package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

/**
 * 交付可达性：同一 NPC 上另有进行中任务时，点 NPC 出任务列表（不认领、不发任务页），
 * 点列表里的交付行仍能打开该任务的奖励窗。
 * <p>2026-10-06 实机 14111：NPC 203126（Abolos）上 1155（SimpleCollectItem，START）与 14111
 * （SimpleHunt，REWARD）并存，旧实现让开门被某个任务认领并直接弹出该任务对话（2026-10-19 实机：
 * 点 NPC 看不到任务列表）。修复后开门交回客户端任务列表（页 10），交付行走任务行（31）打开
 * 奖励窗——本门禁锁定「开门零发页 + 交付行可达」。</p>
 * <p>Deliverability: with another live quest on the same npc, the npc click opens the client task
 * list (unclaimed, no quest page) while the hand-in row still opens the reward window. The old shape
 * made the open jump straight into one quest's dialog, so the list never appeared (live 2026-10-19).</p>
 */
class QuestEngineOpenDoorReplayOrderTest {

	private NpcData originalNpcData;

	@BeforeEach
	void setUp() {
		// 与 QuestEngineRuntimeCompositionTest 同模式：测试环境喂空 NpcData（DD 注册只查模板告警面）。
		// Same pattern as QuestEngineRuntimeCompositionTest: an empty NpcData for the test environment
		// (the DD registration only consults templates for its warning face).
		originalNpcData = DataManager.NPC_DATA;
		DataManager.NPC_DATA = new NpcData();
	}

	@AfterEach
	void tearDown() {
		DataManager.NPC_DATA = originalNpcData;
	}

	/**
	 * 实机数据行：1155 与 14111 同为 NPC 203126（Abolos）的接取/交付任务（原版表
	 * {@code Quest_SimpleCollectItem.xml} / {@code Quest_SimpleHunt.xml}）。1155 进行中、14111 可交付。
	 * Live rows: 1155 and 14111 both acquire and hand in at npc 203126 (Abolos); 1155 is live and
	 * 14111 is deliverable.
	 */
	@Test
	void npcClickShowsTheTaskListAndTheHandInRowStillReachesTheRewardWindow() {
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.start(player, 1155);
		NativeTalkFixture.add(player, 14111, QuestStatus.REWARD, 73);
		QuestEngine engine = new QuestEngine();

		NativeTalkFixture.clearPackets(player);
		assertFalse(engine.onDialog(NativeTalkFixture.dialog(player, 203126, 0, -1)),
			"点 NPC 不被任务引擎认领（客户端渲染任务列表） / the open stays unclaimed for the task list");
		assertTrue(NativeTalkFixture.dialogPages(player).isEmpty(), "点 NPC 零任务页");

		NativeTalkFixture.clearPackets(player);
		assertTrue(engine.onDialog(NativeTalkFixture.dialog(player, 203126, 14111, 31)),
			"交付行必须能打开奖励窗 / the hand-in row reaches the reward window");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 5, 14111);
	}
}
