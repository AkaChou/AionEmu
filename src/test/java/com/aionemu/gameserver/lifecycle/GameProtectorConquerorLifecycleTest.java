package com.aionemu.gameserver.lifecycle;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;

class GameProtectorConquerorLifecycleTest {

    @Test
    void usesProtectorConquerorGatewayCollaborator() {
        assertEquals(GameProtectorConquerorGateway.class, fieldType("protectorConquerorGateway"));
    }

    @Test
    void protectorConquerorGatewayBridgesLegacyServiceThroughSpringProvider() {
        assertEquals(ObjectProvider.class, fieldType(GameProtectorConquerorGateway.class, "protectorConquerorServiceProvider"));
    }

    @Test
    void protectorConquerorGatewayBridgesLegacyFallbackThroughRuntimeBridgeProvider() {
        assertEquals(ObjectProvider.class, fieldType(GameProtectorConquerorGateway.class, "runtimeBridgeProvider"));
    }

    @Test
    void startRunsInitializerOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameProtectorConquerorLifecycle lifecycle = new GameProtectorConquerorLifecycle(
            new RecordingGameProtectorConquerorGateway(events, null)
        );

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "protectorConqueror"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("protector conqueror failed");
        GameProtectorConquerorLifecycle lifecycle = new GameProtectorConquerorLifecycle(
            new RecordingGameProtectorConquerorGateway(events, failure)
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "protectorConqueror", "section", "protectorConqueror"), events);
		assertNull(lifecycle.getLastFailure());
    }

    private static Class<?> fieldType(String name) {
        return fieldType(GameProtectorConquerorLifecycle.class, name);
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameProtectorConquerorGateway extends GameProtectorConquerorGateway {

        private final List<String> events;
        private final RuntimeException firstFailure;

        private RecordingGameProtectorConquerorGateway(List<String> events, RuntimeException firstFailure) {
            this.events = events;
            this.firstFailure = firstFailure;
        }

        @Override
        public void start() {
            events.add("section");
            events.add("protectorConqueror");
            if (events.size() == 2 && firstFailure != null) {
                throw firstFailure;
            }
        }
    }
}
