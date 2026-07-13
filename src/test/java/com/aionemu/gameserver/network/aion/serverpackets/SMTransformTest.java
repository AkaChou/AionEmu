package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.controllers.CreatureController;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.TransformModel;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.skillengine.model.TransformType;

class SMTransformTest {

	@Test
	void writesRetailRestrictionOrderItemFlagAndAnimationSkill() {
		TestCreature creature = new TestCreature(77);
		TransformModel transform = creature.getTransformModel();
		transform.setModelId(202641);
		transform.setTransformType(TransformType.FORM2);
		transform.setActive(true);
		transform.addRestrictions(true, true, true, true, true, true, true);
		transform.setActiveTransform(null, 200, true, 4875);

		SM_TRANSFORM packet = new SM_TRANSFORM(creature, 76, true, 102301000);
		ByteBuffer buffer = ByteBuffer.allocate(40);
		packet.setBuf(buffer);
		packet.writeImpl(null);
		buffer.flip();

		assertEquals(40, buffer.remaining());
		assertEquals(77, buffer.getInt());
		assertEquals(202641, buffer.getInt());
		assertEquals(creature.getState(), Short.toUnsignedInt(buffer.getShort()));
		assertEquals(0.25f, buffer.getFloat());
		assertEquals(2.0f, buffer.getFloat());
		assertEquals(1, buffer.get());
		assertEquals(TransformType.FORM2.getId(), buffer.getInt());
		for (int i = 0; i < 6; i++) {
			assertEquals(1, buffer.get());
		}
		assertEquals(76, buffer.getInt());
		assertEquals(102301000, buffer.getInt());
		assertEquals(1, buffer.get());
		assertEquals(4875, Short.toUnsignedInt(buffer.getShort()));

		transform.addRestrictions(true, false, false, false, false, false, false);
		transform.removeRestrictions(true, true, true, true, true, true, true);
		assertTrue(transform.isFlyDisabled());
		assertFalse(transform.isSkillDisabled());
		transform.removeRestrictions(true, false, false, false, false, false, false);
		assertFalse(transform.isFlyDisabled());
	}

	private static final class TestCreature extends Creature {
		private TestCreature(int objectId) {
			super(objectId, new TestCreatureController(), null, new TestVisibleObjectTemplate(), null);
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

	private static final class TestCreatureController extends CreatureController<Creature> {
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
			return 0;
		}
	}
}
