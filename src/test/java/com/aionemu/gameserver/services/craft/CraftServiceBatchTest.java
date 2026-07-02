package com.aionemu.gameserver.services.craft;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CraftServiceBatchTest {

	@Test
	void usesRequestedMaterialCountForBatchAetherforging() {
		assertEquals(20, CraftService.getCraftCount(15, 300));
	}
}
