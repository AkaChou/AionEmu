package com.aionemu.gameserver.taskmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class AbstractFIFOPeriodicTaskManagerTest {

    @Test
    void executesUniqueTasksInInsertionOrder() {
        TestTaskManager manager = new TestTaskManager();

        manager.add("first");
        manager.add("second");
        manager.add("first");
        manager.run();

        assertEquals(List.of("first", "second"), manager.calledTasks);
    }

    @Test
    void defersTasksAddedDuringRunUntilNextRun() {
        TestTaskManager manager = new TestTaskManager();
        manager.addDuringRun("first", "second");

        manager.add("first");
        manager.run();

        assertEquals(List.of("first"), manager.calledTasks);

        manager.run();

        assertEquals(List.of("first", "second"), manager.calledTasks);
    }

    private static final class TestTaskManager extends AbstractFIFOPeriodicTaskManager<String> {

        private final List<String> calledTasks = new ArrayList<>();
        private String triggerTask;
        private String taskToAdd;

        private TestTaskManager() {
            super(1000);
        }

        private void addDuringRun(String triggerTask, String taskToAdd) {
            this.triggerTask = triggerTask;
            this.taskToAdd = taskToAdd;
        }

        @Override
        protected void callTask(String task) {
            calledTasks.add(task);
            if (task.equals(triggerTask)) {
                add(taskToAdd);
            }
        }

        @Override
        protected String getCalledMethodName() {
            return "test";
        }
    }
}
