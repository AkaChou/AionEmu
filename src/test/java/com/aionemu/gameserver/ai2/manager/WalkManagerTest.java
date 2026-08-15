package com.aionemu.gameserver.ai2.manager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WalkManagerTest {

	@Test
	void randomWalkLeashIgnoresSpawnHeightDifference() {
		assertFalse(WalkManager.isOutsideRandomWalkRange(1532.7224f, 1301.1207f,
			1532.7224f, 1301.1207f, 6));
		assertTrue(WalkManager.isOutsideRandomWalkRange(1539, 1301.1207f,
			1532.7224f, 1301.1207f, 6));
	}
}
