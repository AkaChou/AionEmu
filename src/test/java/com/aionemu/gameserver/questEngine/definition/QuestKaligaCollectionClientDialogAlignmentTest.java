package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;

/**
 * 验证卡里加 20 个收藏品任务的阵营陈列柜和客户端对话合同。
 * <p>
 * P3 重锚（计划 §8.9）：旧 IR 形状断言（节点名/条件/动作/页链）随 SimpleTalk 切换批退场，
 * 本类改为原版表行锚——接取/交付 NPC、中继步、发扣物品、交付门均取自
 * {@code Quest_SimpleTalk.xml} + {@code quest.xml} 与静态数据（{@code npc_template} /
 * 物品 {@code name_desc}），native 处理器必须逐项一致。
 * <p>
 * P3 re-anchor (plan §8.9): the IR-shape assertions retire with the SimpleTalk switch batch;
 * this class now pins the retail table row through the native handler.
 */
class QuestKaligaCollectionClientDialogAlignmentTest {

	@Test
	void allFactionsUseTheirCabinetAndClientPages() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();

		// 原版行 18618：IDCromede_sword → IDCromede_sword
		assertTrue(handler.routes(18618), "18618 必须由 native 车道路由");
		assertEquals(730326, handler.acquireNpc(18618), "接取 NPC");
		assertEquals(730326, handler.rewardNpc(18618), "交付 NPC");
		assertEquals(0, handler.relayCount(18618), "中继步数");
		assertNull(handler.acceptGiveItem(18618), "接取侧无发放");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(185000102, 1)), handler.workItems(18618), "交付门");
		assertNull(handler.cutscene(18618), "该行无过场");

		// 原版行 18619：IDCromede_2hsword → IDCromede_2hsword
		assertTrue(handler.routes(18619), "18619 必须由 native 车道路由");
		assertEquals(730327, handler.acquireNpc(18619), "接取 NPC");
		assertEquals(730327, handler.rewardNpc(18619), "交付 NPC");
		assertEquals(0, handler.relayCount(18619), "中继步数");
		assertNull(handler.acceptGiveItem(18619), "接取侧无发放");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(185000102, 1)), handler.workItems(18619), "交付门");
		assertNull(handler.cutscene(18619), "该行无过场");

		// 原版行 18620：IDCromede_dagger → IDCromede_dagger
		assertTrue(handler.routes(18620), "18620 必须由 native 车道路由");
		assertEquals(730328, handler.acquireNpc(18620), "接取 NPC");
		assertEquals(730328, handler.rewardNpc(18620), "交付 NPC");
		assertEquals(0, handler.relayCount(18620), "中继步数");
		assertNull(handler.acceptGiveItem(18620), "接取侧无发放");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(185000102, 1)), handler.workItems(18620), "交付门");
		assertNull(handler.cutscene(18620), "该行无过场");

		// 原版行 18621：IDCromede_polearm → IDCromede_polearm
		assertTrue(handler.routes(18621), "18621 必须由 native 车道路由");
		assertEquals(730329, handler.acquireNpc(18621), "接取 NPC");
		assertEquals(730329, handler.rewardNpc(18621), "交付 NPC");
		assertEquals(0, handler.relayCount(18621), "中继步数");
		assertNull(handler.acceptGiveItem(18621), "接取侧无发放");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(185000102, 1)), handler.workItems(18621), "交付门");
		assertNull(handler.cutscene(18621), "该行无过场");

		// 原版行 18622：IDCromede_bow → IDCromede_bow
		assertTrue(handler.routes(18622), "18622 必须由 native 车道路由");
		assertEquals(730330, handler.acquireNpc(18622), "接取 NPC");
		assertEquals(730330, handler.rewardNpc(18622), "交付 NPC");
		assertEquals(0, handler.relayCount(18622), "中继步数");
		assertNull(handler.acceptGiveItem(18622), "接取侧无发放");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(185000102, 1)), handler.workItems(18622), "交付门");
		assertNull(handler.cutscene(18622), "该行无过场");

		// 原版行 18623：IDCromede_mace → IDCromede_mace
		assertTrue(handler.routes(18623), "18623 必须由 native 车道路由");
		assertEquals(730331, handler.acquireNpc(18623), "接取 NPC");
		assertEquals(730331, handler.rewardNpc(18623), "交付 NPC");
		assertEquals(0, handler.relayCount(18623), "中继步数");
		assertNull(handler.acceptGiveItem(18623), "接取侧无发放");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(185000102, 1)), handler.workItems(18623), "交付门");
		assertNull(handler.cutscene(18623), "该行无过场");

		// 原版行 18624：IDCromede_staff → IDCromede_staff
		assertTrue(handler.routes(18624), "18624 必须由 native 车道路由");
		assertEquals(730332, handler.acquireNpc(18624), "接取 NPC");
		assertEquals(730332, handler.rewardNpc(18624), "交付 NPC");
		assertEquals(0, handler.relayCount(18624), "中继步数");
		assertNull(handler.acceptGiveItem(18624), "接取侧无发放");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(185000102, 1)), handler.workItems(18624), "交付门");
		assertNull(handler.cutscene(18624), "该行无过场");

		// 原版行 18625：IDCromede_book → IDCromede_book
		assertTrue(handler.routes(18625), "18625 必须由 native 车道路由");
		assertEquals(730333, handler.acquireNpc(18625), "接取 NPC");
		assertEquals(730333, handler.rewardNpc(18625), "交付 NPC");
		assertEquals(0, handler.relayCount(18625), "中继步数");
		assertNull(handler.acceptGiveItem(18625), "接取侧无发放");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(185000102, 1)), handler.workItems(18625), "交付门");
		assertNull(handler.cutscene(18625), "该行无过场");

		// 原版行 18626：IDCromede_orb → IDCromede_orb
		assertTrue(handler.routes(18626), "18626 必须由 native 车道路由");
		assertEquals(730334, handler.acquireNpc(18626), "接取 NPC");
		assertEquals(730334, handler.rewardNpc(18626), "交付 NPC");
		assertEquals(0, handler.relayCount(18626), "中继步数");
		assertNull(handler.acceptGiveItem(18626), "接取侧无发放");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(185000102, 1)), handler.workItems(18626), "交付门");
		assertNull(handler.cutscene(18626), "该行无过场");

		// 原版行 18627：IDCromede_shield → IDCromede_shield
		assertTrue(handler.routes(18627), "18627 必须由 native 车道路由");
		assertEquals(730335, handler.acquireNpc(18627), "接取 NPC");
		assertEquals(730335, handler.rewardNpc(18627), "交付 NPC");
		assertEquals(0, handler.relayCount(18627), "中继步数");
		assertNull(handler.acceptGiveItem(18627), "接取侧无发放");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(185000102, 1)), handler.workItems(18627), "交付门");
		assertNull(handler.cutscene(18627), "该行无过场");

		// 原版行 28618：IDCromede_sword → IDCromede_sword
		assertTrue(handler.routes(28618), "28618 必须由 native 车道路由");
		assertEquals(730326, handler.acquireNpc(28618), "接取 NPC");
		assertEquals(730326, handler.rewardNpc(28618), "交付 NPC");
		assertEquals(0, handler.relayCount(28618), "中继步数");
		assertNull(handler.acceptGiveItem(28618), "接取侧无发放");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(185000102, 1)), handler.workItems(28618), "交付门");
		assertNull(handler.cutscene(28618), "该行无过场");

		// 原版行 28619：IDCromede_2hsword → IDCromede_2hsword
		assertTrue(handler.routes(28619), "28619 必须由 native 车道路由");
		assertEquals(730327, handler.acquireNpc(28619), "接取 NPC");
		assertEquals(730327, handler.rewardNpc(28619), "交付 NPC");
		assertEquals(0, handler.relayCount(28619), "中继步数");
		assertNull(handler.acceptGiveItem(28619), "接取侧无发放");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(185000102, 1)), handler.workItems(28619), "交付门");
		assertNull(handler.cutscene(28619), "该行无过场");

		// 原版行 28620：IDCromede_dagger → IDCromede_dagger
		assertTrue(handler.routes(28620), "28620 必须由 native 车道路由");
		assertEquals(730328, handler.acquireNpc(28620), "接取 NPC");
		assertEquals(730328, handler.rewardNpc(28620), "交付 NPC");
		assertEquals(0, handler.relayCount(28620), "中继步数");
		assertNull(handler.acceptGiveItem(28620), "接取侧无发放");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(185000102, 1)), handler.workItems(28620), "交付门");
		assertNull(handler.cutscene(28620), "该行无过场");

		// 原版行 28621：IDCromede_polearm → IDCromede_polearm
		assertTrue(handler.routes(28621), "28621 必须由 native 车道路由");
		assertEquals(730329, handler.acquireNpc(28621), "接取 NPC");
		assertEquals(730329, handler.rewardNpc(28621), "交付 NPC");
		assertEquals(0, handler.relayCount(28621), "中继步数");
		assertNull(handler.acceptGiveItem(28621), "接取侧无发放");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(185000102, 1)), handler.workItems(28621), "交付门");
		assertNull(handler.cutscene(28621), "该行无过场");

		// 原版行 28622：IDCromede_bow → IDCromede_bow
		assertTrue(handler.routes(28622), "28622 必须由 native 车道路由");
		assertEquals(730330, handler.acquireNpc(28622), "接取 NPC");
		assertEquals(730330, handler.rewardNpc(28622), "交付 NPC");
		assertEquals(0, handler.relayCount(28622), "中继步数");
		assertNull(handler.acceptGiveItem(28622), "接取侧无发放");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(185000102, 1)), handler.workItems(28622), "交付门");
		assertNull(handler.cutscene(28622), "该行无过场");

		// 原版行 28623：IDCromede_mace → IDCromede_mace
		assertTrue(handler.routes(28623), "28623 必须由 native 车道路由");
		assertEquals(730331, handler.acquireNpc(28623), "接取 NPC");
		assertEquals(730331, handler.rewardNpc(28623), "交付 NPC");
		assertEquals(0, handler.relayCount(28623), "中继步数");
		assertNull(handler.acceptGiveItem(28623), "接取侧无发放");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(185000102, 1)), handler.workItems(28623), "交付门");
		assertNull(handler.cutscene(28623), "该行无过场");

		// 原版行 28624：IDCromede_staff → IDCromede_staff
		assertTrue(handler.routes(28624), "28624 必须由 native 车道路由");
		assertEquals(730332, handler.acquireNpc(28624), "接取 NPC");
		assertEquals(730332, handler.rewardNpc(28624), "交付 NPC");
		assertEquals(0, handler.relayCount(28624), "中继步数");
		assertNull(handler.acceptGiveItem(28624), "接取侧无发放");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(185000102, 1)), handler.workItems(28624), "交付门");
		assertNull(handler.cutscene(28624), "该行无过场");

		// 原版行 28625：IDCromede_book → IDCromede_book
		assertTrue(handler.routes(28625), "28625 必须由 native 车道路由");
		assertEquals(730333, handler.acquireNpc(28625), "接取 NPC");
		assertEquals(730333, handler.rewardNpc(28625), "交付 NPC");
		assertEquals(0, handler.relayCount(28625), "中继步数");
		assertNull(handler.acceptGiveItem(28625), "接取侧无发放");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(185000102, 1)), handler.workItems(28625), "交付门");
		assertNull(handler.cutscene(28625), "该行无过场");

		// 原版行 28626：IDCromede_orb → IDCromede_orb
		assertTrue(handler.routes(28626), "28626 必须由 native 车道路由");
		assertEquals(730334, handler.acquireNpc(28626), "接取 NPC");
		assertEquals(730334, handler.rewardNpc(28626), "交付 NPC");
		assertEquals(0, handler.relayCount(28626), "中继步数");
		assertNull(handler.acceptGiveItem(28626), "接取侧无发放");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(185000102, 1)), handler.workItems(28626), "交付门");
		assertNull(handler.cutscene(28626), "该行无过场");

		// 原版行 28627：IDCromede_shield → IDCromede_shield
		assertTrue(handler.routes(28627), "28627 必须由 native 车道路由");
		assertEquals(730335, handler.acquireNpc(28627), "接取 NPC");
		assertEquals(730335, handler.rewardNpc(28627), "交付 NPC");
		assertEquals(0, handler.relayCount(28627), "中继步数");
		assertNull(handler.acceptGiveItem(28627), "接取侧无发放");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(185000102, 1)), handler.workItems(28627), "交付门");
		assertNull(handler.cutscene(28627), "该行无过场");
	}
}
