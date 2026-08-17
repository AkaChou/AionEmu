package com.aionemu.gameserver.controllers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
	void controlledTaskDoesNotRunAfterCancellationClaimsIt() {
		TestCreatureController controller = new TestCreatureController();
		AtomicInteger executions = new AtomicInteger();
		controller.scheduleTask(TaskId.ITEM_USE, executions::incrementAndGet, 1000);

		assertTrue(controller.cancelTask(TaskId.ITEM_USE).isCancelled());
		controller.runScheduledTask();

		assertEquals(0, executions.get());
		assertFalse(controller.hasTask(TaskId.ITEM_USE));
	}

	@Test
	void controlledTaskRemovesItselfBeforeExecuting() {
		TestCreatureController controller = new TestCreatureController();
		AtomicInteger executions = new AtomicInteger();
		controller.scheduleTask(TaskId.ITEM_USE, executions::incrementAndGet, 1000);

		controller.runScheduledTask();

		assertEquals(1, executions.get());
		assertNull(controller.cancelTask(TaskId.ITEM_USE));
	}

	@Test
	void replacementPreventsSupersededControlledTaskFromRunning() {
		TestCreatureController controller = new TestCreatureController();
		AtomicInteger executions = new AtomicInteger();
		controller.scheduleTask(TaskId.ITEM_USE, () -> executions.addAndGet(1), 1000);
		Runnable superseded = controller.scheduledTask;
		controller.scheduleTask(TaskId.ITEM_USE, () -> executions.addAndGet(10), 1000);

		superseded.run();
		controller.runScheduledTask();

		assertEquals(10, executions.get());
	}

	private static class TestCreatureController extends CreatureController<Creature> {
		private Runnable scheduledTask;

		@Override
		protected void scheduleTaskExecution(Runnable task, long delay) {
			scheduledTask = task;
		}

		private void runScheduledTask() {
			scheduledTask.run();
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
