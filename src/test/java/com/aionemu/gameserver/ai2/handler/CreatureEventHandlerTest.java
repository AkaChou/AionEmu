package com.aionemu.gameserver.ai2.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailAiData;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.BoundRadius;
import com.aionemu.gameserver.world.WorldPosition;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class CreatureEventHandlerTest {

	@Test
	void aggroUsesConfiguredSensoryRangeWithoutLegacyMultiplier() {
		TestNpc npc = npcAt(0, 0, 0, 10);
		TestCreature target = creatureAt(10f, 0f, 0f);

		assertTrue(CreatureEventHandler.isInAggroRange(npc, target));

		target.setXYZH(10.01f, 0f, 0f, (byte) 0);
		assertFalse(CreatureEventHandler.isInAggroRange(npc, target));

		target.setXYZH(15f, 0f, 0f, (byte) 0);
		assertFalse(CreatureEventHandler.isInAggroRange(npc, target));
	}

	@Test
	void sensoryAngleUsesShortRangeBehindNpc() {
		TestNpc npc = npcAt(0, 0, 0, 10);
		TestCreature target = creatureAt(9f, 0f, 0f);

		assertTrue(CreatureEventHandler.isInAggroRange(npc, target, 10, 3, 240));

		target.setXYZH(-9f, 0f, 0f, (byte) 0);
		assertFalse(CreatureEventHandler.isInAggroRange(npc, target, 10, 3, 240));

		target.setXYZH(-3f, 0f, 0f, (byte) 0);
		assertTrue(CreatureEventHandler.isInAggroRange(npc, target, 10, 3, 240));

		target.setXYZH(-9f, 0f, 0f, (byte) 0);
		assertTrue(CreatureEventHandler.isInAggroRange(npc, target, 10, 3, 360));
	}

	@Test
	void retailDefinitionNarrowsOnlyLongRangeWhileReturning() {
		RetailAiData previous = DataManager.RETAIL_AI_DATA;
		try {
			RetailAiData.Npc definition = new RetailAiData.Npc(1, "test", "general", 100, 10, 3, 240, 0,
					null, RetailAiData.PathfindFailReaction.RETURN_TO_SP, "walk", 100, 40);
			DataManager.RETAIL_AI_DATA = retailAiData(definition);
			TestNpc npc = npcAt(0, 0, 0, 99);
			NpcAI2 ai = new NpcAI2();
			npc.setAi2(ai);
			TestCreature target = creatureAt(9f, 0f, 0f);

			assertTrue(CreatureEventHandler.isInAggroRange(npc, target));

			target.setXYZH(-9f, 0f, 0f, (byte) 0);
			assertFalse(CreatureEventHandler.isInAggroRange(npc, target));

			ai.setStateIfNot(AIState.RETURNING);
			target.setXYZH(5f, 0f, 0f, (byte) 0);
			assertFalse(CreatureEventHandler.isInAggroRange(npc, target));

			target.setXYZH(-3f, 0f, 0f, (byte) 0);
			assertTrue(CreatureEventHandler.isInAggroRange(npc, target));
		} finally {
			DataManager.RETAIL_AI_DATA = previous;
		}
	}

	@Test
	void sensoryRangeIncludesHalfLargestHorizontalBoundAndCapsAtOneHundred() {
		float boundOffset = CreatureEventHandler.sensoryBoundOffset(new BoundRadius(2, 4, 3), 100);

		assertEquals(2, boundOffset);
		assertEquals(0.2f, CreatureEventHandler.sensoryBoundOffset(new BoundRadius(2, 4, 3), 10));
		assertEquals(12, CreatureEventHandler.effectiveSensoryRange(10, boundOffset));
		assertEquals(100, CreatureEventHandler.effectiveSensoryRange(99, boundOffset));
		assertEquals(0, CreatureEventHandler.effectiveSensoryRange(0, boundOffset));
	}

	@Test
	void aggroRequiresSameWorldInstance() {
		TestNpc npc = npcAt(0, 0, 0, 10);
		TestCreature target = creatureAt(1f, 0f, 0f);
		target.setPosition(position(2, 1, 0f, 0f, 0f));

		assertFalse(CreatureEventHandler.isInAggroRange(npc, target));

		target.setPosition(position(1, 2, 0f, 0f, 0f));
		assertFalse(CreatureEventHandler.isInAggroRange(npc, target));
	}

	private static TestNpc npcAt(float x, float y, float z, int aggroRange) {
		TestNpc npc = new ObjenesisStd().newInstance(TestNpc.class);
		npc.aggroRange = aggroRange;
		npc.setPosition(position(1, 1, x, y, z));
		return npc;
	}

	private static TestCreature creatureAt(float x, float y, float z) {
		TestCreature creature = new ObjenesisStd().newInstance(TestCreature.class);
		creature.setPosition(position(1, 1, x, y, z));
		return creature;
	}

	private static TestWorldPosition position(int worldId, int instanceId, float x, float y, float z) {
		TestWorldPosition position = new TestWorldPosition(worldId, instanceId);
		position.setXYZH(x, y, z, (byte) 0);
		return position;
	}

	private static RetailAiData retailAiData(RetailAiData.Npc npc) {
		return new RetailAiData(Map.of(), Map.of(npc.id(), npc), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
				Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
	}

	private static final class TestNpc extends Npc {

		private int aggroRange;

		@SuppressWarnings("unused")
		private TestNpc() {
			super(1, null, null, null);
		}

		@Override
		public int getAggroRange() {
			return aggroRange;
		}

		@Override
		public int getNpcId() {
			return 1;
		}
	}

	private static final class TestCreature extends Creature {

		@SuppressWarnings("unused")
		private TestCreature() {
			super(1, null, null, null, position(1, 1, 0, 0, 0));
		}

		@Override
		public String getName() {
			return "test-creature";
		}

		@Override
		public byte getLevel() {
			return 1;
		}
	}

	private static final class TestWorldPosition extends WorldPosition {

		private final int instanceId;

		private TestWorldPosition(int worldId, int instanceId) {
			super(worldId);
			this.instanceId = instanceId;
		}

		@Override
		public int getInstanceId() {
			return instanceId;
		}
	}
}
