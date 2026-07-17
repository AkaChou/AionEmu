package com.aionemu.gameserver.skillengine.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.world.WorldPosition;

class MaxCountPropertyTest {

	@Test
	void equalDistanceTargetsDoNotOverwriteEachOther() {
		TestCreature effector = creatureAt(0, 0);
		TestCreature first = creatureAt(1, 0);
		TestCreature second = creatureAt(-1, 0);
		TestCreature third = creatureAt(0, 1);
		Skill skill = new Skill(new SkillTemplate(), effector, 1, effector, null);
		skill.getEffectedList().clear();
		skill.getEffectedList().addAll(List.of(first, second, third));
		Properties properties = new Properties();
		properties.targetType = TargetRangeAttribute.AREA;
		properties.targetMaxCount = 3;

		assertTrue(MaxCountProperty.set(skill, properties));
		assertEquals(List.of(first, second, third), skill.getEffectedList());
	}

	private static TestCreature creatureAt(float x, float y) {
		TestCreature creature = new ObjenesisStd().newInstance(TestCreature.class);
		WorldPosition position = new WorldPosition(1);
		position.setXYZH(x, y, 0f, (byte) 0);
		creature.setPosition(position);
		return creature;
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
