package com.aionemu.gameserver.controllers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.ai2.AI2;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.world.knownlist.KnownList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class CreatureControllerTest {

	@Test
	void cancelAllTasksToleratesTasksMutatingDuringCancelCallbacks() {
		TestCreatureController controller = new TestCreatureController();
		controller.addTask(TaskId.DECAY, new CallbackFuture(() -> controller.addTask(TaskId.DROWN, new NoopFuture())));
		controller.addTask(TaskId.PRISON, new NoopFuture());

		assertDoesNotThrow(controller::cancelAllTasks);
		assertFalse(controller.hasTask(TaskId.DECAY));
		assertFalse(controller.hasTask(TaskId.PRISON));
		assertFalse(controller.hasTask(TaskId.DROWN));
	}

	@Test
	void cancelledTaskCannotBeReplacedByItsRunningCallback() {
		TestCreatureController controller = new TestCreatureController();
		NoopFuture current = new NoopFuture();
		NoopFuture replacement = new NoopFuture();
		controller.addTask(TaskId.HOTSPOT_TELEPORT, current);

		controller.cancelTask(TaskId.HOTSPOT_TELEPORT);

		assertFalse(controller.replaceTask(TaskId.HOTSPOT_TELEPORT, current, replacement));
		assertTrue(replacement.isCancelled());
		assertFalse(controller.hasTask(TaskId.HOTSPOT_TELEPORT));
	}

	@Test
	void cancelAllTasksKeepsRespawnTaskTracked() {
		TestCreatureController controller = new TestCreatureController();
		NoopFuture respawn = new NoopFuture();
		controller.addTask(TaskId.RESPAWN, respawn);

		controller.cancelAllTasks();

		assertTrue(controller.hasScheduledTask(TaskId.RESPAWN));
		assertFalse(respawn.isCancelled());
	}

	@Test
	void cancelCurrentSkillUsesSingleCastingSkillSnapshot() {
		TestCreatureController controller = new TestCreatureController();
		TestCreature creature = new ObjenesisStd().newInstance(TestCreature.class);
		controller.setOwner(creature);
		creature.setFirstCastingSkill(new Skill(new SkillTemplate(), creature, 1, creature, null));

		assertDoesNotThrow(controller::cancelCurrentSkill);
		assertEquals(1, creature.getCastingSkillReadCount());
	}

	private static class TestCreatureController extends CreatureController<Creature> {
	}

	private static class TestCreature extends Creature {
		private Skill firstCastingSkill;
		private int castingSkillReadCount;

		private TestCreature() {
			super(0, null, null, null, null);
		}

		private void setFirstCastingSkill(Skill skill) {
			firstCastingSkill = skill;
		}

		private int getCastingSkillReadCount() {
			return castingSkillReadCount;
		}

		@Override
		public Skill getCastingSkill() {
			return ++castingSkillReadCount == 1 ? firstCastingSkill : null;
		}

		@Override
		public AI2 getAi2() {
			return null;
		}

		@Override
		public KnownList getKnownList() {
			return new KnownList(this);
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

	private static class CallbackFuture extends NoopFuture {
		private final Runnable callback;

		private CallbackFuture(Runnable callback) {
			this.callback = callback;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			callback.run();
			return true;
		}
	}

	private static class NoopFuture implements Future<Object> {
		private boolean cancelled;

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			cancelled = true;
			return true;
		}

		@Override
		public boolean isCancelled() {
			return cancelled;
		}

		@Override
		public boolean isDone() {
			return false;
		}

		@Override
		public Object get() {
			return null;
		}

		@Override
		public Object get(long timeout, TimeUnit unit) {
			return null;
		}
	}
}
