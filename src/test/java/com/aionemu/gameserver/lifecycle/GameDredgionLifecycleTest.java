package com.aionemu.gameserver.lifecycle;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;

class GameDredgionLifecycleTest {

    @Test
    void dredgionLifecycleBridgesLegacyServicesThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameDredgionLifecycle.class, "dredgionServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameDredgionLifecycle.class, "asyunatarServiceProvider"));
    }


    @Test
    void startRunsEnabledInitializersOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameDredgionLifecycle lifecycle = newLifecycle(events, true);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "dredgion", "asyunatar"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void startSkipsInitializersWhenAutoGroupDisabled() {
        List<String> events = new ArrayList<>();
        GameDredgionLifecycle lifecycle = newLifecycle(events, false);

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section"), events);
    }

    @Test
    void startReadsAutoGroupFlagForEachInitializerLikeLegacyCode() {
        List<String> events = new ArrayList<>();
        AtomicInteger reads = new AtomicInteger();
        GameDredgionLifecycle lifecycle = new RecordingGameDredgionLifecycle(events, () -> reads.incrementAndGet() == 1, null);

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(2, reads.get());
        assertEquals(List.of("section", "dredgion"), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("dredgion failed");
        GameDredgionLifecycle lifecycle = new RecordingGameDredgionLifecycle(events, () -> true, failure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "dredgion", "asyunatar", "section", "dredgion", "asyunatar"), events);
		assertNull(lifecycle.getLastFailure());
    }

    private static GameDredgionLifecycle newLifecycle(List<String> events, boolean autoGroupEnabled) {
        return new RecordingGameDredgionLifecycle(
            events,
            () -> autoGroupEnabled,
            null
        );
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameDredgionLifecycle extends GameDredgionLifecycle {

        private final List<String> events;
        private final BooleanSupplier autoGroupEnabled;
        private final RuntimeException firstFailure;

        private RecordingGameDredgionLifecycle(
            List<String> events,
            BooleanSupplier autoGroupEnabled,
            RuntimeException firstFailure
        ) {
            this.events = events;
            this.autoGroupEnabled = autoGroupEnabled;
            this.firstFailure = firstFailure;
        }

        @Override
        protected void runStage() {
            events.add("section");
            if (isAutoGroupEnabled()) {
                events.add("dredgion");
            }
            if (isAutoGroupEnabled()) {
                events.add("asyunatar");
                if (events.size() == 3 && firstFailure != null) {
                    throw firstFailure;
                }
            }
        }

        @Override
        protected boolean isAutoGroupEnabled() {
            return autoGroupEnabled.getAsBoolean();
        }
    }
}
