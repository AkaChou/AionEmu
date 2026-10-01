package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;

/**
 * 验证 28625/28626 在卡里加陈列柜上使用钥匙的客户端对话合同。
 * <p>
 * P3 重锚（计划 §8.9）：旧 IR 形状断言（节点名/条件/动作/页链）随 SimpleTalk 切换批退场，
 * 本类改为真端表行锚——接取/交付 NPC、中继步、发扣物品、交付门均取自
 * {@code Quest_SimpleTalk.xml} + {@code quest.xml} 与静态数据（{@code npc_template} /
 * 物品 {@code name_desc}），native 处理器必须逐项一致。
 * <p>
 * P3 re-anchor (plan §8.9): the IR-shape assertions retire with the SimpleTalk switch batch;
 * this class now pins the retail table row through the native handler.
 */
class Quest28625And28626ClientDialogAlignmentTest {

	@Test
	void reportsTheKaligaCollectionAtTheDisplayCabinet() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();

		// 真端行 28625：IDCromede_book → IDCromede_book
		assertTrue(handler.routes(28625), "28625 必须由 native 车道路由");
		assertEquals(730333, handler.acquireNpc(28625), "接取 NPC");
		assertEquals(730333, handler.rewardNpc(28625), "交付 NPC");
		assertEquals(0, handler.relayCount(28625), "中继步数");
		assertNull(handler.acceptGiveItem(28625), "接取侧无发放");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(185000102, 1)), handler.workItems(28625), "交付门");
		assertNull(handler.cutscene(28625), "该行无过场");

		// 真端行 28626：IDCromede_orb → IDCromede_orb
		assertTrue(handler.routes(28626), "28626 必须由 native 车道路由");
		assertEquals(730334, handler.acquireNpc(28626), "接取 NPC");
		assertEquals(730334, handler.rewardNpc(28626), "交付 NPC");
		assertEquals(0, handler.relayCount(28626), "中继步数");
		assertNull(handler.acceptGiveItem(28626), "接取侧无发放");
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(185000102, 1)), handler.workItems(28626), "交付门");
		assertNull(handler.cutscene(28626), "该行无过场");
	}
}
