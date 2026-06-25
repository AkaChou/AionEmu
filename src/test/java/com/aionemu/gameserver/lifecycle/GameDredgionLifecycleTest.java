package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class GameDredgionLifecycleTest {

    @Test
    void usesDredgionGatewayCollaborator() {
        assertEquals(GameDredgionGateway.class, fieldType("dredgionGateway"));
    }

    @Test
    void dredgionGatewayBridgesLegacyServicesThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameDredgionGateway.class, "dredgionServiceProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameDredgionGateway.class, "asyunatarServiceProvider"));
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
        assertEquals(null, lifecycle.getLastFailure());
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
        GameDredgionLifecycle lifecycle = new GameDredgionLifecycle(
            new RecordingGameDredgionGateway(events, () -> reads.incrementAndGet() == 1, null)
        );

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(2, reads.get());
        assertEquals(List.of("section", "dredgion"), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("dredgion failed");
        GameDredgionLifecycle lifecycle = new GameDredgionLifecycle(
            new RecordingGameDredgionGateway(events, () -> true, failure));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "dredgion", "asyunatar", "section", "dredgion", "asyunatar"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static GameDredgionLifecycle newLifecycle(List<String> events, boolean autoGroupEnabled) {
        return new GameDredgionLifecycle(new RecordingGameDredgionGateway(
            events,
            () -> autoGroupEnabled,
            null
        ));
    }

    private static Class<?> fieldType(String name) {
        return fieldType(GameDredgionLifecycle.class, name);
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameDredgionGateway extends GameDredgionGateway {

        private final List<String> events;
        private final BooleanSupplier autoGroupEnabled;
        private final RuntimeException firstFailure;

        private RecordingGameDredgionGateway(
            List<String> events,
            BooleanSupplier autoGroupEnabled,
            RuntimeException firstFailure
        ) {
            this.events = events;
            this.autoGroupEnabled = autoGroupEnabled;
            this.firstFailure = firstFailure;
        }

        @Override
        public void start() {
            events.add("section");
            if (autoGroupEnabled.getAsBoolean()) {
                events.add("dredgion");
            }
            if (autoGroupEnabled.getAsBoolean()) {
                events.add("asyunatar");
                if (events.size() == 3 && firstFailure != null) {
                    throw firstFailure;
                }
            }
        }
    }
}
