package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SkillCooltimeResetEffectTest {

	@Test
	void calculatesRetailRatioAndFixedReductionsWhileKeepingLegacyTemplates() {
		SkillCooltimeResetEffect effect = new SkillCooltimeResetEffect();
		effect.delta = 5;
		assertTrue(effect.isPercentReset());
		assertEquals(5, effect.getResetAmount());
		assertEquals(9_000, SkillCooltimeResetEffect.calculateRemaining(10_000, 20_000, 5, true));

		effect.percent = false;
		effect.delta = 100;
		effect.value = 2_000;
		assertFalse(effect.isPercentReset());
		assertEquals(2_000, effect.getResetAmount());
		assertEquals(8_000, SkillCooltimeResetEffect.calculateRemaining(10_000, 20_000, 2_000, false));
	}
}
