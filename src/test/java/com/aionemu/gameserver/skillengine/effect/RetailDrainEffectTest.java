package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RetailDrainEffectTest {

	@Test
	void calculatesDamageAndDrainLevelFormulas() {
		SkillAtkDrainInstantEffect physical = new SkillAtkDrainInstantEffect();
		physical.flatDelta = 3;
		physical.flatValue = 10;
		physical.percentDelta = 2;
		physical.percentValue = 20;
		physical.hpPercentDelta = 4;
		physical.hp_percent = 50;
		physical.mpPercentDelta = 1;
		physical.mp_percent = 25;

		assertEquals(22, physical.calculateFlatDamage(4));
		assertEquals(28, physical.calculatePercentDamage(4));
		assertEquals(66, physical.calculateHpPercent(4));
		assertEquals(29, physical.calculateMpPercent(4));

		SpellAtkDrainInstantEffect magical = new SpellAtkDrainInstantEffect();
		magical.hpPercentDelta = 2;
		magical.hp_percent = 30;
		magical.mpPercentDelta = 3;
		magical.mp_percent = 10;
		assertEquals(38, magical.calculateHpPercent(4));
		assertEquals(22, magical.calculateMpPercent(4));
	}
}
