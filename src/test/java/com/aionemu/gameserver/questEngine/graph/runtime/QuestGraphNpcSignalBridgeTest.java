package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.EscortLostTargetEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.EscortReachedTargetEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.NpcProximityEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphNpcSignalBridge.NpcSnapshot;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphNpcSignalBridge.PlayerSnapshot;

class QuestGraphNpcSignalBridgeTest {

	private static final PlayerSnapshot PLAYER = new PlayerSnapshot(7, 210010000, 1, 0, 0, 0);

	/** 验证 bridge 计算服务端距离并保留 NPC 身份与实例。 / Verifies server-distance calculation and preservation of NPC identity and instance. */
	@Test
	void proximityUsesSameContextServerSnapshots() {
		NpcProximityEvent event = QuestGraphNpcSignalBridge.proximity("proximity", 1000, PLAYER,
			new NpcSnapshot(100, 5001, 210010000, 1, 3, 4, 0));

		assertEquals(5.0f, event.distance());
		assertEquals(100, event.npcId());
		assertEquals(5001, event.npcObjectId());
		assertEquals(210010000, event.worldId());
		assertEquals(1, event.instanceId());
	}

	/** 验证旧引擎严格 20 米边界和跨 world/instance 信号均被拒绝。 / Verifies rejection at the legacy strict 20-meter boundary and across world/instance boundaries. */
	@Test
	void proximityRejectsBoundaryAndCrossContextInput() {
		assertThrows(IllegalArgumentException.class, () -> QuestGraphNpcSignalBridge.proximity("boundary", 1000, PLAYER,
			new NpcSnapshot(100, 5001, 210010000, 1, 20, 0, 0)));
		assertThrows(IllegalArgumentException.class, () -> QuestGraphNpcSignalBridge.proximity("world", 1000, PLAYER,
			new NpcSnapshot(100, 5001, 220010000, 1, 1, 0, 0)));
		assertThrows(IllegalArgumentException.class, () -> QuestGraphNpcSignalBridge.proximity("instance", 1000, PLAYER,
			new NpcSnapshot(100, 5001, 210010000, 2, 1, 0, 0)));
	}

	/** 验证 escort reach/lost 使用显式 quest owner 且共享同一 NPC 快照。 / Verifies explicit escort owners and a shared NPC snapshot for reach/loss. */
	@Test
	void escortSignalsRemainOwnerTargeted() {
		NpcSnapshot npc = new NpcSnapshot(100, 5001, 210010000, 1, 3, 4, 0);
		EscortReachedTargetEvent reached = QuestGraphNpcSignalBridge.escortReached("reached", 1001, 42, PLAYER, npc);
		EscortLostTargetEvent lost = QuestGraphNpcSignalBridge.escortLost("lost", 1002, 42, PLAYER, npc);

		assertEquals(42, reached.targetId());
		assertEquals(42, lost.targetId());
		assertEquals(reached.npcObjectId(), lost.npcObjectId());
		assertThrows(IllegalArgumentException.class,
			() -> QuestGraphNpcSignalBridge.escortReached("invalid", 1003, 0, PLAYER, npc));
	}

	/** 验证 snapshot 输入拒绝无效身份和非有限坐标。 / Verifies snapshot rejection of invalid identities and non-finite coordinates. */
	@Test
	void snapshotsRejectInvalidAuthorityFields() {
		assertThrows(IllegalArgumentException.class, () -> new PlayerSnapshot(0, 210010000, 1, 0, 0, 0));
		assertThrows(IllegalArgumentException.class, () -> new NpcSnapshot(0, 5001, 210010000, 1, 0, 0, 0));
		assertThrows(IllegalArgumentException.class,
			() -> new NpcSnapshot(100, 5001, 210010000, 1, Float.NaN, 0, 0));
	}
}
