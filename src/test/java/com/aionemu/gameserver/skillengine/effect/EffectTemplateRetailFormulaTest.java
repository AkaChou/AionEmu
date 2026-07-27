package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.stats.container.StatEnum;

class EffectTemplateRetailFormulaTest {

	@Test
	void calculatesRetailPreEffectAndCriticalLevelFormulas() {
		DummyEffect effect = new DummyEffect();
		effect.preEffectProbDelta = 2;
		effect.preEffectProb = 30;
		effect.critProbMod1 = 7;
		effect.critProbMod2 = 40;
		effect.critAddDmg1 = 3;
		effect.critAddDmg2 = 150;

		assertEquals(38, effect.getPreEffectProbability(4));
		assertEquals(68, effect.getCriticalProbability(4));
		assertEquals(162, effect.getCriticalAdditionalDamage(4));
	}

	@Test
	void combinesAbnormalResistanceIntoOneRetailCheck() {
		assertEquals(350, EffectTemplate.calculateAbnormalResistChance(500, 200, 50, false));
		assertEquals(140, EffectTemplate.calculateAbnormalResistChance(500, 200, 50, true));
		assertEquals(1000, EffectTemplate.calculateAbnormalResistChance(9999, 0, 0, false));
		assertEquals(0, EffectTemplate.calculateAbnormalResistChance(0, 500, 0, false));
	}

	@Test
	void mapsRetailArAllAndStunLikeMasksToRuntimeStats() {
		assertEquals(25, EffectTemplate.ABNORMAL_RESISTANCE_STATS.size());
		assertEquals(19, EffectTemplate.AR_ALL_RESISTANCE_STATS.size());
		assertEquals(6, EffectTemplate.STUNLIKE_RESISTANCE_STATS.size());
		assertTrue(EffectTemplate.AR_ALL_RESISTANCE_STATS.contains(StatEnum.POISON_RESISTANCE));
		assertFalse(EffectTemplate.AR_ALL_RESISTANCE_STATS.contains(StatEnum.STUN_RESISTANCE));
		assertFalse(EffectTemplate.STUNLIKE_RESISTANCE_STATS.contains(StatEnum.POISON_RESISTANCE));
		assertTrue(EffectTemplate.STUNLIKE_RESISTANCE_STATS.contains(StatEnum.STUN_RESISTANCE));
		assertTrue(EffectTemplate.AR_ALL_RESISTANCE_STATS.contains(StatEnum.NOFLY_RESISTANCE));
		assertTrue(EffectTemplate.AR_ALL_RESISTANCE_STATS.contains(StatEnum.SIMPLE_ROOT_RESISTANCE));
		assertEquals(63, StatEnum.BIND_RESISTANCE.getItemStoneMask());
		assertEquals(66, StatEnum.NOFLY_RESISTANCE.getItemStoneMask());
		assertEquals(89, StatEnum.BIND_RESISTANCE_PENETRATION.getItemStoneMask());
		assertEquals(93, StatEnum.SIMPLE_ROOT_RESISTANCE_PENETRATION.getItemStoneMask());
	}
}
