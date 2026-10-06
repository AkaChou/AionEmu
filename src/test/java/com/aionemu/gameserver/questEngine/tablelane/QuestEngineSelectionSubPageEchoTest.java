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
 * 子页动作的引擎兜底回发：契约声明即原样回发该页且必须携带 questId。
 * <p>2026-10-06 实机 13403（DataDriven 族，NPC 对话 203098，交互 Kinesos 203096）：步推进后
 * （step=2，页 select3=1693）点击翻页按钮动作 1694（select3_1）——DD 对话平面的步守卫（step==hit）
 * 不再命中、其余族不路由 ⇒ 落到 NPC 对话平面的两参回显（questId=0）⇒ 客户端在 NPC 对话 html 里
 * 找不到任务页 ⇒ load fail（「结束对话不关窗 / 继续听 load fail」）。正确形 = 9/28 基线（1001-1007
 * 动作 1694 三参回发）。兜底：native 各族与 DD 都未认领的子页动作，按契约声明带 questId 原样回发。</p>
 * <p>Selection sub-page echo fallback: a client-declared SELECT⟨n⟩_… action echoes its page back with
 * the quest context when no lane claimed it. Live 13403: after an advance the DD step guard no longer
 * matched, the action fell through to the two-arg NPC-plane echo (questId=0) and the client failed to
 * load the task page; the 9/28 baseline (1001-1007 action 1694) sent it with the context.</p>
 */
class QuestEngineSelectionSubPageEchoTest {

	private NpcData originalNpcData;

	@BeforeEach
	void setUp() {
		// 与 QuestEngineRuntimeCompositionTest 同模式：测试环境喂空 NpcData（DD 注册只查模板告警面）。
		// Same pattern as QuestEngineRuntimeCompositionTest: an empty NpcData for the test environment.
		originalNpcData = DataManager.NPC_DATA;
		DataManager.NPC_DATA = new NpcData();
	}

	@AfterEach
	void tearDown() {
		DataManager.NPC_DATA = originalNpcData;
	}

	/**
	 * 实机行 13403（DD 表；Kinesos=203096 为第 0 步 Talk 目标）：玩家停在 step 2（vars=2，Beris 步）
	 * 时，页链 select3(1693) 的翻页动作 1694 到达——DD 守卫（step(=2) != hit.stepIndex(=0)）不认领，
	 * 兜底必须回发页 1694 且携带 questId=13403（两参形态即实机 load fail 的成因）。
	 * Live row 13403 (DD table; Kinesos=203096 is the step-0 talk target): with the player at step 2
	 * (vars=2), the turn-page action 1694 of select3(1693) must be echoed as page 1694 with
	 * questId=13403 when the DD step guard misses (the two-arg form was the live load-fail cause).
	 */
	@Test
	void selectionSubPageEchoesBackWithTheQuestContextWhenNoLaneClaimsIt() {
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, 13403, QuestStatus.START, 2);
		QuestEngine engine = new QuestEngine();

		assertTrue(engine.onDialog(NativeTalkFixture.dialog(player, 203096, 13403, 1694)),
			"子页动作必须被回发兜底认领 / the sub-page action must be claimed by the echo fallback");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1694, 13403);
	}
}
