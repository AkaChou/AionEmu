package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;

/**
 * 验证任务 13902 将客户端简易接取与报告、领奖路由限定在各自的正式 NPC owner。
 * <p>
 * P3 重锚（计划 §8.9）：旧 IR 形状断言（节点名/条件/动作/页链）随 SimpleTalk 切换批退场，
 * 本类改为原版表行锚——接取/交付 NPC、中继步、发扣物品、交付门均取自
 * {@code Quest_SimpleTalk.xml} + {@code quest.xml} 与静态数据（{@code npc_template} /
 * 物品 {@code name_desc}），native 处理器必须逐项一致。
 * <p>
 * P3 re-anchor (plan §8.9): the IR-shape assertions retire with the SimpleTalk switch batch;
 * this class now pins the retail table row through the native handler.
 */
class Quest13902ClientDialogAlignmentTest {

	@Test
	void keepsTheRetailSimpleStartReportAndRewardOwnersExclusive() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();

		// 原版行 13900：LF5_Atmos_E → Corridor_L_MainQuest_E
		assertTrue(handler.routes(13900), "13900 必须由 native 车道路由");
		assertEquals(804699, handler.acquireNpc(13900), "接取 NPC");
		assertEquals(798514, handler.rewardNpc(13900), "交付 NPC");
		assertFalse(handler.acquireNpc(13900).equals(handler.rewardNpc(13900)),
				"接取与交付 owner 分离");
		assertEquals(2, handler.relayCount(13900), "中继步数");
		assertTrue(handler.relaysForNpc(798926).stream().anyMatch(relay -> relay.questId() == 13900 && relay.step() == 1),
				"中继 1 必须挂在该 NPC 上: Cainus");
		assertTrue(handler.relaysForNpc(203726).stream().anyMatch(relay -> relay.questId() == 13900 && relay.step() == 2),
				"中继 2 必须挂在该 NPC 上: Polyidus");
		assertNull(handler.acceptGiveItem(13900), "接取侧无发放");
		assertTrue(handler.workItems(13900).isEmpty(), "该行未声明 item_check：无交付门");
		assertFalse(handler.unresolvedGate(13900), "无门行不得 fail-closed");
		assertNull(handler.cutscene(13900), "该行无过场");

		// 原版行 13902：Corridor_L_MainQuest_E → Cainus
		assertTrue(handler.routes(13902), "13902 必须由 native 车道路由");
		assertEquals(798514, handler.acquireNpc(13902), "接取 NPC");
		assertEquals(798926, handler.rewardNpc(13902), "交付 NPC");
		assertFalse(handler.acquireNpc(13902).equals(handler.rewardNpc(13902)),
				"接取与交付 owner 分离");
		assertEquals(0, handler.relayCount(13902), "中继步数");
		assertNull(handler.acceptGiveItem(13902), "接取侧无发放");
		assertTrue(handler.workItems(13902).isEmpty(), "该行未声明 item_check：无交付门");
		assertFalse(handler.unresolvedGate(13902), "无门行不得 fail-closed");
		assertNull(handler.cutscene(13902), "该行无过场");
	}
}
