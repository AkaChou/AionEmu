package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.NpcAggroListedEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphAiSignalBridge.RecipientSnapshot;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphNpcSignalBridge.NpcSnapshot;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphNpcSignalBridge.PlayerSnapshot;

class QuestGraphAiSignalBridgeTest {

	private static final PlayerSnapshot AGGRO_SOURCE = player(7, 30, 1);
	private static final NpcSnapshot NPC = new NpcSnapshot(277224, 5001, 400010000, 1, 0, 0, 0);

	/** 验证仇恨信号区分实际仇恨来源和半径广播接收者。 / Verifies that aggro signals distinguish the actual aggro source from a radius-broadcast recipient. */
	@Test
	void aggroSignalCarriesActorRecipientAndNpcAuthority() {
		NpcAggroListedEvent event = QuestGraphAiSignalBridge.aggroListed("aggro", 1000, recipient(8, 3, 1), AGGRO_SOURCE, NPC);

		assertEquals(8, event.playerId());
		assertEquals(7, event.aggroPlayerId());
		assertEquals(277224, event.npcId());
		assertEquals(5001, event.npcObjectId());
		assertEquals(3.0f, event.recipientDistance());
		assertEquals(true, event.recipientKnownToNpc());
	}

	/** 验证 50 米边界、跨实例和缺失快照被拒绝。 / Verifies rejection at the 50-meter boundary, across instances, and for missing snapshots. */
	@Test
	void aggroSignalRejectsInvalidRecipients() {
		assertThrows(IllegalArgumentException.class,
			() -> QuestGraphAiSignalBridge.aggroListed("boundary", 1000, recipient(8, 50, 1), AGGRO_SOURCE, NPC));
		assertThrows(IllegalArgumentException.class,
			() -> QuestGraphAiSignalBridge.aggroListed("instance", 1000, recipient(8, 3, 2), AGGRO_SOURCE, NPC));
		assertThrows(IllegalArgumentException.class,
			() -> QuestGraphAiSignalBridge.aggroListed("missing", 1000, null, AGGRO_SOURCE, NPC));
		assertThrows(IllegalArgumentException.class, () -> new RecipientSnapshot(player(8, 3, 1), false));
	}

	/** 创建来自 NPC known list 的接收者快照。 / Creates a recipient snapshot read from the NPC known list. */
	private static RecipientSnapshot recipient(int playerId, float x, int instanceId) {
		return new RecipientSnapshot(player(playerId, x, instanceId), true);
	}

	/** 创建指定位置和实例的玩家快照。 / Creates a player snapshot at the requested position and instance. */
	private static PlayerSnapshot player(int playerId, float x, int instanceId) {
		return new PlayerSnapshot(playerId, 400010000, instanceId, x, 0, 0);
	}
}
