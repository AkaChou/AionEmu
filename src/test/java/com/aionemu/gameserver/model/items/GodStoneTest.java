package com.aionemu.gameserver.model.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GodStoneTest {

	@Test
	void usesExactPerThousandProbabilityBoundaries() {
		assertFalse(GodStone.roll(0));
		assertTrue(GodStone.roll(1000));
	}

	@Test
	void alignsRateReductionAndBreakThreshold() {
		assertEquals(100, GodStone.adjustProbability(150, 50, 1));
		assertEquals(200, GodStone.adjustProbability(150, 50, 2));
		assertEquals(0, GodStone.adjustProbability(50, 50, 2));
		assertFalse(GodStone.shouldBreak(10, 10, 1000));
		assertTrue(GodStone.shouldBreak(11, 10, 1000));
	}
}
