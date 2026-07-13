package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NoReduceSpellATKInstantEffectTest {

	@Test
	void retailPercentDamageUsesR3AsMaximum() {
		NoReduceSpellATKInstantEffect effect = new NoReduceSpellATKInstantEffect();
		effect.value = 30;
		effect.delta = 1;
		effect.percent = true;
		effect.maxdamage = 500;

		assertEquals(500, effect.calculateBaseDamage(1, 10_000));
	}

	@Test
	void retailDamageNeverDropsBelowOne() {
		assertEquals(1, new NoReduceSpellATKInstantEffect().calculateBaseDamage(1, 10_000));
	}
}
