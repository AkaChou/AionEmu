package com.aionemu.gameserver.services.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HotspotTeleportServiceTest {

	@Test
	void resolvesBalaureaMasterWorldsToLiveWorlds() {
		assertEquals(210050000, HotspotTeleportService.resolveLiveWorldId(210130000));
		assertEquals(220070000, HotspotTeleportService.resolveLiveWorldId(220140000));
		assertEquals(210040000, HotspotTeleportService.resolveLiveWorldId(210040000));
	}
}
