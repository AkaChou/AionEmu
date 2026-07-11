package com.aionemu.gameserver.geoEngine.math;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FastMathTest {

	@Test
	void convertsHalfPrecisionNaN() {
		assertTrue(Float.isNaN(FastMath.convertHalfToFloat(0x7c01)));
		assertTrue(Float.isNaN(FastMath.convertHalfToFloat(0xfe00)));
	}
}
