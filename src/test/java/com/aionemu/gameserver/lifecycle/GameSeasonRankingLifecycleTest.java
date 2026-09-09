package com.aionemu.gameserver.lifecycle;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;

class GameSeasonRankingLifecycleTest {

    @Test
    void usesSeasonRankingGatewayCollaborator() {
        assertEquals(GameSeasonRankingGateway.class, fieldType("seasonRankingGateway"));
    }

    @Test
    void seasonRankingGatewayBridgesLegacyServiceThroughSpringProvider() {
        assertEquals(ObjectProvider.class, fieldType(GameSeasonRankingGateway.class, "seasonRankingUpdateServiceProvider"));
    }

    @Test
    void seasonRankingGatewayBridgesLegacyFallbackThroughRuntimeBridgeProvider() {
        assertEquals(ObjectProvider.class, fieldType(GameSeasonRankingGateway.class, "runtimeBridgeProvider"));
    }

    @Test
    void startRunsInitializerOnceAndRecordsLoadTime() {
        List<String> events = new ArrayList<>();
        GameSeasonRankingLifecycle lifecycle = new GameSeasonRankingLifecycle(
            new RecordingGameSeasonRankingGateway(events, null)
        );

        lifecycle.start();
        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "seasonRanking"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("season ranking failed");
        GameSeasonRankingLifecycle lifecycle = new GameSeasonRankingLifecycle(
            new RecordingGameSeasonRankingGateway(events, failure)
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, lifecycle::start);

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start();

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("section", "seasonRanking", "section", "seasonRanking"), events);
		assertNull(lifecycle.getLastFailure());
    }

    private static Class<?> fieldType(String name) {
        return fieldType(GameSeasonRankingLifecycle.class, name);
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            return field.getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameSeasonRankingGateway extends GameSeasonRankingGateway {

        private final List<String> events;
        private final RuntimeException firstFailure;

        private RecordingGameSeasonRankingGateway(List<String> events, RuntimeException firstFailure) {
            this.events = events;
            this.firstFailure = firstFailure;
        }

        @Override
        public void start() {
            events.add("section");
            events.add("seasonRanking");
            if (events.size() == 2 && firstFailure != null) {
                throw firstFailure;
            }
        }
    }
}
