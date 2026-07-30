package com.aionemu.gameserver.questEngine.graph.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortCoordinatesDestination;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortSource;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StartEscortAction;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.CleanupLease;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.EscortResourceIdentity;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.InstanceSpawnResourceIdentity;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.SpawnPlacementKind;

class QuestGraphResourceOperationCodecTest {

	@Test
	void roundTripsMaterializedClosedResourceIdentities() {
		CleanupLease spawn = CleanupLease.instanceSpawn(new InstanceSpawnResourceIdentity(7, 1, 900001, 204830,
			SpawnPlacementKind.PLAYER, 0, 7, 210040000, 3, 1, 2, 3, (byte) 4, "spawn-op"));
		assertEquals(spawn, PlayerQuestGraphStateCodec.decodeCleanupLease(
			PlayerQuestGraphStateCodec.encodeCleanupLease(spawn)));

		StartEscortAction action = new StartEscortAction(EscortSource.PLAYER_POSITION_SPAWN, 204416, (byte) 8,
			null, true, true, true, false, new EscortCoordinatesDestination(1, 2, 3));
		CleanupLease escort = CleanupLease.escort(new EscortResourceIdentity(7, 2, 900002, 204416,
			210040000, 3, 1, 2, 3, 0, 0, true, null, action, "escort-op"));
		assertEquals(escort, PlayerQuestGraphStateCodec.decodeCleanupLease(
			PlayerQuestGraphStateCodec.encodeCleanupLease(escort)));
	}

	@Test
	void rejectsUnmaterializedOperationIdentity() {
		CleanupLease plan = CleanupLease.instanceSpawn(new InstanceSpawnResourceIdentity(7, 1, 0, 204830,
			SpawnPlacementKind.PLAYER, 0, 7, 210040000, 3, 1, 2, 3, (byte) 4, "spawn-op"));
		assertThrows(IllegalArgumentException.class, () -> PlayerQuestGraphStateCodec.encodeCleanupLease(plan));
	}
}
