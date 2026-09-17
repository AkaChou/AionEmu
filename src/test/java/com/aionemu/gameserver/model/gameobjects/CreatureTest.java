package com.aionemu.gameserver.model.gameobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.aionemu.gameserver.controllers.CreatureController;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.world.WorldPosition;
import org.junit.jupiter.api.Test;

class CreatureTest {

	@Test
	void usesRetailPlayerStatRatioCurve() {
		assertEquals(1000, Creature.getPlayerStatRatio(65));
		assertEquals(1015, Creature.getPlayerStatRatio(66));
		assertEquals(1150, Creature.getPlayerStatRatio(75));
	}

	@Test
	void cooldownMapsStayLazyAndRemovable() {
		TestCreature creature = new TestCreature();

		assertNull(creature.getSkillCoolDowns());
		creature.setSkillCoolDown(0, 123L);
		assertNull(creature.getSkillCoolDowns());

		creature.setSkillCoolDown(7, 456L);
		creature.setSkillCoolDownBase(7, 123L);
		assertEquals(456L, creature.getSkillCoolDown(7));
		assertEquals(123L, creature.getSkillCoolDownBase(7));

		creature.removeSkillCoolDown(7);
		assertEquals(0L, creature.getSkillCoolDown(7));
		assertEquals(0L, creature.getSkillCoolDownBase(7));
	}

	private static final class TestCreature extends Creature {

		private TestCreature() {
			super(1, new CreatureController<>() {}, null, new TestVisibleObjectTemplate(), new WorldPosition(1));
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

	private static final class TestVisibleObjectTemplate extends VisibleObjectTemplate {

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
