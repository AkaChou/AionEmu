package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

class SignetEffectTest {

	@Test
	void usesRetailTypeLevelAndFamilyBaseSkillId() throws ReflectiveOperationException {
		SignetEffect signet = new SignetEffect();
		setField(signet, "signetType", 6);
		setField(signet, "signetLevel", 2);

		assertEquals(6, signet.getSignetType());
		assertEquals(2, signet.getSignetLevel());
		assertEquals(8456, CarveSignetEffect.nextSignetSkillId(8458, 5, 3));
	}

	@Test
	void usesRetailBurstDamageBySignetTypeAndLevel() {
		assertEquals(20, SignetBurstEffect.getBurstDamagePercent(1, 1));
		assertEquals(30, SignetBurstEffect.getBurstDamagePercent(2, 1));
		assertEquals(75, SignetBurstEffect.getBurstDamagePercent(5, 2));
		assertEquals(100, SignetBurstEffect.getBurstDamagePercent(6, 2));
		assertEquals(400, SignetBurstEffect.scaleBurstDamage(475, 100));
		assertEquals(900, SignetBurstEffect.scaleBurstDamage(1265, 75));
	}

	private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
