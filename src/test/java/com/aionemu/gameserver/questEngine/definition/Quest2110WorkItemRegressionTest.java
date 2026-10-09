package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.tablelane.NativeQuestXmlTable;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 锁定 2110 的原版行事实：无工作物品门、接取不发放物品。
 * <p>
 * P3 重锚（计划 §8.9）：2110 自 SimpleTalk 切换批起由 native 车道直驱，typed 定义退出生产视图，
 * 旧断言（{@code ProductionQuestDefinitions.definition(2110)} 的 unaccepted→started 边、SELECT5 报告页）
 * 属 IR 形状，随切换批退场。改锚原版表行 + {@code quest.xml} 交付通道。
 * <p>
 * Locks quest 2110's retail row: no work-item gate and no grant on accept. The former IR-shape
 * assertions retired with the SimpleTalk switch batch (P3, plan §8.9).
 */
class Quest2110WorkItemRegressionTest {
	private static final int QUEST_ID = 2110;
	private static final int ACQUIRE_NPC = 203522;
	private static final int REWARD_NPC = 203533;

	@Test
	void retailRowCarriesNoWorkItemGateAndAcceptsWithoutGrantingTheLegacyItem() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		// 原版行：acquired=Kaindal / reward=Motgar、单步、零发放零回收列。
		// Retail row: acquired=Kaindal / reward=Motgar, single step, no grant or removal column.
		assertEquals(ACQUIRE_NPC, handler.acquireNpc(QUEST_ID));
		assertEquals(REWARD_NPC, handler.rewardNpc(QUEST_ID));
		assertEquals(0, handler.relayCount(QUEST_ID));
		assertNull(handler.acceptGiveItem(QUEST_ID), "接取不得发放任何物品（旧断言点 182203110）");
		assertNull(handler.stepGiveItem(QUEST_ID, 1));
		assertNull(handler.stepRemoveItem(QUEST_ID, 1));

		// 无 item_check ⇒ 无交付门；也不得因门通道缺失而 fail-closed。
		// No item_check means no hand-in gate, and a missing gate channel must not fail closed here.
		assertFalse(handler.requireRow(QUEST_ID).itemCheck(), "2110 原版行不得声明 item_check");
		assertEquals(List.of(), handler.workItems(QUEST_ID), "无 item_check 行不得带交付门物品");
		assertFalse(handler.unresolvedGate(QUEST_ID), "无 item_check 行不得 fail-closed");

		// quest.xml 侧同结论：两条工作物品通道都未声明（未声明 = 无门，不是缺数据）。
		// Same verdict on the quest.xml side: neither work-item channel is declared.
		NativeQuestXmlTable.QuestRow row = NativeQuestXmlTable.instance().find(QUEST_ID).orElseThrow();
		assertEquals(List.of(), row.numbered("collect_item"));
		assertEquals(List.of(), row.numbered("quest_work_item"));
	}
}
