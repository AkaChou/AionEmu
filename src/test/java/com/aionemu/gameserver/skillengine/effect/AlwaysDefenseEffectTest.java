package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

class AlwaysDefenseEffectTest {

	@Test
	void calculatesRetailChargeCountAndExposesConsumeFlag() throws ReflectiveOperationException {
		AlwaysBlockEffect effect = new AlwaysBlockEffect();
		setField(effect, "delta", 2);
		setField(effect, "value", 3);
		setField(effect, "consume", false);

		assertEquals(11, effect.calculateValue(4));
		assertEquals(false, effect.isConsume());
	}

	private static void setField(EffectTemplate target, String name, Object value) throws ReflectiveOperationException {
		Field field = EffectTemplate.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}
}
