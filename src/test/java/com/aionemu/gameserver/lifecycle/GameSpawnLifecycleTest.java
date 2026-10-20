package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.testutil.ConfigSnapshot;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameSpawnLifecycleTest {

    private final ConfigSnapshot geoSnapshot = ConfigSnapshot.of(GeoDataConfig.class,
        "GEO_COLLISION_PREBUILD_ENABLE", "GEO_COLLISION_PREBUILD_DELAY_MS");

    @BeforeEach
    void enableCollisionWarmUp() {
        GeoDataConfig.GEO_COLLISION_PREBUILD_ENABLE = true;
        GeoDataConfig.GEO_COLLISION_PREBUILD_DELAY_MS = 15000;
    }

    @Test
    void startSpawnsOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameSpawnLifecycle lifecycle = new RecordingGameSpawnLifecycle(events, null, new GeoService());

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "spawn", "schedule:15000"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("spawn failed");
        GameSpawnLifecycle lifecycle = new RecordingGameSpawnLifecycle(events, failure, new GeoService());

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "spawn", "section", "spawn", "schedule:15000"), events);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void schedulesCollisionWarmUpAfterSpawnWithConfiguredDelay() {
        GeoDataConfig.GEO_COLLISION_PREBUILD_ENABLE = true;
        GeoDataConfig.GEO_COLLISION_PREBUILD_DELAY_MS = 2500;
        List<String> events = new ArrayList<>();
        GeoService geoService = new GeoService();
        RecordingGameSpawnLifecycle lifecycle = new RecordingGameSpawnLifecycle(events, null, geoService);

        lifecycle.start();

        assertEquals(List.of("section", "spawn", "schedule:2500"), events);
        assertEquals(1, lifecycle.delayedTasks.size());
        lifecycle.delayedTasks.getFirst().run();
        assertEquals(List.of("section", "spawn", "schedule:2500", "warm"), events);
    }

    @Test
    void skipsCollisionWarmUpWhenDisabled() {
        GeoDataConfig.GEO_COLLISION_PREBUILD_ENABLE = false;
        List<String> events = new ArrayList<>();

        new RecordingGameSpawnLifecycle(events, null, new GeoService()).start();

        assertEquals(List.of("section", "spawn"), events);
    }

    @AfterEach
    void restoreConfig() {
        geoSnapshot.restore();
    }

    private static final class RecordingGameSpawnLifecycle extends GameSpawnLifecycle {

        private final List<String> events;
        private final RuntimeException firstFailure;
        private final GeoService geoService;
        private final List<Runnable> delayedTasks = new ArrayList<>();

        private RecordingGameSpawnLifecycle(List<String> events, RuntimeException firstFailure, GeoService geoService) {
            this.events = events;
            this.firstFailure = firstFailure;
            this.geoService = geoService;
        }

        @Override
        protected void printSection() {
            events.add("section");
        }

        @Override
        protected void spawnAll() {
            events.add("spawn");
            if (firstFailure != null && events.stream().filter("spawn"::equals).count() == 1) {
                throw firstFailure;
            }
        }

        @Override
        protected GeoService geoService() {
            return geoService;
        }

        @Override
        protected void submitDelayed(Runnable task, long delayMs) {
            events.add("schedule:" + delayMs);
            delayedTasks.add(task);
        }

        @Override
        protected void warmCollisionData(GeoService resolved) {
            events.add("warm");
        }
    }
}
