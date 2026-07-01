package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class GameWorldBootstrapLifecycleTest {

    @Test
    void usesWorldBootstrapGatewayCollaborator() {
        assertEquals(GameWorldBootstrapGateway.class, fieldType("worldBootstrapGateway"));
    }

    @Test
    void worldBootstrapGatewayBridgesWorldServicesThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameWorldBootstrapGateway.class, "idFactoryProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameWorldBootstrapGateway.class, "zoneServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameWorldBootstrapGateway.class, "hotspotTeleportServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameWorldBootstrapGateway.class, "roadServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameWorldBootstrapGateway.class, "worldProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameWorldBootstrapGateway.class, "runtimeBridgeProvider"));
    }

    @Test
    void startRunsWorldBootstrappersOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameWorldBootstrapLifecycle lifecycle = new GameWorldBootstrapLifecycle(new RecordingGameWorldBootstrapGateway(events, null));

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("worldBootstrap"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("zone failed");
        GameWorldBootstrapLifecycle lifecycle = new GameWorldBootstrapLifecycle(new RecordingGameWorldBootstrapGateway(events, failure));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("worldBootstrap", "worldBootstrap"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void bootstrapStartsWorldStepsInParallelBeforeWaitingForCompletion() throws Exception {
        int stepCount = 5;
        CountDownLatch allStarted = new CountDownLatch(stepCount);
        CountDownLatch releaseSteps = new CountDownLatch(1);
        BlockingGameWorldBootstrapGateway gateway = new BlockingGameWorldBootstrapGateway(allStarted, releaseSteps);

        Thread starter = new Thread(gateway::bootstrap);
        starter.start();

        try {
            assertTrue(allStarted.await(2, TimeUnit.SECONDS));
            assertTrue(starter.isAlive());
        } finally {
            releaseSteps.countDown();
        }
        starter.join(2000);

        assertFalse(starter.isAlive());
        assertEquals(stepCount, gateway.completedSteps());
    }

    private static final class RecordingGameWorldBootstrapGateway extends GameWorldBootstrapGateway {

        private final List<String> events;
        private final RuntimeException firstFailure;

        private RecordingGameWorldBootstrapGateway(List<String> events, RuntimeException firstFailure) {
            this.events = events;
            this.firstFailure = firstFailure;
        }

        @Override
        public void bootstrap() {
            events.add("worldBootstrap");
            if (events.size() == 1 && firstFailure != null) {
                throw firstFailure;
            }
        }
    }

    private static final class BlockingGameWorldBootstrapGateway extends GameWorldBootstrapGateway {

        private final CountDownLatch allStarted;
        private final CountDownLatch releaseSteps;
        private final AtomicInteger completedSteps = new AtomicInteger();

        private BlockingGameWorldBootstrapGateway(CountDownLatch allStarted, CountDownLatch releaseSteps) {
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

    private static Class<?> fieldType(String name) {
        return fieldType(GameWorldBootstrapLifecycle.class, name);
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            return type.getDeclaredField(name).getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }
}
