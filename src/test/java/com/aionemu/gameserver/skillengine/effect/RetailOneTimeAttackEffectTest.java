package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

class RetailOneTimeAttackEffectTest {

	@Test
	void calculatesRetailAttackAndMovementFormulas() throws ReflectiveOperationException {
		OneTimeBoostSkillAttackEffect boost = new OneTimeBoostSkillAttackEffect();
		setInt(EffectTemplate.class, boost, "delta", 2);
		setInt(EffectTemplate.class, boost, "value", 30);
		setInt(OneTimeBoostSkillAttackEffect.class, boost, "countDelta", 1);
		setInt(OneTimeBoostSkillAttackEffect.class, boost, "count", 5);
		setInt(OneTimeBoostSkillAttackEffect.class, boost, "damageFlatDelta", 3);
		setInt(OneTimeBoostSkillAttackEffect.class, boost, "damageFlatValue", 4);
		setInt(OneTimeBoostSkillAttackEffect.class, boost, "accuracyDelta", 2);
		setInt(OneTimeBoostSkillAttackEffect.class, boost, "accuracyValue", 10);
		setInt(OneTimeBoostSkillAttackEffect.class, boost, "accuracyFlatDelta", 4);
		setInt(OneTimeBoostSkillAttackEffect.class, boost, "accuracyFlatValue", 50);

		assertEquals(38, boost.calculateValue(4));
		assertEquals(9, boost.calculateCount(4));
		assertEquals(16, boost.calculateFlatDamage(4));
		assertEquals(246, boost.calculateAccuracyModifier(1000, 4));

		RandomMoveLocEffect move = new RandomMoveLocEffect();
		setFloat(move, "distanceDelta", 2);
		setFloat(move, "distance", 15);
		assertEquals(23, move.calculateDistance(4));
	}

	private static void setInt(Class<?> owner, Object target, String name, int value)
			throws ReflectiveOperationException {
		Field field = owner.getDeclaredField(name);
		field.setAccessible(true);
		field.setInt(target, value);
	}

	private static void setFloat(Object target, String name, float value) throws ReflectiveOperationException {
		Field field = RandomMoveLocEffect.class.getDeclaredField(name);
		field.setAccessible(true);
		field.setFloat(target, value);
	}
}
