package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.skillengine.model.ActivationAttribute;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

class BuffStunEffectTest {

	@Test
	void succeedsWithoutResistanceCheck() {
		BuffStunEffect template = new BuffStunEffect();
		SkillTemplate skill = new SkillTemplate();
		setActivation(skill);
		Effect effect = new Effect(null, null, skill, 1, 0);

		template.calculate(effect);

		assertEquals(template, effect.getSuccessEffect().iterator().next());
	}

	private static void setActivation(SkillTemplate skill) {
		try {
			Field field = SkillTemplate.class.getDeclaredField("activationAttribute");
			field.setAccessible(true);
			field.set(skill, ActivationAttribute.ACTIVE);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}
}
