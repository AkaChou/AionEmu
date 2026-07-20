package com.aionemu.gameserver.controllers.observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.skillengine.model.HitType;
import org.junit.jupiter.api.Test;

class AttackShieldObserverTest {

	@Test
	void usesPerMilleProbabilityAndAddsConfiguredReflectDamage() {
		assertTrue(AttackShieldObserver.isTriggered(32, 31));
		assertFalse(AttackShieldObserver.isTriggered(32, 32));
		assertTrue(AttackShieldObserver.isTriggered(1000, 999));
		assertEquals(51, AttackShieldObserver.calculateReflectedDamage(100, 35, 16));
		assertEquals(19, AttackShieldObserver.calculateReflectedDamage(10, 35, 16));
	}

	@Test
	void protectUsesTransferredDamageAndShieldsUseRetailOrder() {
		assertEquals(500, AttackShieldObserver.calculateProtectedDamage(1000, 50, true));
		assertEquals(250, AttackShieldObserver.calculateProtectorDamage(500, 50));

		assertEquals(0, observer(2, 0).getShieldPriority());
		assertEquals(1, observer(2, 50).getShieldPriority());
		assertEquals(2, observer(1, 0).getShieldPriority());
		assertEquals(3, observer(8, 0).getShieldPriority());
		assertEquals(4, observer(0, 0).getShieldPriority());
	}

	@Test
	void capsMpShieldByMpAndConvertsOnlyThePaidShare() {
		assertEquals(200, AttackShieldObserver.calculateShieldUse(1000, 1000, false, 2000, 200));
		assertEquals(100, AttackShieldObserver.calculatePercent(200, 50));
	}

	@Test
	void convertHealUsesActualAbsorptionAndLeavesOnePointOutOfConversion() {
		assertEquals(500, AttackShieldObserver.calculatePercent(1000, 50));
		assertEquals(999, AttackShieldObserver.calculateConversionBase(1000, 1000));
	}

	private static AttackShieldObserver observer(int shieldType, int mpValue) {
		return new AttackShieldObserver(0, 0, false, null, HitType.EVERYHIT, shieldType, 100, mpValue);
	}
}
