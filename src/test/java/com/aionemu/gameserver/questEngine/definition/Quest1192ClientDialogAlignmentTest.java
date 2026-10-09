package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;

/**
 * 验证任务 1192 的接取页链不会把不存在的 1012 页面回显给客户端。
 * <p>
 * P3 重锚（计划 §8.9）：旧 IR 形状断言（节点名/条件/动作/页链）随 SimpleTalk 切换批退场，
 * 本类改为原版表行锚——接取/交付 NPC、中继步、发扣物品、交付门均取自
 * {@code Quest_SimpleTalk.xml} + {@code quest.xml} 与静态数据（{@code npc_template} /
 * 物品 {@code name_desc}），native 处理器必须逐项一致。
 * <p>
 * P3 re-anchor (plan §8.9): the IR-shape assertions retire with the SimpleTalk switch batch;
 * this class now pins the retail table row through the native handler.
 */
class Quest1192ClientDialogAlignmentTest {

	@Test
	void acceptDialogChainUsesTheClientAcceptanceAction() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();

		// 原版行 1192：Spatalos → Spatalos
		assertTrue(handler.routes(1192), "1192 必须由 native 车道路由");
		assertEquals(203098, handler.acquireNpc(1192), "接取 NPC");
		assertEquals(203098, handler.rewardNpc(1192), "交付 NPC");
		assertEquals(2, handler.relayCount(1192), "中继步数");
		assertTrue(handler.relaysForNpc(203701).stream().anyMatch(relay -> relay.questId() == 1192 && relay.step() == 1),
				"中继 1 必须挂在该 NPC 上: Lavirintos");
		assertTrue(handler.relaysForNpc(203833).stream().anyMatch(relay -> relay.questId() == 1192 && relay.step() == 2),
				"中继 2 必须挂在该 NPC 上: Xenophon");
		assertEquals(new SimpleTalkHandler.ItemStack(182200556, 1),
				handler.acceptGiveItem(1192), "接取侧发放");
		assertEquals(new SimpleTalkHandler.ItemStack(182200556, 1),
				handler.stepRemoveItem(1192, 1), "第 1 步扣除");
		assertTrue(handler.workItems(1192).isEmpty(), "该行未声明 item_check：无交付门");
		assertFalse(handler.unresolvedGate(1192), "无门行不得 fail-closed");
		assertNull(handler.cutscene(1192), "该行无过场");
	}
}
