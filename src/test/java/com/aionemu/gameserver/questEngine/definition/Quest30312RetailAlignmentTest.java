package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;

/**
 * Quest30312RetailAlignmentTest
 * <p>
 * P3 重锚（计划 §8.9）：旧 IR 形状断言（节点名/条件/动作/页链）随 SimpleTalk 切换批退场，
 * 本类改为原版表行锚——接取/交付 NPC、中继步、发扣物品、交付门均取自
 * {@code Quest_SimpleTalk.xml} + {@code quest.xml} 与静态数据（{@code npc_template} /
 * 物品 {@code name_desc}），native 处理器必须逐项一致。
 * <p>
 * P3 re-anchor (plan §8.9): the IR-shape assertions retire with the SimpleTalk switch batch;
 * this class now pins the retail table row through the native handler.
 */
class Quest30312RetailAlignmentTest {

	@Test
	void dropsAndEveryTurnInMatchRetailItemRequirements() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();

		// 原版行 30312：Herka → Herka
		assertTrue(handler.routes(30312), "30312 必须由 native 车道路由");
		assertEquals(799322, handler.acquireNpc(30312), "接取 NPC");
		assertEquals(799322, handler.rewardNpc(30312), "交付 NPC");
		assertEquals(0, handler.relayCount(30312), "中继步数");
		assertNull(handler.acceptGiveItem(30312), "接取侧无发放");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(182209715, 20)), handler.workItems(30312), "交付门");
		assertNull(handler.cutscene(30312), "该行无过场");
	}
}
