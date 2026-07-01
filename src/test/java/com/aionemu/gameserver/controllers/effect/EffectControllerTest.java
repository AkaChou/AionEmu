package com.aionemu.gameserver.controllers.effect;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.effect.EffectTemplate;
import com.aionemu.gameserver.skillengine.effect.Effects;
import com.aionemu.gameserver.skillengine.model.ActivationAttribute;
import com.aionemu.gameserver.skillengine.model.DispelCategoryType;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillSubType;
import com.aionemu.gameserver.skillengine.model.SkillTargetSlot;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import org.junit.jupiter.api.Test;

class EffectControllerTest {

	@Test
	void replacingPassiveEffectByEffectIdToleratesEndEffectRemovingFromController() {
		TestEffectController controller = new TestEffectController();
		TestEffect oldEffect = passiveEffect(controller, "old", 1, 1);
		TestEffect unrelatedEffect = passiveEffect(controller, "unrelated", 2, 1);
		TestEffect replacementEffect = passiveEffect(controller, "replacement", 1, 1);

		controller.addEffect(oldEffect);
		controller.addEffect(unrelatedEffect);

		assertDoesNotThrow(() -> controller.addEffect(replacementEffect));
		assertNull(controller.passiveEffect("old"));
		assertSame(unrelatedEffect, controller.passiveEffect("unrelated"));
		assertSame(replacementEffect, controller.passiveEffect("replacement"));
	}

	@Test
	void replacingAbnormalEffectByEffectIdToleratesEndEffectRemovingFromController() {
		TestEffectController controller = new TestEffectController();
		TestEffect oldEffect = abnormalEffect(controller, "old", 10, 1, 1);
		TestEffect unrelatedEffect = abnormalEffect(controller, "unrelated", 11, 2, 1);
		TestEffect replacementEffect = abnormalEffect(controller, "replacement", 12, 1, 1);

		controller.addEffect(oldEffect);
		controller.addEffect(unrelatedEffect);

		assertDoesNotThrow(() -> controller.addEffect(replacementEffect));
		assertNull(controller.abnormalEffect("old"));
		assertSame(unrelatedEffect, controller.abnormalEffect("unrelated"));
		assertSame(replacementEffect, controller.abnormalEffect("replacement"));
	}

	@Test
	void removeEffectBySkillIdToleratesEndEffectRemovingFromController() {
		TestEffectController controller = new TestEffectController();
		TestEffect matchingEffect = abnormalEffect(controller, "matching", 10, 1, 1);
		TestEffect unrelatedEffect = abnormalEffect(controller, "unrelated", 11, 2, 1);

		controller.addEffect(matchingEffect);
		controller.addEffect(unrelatedEffect);

		assertDoesNotThrow(() -> controller.removeEffect(10));
		assertNull(controller.abnormalEffect("matching"));
		assertSame(unrelatedEffect, controller.abnormalEffect("unrelated"));
	}

	@Test
	void removePassiveEffectBySkillIdToleratesEndEffectRemovingFromController() {
		TestEffectController controller = new TestEffectController();
		TestEffect matchingEffect = passiveEffect(controller, "matching", 10, 1, 1);
		TestEffect unrelatedEffect = passiveEffect(controller, "unrelated", 11, 2, 1);

		controller.addEffect(matchingEffect);
		controller.addEffect(unrelatedEffect);

		assertDoesNotThrow(() -> controller.removePassiveEffect(10));
		assertNull(controller.passiveEffect("matching"));
		assertSame(unrelatedEffect, controller.passiveEffect("unrelated"));
	}

	@Test
	void removeNoshowEffectBySkillIdToleratesEndEffectRemovingFromController() {
		TestEffectController controller = new TestEffectController();
		TestEffect matchingEffect = noshowEffect(controller, "matching", 10, 1, 1);
		TestEffect unrelatedEffect = noshowEffect(controller, "unrelated", 11, 2, 1);

		controller.addEffect(matchingEffect);
		controller.addEffect(unrelatedEffect);

		assertDoesNotThrow(() -> controller.removeNoshowEffect(10));
		assertNull(controller.noshowEffect("matching"));
		assertSame(unrelatedEffect, controller.noshowEffect("unrelated"));
	}

	@Test
	void removeEffectByDispelCategoryToleratesEndEffectRemovingFromController() {
		TestEffectController controller = new TestEffectController();
		TestEffect matchingEffect = abnormalEffect(controller, "matching", 10, 1, 1);
		TestEffect unrelatedEffect = abnormalEffect(controller, "unrelated", 11, 2, 1);

		controller.addEffect(matchingEffect);
		controller.addEffect(unrelatedEffect);

		assertDoesNotThrow(() -> controller.removeEffectByDispelCat(DispelCategoryType.BUFF, SkillTargetSlot.BUFF, 1, 0, 1, false));
		assertNull(controller.abnormalEffect("matching"));
		assertSame(unrelatedEffect, controller.abnormalEffect("unrelated"));
	}

	@Test
	void removeAllEffectsToleratesEndEffectRemovingFromController() {
		TestEffectController controller = new TestEffectController();
		TestEffect firstEffect = abnormalEffect(controller, "first", 10, 1, 1);
		TestEffect secondEffect = abnormalEffect(controller, "second", 11, 2, 1);

		controller.addEffect(firstEffect);
		controller.addEffect(secondEffect);

		assertDoesNotThrow(() -> controller.removeAllEffects());
		assertNull(controller.abnormalEffect("first"));
		assertNull(controller.abnormalEffect("second"));
	}

	private static TestEffect abnormalEffect(TestEffectController controller, String stack, int skillId, int effectId,
			int basicLevel) {
		return effect(controller, stack, skillId, effectId, basicLevel, ActivationAttribute.ACTIVE, SkillTargetSlot.BUFF);
	}

	private static TestEffect passiveEffect(TestEffectController controller, String stack, int effectId, int basicLevel) {
		return passiveEffect(controller, stack, effectId, effectId, basicLevel);
	}

	private static TestEffect passiveEffect(TestEffectController controller, String stack, int skillId, int effectId,
			int basicLevel) {
		return effect(controller, stack, skillId, effectId, basicLevel, ActivationAttribute.PASSIVE, SkillTargetSlot.BUFF);
	}

	private static TestEffect noshowEffect(TestEffectController controller, String stack, int skillId, int effectId,
			int basicLevel) {
		return effect(controller, stack, skillId, effectId, basicLevel, ActivationAttribute.TOGGLE, SkillTargetSlot.NOSHOW);
	}

	private static TestEffect effect(TestEffectController controller, String stack, int skillId, int effectId,
			int basicLevel, ActivationAttribute activationAttribute, SkillTargetSlot targetSlot) {
		SkillTemplate skillTemplate = new SkillTemplate();
		setField(skillTemplate, "skillId", skillId);
		setField(skillTemplate, "stack", stack);
		setField(skillTemplate, "activationAttribute", activationAttribute);
		setField(skillTemplate, "subType", SkillSubType.NONE);
		setField(skillTemplate, "targetSlot", targetSlot);
		setField(skillTemplate, "dispelCategory", DispelCategoryType.BUFF);
		setField(skillTemplate, "effects", effects(effectId, basicLevel));
		return new TestEffect(controller, skillTemplate);
	}

	private static Effects effects(int effectId, int basicLevel) {
		Effects effects = new Effects();
		effects.getEffects().add(new TestEffectTemplate(effectId, basicLevel));
		return effects;
	}

	private static void setField(Object target, String name, Object value) {
		try {
			Field field = target.getClass().getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static final class TestEffectController extends EffectController {

		private TestEffectController() {
			super((Creature) null);
		}

		private Effect passiveEffect(String stack) {
			return passiveEffectMap.get(stack);
		}

		private Effect noshowEffect(String stack) {
			return noshowEffects.get(stack);
		}

		private Effect abnormalEffect(String stack) {
			return abnormalEffectMap.get(stack);
		}

		@Override
		public void broadCastEffects() {
		}
	}

	private static final class TestEffect extends Effect {

		private final TestEffectController controller;

		private TestEffect(TestEffectController controller, SkillTemplate skillTemplate) {
			super(null, null, skillTemplate, 1, 0);
			this.controller = controller;
		}

		@Override
		public synchronized void endEffect() {
			controller.clearEffect(this);
		}

		@Override
		public void startEffect(boolean restored) {
		}

		@Override
		public int removePower(int power) {
			return 0;
		}
	}

	private static final class TestEffectTemplate extends EffectTemplate {

		private TestEffectTemplate(int effectId, int basicLevel) {
			this.effectid = effectId;
			this.basicLvl = basicLevel;
		}

		@Override
		public void applyEffect(Effect effect) {
		}
	}
}
