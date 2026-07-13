package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.skillengine.model.HitType;
import org.junit.jupiter.api.Test;

class ProvokerEffectTest {

	@Test
	void retailTriggerLevelUsesReserved15And16() {
		ProvokerEffect effect = new ProvokerEffect();
		effect.delta = 1;
		effect.value = 10;

		assertEquals(15, effect.getTriggeredSkillLevel(5));
	}

	@Test
	void retailHitTypesSelectTheCorrectAttackSide() {
		ProvokerEffect effect = new ProvokerEffect();
		effect.hitType = HitType.BACKATK;
		assertEquals(ObserverType.ATTACK, effect.getTriggerObserverType());

		effect.hitType = HitType.PHHIT;
		assertEquals(ObserverType.ATTACKED, effect.getTriggerObserverType());
		assertTrue(effect.acceptsAttackedType(false));
		assertFalse(effect.acceptsAttackedType(true));

		effect.hitType = HitType.MAHIT;
		assertTrue(effect.acceptsAttackedType(true));
		assertFalse(effect.acceptsAttackedType(false));
	}
}
