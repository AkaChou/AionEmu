package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;

/**
 * 验证任务 3103 的前置条件以及接取、报告和领奖 NPC owner 合同。
 * <p>
 * P3 重锚（计划 §8.9）：旧 IR 形状断言（节点名/条件/动作/页链）随 SimpleTalk 切换批退场，
 * 本类改为真端表行锚——接取/交付 NPC、中继步、发扣物品、交付门均取自
 * {@code Quest_SimpleTalk.xml} + {@code quest.xml} 与静态数据（{@code npc_template} /
 * 物品 {@code name_desc}），native 处理器必须逐项一致。
 * <p>
 * P3 re-anchor (plan §8.9): the IR-shape assertions retire with the SimpleTalk switch batch;
 * this class now pins the retail table row through the native handler.
 */
class Quest3103ClientDialogAlignmentTest {

	@Test
	void keepsTheRetailStartReportAndRewardOwnersExclusive() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();

		// 真端行 3102：Pygmalion → Pyrrha
		assertTrue(handler.routes(3102), "3102 必须由 native 车道路由");
		assertEquals(798206, handler.acquireNpc(3102), "接取 NPC");
		assertEquals(798225, handler.rewardNpc(3102), "交付 NPC");
		assertFalse(handler.acquireNpc(3102).equals(handler.rewardNpc(3102)),
				"接取与交付 owner 分离");
		assertEquals(2, handler.relayCount(3102), "中继步数");
		assertTrue(handler.relaysForNpc(798167).stream().anyMatch(relay -> relay.questId() == 3102 && relay.step() == 1),
				"中继 1 必须挂在该 NPC 上: Shugo_LF2a_9");
		assertTrue(handler.relaysForNpc(798177).stream().anyMatch(relay -> relay.questId() == 3102 && relay.step() == 2),
				"中继 2 必须挂在该 NPC 上: LF2A_NPC_Gastak");
		assertNull(handler.acceptGiveItem(3102), "接取侧无发放");
		assertTrue(handler.workItems(3102).isEmpty(), "该行未声明 item_check：无交付门");
		assertFalse(handler.unresolvedGate(3102), "无门行不得 fail-closed");
		assertNull(handler.cutscene(3102), "该行无过场");

		// 真端行 3103：Pyrrha → Cyprus
		assertTrue(handler.routes(3103), "3103 必须由 native 车道路由");
		assertEquals(798225, handler.acquireNpc(3103), "接取 NPC");
		assertEquals(798226, handler.rewardNpc(3103), "交付 NPC");
		assertFalse(handler.acquireNpc(3103).equals(handler.rewardNpc(3103)),
				"接取与交付 owner 分离");
		assertEquals(0, handler.relayCount(3103), "中继步数");
		assertNull(handler.acceptGiveItem(3103), "接取侧无发放");
		assertTrue(handler.workItems(3103).isEmpty(), "该行未声明 item_check：无交付门");
		assertFalse(handler.unresolvedGate(3103), "无门行不得 fail-closed");
		assertNull(handler.cutscene(3103), "该行无过场");
	}
}
