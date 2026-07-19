package com.aionemu.gameserver.services.craft;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

class CraftServiceBatchTest {

	@Test
	void usesRequestedMaterialCountForBatchAetherforging() {
		assertEquals(20, CraftService.getCraftCount(15, 300));
	}

	@Test
	void usesSmallestMaterialCountWithoutConsumingOtherMaterialsAtTheirOwnMaximum() {
		assertEquals(2, CraftService.getCraftCount(Map.of(1001, 2, 1002, 3), Map.of(1001, 6L, 1002, 7L)));
		assertEquals(0, CraftService.getCraftCount(Map.of(1001, 2, 1002, 3), Map.of(1001, 6L)));
	}
}
