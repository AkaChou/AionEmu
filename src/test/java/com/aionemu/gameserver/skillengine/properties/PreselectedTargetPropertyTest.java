package com.aionemu.gameserver.skillengine.properties;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreselectedTargetPropertyTest {

	@Test
	void skipsSkillGeometryAndMaxCount() {
		ObjenesisStd objenesis = new ObjenesisStd();
		TestCreature effector = objenesis.newInstance(TestCreature.class);
		TestCreature first = objenesis.newInstance(TestCreature.class);
		TestCreature second = objenesis.newInstance(TestCreature.class);
		Skill skill = new Skill(new SkillTemplate(), effector, 1, effector, null);
		skill.getEffectedList().addAll(List.of(effector, first, second));
		Properties properties = new Properties();
		properties.firstTarget = FirstTargetAttribute.TARGET;
		properties.targetType = TargetRangeAttribute.AREA;
		properties.targetDistance = 5;
		properties.targetMaxCount = 1;
		properties.otherTargetOnly = true;

		assertTrue(properties.validatePreselectedTargets(skill));
		assertEquals(List.of(first, second), skill.getEffectedList());
	}

	private static final class TestCreature extends Creature {

		@SuppressWarnings("unused")
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
