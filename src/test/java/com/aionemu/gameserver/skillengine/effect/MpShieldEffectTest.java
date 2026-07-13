package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MpShieldEffectTest {

	@Test
	void calculatesRetailMpCostFormulaAndCap() {
		MpShieldEffect effect = new MpShieldEffect();
		effect.mpDelta = 5;
		effect.mpValue = 50;
		assertEquals(65, effect.calculateMpValue(3));
		assertEquals(100, effect.calculateMpValue(20));
	}
}
