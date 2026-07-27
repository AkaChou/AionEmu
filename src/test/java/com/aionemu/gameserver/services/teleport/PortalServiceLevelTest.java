package com.aionemu.gameserver.services.teleport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PortalServiceLevelTest {

	@Test
	void portalRequirementsEnforceBothLevelBounds() {
		assertTrue(PortalService.isLevelAllowed(1, 1, 100));
		assertTrue(PortalService.isLevelAllowed(100, 1, 100));
		assertFalse(PortalService.isLevelAllowed(0, 1, 100));
		assertFalse(PortalService.isLevelAllowed(101, 1, 100));
	}
}
