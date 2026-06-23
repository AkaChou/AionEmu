package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.model.GameEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;

class GameEnginesLifecycleTest {

    @Test
    void startSubmitsEachEngineLoadOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        List<Runnable> submittedTasks = new ArrayList<>();
        GameEnginesLifecycle lifecycle = new GameEnginesLifecycle(
            () -> List.of(
                new RecordingEngine("quest", events),
                new RecordingEngine("instance", events),
                new RecordingEngine("ai", events),
                new RecordingEngine("chat", events)
            ),
            task -> {
                submittedTasks.add(task);
                task.run();
            }
        );

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(4, submittedTasks.size());
        assertEquals(List.of("quest", "instance", "ai", "chat"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("engine failed");
        FailingOnceEngine engine = new FailingOnceEngine(events, failure);
        GameEnginesLifecycle lifecycle = new GameEnginesLifecycle(
            () -> List.of(engine),
            Runnable::run
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("engine", "engine"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void interruptedWaitRestoresInterruptFlagAndMarksLoaded() {
        GameEnginesLifecycle lifecycle = new GameEnginesLifecycle(
            () -> List.of(),
            Runnable::run
        );

        Thread.currentThread().interrupt();
        try {
            lifecycle.start();

            assertTrue(Thread.currentThread().isInterrupted());
            assertTrue(lifecycle.isLoaded());
        } finally {
            Thread.interrupted();
        }
    }

    private static final class RecordingEngine implements GameEngine {

        private final String name;
        private final List<String> events;

        private RecordingEngine(String name, List<String> events) {
            this.name = name;
            this.events = events;
        }

        @Override
        public void load(CountDownLatch progressLatch) {
            events.add(name);
            progressLatch.countDown();
        }

        @Override
        public void shutdown() {
        }
    }

    private static final class FailingOnceEngine implements GameEngine {

        private final List<String> events;
        private final IllegalStateException failure;
        private boolean failed;

        private FailingOnceEngine(List<String> events, IllegalStateException failure) {
            this.events = events;
            this.failure = failure;
        }

        @Override
        public void load(CountDownLatch progressLatch) {
            events.add("engine");
            progressLatch.countDown();
            if (!failed) {
                failed = true;
                throw failure;
            }
        }

        @Override
        public void shutdown() {
        }
    }
}
