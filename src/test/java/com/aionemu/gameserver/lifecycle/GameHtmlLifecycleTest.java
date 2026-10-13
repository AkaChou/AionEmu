package com.aionemu.gameserver.lifecycle;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;

class GameHtmlLifecycleTest {

    @Test
    void htmlLifecycleBridgesLegacyCacheThroughSpringProvider() {
        assertEquals(ObjectProvider.class, fieldType(GameHtmlLifecycle.class, "htmlCacheProvider"));
    }


    @Test
    void startRunsInitializerOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameHtmlLifecycle lifecycle = new RecordingGameHtmlLifecycle(events, null);

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "html"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("html failed");
        GameHtmlLifecycle lifecycle = new RecordingGameHtmlLifecycle(events, failure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "html", "section", "html"), events);
		assertNull(lifecycle.getLastFailure());
    }

    private static Class<?> fieldType(String name) {
        return fieldType(GameHtmlLifecycle.class, name);
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameHtmlLifecycle extends GameHtmlLifecycle {

        private final List<String> events;
        private final RuntimeException firstFailure;

        private RecordingGameHtmlLifecycle(List<String> events, RuntimeException firstFailure) {
            this.events = events;
            this.firstFailure = firstFailure;
        }

        @Override
        protected void runStage() {
            events.add("section");
            events.add("html");
            if (events.size() == 2 && firstFailure != null) {
                throw firstFailure;
            }
        }
    }
}
