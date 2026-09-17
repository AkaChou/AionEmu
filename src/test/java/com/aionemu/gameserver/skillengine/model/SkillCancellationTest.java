package com.aionemu.gameserver.skillengine.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.controllers.CreatureController;
import com.aionemu.gameserver.controllers.effect.EffectController;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.SkillData;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.condition.Condition;
import com.aionemu.gameserver.skillengine.condition.Conditions;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.skillengine.properties.FirstTargetAttribute;

class SkillCancellationTest {

	@Test
	void silenceBlocksMagicalSkillsButNotPhysicalSkills() throws Exception {
		TestCreature caster = new ObjenesisStd().newInstance(TestCreature.class);
		EffectController effectController = new EffectController(caster);
		setField(effectController, "abnormals", AbnormalState.SILENCE.getId());
		caster.setEffectController(effectController);

		SkillTemplate magicalTemplate = new SkillTemplate();
		magicalTemplate.type = SkillType.MAGICAL;
		Skill magicalSkill = new Skill(magicalTemplate, caster, 1, caster, null);
		magicalSkill.setFirstTargetAttribute(FirstTargetAttribute.ME);

		SkillTemplate physicalTemplate = new SkillTemplate();
		physicalTemplate.type = SkillType.PHYSICAL;
		Skill physicalSkill = new Skill(physicalTemplate, caster, 1, caster, null);
		physicalSkill.setFirstTargetAttribute(FirstTargetAttribute.ME);

		assertFalse(magicalSkill.canUseSkill());
		assertTrue(physicalSkill.canUseSkill());
	}

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
	void cancellationWinsAfterEndCastHasEnteredButBeforeCompletionIsClaimed() throws Exception {
		TestCreature caster = new ObjenesisStd().newInstance(TestCreature.class);
		SkillTemplate template = new SkillTemplate();
		template.activationAttribute = ActivationAttribute.PASSIVE;
		CountDownLatch validationEntered = new CountDownLatch(1);
		CountDownLatch continueValidation = new CountDownLatch(1);
		Conditions conditions = new Conditions();
		conditions.getConditions().add(new Condition() {
			@Override
			public boolean validate(Skill skill) {
				validationEntered.countDown();
				try {
					return continueValidation.await(5, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return false;
				}
			}
		});
		setField(template, "useconditions", conditions);
		Skill skill = new Skill(template, caster, 1, caster, null);
		skill.setFirstTargetAttribute(FirstTargetAttribute.ME);
		caster.setCasting(skill);

		try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
			Future<?> completion = executor.submit(() -> {
				try {
					invokeEndCast(skill);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			assertTrue(validationEntered.await(5, TimeUnit.SECONDS));
			assertTrue(skill.tryCancelCast());
			continueValidation.countDown();
			completion.get(5, TimeUnit.SECONDS);
		}

		assertSame(skill, caster.getCastingSkill());
	}

	@Test
	void failedEndCastConditionCancelsCurrentSkill() throws Exception {
		TestCreature caster = new ObjenesisStd().newInstance(TestCreature.class);
		caster.controller = new TestCreatureController();
		caster.controller.setOwner(caster);
		SkillTemplate template = new SkillTemplate();
		template.activationAttribute = ActivationAttribute.PASSIVE;
		Conditions conditions = new Conditions();
		conditions.getConditions().add(new Condition() {
			@Override
			public boolean validate(Skill skill) {
				return false;
			}
		});
		setField(template, "useconditions", conditions);
		Skill skill = new Skill(template, caster, 1, caster, null);
		skill.setFirstTargetAttribute(FirstTargetAttribute.ME);
		caster.setCasting(skill);

		invokeEndCast(skill);

		assertTrue(caster.controller.cancelled);
		assertNull(caster.getCastingSkill());
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

			// 施法时长 690ms / 模板 1000ms ⇒ 系数 0.69，阶段窗口 1035 / 1035 / 4830ms。
			// Cast 690ms over a 1000ms template gives factor 0.69 and stage windows 1035 / 1035 / 4830ms.
			assertEquals(0.69f, chargeTimeMultiplier(first, caster), 0.001f);
			assertSame(first, chargedStage(first, caster, 800));
			assertSame(second, chargedStage(first, caster, 1500));
			// 客户端刚进入第三阶段（2 × 1035ms）就松手，必须结算第三阶段，而不是前两个阶段。
			// Releasing right after the client's stage-three boundary (2 × 1035ms) must resolve stage three.
			assertSame(third, chargedStage(first, caster, 2100));
			assertSame(third, chargedStage(first, caster, 6000));
		} finally {
			DataManager.SKILL_DATA = previousSkillData;
		}
	}

	private static float chargeTimeMultiplier(SkillTemplate first, TestCreature caster) throws Exception {
		return invokeChargeTimeMultiplier(chargedSkill(first, caster));
	}

	private static SkillTemplate chargedStage(SkillTemplate first, TestCreature caster, long chargeMillis)
			throws Exception {
		Skill skill = chargedSkill(first, caster);
		setField(skill, "castStart", System.currentTimeMillis() - chargeMillis);
		invokeEndCast(skill);
		return skill.getSkillTemplate();
	}

	private static Skill chargedSkill(SkillTemplate first, TestCreature caster) throws Exception {
		Skill skill = new Skill(first, caster, 2, caster, null);
		skill.setFirstTargetAttribute(FirstTargetAttribute.ME);
		skill.setDuration(690);
		ChargeSkillTemplate chargeTemplate = new ChargeSkillTemplate();
		setField(chargeTemplate, "min_charge", 400);
		setField(chargeTemplate, "type", BonusChargeType.MAGICAL);
		setField(chargeTemplate, "charges", List.of(charge(4303, 1500), charge(4304, 1500), charge(4305, 7000)));
		setField(skill, "chargeTemplate", chargeTemplate);
		setField(skill, "chargeTimeMultiplier", invokeChargeTimeMultiplier(skill));
		caster.setCasting(skill);
		return skill;
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
		private TestCreatureController controller;

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

		@Override
		public TestCreatureController getController() {
			return controller;
		}
	}

	private static final class TestCreatureController extends CreatureController<TestCreature> {
		private boolean cancelled;

		@Override
		public boolean cancelCurrentSkill(Skill expectedSkill) {
			cancelled = true;
			expectedSkill.tryCancelCast();
			getOwner().clearCasting(expectedSkill);
			return true;
		}
	}
}
