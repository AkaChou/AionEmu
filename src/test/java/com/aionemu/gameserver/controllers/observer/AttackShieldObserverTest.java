package com.aionemu.gameserver.controllers.observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AttackShieldObserverTest {

	@Test
	void usesPerMilleProbabilityAndReflectsAtLeastTheConfiguredDamage() {
		assertTrue(AttackShieldObserver.isTriggered(32, 31));
		assertFalse(AttackShieldObserver.isTriggered(32, 32));
		assertTrue(AttackShieldObserver.isTriggered(1000, 999));
		assertEquals(35, AttackShieldObserver.calculateReflectedDamage(100, 35, 16));
		assertEquals(16, AttackShieldObserver.calculateReflectedDamage(10, 35, 16));
	}
}
