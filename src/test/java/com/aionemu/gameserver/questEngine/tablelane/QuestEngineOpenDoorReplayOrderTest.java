package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * 交付可达性：同一 NPC 上另有进行中任务时，点 NPC 直接打开**可交付**任务的奖励窗（客户端任务列表
 * 不渲染待交付行，19683），不触碰进行中的任务；没有可交付任务时才回落到客户端任务列表。
 * <p>2026-10-06 实机 14111：NPC 203126（Abolos）上 1155（SimpleCollectItem，START）与 14111
 * （SimpleHunt，REWARD）并存。2026-10-19 实机（Roseino 1173 交付/1174 进行中）：点 NPC 只出页 10
 * 任务列表、看不到可交付行，奖励拿不到。修复后开门只重放 REWARD 行——本门禁锁定「交付窗 + 进行中
 * 行不被推进」。</p>
 * <p>Deliverability: with another live quest on the same npc, the npc click opens the deliverable
 * quest's reward window (the client task list never renders hand-in rows, 19683) and never touches the
 * live row; with nothing deliverable it falls back to the client-rendered list.</p>
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
		assertTrue(engine.onDialog(NativeTalkFixture.dialog(player, 203126, 0, -1)),
			"可交付行必须由开门认领 / the deliverable quest claims the open");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 5, 14111);
		assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(14111).getStatus(),
			"开门只开奖励窗 / the open only shows the reward window");
		assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(1155).getStatus(),
			"进行中的行不得被开门推进 / the live row is untouched");
	}
}
