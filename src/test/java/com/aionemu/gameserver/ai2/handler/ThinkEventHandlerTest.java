package com.aionemu.gameserver.ai2.handler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ThinkEventHandlerTest {

	@Test
	void patrolWaypointTakesPriorityOverSpawnPositionAfterCombat() {
		assertTrue(ThinkEventHandler.shouldReturn(true, true));
		assertTrue(ThinkEventHandler.shouldReturn(true, false));
		assertTrue(ThinkEventHandler.shouldReturn(false, false));
		assertFalse(ThinkEventHandler.shouldReturn(false, true));
	}
}
