package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DelayedSpellAttackInstantEffectTest {

	@Test
	void calculatesRetailDelayFormulaAndFallback() {
		DelayedSpellAttackInstantEffect effect = new DelayedSpellAttackInstantEffect();
		effect.delay = 5000;
		effect.delaydelta = 400;
		assertEquals(6200, effect.calculateDelay(3));

		effect.delay = -1000;
		effect.delaydelta = 0;
		assertEquals(500, effect.calculateDelay(1));
	}
}
