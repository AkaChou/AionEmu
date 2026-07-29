package com.aionemu.gameserver.questEngine.graph.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.WorldEnteredEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphInvasionWorldSignalBridge.WorldEntrySnapshot;

/** 验证入侵世界 bridge 的 authority、恢复快照和失败关闭合同。 / Verifies invasion-world bridge authority, recovery snapshot, and fail-closed contracts. */
class QuestGraphInvasionWorldSignalBridgeTest {

	private static final WorldEntrySnapshot SNAPSHOT = new WorldEntrySnapshot(7, 220050000, 1, 1, 2, 3);

	/** 验证 vortex 或 rift 任一活跃即冻结凭据，且 codec 恢复后保持不变。 / Verifies either active vortex or rift freezes authority that survives codec recovery. */
	@Test
	void freezesEitherActiveInvasionChannelIntoEvent() {
		WorldEnteredEvent vortex = QuestGraphInvasionWorldSignalBridge.worldEntered("vortex", 1000, SNAPSHOT,
			worldId -> true, worldId -> false);
		WorldEnteredEvent rift = QuestGraphInvasionWorldSignalBridge.worldEntered("rift", 1001, SNAPSHOT,
			worldId -> false, worldId -> true);

		assertTrue(vortex.invasionAccessActive());
		assertTrue(rift.invasionAccessActive());
		assertEquals(vortex, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(vortex)));
		assertEquals(rift, QuestGraphEventCodec.decode(QuestGraphEventCodec.encode(rift)));
		byte[] legacyPayload = QuestGraphEventCodec.encode(vortex);
		legacyPayload[3] = 0x31;
		assertThrows(IllegalArgumentException.class, () -> QuestGraphEventCodec.decode(legacyPayload));
	}

	/** 验证无活跃通道不会授予访问凭据。 / Verifies no active channel grants no access authority. */
	@Test
	void preservesInactiveInvasionResult() {
		WorldEnteredEvent event = QuestGraphInvasionWorldSignalBridge.worldEntered("closed", 1000, SNAPSHOT,
			worldId -> false, worldId -> false);

		assertFalse(event.invasionAccessActive());
	}

	/** 验证服务读取异常不会被静默解释为关闭或成功。 / Verifies service-read failures are not silently interpreted as closed or successful. */
	@Test
	void propagatesServiceReadFailure() {
		assertThrows(IllegalStateException.class, () -> QuestGraphInvasionWorldSignalBridge.worldEntered("failed", 1000, SNAPSHOT,
			worldId -> {
				throw new IllegalStateException("unavailable");
			}, worldId -> false));
	}
}
