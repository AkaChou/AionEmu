package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameRatioLimitLifecycleTest {

    @Test
    void startRegistersRatioHookWhenRatioLimitationIsEnabled() {
        List<String> events = new ArrayList<>();
        GameRatioLimitLifecycle lifecycle = new GameRatioLimitLifecycle(
            new RecordingGameRatioLimitGateway(events, true)
        );

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("registerRatioHook"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void startSkipsRatioHookWhenRatioLimitationIsDisabled() {
        List<String> events = new ArrayList<>();
        GameRatioLimitLifecycle lifecycle = new GameRatioLimitLifecycle(
            new RecordingGameRatioLimitGateway(events, false)
        );

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("ratio hook failed");
        GameRatioLimitLifecycle lifecycle = new GameRatioLimitLifecycle(
            new RecordingGameRatioLimitGateway(events, true, failure)
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("registerRatioHook", "registerRatioHook"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void usesRatioLimitGatewayCollaborator() {
        assertEquals(GameRatioLimitGateway.class, fieldType("ratioLimitGateway"));
    }

    private static Class<?> fieldType(String name) {
        try {
            return GameRatioLimitLifecycle.class.getDeclaredField(name).getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameRatioLimitGateway extends GameRatioLimitGateway {

        private final List<String> events;
        private final boolean enabled;
        private final RuntimeException failure;
        private long currentTimeMillis;

        private RecordingGameRatioLimitGateway(List<String> events, boolean enabled) {
            this(events, enabled, null);
        }

        private RecordingGameRatioLimitGateway(List<String> events, boolean enabled, RuntimeException failure) {
            this.events = events;
            this.enabled = enabled;
            this.failure = failure;
        }

        @Override
        public boolean isRatioLimitationEnabled() {
            return enabled;
        }

        @Override
        public void registerRatioLimitStartupHook() {
            events.add("registerRatioHook");
            if (failure != null && events.size() == 1) {
                throw failure;
            }
        }

        @Override
        public long currentTimeMillis() {
            return currentTimeMillis++;
        }
    }
}
