package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class GameThreadPoolLifecycleTest {

    @Test
    void usesThreadPoolGatewayCollaborator() {
        assertEquals(GameThreadPoolGateway.class, fieldType("threadPoolGateway"));
    }

    @Test
    void threadPoolGatewayBridgesLegacyManagerThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameThreadPoolGateway.class, "threadPoolManagerProvider"));
    }

    @Test
    void startAndStopAreIdempotent() {
        AtomicInteger starts = new AtomicInteger();
        AtomicInteger stops = new AtomicInteger();
        GameThreadPoolLifecycle lifecycle = new GameThreadPoolLifecycle(new RecordingGameThreadPoolGateway(starts, stops));

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isStarted());
        assertEquals(1, starts.get());

        lifecycle.stop();
        lifecycle.stop();

        assertFalse(lifecycle.isStarted());
        assertEquals(1, stops.get());
    }

    @Test
    void stopBeforeStartDoesNotRunShutdown() {
        AtomicInteger stops = new AtomicInteger();
        GameThreadPoolLifecycle lifecycle = new GameThreadPoolLifecycle(new RecordingGameThreadPoolGateway(new AtomicInteger(), stops));

        lifecycle.stop();

        assertFalse(lifecycle.isStarted());
        assertEquals(0, stops.get());
    }

    private static Class<?> fieldType(String name) {
        try {
            Field field = GameThreadPoolLifecycle.class.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
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

    private static final class RecordingGameThreadPoolGateway extends GameThreadPoolGateway {

        private final AtomicInteger starts;
        private final AtomicInteger stops;

        private RecordingGameThreadPoolGateway(AtomicInteger starts, AtomicInteger stops) {
            this.starts = starts;
            this.stops = stops;
        }

        @Override
        public void start() {
            starts.incrementAndGet();
        }

        @Override
        public void stop() {
            stops.incrementAndGet();
        }
    }
}
