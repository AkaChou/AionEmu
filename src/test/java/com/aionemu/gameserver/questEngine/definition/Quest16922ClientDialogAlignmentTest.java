package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;

/**
 * 验证任务 16922 将客户端简易接取与报告、领奖路由限定在各自的正式 NPC owner。
 * <p>
 * P3 重锚（计划 §8.9）：旧 IR 形状断言（节点名/条件/动作/页链）随 SimpleTalk 切换批退场，
 * 本类改为原版表行锚——接取/交付 NPC、中继步、发扣物品、交付门均取自
 * {@code Quest_SimpleTalk.xml} + {@code quest.xml} 与静态数据（{@code npc_template} /
 * 物品 {@code name_desc}），native 处理器必须逐项一致。
 * <p>
 * P3 re-anchor (plan §8.9): the IR-shape assertions retire with the SimpleTalk switch batch;
 * this class now pins the retail table row through the native handler.
 */
class Quest16922ClientDialogAlignmentTest {

	@Test
	void keepsTheRetailSimpleStartReportAndRewardOwnersExclusive() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();

		// 原版行 16922：LDF5_Fortress_Alphion_E → LDF5_Fortress_Fitreung_E
		assertTrue(handler.routes(16922), "16922 必须由 native 车道路由");
		assertEquals(802431, handler.acquireNpc(16922), "接取 NPC");
		assertEquals(804628, handler.rewardNpc(16922), "交付 NPC");
		assertFalse(handler.acquireNpc(16922).equals(handler.rewardNpc(16922)),
				"接取与交付 owner 分离");
		assertEquals(0, handler.relayCount(16922), "中继步数");
		assertNull(handler.acceptGiveItem(16922), "接取侧无发放");
		assertTrue(handler.workItems(16922).isEmpty(), "该行未声明 item_check：无交付门");
		assertFalse(handler.unresolvedGate(16922), "无门行不得 fail-closed");
		assertNull(handler.cutscene(16922), "该行无过场");
	}
}
