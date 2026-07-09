package com.aionemu.gameserver.skillengine.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

import com.aionemu.gameserver.skillengine.effect.EffectTemplate;
import com.aionemu.gameserver.skillengine.effect.HealOverTimeEffect;
import org.junit.jupiter.api.Test;

class EffectTest {

	@Test
	void keepsSuccessfulEffectsWithSamePosition() {
		Effect effect = new Effect(null, null, skillTemplate(), 1, 0);
		EffectTemplate first = effectTemplate(1);
		EffectTemplate second = effectTemplate(1);

		effect.addSucessEffect(first);
		effect.addSucessEffect(second);

		assertEquals(Arrays.asList(first, second), new ArrayList<>(effect.getSuccessEffect()));
		assertTrue(effect.isInSuccessEffects(1));
	}

	@Test
	void healOverTimeRegistersAsSuccessfulOnce() {
		Effect effect = new Effect(null, null, skillTemplate(), 1, 0);
		effect.setIsForcedEffect(true);
		TestHealOverTimeEffect heal = new TestHealOverTimeEffect();
		setField(heal, EffectTemplate.class, "position", 1);
		setField(heal, EffectTemplate.class, "value", 10);

		heal.calculate(effect, HealType.MP);

		assertEquals(1, effect.getSuccessEffect().size());
	}

	private static SkillTemplate skillTemplate() {
		SkillTemplate skillTemplate = new SkillTemplate();
		setField(skillTemplate, SkillTemplate.class, "activationAttribute", ActivationAttribute.ACTIVE);
		return skillTemplate;
	}

	private static EffectTemplate effectTemplate(int position) {
		EffectTemplate effectTemplate = new TestEffectTemplate();
		setField(effectTemplate, EffectTemplate.class, "position", position);
		return effectTemplate;
	}

	private static void setField(Object target, Class<?> owner, String name, Object value) {
		try {
			Field field = owner.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static final class TestEffectTemplate extends EffectTemplate {

		@Override
		public void applyEffect(Effect effect) {
		}
	}

	private static final class TestHealOverTimeEffect extends HealOverTimeEffect {

		@Override
		protected int getCurrentStatValue(Effect effect) {
			return 0;
		}

		@Override
		protected int getMaxStatValue(Effect effect) {
			return 100;
		}
	}
}
