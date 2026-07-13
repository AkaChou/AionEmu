package com.aionemu.gameserver.skillengine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.container.CreatureLifeStats;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.skillengine.model.ActivationAttribute;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

class SwitchHpMpEffectTest {

	@Test
	void exchangesRetailPercentagesAndKeepsOldTemplatesWorking() {
		TestCreature creature = new ObjenesisStd().newInstance(TestCreature.class);
		TestLifeStats lifeStats = new TestLifeStats(creature, 800, 200, 1000, 1000);
		creature.setLifeStats(lifeStats);
		SwitchHpMpEffect template = new SwitchHpMpEffect();
		setField(EffectTemplate.class, template, "delta", 10);
		setField(EffectTemplate.class, template, "value", 30);
		setField(SwitchHpMpEffect.class, template, "mpDelta", 5);
		setField(SwitchHpMpEffect.class, template, "mpValue", 15);

		template.applyEffect(effect(creature, 2));

		assertEquals(450, lifeStats.getCurrentHp());
		assertEquals(550, lifeStats.getCurrentMp());

		lifeStats.current(800, 200);
		new SwitchHpMpEffect().applyEffect(effect(creature, 1));
		assertEquals(200, lifeStats.getCurrentHp());
		assertEquals(800, lifeStats.getCurrentMp());
	}

	private static Effect effect(Creature creature, int level) {
		SkillTemplate skill = new SkillTemplate();
		setField(SkillTemplate.class, skill, "activationAttribute", ActivationAttribute.ACTIVE);
		return new Effect(creature, creature, skill, level, 0);
	}

	private static void setField(Class<?> owner, Object target, String name, Object value) {
		try {
			Field field = owner.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static final class TestLifeStats extends CreatureLifeStats<TestCreature> {
		private final int maxHp;
		private final int maxMp;

		private TestLifeStats(TestCreature owner, int hp, int mp, int maxHp, int maxMp) {
			super(owner, hp, mp);
			this.maxHp = maxHp;
			this.maxMp = maxMp;
		}

		private void current(int hp, int mp) {
			currentHp = hp;
			currentMp = mp;
		}

		@Override
		public int getMaxHp() {
			return maxHp;
		}

		@Override
		public int getMaxMp() {
			return maxMp;
		}

		@Override
		public int increaseHp(TYPE type, int value) {
			return currentHp += value;
		}

		@Override
		public int increaseMp(TYPE type, int value) {
			return currentMp += value;
		}

		@Override
		protected void onIncreaseMp(TYPE type, int value, int skillId, LOG log) {
		}

		@Override
		protected void onReduceMp() {
		}

		@Override
		protected void onIncreaseHp(TYPE type, int value, int skillId, LOG log) {
		}

		@Override
		protected void onReduceHp() {
		}
	}

	private static final class TestCreature extends Creature {
		private TestCreature() {
			super(1, null, null, null, null);
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
}
