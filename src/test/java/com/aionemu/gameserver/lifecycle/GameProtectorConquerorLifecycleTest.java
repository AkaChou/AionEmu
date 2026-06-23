package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameProtectorConquerorLifecycleTest {

    @Test
    void startRunsInitializerOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameProtectorConquerorLifecycle lifecycle = new GameProtectorConquerorLifecycle(
            () -> events.add("protectorConqueror")
        );

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("protectorConqueror"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("protector conqueror failed");
        GameProtectorConquerorLifecycle lifecycle = new GameProtectorConquerorLifecycle(() -> {
            events.add("protectorConqueror");
            if (events.size() == 1) {
                throw failure;
            }
        });

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("protectorConqueror", "protectorConqueror"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }
}
