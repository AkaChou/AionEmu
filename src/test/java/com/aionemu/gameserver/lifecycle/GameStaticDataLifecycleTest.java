package com.aionemu.gameserver.lifecycle;

import aionemu.testkit.StubPersistenceDaos;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.WorldMapsData;
import com.aionemu.gameserver.services.GameLegacyServiceBridgeConfiguration;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class GameStaticDataLifecycleTest {

    @Test
    void staticDataLifecycleBridgesLegacyDataManagerAndMovementLoopsThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameStaticDataLifecycle.class, "dataManagerProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameStaticDataLifecycle.class, "movementNotifyTaskProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameStaticDataLifecycle.class, "moveTaskManagerProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameStaticDataLifecycle.class, "playerMoveTaskManagerProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameStaticDataLifecycle.class, "zoneUpdateServiceProvider"));
    }

    @Test
    void staticDataLifecycleInitializesMovementLoopsAfterStaticData() {
        AtomicInteger loads = new AtomicInteger();
        AtomicInteger movementLoops = new AtomicInteger();
        GameStaticDataLifecycle lifecycle = new RecordingGameStaticDataLifecycle(loads, movementLoops, null);

        lifecycle.start();
        lifecycle.start();

        assertEquals(1, loads.get());
        assertEquals(1, movementLoops.get());
    }

    @Test
    void movementLoopProviderRegistrationDoesNotReadWorldMapsBeforeStaticDataLoads() {
        WorldMapsData oldWorldMapsData = DataManager.WORLD_MAPS_DATA;
        DataManager.WORLD_MAPS_DATA = null;
        try {
            assertDoesNotThrow(() -> {
                try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
                    context.register(GameLegacyServiceBridgeConfiguration.class,
                            StubPersistenceDaos.class, MovementLoopServiceBindings.class);
                    context.refresh();
                }
            });
        } finally {
            DataManager.WORLD_MAPS_DATA = oldWorldMapsData;
        }
    }

    @Test
    void startLoadsStaticDataOnceAndRecordsLoadTime() {
        AtomicInteger loads = new AtomicInteger();
        GameStaticDataLifecycle lifecycle = new RecordingGameStaticDataLifecycle(loads, new AtomicInteger(), null);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(1, loads.get());
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        AtomicInteger loads = new AtomicInteger();
        IllegalStateException failure = new IllegalStateException("static data failed");
        GameStaticDataLifecycle lifecycle = new RecordingGameStaticDataLifecycle(loads, new AtomicInteger(), failure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(2, loads.get());
		assertNull(lifecycle.getLastFailure());
    }

    private static final class RecordingGameStaticDataLifecycle extends GameStaticDataLifecycle {

        private final AtomicInteger loads;
        private final AtomicInteger movementLoops;
        private final RuntimeException firstFailure;

        private RecordingGameStaticDataLifecycle(AtomicInteger loads, AtomicInteger movementLoops, RuntimeException firstFailure) {
            this.loads = loads;
            this.movementLoops = movementLoops;
            this.firstFailure = firstFailure;
        }

        @Override
        protected void loadStaticData() {
            if (loads.incrementAndGet() == 1 && firstFailure != null) {
                throw firstFailure;
            }
        }

        @Override
        protected void initializeMovementLoops() {
            movementLoops.incrementAndGet();
        }
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }
}
