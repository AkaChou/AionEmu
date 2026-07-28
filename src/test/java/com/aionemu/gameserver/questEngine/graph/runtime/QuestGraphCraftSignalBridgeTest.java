package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphCraftSignalBridge.FailureSnapshot;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.CraftFailedEvent;

class QuestGraphCraftSignalBridgeTest {

	/** 验证制作失败事件冻结失败产品和制作后零库存。 / Verifies that craft-failure events freeze the failed product and zero post-attempt inventory. */
	@Test
	void craftFailureCarriesServerInventoryAuthority() {
		CraftFailedEvent event = QuestGraphCraftSignalBridge.craftFailed("craft-failed", 1000,
			new FailureSnapshot(7, 182206773, 0));

		assertEquals(7, event.playerId());
		assertEquals(182206773, event.itemId());
		assertEquals(0, event.inventoryCountAfterAttempt());
	}

	/** 验证产品仍存在、快照缺失或字段非法时拒绝制作失败 credit。 / Verifies rejection when the product remains, the snapshot is missing, or fields are invalid. */
	@Test
	void craftFailureRejectsIneligibleSnapshots() {
		assertThrows(IllegalArgumentException.class, () -> QuestGraphCraftSignalBridge.craftFailed("present", 1000,
			new FailureSnapshot(7, 182206773, 1)));
		assertThrows(IllegalArgumentException.class, () -> QuestGraphCraftSignalBridge.craftFailed("missing", 1000, null));
		assertThrows(IllegalArgumentException.class, () -> new FailureSnapshot(0, 182206773, 0));
		assertThrows(IllegalArgumentException.class, () -> new FailureSnapshot(7, 182206773, -1));
	}
}
