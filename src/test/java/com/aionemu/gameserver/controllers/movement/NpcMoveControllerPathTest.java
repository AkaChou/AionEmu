package com.aionemu.gameserver.controllers.movement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NpcMoveControllerPathTest {

	@Test
	void pathWithIntermediateWaypointRemainsValidWhileTargetMoves() {
		float[][] path = {{1, 1, 1}, {2, 2, 2}};

		assertTrue(NpcMoveController.hasIntermediateWaypoint(path));
	}

	@Test
	void finalOrMissingPathRequiresRecalculation() {
		assertFalse(NpcMoveController.hasIntermediateWaypoint(null));
		assertFalse(NpcMoveController.hasIntermediateWaypoint(new float[0][]));
		assertFalse(NpcMoveController.hasIntermediateWaypoint(new float[][] {{1, 1, 1}}));
	}

	@Test
	void changedDestinationRequiresMovePacketEvenWhenMaskStaysTheSame() {
		assertTrue(NpcMoveController.shouldBroadcastMovement((byte) -32, (byte) -32, true));
		assertFalse(NpcMoveController.shouldBroadcastMovement((byte) -32, (byte) -32, false));
	}
}
