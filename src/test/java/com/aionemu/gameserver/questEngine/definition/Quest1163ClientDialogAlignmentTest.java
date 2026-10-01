package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;

/**
 * 页梯退役重锚：1163 的阶段首屏由客户端 select2 页派生，行内翻页（select2_1）是客户端本地行为
 * <p>
 * P3 重锚（计划 §8.9）：旧 IR 形状断言（节点名/条件/动作/页链）随 SimpleTalk 切换批退场，
 * 本类改为真端表行锚——接取/交付 NPC、中继步、发扣物品、交付门均取自
 * {@code Quest_SimpleTalk.xml} + {@code quest.xml} 与静态数据（{@code npc_template} /
 * 物品 {@code name_desc}），native 处理器必须逐项一致。
 * <p>
 * P3 re-anchor (plan §8.9): the IR-shape assertions retire with the SimpleTalk switch batch;
 * this class now pins the retail table row through the native handler.
 */
class Quest1163ClientDialogAlignmentTest {

	@Test
	void followsTheRetailPotionHandoffAndRewardOwner() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();

		// 真端行 1163：Kinesos → Roseino
		assertTrue(handler.routes(1163), "1163 必须由 native 车道路由");
		assertEquals(203096, handler.acquireNpc(1163), "接取 NPC");
		assertEquals(203155, handler.rewardNpc(1163), "交付 NPC");
		assertFalse(handler.acquireNpc(1163).equals(handler.rewardNpc(1163)),
				"接取与交付 owner 分离");
		assertEquals(1, handler.relayCount(1163), "中继步数");
		assertTrue(handler.relaysForNpc(203151).stream().anyMatch(relay -> relay.questId() == 1163 && relay.step() == 1),
				"中继 1 必须挂在该 NPC 上: Illos");
		assertEquals(new SimpleTalkHandler.ItemStack(182200564, 1),
				handler.acceptGiveItem(1163), "接取侧发放");
		assertTrue(handler.workItems(1163).isEmpty(), "该行未声明 item_check：无交付门");
		assertFalse(handler.unresolvedGate(1163), "无门行不得 fail-closed");
		assertNull(handler.cutscene(1163), "该行无过场");
	}
}
