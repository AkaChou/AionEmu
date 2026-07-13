package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CaseStatUpEffectTest {

	@Test
	void scalesFromHighHpValueToLowHpValue() {
		assertEquals(200, CaseStatUpEffect.interpolate(900, 1000, 20, 80, 500, 200));
		assertEquals(200, CaseStatUpEffect.interpolate(800, 1000, 20, 80, 500, 200));
		assertEquals(350, CaseStatUpEffect.interpolate(500, 1000, 20, 80, 500, 200));
		assertEquals(500, CaseStatUpEffect.interpolate(100, 1000, 20, 80, 500, 200));
	}
}
