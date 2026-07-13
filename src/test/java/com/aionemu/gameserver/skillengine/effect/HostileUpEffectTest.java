package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HostileUpEffectTest {

	@Test
	void retailTotemHateKeepsNinetyNinePercentOnTheTotem() {
		assertEquals(24750, HostileUpEffect.totemHate(25000));
		assertEquals(74250, HostileUpEffect.totemHate(75000));
	}
}
