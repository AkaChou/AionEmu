package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;

/**
 * 接取段走 {@code canonicalAcceptFlow}；交付段现在是**规范交付**——阶段首屏 = 客户端 select2 页，
 * <p>
 * P3 重锚（计划 §8.9）：旧 IR 形状断言（节点名/条件/动作/页链）随 SimpleTalk 切换批退场，
 * 本类改为原版表行锚——接取/交付 NPC、中继步、发扣物品、交付门均取自
 * {@code Quest_SimpleTalk.xml} + {@code quest.xml} 与静态数据（{@code npc_template} /
 * 物品 {@code name_desc}），native 处理器必须逐项一致。
 * <p>
 * P3 re-anchor (plan §8.9): the IR-shape assertions retire with the SimpleTalk switch batch;
 * this class now pins the retail table row through the native handler.
 */
class Quest1152RetailAlignmentTest {

	@Test
	void followsTheClientChefDialogAndLegacyTwoStepItemContract() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();

		// 原版行 1152：Nemia → Eradis
		assertTrue(handler.routes(1152), "1152 必须由 native 车道路由");
		assertEquals(203132, handler.acquireNpc(1152), "接取 NPC");
		assertEquals(203130, handler.rewardNpc(1152), "交付 NPC");
		assertFalse(handler.acquireNpc(1152).equals(handler.rewardNpc(1152)),
				"接取与交付 owner 分离");
		assertEquals(1, handler.relayCount(1152), "中继步数");
		assertTrue(handler.relaysForNpc(203130).stream().anyMatch(relay -> relay.questId() == 1152 && relay.step() == 1),
				"中继 1 必须挂在该 NPC 上: Eradis");
		assertEquals(new SimpleTalkHandler.ItemStack(182200526, 1),
				handler.acceptGiveItem(1152), "接取侧发放");
		assertEquals(new SimpleTalkHandler.ItemStack(182200526, 1),
				handler.stepRemoveItem(1152, 1), "第 1 步扣除");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(169400112, 1)), handler.workItems(1152), "交付门");
		assertNull(handler.cutscene(1152), "该行无过场");
	}
}
