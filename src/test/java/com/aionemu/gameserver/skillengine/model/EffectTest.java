package com.aionemu.gameserver.skillengine.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

import com.aionemu.gameserver.skillengine.effect.EffectTemplate;
import com.aionemu.gameserver.skillengine.effect.Effects;
import com.aionemu.gameserver.skillengine.effect.FlyoffEffect;
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
	void ignoresDuplicateRegistrationOfSameEffect() {
		Effect effect = new Effect(null, null, skillTemplate(), 1, 0);
		EffectTemplate template = effectTemplate(1);

		effect.addSucessEffect(template);
		effect.addSucessEffect(template);

		assertEquals(1, effect.getSuccessEffect().size());
	}

	@Test
	void keepsReservedDamageForEachEffectTemplate() {
		TestReservedEffect first = new TestReservedEffect(100);
		TestReservedEffect second = new TestReservedEffect(25);
		Effect effect = new Effect(null, null, skillTemplate(first, second), 1, 0);

		effect.initialize();
		effect.applyEffect();

		assertEquals(100, first.appliedValue);
		assertEquals(25, second.appliedValue);
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

	@Test
	void flyoffUsesRetailSkillAddEffectType() {
		Effect effect = new Effect(null, null, skillTemplate(), 1, 0);
		effect.setIsForcedEffect(true);

		new FlyoffEffect().calculate(effect);

		assertEquals(SkillMoveType.FLYOFF, effect.getSkillMoveType());
	}

	private static SkillTemplate skillTemplate() {
		SkillTemplate skillTemplate = new SkillTemplate();
		setField(skillTemplate, SkillTemplate.class, "activationAttribute", ActivationAttribute.ACTIVE);
		return skillTemplate;
	}

	private static SkillTemplate skillTemplate(EffectTemplate... templates) {
		SkillTemplate skillTemplate = skillTemplate();
		Effects effects = new Effects();
		effects.getEffects().addAll(Arrays.asList(templates));
		setField(skillTemplate, SkillTemplate.class, "effects", effects);
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

	private static final class TestReservedEffect extends EffectTemplate {

		private final int calculatedValue;
		private int appliedValue;

		private TestReservedEffect(int calculatedValue) {
			this.calculatedValue = calculatedValue;
		}

		@Override
		public void calculate(Effect effect) {
			effect.addSucessEffect(this);
			effect.setReserved1(calculatedValue);
		}

		@Override
		public void applyEffect(Effect effect) {
			appliedValue = effect.getReserved1();
		}
	}
}
