package com.aionemu.gameserver.services.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.Race;

class MultiReturnServiceTest {
	@Test
	void mapsMergedBalaureaWorldsToExistingPortalLocations() {
		assertEquals(2101300, MultiReturnService.getTeleportWorldId(210130000, Race.ELYOS));
		assertEquals(2201400, MultiReturnService.getTeleportWorldId(220140000, Race.ASMODIANS));
	}
}
