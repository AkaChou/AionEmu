package com.aionemu.gameserver.taskmanager.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.base.Predicate;

import com.aionemu.gameserver.ai2.AI2;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AISubState;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.ai2.poll.AIAnswer;
import com.aionemu.gameserver.ai2.poll.AIQuestion;
import com.aionemu.gameserver.controllers.CreatureController;
import com.aionemu.gameserver.controllers.movement.MoveController;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.model.templates.item.ItemAttackType;

class MoveTaskManagerTest {

	@Test
	void runMovesRegisteredCreaturesOnce() {
		MoveTaskManager manager = new MoveTaskManager();
		TestCreature first = new TestCreature(1);
		TestCreature second = new TestCreature(2);

		manager.addCreature(first);
		manager.addCreature(second);
		manager.run();

		assertEquals(1, first.moveController.moveCalls);
		assertEquals(1, second.moveController.moveCalls);
		assertEquals(1, first.ai.validateEvents);
		assertEquals(1, second.ai.validateEvents);
	}

	@Test
	void removedCreatureIsNotMoved() {
		MoveTaskManager manager = new MoveTaskManager();
		TestCreature creature = new TestCreature(1);

		manager.addCreature(creature);
		manager.removeCreature(creature);
		manager.run();

		assertEquals(0, creature.moveController.moveCalls);
	}

	@Test
	void completedOldMoveCannotRemoveANewerRegistration() {
		MoveTaskManager manager = new MoveTaskManager();
		TestCreature creature = new TestCreature(1);
		creature.ai.destinationReached = true;
		creature.ai.onDestinationReached = () -> {
			creature.ai.destinationReached = false;
			manager.addCreature(creature);
		};

		manager.addCreature(creature);
		manager.run();
		manager.run();

		assertEquals(2, creature.moveController.moveCalls);
		assertEquals(0, creature.ai.arrivedEvents);
		assertEquals(1, creature.ai.validateEvents);
	}

	@Test
	@SuppressWarnings("unchecked")
	void readdingTheSameCreatureKeepsItsRegistration() throws Exception {
		MoveTaskManager manager = new MoveTaskManager();
		TestCreature creature = new TestCreature(1);
		Field movingField = MoveTaskManager.class.getDeclaredField("movingCreatures");
		movingField.setAccessible(true);
		Map<Integer, Object> moving = (Map<Integer, Object>) movingField.get(manager);

		manager.addCreature(creature);
		Object captured = moving.get(creature.getObjectId());
		manager.addCreature(creature);
		assertSame(captured, moving.get(creature.getObjectId()));
		Field predicateField = MoveTaskManager.class.getDeclaredField("CREATURE_MOVE_PREDICATE");
		predicateField.setAccessible(true);
		Predicate<Object> predicate = (Predicate<Object>) predicateField.get(manager);
		predicate.apply(captured);

		assertEquals(1, creature.moveController.moveCalls);
	}

	@Test
	void onlyDistantIdleAndWalkingNpcsUseSlowerMovementTicks() {
		assertEquals(100, MoveTaskManager.movementUpdatePeriod(false, AIState.WALKING, Float.POSITIVE_INFINITY));
		assertEquals(100, MoveTaskManager.movementUpdatePeriod(true, AIState.FIGHT, Float.POSITIVE_INFINITY));
		assertEquals(100, MoveTaskManager.movementUpdatePeriod(true, AIState.WALKING, 30 * 30));
		assertEquals(200, MoveTaskManager.movementUpdatePeriod(true, AIState.WALKING, 30 * 30 + 1));
		assertEquals(200, MoveTaskManager.movementUpdatePeriod(true, AIState.IDLE, 60 * 60));
		assertEquals(500, MoveTaskManager.movementUpdatePeriod(true, AIState.WALKING, 60 * 60 + 1));
	}

	private static final class TestCreature extends Creature {

		private final TestMoveController moveController = new TestMoveController();
		private final TestAI2 ai = new TestAI2();

		private TestCreature(int objectId) {
			super(objectId, new CreatureController<>() {}, null, new TestVisibleObjectTemplate(), null);
		}

		@Override
		public MoveController getMoveController() {
			return moveController;
		}

		@Override
		public AI2 getAi2() {
			return ai;
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

	private static final class TestMoveController implements MoveController {

		private int moveCalls;

		@Override
		public void moveToDestination() {
			moveCalls++;
		}

		@Override
		public float getTargetX2() {
			return 0;
		}

		@Override
		public float getTargetY2() {
			return 0;
		}

		@Override
		public float getTargetZ2() {
			return 0;
		}

		@Override
		public void setNewDirection(float x, float y, float z, byte heading) {
		}

		@Override
		public void startMovingToDestination() {
		}

		@Override
		public void abortMove() {
		}

		@Override
		public byte getMovementMask() {
			return 0;
		}

		@Override
		public boolean isInMove() {
			return false;
		}

		@Override
		public void setInMove(boolean value) {
		}

		@Override
		public void skillMovement() {
		}
	}

	private static final class TestAI2 implements AI2 {

		private int validateEvents;
		private int arrivedEvents;
		private boolean destinationReached;
		private Runnable onDestinationReached;

		@Override
		public void onCreatureEvent(AIEventType event, Creature creature) {
		}

		@Override
		public void onCustomEvent(int eventId, Object... args) {
		}

		@Override
		public void onGeneralEvent(AIEventType event) {
			if (event == AIEventType.MOVE_VALIDATE) {
				validateEvents++;
			} else if (event == AIEventType.MOVE_ARRIVED) {
				arrivedEvents++;
			}
		}

		@Override
		public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
			return false;
		}

		@Override
		public void think() {
		}

		@Override
		public boolean canThink() {
			return false;
		}

		@Override
		public AIState getState() {
			return null;
		}

		@Override
		public AISubState getSubState() {
			return null;
		}

		@Override
		public String getName() {
			return "test";
		}

		@Override
		public boolean poll(AIQuestion question) {
			boolean reached = question == AIQuestion.DESTINATION_REACHED && destinationReached;
			if (reached && onDestinationReached != null) {
				Runnable action = onDestinationReached;
				onDestinationReached = null;
				action.run();
			}
			return reached;
		}

		@Override
		public AIAnswer ask(AIQuestion question) {
			return null;
		}

		@Override
		public boolean isLogging() {
			return false;
		}

		@Override
		public long getRemainigTime() {
			return 0;
		}

		@Override
		public int modifyDamage(int damage) {
			return damage;
		}

		@Override
		public int modifyOwnerDamage(int damage) {
			return damage;
		}

		@Override
		public void onIndividualNpcEvent(Creature npc) {
		}

		@Override
		public int modifyHealValue(int value) {
			return value;
		}

		@Override
		public int modifyMaccuracy(int value) {
			return value;
		}

		@Override
		public int modifySensoryRange(int value) {
			return value;
		}

		@Override
		public ItemAttackType modifyAttackType(ItemAttackType type) {
			return type;
		}
	}
}
