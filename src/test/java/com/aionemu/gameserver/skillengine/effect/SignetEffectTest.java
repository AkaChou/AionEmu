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

	private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
