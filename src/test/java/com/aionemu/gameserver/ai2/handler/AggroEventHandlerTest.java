package com.aionemu.gameserver.ai2.handler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.BiPredicate;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.world.WorldPosition;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class AggroEventHandlerTest {

	@Test
	void supportRequiresSameFloorRangeAndLineOfSight() {
		Npc supporter = npcAt(0, 0, 0);
		TestCreature requester = creatureAt(0, 0, 8, true);
		TestCreature target = creatureAt(1, 0, 0, false);
		BiPredicate<VisibleObject, VisibleObject> canSee = (object, other) -> true;

		assertFalse(AggroEventHandler.canReceiveSupport(supporter, requester, target, 5, canSee));

		requester.setXYZH(3f, 0f, 0f, (byte) 0);
		assertTrue(AggroEventHandler.canReceiveSupport(supporter, requester, target, 5, canSee));

		assertFalse(AggroEventHandler.canReceiveSupport(supporter, requester, target, 5, (object, other) -> false));
		assertFalse(AggroEventHandler.canReceiveSupport(supporter, requester, target, 5,
				(object, other) -> other != target));
	}

	private static Npc npcAt(float x, float y, float z) {
		Npc npc = new ObjenesisStd().newInstance(Npc.class);
		npc.setPosition(position(x, y, z));
		return npc;
	}

	private static TestCreature creatureAt(float x, float y, float z, boolean supportFromNpc) {
		TestCreature creature = new ObjenesisStd().newInstance(TestCreature.class);
		creature.setPosition(position(x, y, z));
		creature.supportFromNpc = supportFromNpc;
		return creature;
	}

	private static TestWorldPosition position(float x, float y, float z) {
		TestWorldPosition position = new TestWorldPosition();
		position.setXYZH(x, y, z, (byte) 0);
		return position;
	}

	private static final class TestCreature extends Creature {

		private boolean supportFromNpc;

		@SuppressWarnings("unused")
		private TestCreature() {
			super(1, null, null, null, position(0, 0, 0));
		}

		@Override
		public String getName() {
			return "test-creature";
		}

		@Override
		public boolean isSupportFrom(Npc npc) {
			return supportFromNpc;
		}

		@Override
		public byte getLevel() {
			return 1;
		}
	}

	private static final class TestWorldPosition extends WorldPosition {

		private TestWorldPosition() {
			super(1);
		}

		@Override
		public int getInstanceId() {
			return 1;
		}
	}
}
