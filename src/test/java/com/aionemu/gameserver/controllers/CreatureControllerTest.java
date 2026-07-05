package com.aionemu.gameserver.controllers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

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
	void cancelAllTasksKeepsRespawnTaskTracked() {
		TestCreatureController controller = new TestCreatureController();
		NoopFuture respawn = new NoopFuture();
		controller.addTask(TaskId.RESPAWN, respawn);

		controller.cancelAllTasks();

		assertTrue(controller.hasScheduledTask(TaskId.RESPAWN));
		assertFalse(respawn.isCancelled());
	}

	private static class TestCreatureController extends CreatureController<Creature> {
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
