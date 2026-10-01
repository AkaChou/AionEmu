package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;

/**
 * 验证任务 51009 将客户端标准接取与报告、领奖路由限定在正式 NPC owner。
 * <p>
 * P3 重锚（计划 §8.9）：旧 IR 形状断言（节点名/条件/动作/页链）随 SimpleTalk 切换批退场，
 * 本类改为真端表行锚——接取/交付 NPC、中继步、发扣物品、交付门均取自
 * {@code Quest_SimpleTalk.xml} + {@code quest.xml} 与静态数据（{@code npc_template} /
 * 物品 {@code name_desc}），native 处理器必须逐项一致。
 * <p>
 * P3 re-anchor (plan §8.9): the IR-shape assertions retire with the SimpleTalk switch batch;
 * this class now pins the retail table row through the native handler.
 */
class Quest51009ClientDialogAlignmentTest {

	@Test
	void keepsTheRetailStandardStartReportAndRewardOwnersExclusive() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();

		// 真端行 51009：DC1_ChristmasEvent_Santa → DC1_ChristmasEvent_Santa
		assertTrue(handler.routes(51009), "51009 必须由 native 车道路由");
		assertEquals(831033, handler.acquireNpc(51009), "接取 NPC");
		assertEquals(831033, handler.rewardNpc(51009), "交付 NPC");
		assertEquals(0, handler.relayCount(51009), "中继步数");
		assertNull(handler.acceptGiveItem(51009), "接取侧无发放");
		assertTrue(handler.workItems(51009).isEmpty(), "该行未声明 item_check：无交付门");
		assertFalse(handler.unresolvedGate(51009), "无门行不得 fail-closed");
		assertNull(handler.cutscene(51009), "该行无过场");
	}
}
