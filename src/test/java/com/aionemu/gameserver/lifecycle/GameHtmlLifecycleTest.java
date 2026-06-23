package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameHtmlLifecycleTest {

    @Test
    void startRunsInitializerOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameHtmlLifecycle lifecycle = new GameHtmlLifecycle(
            () -> events.add("section"),
            () -> events.add("html")
        );

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "html"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("html failed");
        GameHtmlLifecycle lifecycle = new GameHtmlLifecycle(() -> events.add("section"), () -> {
            events.add("html");
            if (events.size() == 2) {
                throw failure;
            }
        });

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "html", "section", "html"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }
}
