package com.aionemu.gameserver.taskmanager.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.controllers.CreatureController;
import com.aionemu.gameserver.controllers.movement.MoveController;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;

class PlayerMoveTaskManagerTest {

	@Test
	void runMovesRegisteredPlayersOnce() {
		PlayerMoveTaskManager manager = new PlayerMoveTaskManager();
		TestCreature first = new TestCreature(1);
		TestCreature second = new TestCreature(2);

		manager.addPlayer(first);
		manager.addPlayer(second);
		manager.run();

		assertEquals(1, first.moveController.moveCalls);
		assertEquals(1, second.moveController.moveCalls);
	}

	@Test
	void removedPlayerIsNotMoved() {
		PlayerMoveTaskManager manager = new PlayerMoveTaskManager();
		TestCreature creature = new TestCreature(1);

		manager.addPlayer(creature);
		manager.removePlayer(creature);
		manager.run();

		assertEquals(0, creature.moveController.moveCalls);
	}

	private static final class TestCreature extends Creature {

		private final TestMoveController moveController = new TestMoveController();

		private TestCreature(int objectId) {
			super(objectId, new CreatureController<>() {}, null, new TestVisibleObjectTemplate(), null);
		}

		@Override
		public MoveController getMoveController() {
			return moveController;
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
}
