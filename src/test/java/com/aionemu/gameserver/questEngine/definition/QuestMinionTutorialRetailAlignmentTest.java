package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;

/**
 * QuestMinionTutorialRetailAlignmentTest
 * <p>
 * P3 重锚（计划 §8.9）：旧 IR 形状断言（节点名/条件/动作/页链）随 SimpleTalk 切换批退场，
 * 本类改为真端表行锚——接取/交付 NPC、中继步、发扣物品、交付门均取自
 * {@code Quest_SimpleTalk.xml} + {@code quest.xml} 与静态数据（{@code npc_template} /
 * 物品 {@code name_desc}），native 处理器必须逐项一致。
 * <p>
 * P3 re-anchor (plan §8.9): the IR-shape assertions retire with the SimpleTalk switch batch;
 * this class now pins the retail table row through the native handler.
 */
class QuestMinionTutorialRetailAlignmentTest {

	@Test
	void legacyAcceptItemGrantsSurviveTheTypedMigration() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();

		// 真端行 2266：Valuerin → Aurtri
		assertTrue(handler.routes(2266), "2266 必须由 native 车道路由");
		assertEquals(203558, handler.acquireNpc(2266), "接取 NPC");
		assertEquals(203654, handler.rewardNpc(2266), "交付 NPC");
		assertFalse(handler.acquireNpc(2266).equals(handler.rewardNpc(2266)),
				"接取与交付 owner 分离");
		assertEquals(1, handler.relayCount(2266), "中继步数");
		assertTrue(handler.relaysForNpc(203655).stream().anyMatch(relay -> relay.questId() == 2266 && relay.step() == 1),
				"中继 1 必须挂在该 NPC 上: Neifenmer");
		assertEquals(new SimpleTalkHandler.ItemStack(182203244, 1),
				handler.acceptGiveItem(2266), "接取侧发放");
		assertTrue(handler.workItems(2266).isEmpty(), "该行未声明 item_check：无交付门");
		assertFalse(handler.unresolvedGate(2266), "无门行不得 fail-closed");
		assertNull(handler.cutscene(2266), "该行无过场");

		// 真端行 3085：Talos → Shugo_LF2a_1
		assertTrue(handler.routes(3085), "3085 必须由 native 车道路由");
		assertEquals(798144, handler.acquireNpc(3085), "接取 NPC");
		assertEquals(798132, handler.rewardNpc(3085), "交付 NPC");
		assertFalse(handler.acquireNpc(3085).equals(handler.rewardNpc(3085)),
				"接取与交付 owner 分离");
		assertEquals(1, handler.relayCount(3085), "中继步数");
		assertTrue(handler.relaysForNpc(203830).stream().anyMatch(relay -> relay.questId() == 3085 && relay.step() == 1),
				"中继 1 必须挂在该 NPC 上: Vatonia");
		assertEquals(new SimpleTalkHandler.ItemStack(182208048, 1),
				handler.acceptGiveItem(3085), "接取侧发放");
		assertTrue(handler.workItems(3085).isEmpty(), "该行未声明 item_check：无交付门");
		assertFalse(handler.unresolvedGate(3085), "无门行不得 fail-closed");
		assertNull(handler.cutscene(3085), "该行无过场");

		// 真端行 28808：Aruza → NPC_Housing_FOBJ_01
		assertTrue(handler.owns(28808), "28808 在真端表行集内");
		assertFalse(handler.routes(28808), "28808 仍保留 XML 定义：native 只装载不路由（单一 owner）");
		assertEquals(830392, handler.acquireNpc(28808), "接取 NPC");
		// P9 前缀归一化解冻（91eaef381，2026-10-03）：表写 NPC_Housing_FOBJ_01 而模板写
		// Housing_FOBJ_01——精确前缀变体解析到对象模板 730534（旧「未唯一解析」冻结已废止）。
		// The P9 NPC_ prefix normalization lifts the old freeze: the table name resolves to the
		// Housing_FOBJ_01 object template 730534.
		assertEquals(730534, handler.rewardNpc(28808), "交付 NPC（Housing_FOBJ_01 对象模板）");
		assertEquals(0, handler.relayCount(28808), "中继步数");
		assertEquals(new SimpleTalkHandler.ItemStack(182213216, 1),
				handler.acceptGiveItem(28808), "接取侧发放");
		assertTrue(handler.workItems(28808).isEmpty(), "该行未声明 item_check：无交付门");
		assertFalse(handler.unresolvedGate(28808), "无门行不得 fail-closed");
		assertNull(handler.cutscene(28808), "该行无过场");
	}
}
