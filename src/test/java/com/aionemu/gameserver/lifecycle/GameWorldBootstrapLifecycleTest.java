package com.aionemu.gameserver.lifecycle;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;

class GameWorldBootstrapLifecycleTest {

    @Test
    void worldBootstrapLifecycleBridgesWorldServicesThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameWorldBootstrapLifecycle.class, "idFactoryProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameWorldBootstrapLifecycle.class, "zoneServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameWorldBootstrapLifecycle.class, "hotspotTeleportServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameWorldBootstrapLifecycle.class, "roadServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameWorldBootstrapLifecycle.class, "worldProvider"));
    }

    @Test
    void startRunsWorldBootstrappersOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameWorldBootstrapLifecycle lifecycle = new RecordingGameWorldBootstrapLifecycle(events, null);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("worldBootstrap"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("zone failed");
        GameWorldBootstrapLifecycle lifecycle = new RecordingGameWorldBootstrapLifecycle(events, failure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("worldBootstrap", "worldBootstrap"), events);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void startBootstrapsWorldStepsInParallelBeforeWaitingForCompletion() throws Exception {
        int stepCount = 5;
        CountDownLatch allStarted = new CountDownLatch(stepCount);
        CountDownLatch releaseSteps = new CountDownLatch(1);
        BlockingGameWorldBootstrapLifecycle lifecycle = new BlockingGameWorldBootstrapLifecycle(allStarted, releaseSteps);

        Thread starter = new Thread(lifecycle::start);
        starter.start();

        try {
            assertTrue(allStarted.await(2, TimeUnit.SECONDS));
            assertTrue(starter.isAlive());
        } finally {
            releaseSteps.countDown();
        }
        starter.join(2000);

        assertFalse(starter.isAlive());
        assertEquals(stepCount, lifecycle.completedSteps());
    }

    @Test
    void startReportsProgressForWorldStartupSteps() {
        List<String> events = Collections.synchronizedList(new ArrayList<>());
        GameWorldBootstrapLifecycle lifecycle = new ProgressRecordingGameWorldBootstrapLifecycle(
            events,
            new RecordingStartupProgressReporter(events)
        );

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals("progress:start:game world", events.getFirst());
        assertEquals("progress:finish:game world", events.getLast());
        assertEquals(17, events.size());
        assertStepReported(events, "IDFactory");
        assertStepReported(events, "Zone");
        assertStepReported(events, "Hotspot Teleport");
        assertStepReported(events, "Road");
        assertStepReported(events, "World");
    }

    private static void assertStepReported(List<String> events, String stepName) {
        int started = events.indexOf("progress:started:" + stepName);
        int loaded = events.indexOf("load:" + stepName);
        int finished = events.indexOf("progress:finished:" + stepName);

        assertTrue(started > 0, stepName);
        assertTrue(loaded > started, stepName);
        assertTrue(finished > loaded, stepName);
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            return type.getDeclaredField(name).getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameWorldBootstrapLifecycle extends GameWorldBootstrapLifecycle {

        private final List<String> events;
        private final RuntimeException firstFailure;

        private RecordingGameWorldBootstrapLifecycle(List<String> events, RuntimeException firstFailure) {
            this.events = events;
            this.firstFailure = firstFailure;
        }

        @Override
        protected void runStage() {
            events.add("worldBootstrap");
            if (events.size() == 1 && firstFailure != null) {
                throw firstFailure;
            }
        }
    }

    private static final class ProgressRecordingGameWorldBootstrapLifecycle extends GameWorldBootstrapLifecycle {

        private final List<String> events;

        private ProgressRecordingGameWorldBootstrapLifecycle(List<String> events, StartupProgressReporter progressReporter) {
            super(progressReporter);
            this.events = events;
        }

        @Override
        protected void initializeIDFactory() {
            events.add("load:IDFactory");
        }

        @Override
        protected void loadZoneService() {
            events.add("load:Zone");
        }

        @Override
        protected void initializeHotspotTeleportService() {
            events.add("load:Hotspot Teleport");
        }

        @Override
        protected void initializeRoadService() {
            events.add("load:Road");
        }

        @Override
        protected void initializeWorld() {
            events.add("load:World");
        }
    }

    private static final class BlockingGameWorldBootstrapLifecycle extends GameWorldBootstrapLifecycle {

        private final CountDownLatch allStarted;
        private final CountDownLatch releaseSteps;
        private final AtomicInteger completedSteps = new AtomicInteger();

        private BlockingGameWorldBootstrapLifecycle(CountDownLatch allStarted, CountDownLatch releaseSteps) {
            super(StartupProgressReporter.noop());
            this.allStarted = allStarted;
            this.releaseSteps = releaseSteps;
        }

        @Override
        protected void initializeIDFactory() {
            blockStep();
        }

        @Override
        protected void loadZoneService() {
            blockStep();
        }

        @Override
        protected void initializeHotspotTeleportService() {
            blockStep();
        }

        @Override
        protected void initializeRoadService() {
            blockStep();
        }

        @Override
        protected void initializeWorld() {
            blockStep();
        }

        private void blockStep() {
            allStarted.countDown();
            try {
                releaseSteps.await();
                completedSteps.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }

        private int completedSteps() {
            return completedSteps.get();
        }
    }
}
