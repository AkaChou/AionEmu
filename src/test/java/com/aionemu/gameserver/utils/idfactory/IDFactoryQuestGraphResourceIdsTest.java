package com.aionemu.gameserver.utils.idfactory;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.CleanupLease;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.InstanceSpawnResourceIdentity;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.SpawnPlacementKind;

class IDFactoryQuestGraphResourceIdsTest {

	@Test
	void deduplicatesIdsReferencedByBothStateAndOperationRegistry() {
		CleanupLease seven = lease(7, 1, "seven");
		CleanupLease eight = lease(8, 2, "eight");
		CleanupLease nine = lease(9, 3, "nine");
		assertArrayEquals(new int[] { 7, 8, 9 },
			IDFactory.questGraphResourceIds(Map.of(9, nine, 7, seven), Map.of(8, eight, 9, nine)));
	}

	@Test
	void rejectsSameObjectIdWithDifferentDurableIdentities() {
		assertThrows(IDFactoryError.class,
			() -> IDFactory.questGraphResourceIds(Map.of(9, lease(9, 1, "state")), Map.of(9, lease(9, 2, "operation"))));
	}

	private static CleanupLease lease(int objectId, int questId, String key) {
		return CleanupLease.instanceSpawn(new InstanceSpawnResourceIdentity(7, questId, objectId, 204416,
			SpawnPlacementKind.FIXED, 0, 0, 210010000, 1, 1, 2, 3, (byte) 0, key));
	}
}
