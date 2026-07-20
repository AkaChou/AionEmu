package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.controllers.CreatureController;
import com.aionemu.gameserver.controllers.effect.EffectController;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.skillengine.model.ActivationAttribute;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillSubType;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

class AlwaysAttackEffectTest {

	@Test
	void exposesAndConsumesPhysicalAndMagicalBypassCharges() throws ReflectiveOperationException {
		TestCreature creature = new TestCreature();
		SkillTemplate skill = new SkillTemplate();
		setField(skill, "activationAttribute", ActivationAttribute.ACTIVE);
		setField(skill, "subType", SkillSubType.NONE);

		AlwaysHitEffect alwaysHit = new AlwaysHitEffect();
		setField(alwaysHit, "value", 2);
		setField(alwaysHit, "position", 1);
		Effect hitEffect = new Effect(creature, creature, skill, 1, 0);
		hitEffect.addSucessEffect(alwaysHit);
		hitEffect.startEffect(false);
		assertTrue(creature.getObserveController().hasAlwaysHit());
		assertTrue(creature.getObserveController().consumeAlwaysHit());
		assertTrue(creature.getObserveController().hasAlwaysHit());

		AlwaysNoResistEffect noResist = new AlwaysNoResistEffect();
		setField(noResist, "value", 1);
		setField(noResist, "position", 2);
		Effect noResistEffect = new Effect(creature, creature, skill, 1, 0);
		noResistEffect.addSucessEffect(noResist);
		noResistEffect.startEffect(false);
		assertTrue(creature.getObserveController().hasAlwaysNoResist());
		assertTrue(creature.getObserveController().consumeAlwaysNoResist());
		assertFalse(creature.getObserveController().hasAlwaysNoResist());
	}

	private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
		Class<?> type = target instanceof EffectTemplate ? EffectTemplate.class : target.getClass();
		Field field = type.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static final class TestCreature extends Creature {
		private TestCreature() {
			super(1, new CreatureController<>() {}, null, new TestTemplate(), null);
			setEffectController(new EffectController(this));
		}

		@Override
		public String getName() {
			return "test";
		}

		@Override
		public byte getLevel() {
			return 1;
		}
	}

	private static final class TestTemplate extends VisibleObjectTemplate {
		@Override
		public int getTemplateId() {
			return 1;
		}

		@Override
		public String getName() {
			return "test";
		}

		@Override
		public int getNameId() {
			return 1;
		}
	}
}
