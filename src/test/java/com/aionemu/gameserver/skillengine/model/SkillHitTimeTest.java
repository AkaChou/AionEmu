package com.aionemu.gameserver.skillengine.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SkillHitTimeTest {

	@Test
	void calculatesProjectileTravelTimeInMilliseconds() {
		assertEquals(625, Skill.calculateAmmoTime(25, 40));
		assertEquals(0, Skill.calculateAmmoTime(25, 0));
	}
}
