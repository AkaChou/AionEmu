package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.FlyingRingPassedEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.WindstreamEnteredEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphMovementSignalBridge.FlyingRingPassSnapshot;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphMovementSignalBridge.WindstreamEntrySnapshot;

/** 验证 movement bridge 只接受完整服务端 authority 快照。 / Verifies the movement bridge accepts only complete server-authority snapshots. */
class QuestGraphMovementSignalBridgeTest {

	/** 验证 windstream route、位置、pending path 与飞行状态必须全部有效。 / Verifies windstream route, position, pending path, and flight state must all be valid. */
	@Test
	void createsWindstreamEventOnlyFromCompleteServerAuthority() {
		WindstreamEntrySnapshot snapshot = new WindstreamEntrySnapshot(7, 210130000, 1, 405001, 405, 120, true, true, true);

		WindstreamEnteredEvent event = QuestGraphMovementSignalBridge.windstreamEntered("windstream", 1000, snapshot);

		assertEquals(7, event.playerId());
		assertEquals(210130000, event.worldId());
		assertEquals(405001, event.teleportId());
		assertThrows(IllegalArgumentException.class, () -> QuestGraphMovementSignalBridge.windstreamEntered("position", 1000,
			new WindstreamEntrySnapshot(7, 210130000, 1, 405001, 405, 120, false, true, true)));
		assertThrows(IllegalArgumentException.class, () -> QuestGraphMovementSignalBridge.windstreamEntered("pending", 1000,
			new WindstreamEntrySnapshot(7, 210130000, 1, 405001, 405, 120, true, false, true)));
		assertThrows(IllegalArgumentException.class, () -> QuestGraphMovementSignalBridge.windstreamEntered("flight", 1000,
			new WindstreamEntrySnapshot(7, 210130000, 1, 405001, 405, 120, true, true, false)));
		assertThrows(IllegalArgumentException.class, () -> QuestGraphMovementSignalBridge.windstreamEntered("route", 1000,
			new WindstreamEntrySnapshot(7, 210130000, 1, 405001, 406, 120, true, true, true)));
		assertThrows(IllegalArgumentException.class, () -> QuestGraphMovementSignalBridge.windstreamEntered("missing", 1000, null));
	}

	/** 验证 flying ring 保留平面交点与旧 3D radius fallback 两条服务端命中路径。 / Verifies flying rings retain both plane-intersection and legacy 3D-radius fallback paths. */
	@Test
	void createsFlyingRingEventOnlyAfterServerIntersectionAndStrictRadiusChecks() {
		FlyingRingPassedEvent event = QuestGraphMovementSignalBridge.flyingRingPassed("ring", 1000,
			new FlyingRingPassSnapshot(7, 210020000, 1, "ELTNEN_AIR_BOOSTER_1", 6, 5.9f, true, true));
		FlyingRingPassedEvent fallbackEvent = QuestGraphMovementSignalBridge.flyingRingPassed("ring-fallback", 1001,
			new FlyingRingPassSnapshot(7, 210020000, 1, "ELTNEN_AIR_BOOSTER_1", 6, 5.9f, true, false));

		assertEquals("ELTNEN_AIR_BOOSTER_1", event.ringName());
		assertEquals(210020000, event.targetId());
		assertFalse(fallbackEvent.intersectionPointAvailable());
		assertThrows(IllegalArgumentException.class, () -> QuestGraphMovementSignalBridge.flyingRingPassed("boundary", 1000,
			new FlyingRingPassSnapshot(7, 210020000, 1, "ELTNEN_AIR_BOOSTER_1", 6, 6, true, true)));
		assertThrows(IllegalArgumentException.class, () -> QuestGraphMovementSignalBridge.flyingRingPassed("plane", 1000,
			new FlyingRingPassSnapshot(7, 210020000, 1, "ELTNEN_AIR_BOOSTER_1", 6, 1, false, false)));
		assertThrows(IllegalArgumentException.class, () -> QuestGraphMovementSignalBridge.flyingRingPassed("name", 1000,
			new FlyingRingPassSnapshot(7, 210020000, 1, "eltnen_air_booster_1", 6, 1, true, true)));
		assertThrows(IllegalArgumentException.class, () -> QuestGraphMovementSignalBridge.flyingRingPassed("missing", 1000, null));
	}
}
