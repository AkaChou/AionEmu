package com.aionemu.gameserver.questEngine.tablelane;

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
 * 打开（-1）重放顺序：可交付（REWARD）任务优先于进行中（START）任务。
 * <p>2026-10-06 实机 14111：NPC 203126（Abolos）上 1155（SimpleCollectItem，START，questId 较小）
 * 先认领开门、下发两参通用页 10（questId=0）并短路重放循环，遮挡 14111（SimpleHunt，REWARD）的
 * 奖励窗重放——客户端任务列表不渲染交付行（19683 教训），任务就此不可交。修复：引擎开门重放
 * 分两遍（QuestEngine.onDialog，先 REWARD 后 START）；本门禁锁定该顺序。</p>
 * <p>Open-door (-1) replay order: deliverable (REWARD) quests before live (START) quests.
 * Live 14111: 1155 — a live collect quest with a smaller id on the same npc — claimed the open
 * first, sent the generic page 10 and short-circuited the replay, shadowing 14111's reward window;
 * the client task list never renders a hand-in row, so the quest became undeliverable. The engine
 * now replays REWARD before START (QuestEngine.onDialog); this gate locks that order.</p>
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
	 * 实机数据行：1155 与 14111 同为 NPC 203126（Abolos）的接取/交付任务（真端表
	 * {@code Quest_SimpleCollectItem.xml} / {@code Quest_SimpleHunt.xml}）。1155 为进行中且 questId
	 * 较小，14111 可交付——开门必须命中 14111 的奖励窗（页 5 带 questId），而不是 1155 的通用页 10。
	 * Live rows: 1155 and 14111 both acquire and hand in at npc 203126 (Abolos) per the retail table.
	 * 1155 is live with the smaller id and 14111 is deliverable — the open must reach 14111's reward
	 * window (page 5 with the quest context), not 1155's generic page 10.
	 */
	@Test
	void openDoorReplaysTheDeliverableQuestBeforeAnEarlierLiveOne() {
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.start(player, 1155);
		NativeTalkFixture.add(player, 14111, QuestStatus.REWARD, 73);
		QuestEngine engine = new QuestEngine();

		assertTrue(engine.onDialog(NativeTalkFixture.dialog(player, 203126, 0, -1)),
			"开门必须被可交付任务认领 / the open must be claimed by the deliverable quest");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 5, 14111);
	}
}
