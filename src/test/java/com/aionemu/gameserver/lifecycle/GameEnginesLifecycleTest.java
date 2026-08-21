package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.model.GameEngine;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class GameEnginesLifecycleTest {

    @Test
    void usesEnginesGatewayCollaborator() {
        assertEquals(GameEnginesGateway.class, fieldType("enginesGateway"));
    }

    @Test
    void enginesGatewayBridgesEnginesThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameEnginesGateway.class, "questEngineProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameEnginesGateway.class, "instanceEngineProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameEnginesGateway.class, "ai2EngineProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameEnginesGateway.class, "chatProcessorProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameEnginesGateway.class, "threadPoolManagerProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameEnginesGateway.class, "runtimeBridgeProvider"));
    }

    @Test
    void startSubmitsEachEngineLoadOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        List<Runnable> submittedTasks = new ArrayList<>();
        GameEnginesLifecycle lifecycle = new GameEnginesLifecycle(
            new RecordingGameEnginesGateway(
                List.of(
                    new RecordingEngine("quest", events),
                    new RecordingEngine("instance", events),
                    new RecordingEngine("ai", events),
                    new RecordingEngine("chat", events)
                ),
                task -> {
                    submittedTasks.add(task);
                    task.run();
                }
            ));

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(4, submittedTasks.size());
        assertEquals(List.of("quest", "instance", "ai", "chat"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void preloadProductionCatalogStartsOnceWithoutMarkingEnginesLoaded() {
        List<String> events = new ArrayList<>();
        RecordingGameEnginesGateway gateway = new RecordingGameEnginesGateway(List.of(), Runnable::run);
        GameEnginesLifecycle lifecycle = new GameEnginesLifecycle(gateway);

        lifecycle.preloadProductionCatalog();
        lifecycle.preloadProductionCatalog();

        assertEquals(1, gateway.preloadCount);
        assertFalse(lifecycle.isLoaded());
        assertTrue(events.isEmpty());
    }

    @Test
    void startSubmitsEngineLoadsInParallelBeforeWaitingForCompletion() throws Exception {
        int engineCount = 4;
        CountDownLatch allStarted = new CountDownLatch(engineCount);
        CountDownLatch releaseEngines = new CountDownLatch(1);
        List<Thread> workers = new ArrayList<>();
        List<GameEngine> engines = new ArrayList<>();
        for (int i = 0; i < engineCount; i++) {
            engines.add(new BlockingEngine(allStarted, releaseEngines));
        }
        GameEnginesLifecycle lifecycle = new GameEnginesLifecycle(
            new RecordingGameEnginesGateway(engines, task -> {
                Thread worker = new Thread(task);
                workers.add(worker);
                worker.start();
            }));

        Thread starter = new Thread(lifecycle::start);
        starter.start();

        try {
            assertTrue(allStarted.await(2, TimeUnit.SECONDS));
            assertTrue(starter.isAlive());
        } finally {
            releaseEngines.countDown();
        }
        starter.join(2000);

        assertFalse(starter.isAlive());
        assertTrue(lifecycle.isLoaded());
        for (Thread worker : workers) {
            worker.join(2000);
            assertFalse(worker.isAlive());
        }
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("engine failed");
        FailingOnceEngine engine = new FailingOnceEngine(events, failure);
        GameEnginesLifecycle lifecycle = new GameEnginesLifecycle(
            new RecordingGameEnginesGateway(List.of(engine), Runnable::run));

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
            new RecordingGameEnginesGateway(List.of(), Runnable::run));

        Thread.currentThread().interrupt();
        try {
            lifecycle.start();

            assertTrue(Thread.currentThread().isInterrupted());
            assertTrue(lifecycle.isLoaded());
        } finally {
            Thread.interrupted();
        }
    }

    private static Class<?> fieldType(String name) {
        return fieldType(GameEnginesLifecycle.class, name);
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameEnginesGateway extends GameEnginesGateway {

        private final List<GameEngine> engines;
        private final Consumer<Runnable> taskExecutor;
        private int preloadCount;

        private RecordingGameEnginesGateway(
            List<GameEngine> engines,
            Consumer<Runnable> taskExecutor
        ) {
            this.engines = engines;
            this.taskExecutor = taskExecutor;
        }

        @Override
        public void printSection() {
        }

        @Override
        public List<GameEngine> engines() {
            return engines;
        }

        @Override
        public void execute(Runnable runnable) {
            taskExecutor.accept(runnable);
        }

        @Override
        public void preloadProductionCatalog() {
            preloadCount++;
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

    private static final class BlockingEngine implements GameEngine {

        private final CountDownLatch allStarted;
        private final CountDownLatch release;

        private BlockingEngine(CountDownLatch allStarted, CountDownLatch release) {
            this.allStarted = allStarted;
            this.release = release;
        }

        @Override
        public void load(CountDownLatch progressLatch) {
            allStarted.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                progressLatch.countDown();
            }
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
