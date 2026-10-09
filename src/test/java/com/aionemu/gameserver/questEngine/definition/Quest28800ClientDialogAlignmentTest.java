package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;

/**
 * 验证任务 28800 将客户端简易接取与报告、领奖路由限定在各自的正式 NPC owner。
 * <p>
 * P3 重锚（计划 §8.9）：旧 IR 形状断言（节点名/条件/动作/页链）随 SimpleTalk 切换批退场，
 * 本类改为原版表行锚——接取/交付 NPC、中继步、发扣物品、交付门均取自
 * {@code Quest_SimpleTalk.xml} + {@code quest.xml} 与静态数据（{@code npc_template} /
 * 物品 {@code name_desc}），native 处理器必须逐项一致。
 * <p>
 * P3 re-anchor (plan §8.9): the IR-shape assertions retire with the SimpleTalk switch batch;
 * this class now pins the retail table row through the native handler.
 */
class Quest28800ClientDialogAlignmentTest {

	@Test
	void keepsTheRetailSimpleStartReportAndRewardOwnersExclusive() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();

		// 原版行 18800：Harinus → Leejunius
		assertTrue(handler.routes(18800), "18800 必须由 native 车道路由");
		assertEquals(798458, handler.acquireNpc(18800), "接取 NPC");
		assertEquals(830365, handler.rewardNpc(18800), "交付 NPC");
		assertFalse(handler.acquireNpc(18800).equals(handler.rewardNpc(18800)),
				"接取与交付 owner 分离");
		assertEquals(0, handler.relayCount(18800), "中继步数");
		assertNull(handler.acceptGiveItem(18800), "接取侧无发放");
		assertTrue(handler.workItems(18800).isEmpty(), "该行未声明 item_check：无交付门");
		assertFalse(handler.unresolvedGate(18800), "无门行不得 fail-closed");
		assertNull(handler.cutscene(18800), "该行无过场");

		// 原版行 18801：Vexramas → Celaeno
		assertTrue(handler.routes(18801), "18801 必须由 native 车道路由");
		assertEquals(830001, handler.acquireNpc(18801), "接取 NPC");
		assertEquals(830005, handler.rewardNpc(18801), "交付 NPC");
		assertFalse(handler.acquireNpc(18801).equals(handler.rewardNpc(18801)),
				"接取与交付 owner 分离");
		assertEquals(0, handler.relayCount(18801), "中继步数");
		assertNull(handler.acceptGiveItem(18801), "接取侧无发放");
		assertTrue(handler.workItems(18801).isEmpty(), "该行未声明 item_check：无交付门");
		assertFalse(handler.unresolvedGate(18801), "无门行不得 fail-closed");
		assertNull(handler.cutscene(18801), "该行无过场");

		// 原版行 28800：Randiten → Harretion
		assertTrue(handler.routes(28800), "28800 必须由 native 车道路由");
		assertEquals(798459, handler.acquireNpc(28800), "接取 NPC");
		assertEquals(830532, handler.rewardNpc(28800), "交付 NPC");
		assertFalse(handler.acquireNpc(28800).equals(handler.rewardNpc(28800)),
				"接取与交付 owner 分离");
		assertEquals(0, handler.relayCount(28800), "中继步数");
		assertNull(handler.acceptGiveItem(28800), "接取侧无发放");
		assertTrue(handler.workItems(28800).isEmpty(), "该行未声明 item_check：无交付门");
		assertFalse(handler.unresolvedGate(28800), "无门行不得 fail-closed");
		assertNull(handler.cutscene(28800), "该行无过场");

		// 原版行 28801：Hemenlin → Asnin
		assertTrue(handler.routes(28801), "28801 必须由 native 车道路由");
		assertEquals(830085, handler.acquireNpc(28801), "接取 NPC");
		assertEquals(830102, handler.rewardNpc(28801), "交付 NPC");
		assertFalse(handler.acquireNpc(28801).equals(handler.rewardNpc(28801)),
				"接取与交付 owner 分离");
		assertEquals(0, handler.relayCount(28801), "中继步数");
		assertNull(handler.acceptGiveItem(28801), "接取侧无发放");
		assertTrue(handler.workItems(28801).isEmpty(), "该行未声明 item_check：无交付门");
		assertFalse(handler.unresolvedGate(28801), "无门行不得 fail-closed");
		assertNull(handler.cutscene(28801), "该行无过场");
	}
}
