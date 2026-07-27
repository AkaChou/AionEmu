package com.aionemu.gameserver.skillengine.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.FutureTask;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.SkillData;
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

	@Test
	void stoppingChargeCancelsTimerAndFinishesCast() throws Exception {
		TestCreature caster = new ObjenesisStd().newInstance(TestCreature.class);
		SkillTemplate template = new SkillTemplate();
		template.skillId = 4300;
		template.activationAttribute = ActivationAttribute.PASSIVE;
		SkillData previousSkillData = DataManager.SKILL_DATA;
		try {
			SkillData skillData = new SkillData();
			skillData.setSkillTemplates(List.of(template));
			DataManager.SKILL_DATA = skillData;

			Skill skill = new Skill(template, caster, 1, caster, null);
			skill.setFirstTargetAttribute(FirstTargetAttribute.ME);
			ChargeSkillTemplate chargeTemplate = new ChargeSkillTemplate();
			setField(chargeTemplate, "charges", List.of());
			setField(skill, "chargeTemplate", chargeTemplate);
			setField(skill, "castStart", System.currentTimeMillis());
			FutureTask<Void> castingTask = new FutureTask<>(() -> null);
			setField(skill, "castingTask", castingTask);
			caster.setCasting(skill);

			skill.stopCharging();

			assertTrue(castingTask.isCancelled());
			assertNull(caster.getCastingSkill());
		} finally {
			DataManager.SKILL_DATA = previousSkillData;
		}
	}

	@Test
	void chargedStageUsesTheSameSpeedMultiplierAsTheClient() throws Exception {
		TestCreature caster = new ObjenesisStd().newInstance(TestCreature.class);
		SkillTemplate first = skillTemplate(4303);
		SkillTemplate second = skillTemplate(4304);
		SkillTemplate third = skillTemplate(4305);
		SkillData previousSkillData = DataManager.SKILL_DATA;
		try {
			SkillData skillData = new SkillData();
			skillData.setSkillTemplates(List.of(first, second, third));
			DataManager.SKILL_DATA = skillData;

			Skill skill = new Skill(first, caster, 2, caster, null);
			skill.setFirstTargetAttribute(FirstTargetAttribute.ME);
			skill.setDuration(690);
			ChargeSkillTemplate chargeTemplate = new ChargeSkillTemplate();
			setField(chargeTemplate, "min_charge", 400);
			setField(chargeTemplate, "type", BonusChargeType.MAGICAL);
			setField(chargeTemplate, "charges", List.of(charge(4303, 1500), charge(4304, 1500), charge(4305, 7000)));
			setField(skill, "chargeTemplate", chargeTemplate);
			float multiplier = invokeChargeTimeMultiplier(skill);
			setField(skill, "chargeTimeMultiplier", multiplier);
			setField(skill, "castStart", System.currentTimeMillis() - 2500);
			caster.setCasting(skill);

			invokeEndCast(skill);

			assertEquals(0.845f, multiplier, 0.001f);
			assertSame(third, skill.getSkillTemplate());
		} finally {
			DataManager.SKILL_DATA = previousSkillData;
		}
	}

	private static SkillTemplate skillTemplate(int skillId) {
		SkillTemplate template = new SkillTemplate();
		template.skillId = skillId;
		template.duration = 1000;
		template.activationAttribute = ActivationAttribute.PASSIVE;
		return template;
	}

	private static ChargeTemplate charge(int skillId, int time) throws Exception {
		ChargeTemplate charge = new ChargeTemplate();
		setField(charge, "skill_id", skillId);
		setField(charge, "time", time);
		return charge;
	}

	private static void invokeEndCast(Skill skill) throws Exception {
		Method endCast = Skill.class.getDeclaredMethod("endCast");
		endCast.setAccessible(true);
		endCast.invoke(skill);
	}

	private static float invokeChargeTimeMultiplier(Skill skill) throws Exception {
		Method method = Skill.class.getDeclaredMethod("calculateChargeTimeMultiplier");
		method.setAccessible(true);
		return (float) method.invoke(skill);
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
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
