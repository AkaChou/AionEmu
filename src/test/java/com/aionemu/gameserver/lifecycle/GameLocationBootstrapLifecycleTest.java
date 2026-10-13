package com.aionemu.gameserver.lifecycle;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;

class GameLocationBootstrapLifecycleTest {

    @Test
    void locationBootstrapLifecycleBridgesLocationServicesThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameLocationBootstrapLifecycle.class, "siegeServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameLocationBootstrapLifecycle.class, "baseServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameLocationBootstrapLifecycle.class, "outpostServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameLocationBootstrapLifecycle.class, "vortexServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameLocationBootstrapLifecycle.class, "beritraServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameLocationBootstrapLifecycle.class, "agentServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameLocationBootstrapLifecycle.class, "anohaServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameLocationBootstrapLifecycle.class, "svsServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameLocationBootstrapLifecycle.class, "rvrServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameLocationBootstrapLifecycle.class, "iuServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameLocationBootstrapLifecycle.class, "nightmareCircusServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameLocationBootstrapLifecycle.class, "dynamicRiftServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameLocationBootstrapLifecycle.class, "instanceRiftServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameLocationBootstrapLifecycle.class, "zorshivDredgionServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameLocationBootstrapLifecycle.class, "moltenusServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameLocationBootstrapLifecycle.class, "riftServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameLocationBootstrapLifecycle.class, "conquestServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameLocationBootstrapLifecycle.class, "idianDepthsServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameLocationBootstrapLifecycle.class, "towerOfEternityServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameLocationBootstrapLifecycle.class, "abyssLandingServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameLocationBootstrapLifecycle.class, "landingUpdateServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameLocationBootstrapLifecycle.class, "abyssLandingSpecialServiceProvider"));
    }

    @Test
    void startRunsLocationBootstrappersOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameLocationBootstrapLifecycle lifecycle = new RecordingGameLocationBootstrapLifecycle(events, null);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("siege", "base", "vortex", "abyss"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("location failed");
        GameLocationBootstrapLifecycle lifecycle = new RecordingGameLocationBootstrapLifecycle(events, failure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("siege", "base", "siege", "base", "vortex", "abyss"), events);
		assertNull(lifecycle.getLastFailure());
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameLocationBootstrapLifecycle extends GameLocationBootstrapLifecycle {

        private final List<String> events;
        private final RuntimeException firstFailure;

        private RecordingGameLocationBootstrapLifecycle(List<String> events, RuntimeException firstFailure) {
            this.events = events;
            this.firstFailure = firstFailure;
        }

        @Override
        protected void runStage() {
            events.add("siege");
            events.add("base");
            if (events.size() == 2 && firstFailure != null) {
                throw firstFailure;
            }
            events.add("vortex");
            events.add("abyss");
        }
    }
}
