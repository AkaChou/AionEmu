package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;

/**
 * 验证任务 23902 将客户端简易接取与报告、领奖路由限定在各自的正式 NPC owner。
 * <p>
 * P3 重锚（计划 §8.9）：旧 IR 形状断言（节点名/条件/动作/页链）随 SimpleTalk 切换批退场，
 * 本类改为原版表行锚——接取/交付 NPC、中继步、发扣物品、交付门均取自
 * {@code Quest_SimpleTalk.xml} + {@code quest.xml} 与静态数据（{@code npc_template} /
 * 物品 {@code name_desc}），native 处理器必须逐项一致。
 * <p>
 * P3 re-anchor (plan §8.9): the IR-shape assertions retire with the SimpleTalk switch batch;
 * this class now pins the retail table row through the native handler.
 */
class Quest23902ClientDialogAlignmentTest {

	@Test
	void keepsTheRetailSimpleStartReportAndRewardOwnersExclusive() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();

		// 原版行 23900：DF5_Haldor_E → Corridor_D_MainQuest_E
		assertTrue(handler.routes(23900), "23900 必须由 native 车道路由");
		assertEquals(804719, handler.acquireNpc(23900), "接取 NPC");
		assertEquals(798718, handler.rewardNpc(23900), "交付 NPC");
		assertFalse(handler.acquireNpc(23900).equals(handler.rewardNpc(23900)),
				"接取与交付 owner 分离");
		assertEquals(2, handler.relayCount(23900), "中继步数");
		assertTrue(handler.relaysForNpc(799225).stream().anyMatch(relay -> relay.questId() == 23900 && relay.step() == 1),
				"中继 1 必须挂在该 NPC 上: Hler");
		assertTrue(handler.relaysForNpc(204182).stream().anyMatch(relay -> relay.questId() == 23900 && relay.step() == 2),
				"中继 2 必须挂在该 NPC 上: Heimdall");
		assertNull(handler.acceptGiveItem(23900), "接取侧无发放");
		assertTrue(handler.workItems(23900).isEmpty(), "该行未声明 item_check：无交付门");
		assertFalse(handler.unresolvedGate(23900), "无门行不得 fail-closed");
		assertNull(handler.cutscene(23900), "该行无过场");

		// 原版行 23902：Corridor_D_MainQuest_E → Hler
		assertTrue(handler.routes(23902), "23902 必须由 native 车道路由");
		assertEquals(798718, handler.acquireNpc(23902), "接取 NPC");
		assertEquals(799225, handler.rewardNpc(23902), "交付 NPC");
		assertFalse(handler.acquireNpc(23902).equals(handler.rewardNpc(23902)),
				"接取与交付 owner 分离");
		assertEquals(0, handler.relayCount(23902), "中继步数");
		assertNull(handler.acceptGiveItem(23902), "接取侧无发放");
		assertTrue(handler.workItems(23902).isEmpty(), "该行未声明 item_check：无交付门");
		assertFalse(handler.unresolvedGate(23902), "无门行不得 fail-closed");
		assertNull(handler.cutscene(23902), "该行无过场");
	}
}
