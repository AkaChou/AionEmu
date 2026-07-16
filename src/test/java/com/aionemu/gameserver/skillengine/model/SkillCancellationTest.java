package com.aionemu.gameserver.skillengine.model;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.properties.FirstTargetAttribute;

class SkillCancellationTest {

	@Test
	void cancelledCastCannotFinishWhileTheControllerIsClearingIt() throws Exception {
		TestCreature caster = new ObjenesisStd().newInstance(TestCreature.class);
		SkillTemplate template = new SkillTemplate();
		template.activationAttribute = ActivationAttribute.PASSIVE;
		Skill skill = new Skill(template, caster, 1, caster, null);
		skill.setFirstTargetAttribute(FirstTargetAttribute.ME);
		caster.setCasting(skill);

		skill.cancelCast();
		invokeEndCast(skill);

		assertSame(skill, caster.getCastingSkill());
	}

	private static void invokeEndCast(Skill skill) throws Exception {
		Method endCast = Skill.class.getDeclaredMethod("endCast");
		endCast.setAccessible(true);
		endCast.invoke(skill);
	}

	private static final class TestCreature extends Creature {

		private TestCreature() {
			super(1, null, null, null, null);
		}

		@Override
		public String getName() {
			return "caster";
		}

		@Override
		public byte getLevel() {
			return 1;
		}
	}
}
