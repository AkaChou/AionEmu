package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定 30314（成就奖励任务）的交付门：必须持有并交出 100 个认证物。
 * <p>
 * P3 重锚（计划 §8.9）：30314 自 SimpleTalk 切换批起由 native 车道直驱，typed 定义退出生产视图，
 * 旧断言（overlay 定义里 started→reward 边的 HasItem/RemoveItem）属 IR 形状；真端事实不变
 * （quest.xml {@code collect_item1} = 认证物 ×100），改锚真端表行 + quest.xml 交付通道。
 * Locks quest 30314's hand-in gate: the player must carry and hand over 100 certification items. The
 * former IR-shape assertions are replaced by the retail row plus the quest.xml collect channel.
 */
class Quest30314RetailAlignmentTest {
	private static final int QUEST_ID = 30314;
	private static final int CERTIFICATION_ITEM = 186000098;

	@Test
	void setRewardRequiresAndConsumesTheCertificationItems() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		// 真端行：acquired / reward 同 NPC（Barretta）、单步、声明 item_check。
		// Retail row: one NPC for acquire and reward (Barretta), single step, item_check declared.
		assertTrue(handler.routes(QUEST_ID), "30314 必须由 native 车道路由");
		assertTrue(handler.requireRow(QUEST_ID).itemCheck(), "30314 真端行必须声明 item_check");
		assertEquals(handler.acquireNpc(QUEST_ID), handler.rewardNpc(QUEST_ID), "真端接取与交付同 NPC");
		assertEquals(0, handler.relayCount(QUEST_ID), "30314 是单步行");
		// 交付门 = quest.xml collect_item1 整组（认证物 ×100）：持有不足即不放行，放行即整组扣除。
		// The gate is the whole quest.xml collect_item1 group (100 items): the shortfall blocks the report
		// and the pass deducts the same group.
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(CERTIFICATION_ITEM, 100)), handler.workItems(QUEST_ID));
		assertFalse(handler.unresolvedGate(QUEST_ID), "门必须可解（不得 fail-closed）");
	}
}
