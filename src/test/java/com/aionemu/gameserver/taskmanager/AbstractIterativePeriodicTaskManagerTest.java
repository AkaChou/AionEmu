package com.aionemu.gameserver.taskmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class AbstractIterativePeriodicTaskManagerTest {

	@Test
	void runsUniqueActiveTasksInInsertionOrder() {
		TestTaskManager manager = new TestTaskManager();

		manager.startTask("first");
		manager.startTask("second");
		manager.startTask("first");
		manager.run();

		assertEquals(List.of("first", "second"), manager.calledTasks);
	}

	@Test
	void stopTaskCancelsPendingStartBeforeRun() {
		TestTaskManager manager = new TestTaskManager();

		manager.startTask("first");
		assertTrue(manager.hasTask("first"));

		manager.stopTask("first");

		assertFalse(manager.hasTask("first"));
		manager.run();
		assertEquals(List.of(), manager.calledTasks);
	}

	@Test
	void tasksChangedDuringRunAreAppliedOnNextRun() {
		TestTaskManager manager = new TestTaskManager();
		manager.changeDuringRun("first", "second");

		manager.startTask("first");
		manager.run();

		assertEquals(List.of("first"), manager.calledTasks);
		assertFalse(manager.hasTask("first"));
		assertTrue(manager.hasTask("second"));

		manager.run();

		assertEquals(List.of("first", "second"), manager.calledTasks);
		assertFalse(manager.hasTask("first"));
		assertTrue(manager.hasTask("second"));
	}

	private static final class TestTaskManager extends AbstractIterativePeriodicTaskManager<String> {

		private final List<String> calledTasks = new ArrayList<>();
		private String triggerTask;
		private String taskToStart;

		private TestTaskManager() {
			super(1000);
		}

		private void changeDuringRun(String triggerTask, String taskToStart) {
			this.triggerTask = triggerTask;
			this.taskToStart = taskToStart;
		}

		@Override
		protected void callTask(String task) {
			calledTasks.add(task);
			if (task.equals(triggerTask) && calledTasks.size() == 1) {
				stopTask(task);
				startTask(taskToStart);
			}
		}

		@Override
		protected String getCalledMethodName() {
			return "test";
		}
	}
}
