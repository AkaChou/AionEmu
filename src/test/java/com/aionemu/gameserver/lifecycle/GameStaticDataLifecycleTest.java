package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class GameStaticDataLifecycleTest {

    @Test
    void usesStaticDataGatewayCollaborator() {
        assertEquals(GameStaticDataGateway.class, fieldType("staticDataGateway"));
    }

    @Test
    void startLoadsStaticDataOnceAndRecordsLoadTime() {
        AtomicInteger loads = new AtomicInteger();
        GameStaticDataLifecycle lifecycle = new GameStaticDataLifecycle(new RecordingGameStaticDataGateway(loads, null));

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(1, loads.get());
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        AtomicInteger loads = new AtomicInteger();
        IllegalStateException failure = new IllegalStateException("static data failed");
        GameStaticDataLifecycle lifecycle = new GameStaticDataLifecycle(new RecordingGameStaticDataGateway(loads, failure));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(2, loads.get());
        assertEquals(null, lifecycle.getLastFailure());
    }

    private static final class RecordingGameStaticDataGateway extends GameStaticDataGateway {

        private final AtomicInteger loads;
        private final RuntimeException firstFailure;

        private RecordingGameStaticDataGateway(AtomicInteger loads, RuntimeException firstFailure) {
            this.loads = loads;
            this.firstFailure = firstFailure;
        }

        @Override
        public void load() {
            if (loads.incrementAndGet() == 1 && firstFailure != null) {
                throw firstFailure;
            }
        }
    }

    private static Class<?> fieldType(String name) {
        try {
            return GameStaticDataLifecycle.class.getDeclaredField(name).getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }
}
