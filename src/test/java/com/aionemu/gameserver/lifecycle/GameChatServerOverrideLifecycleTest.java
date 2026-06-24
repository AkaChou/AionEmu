package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameChatServerOverrideLifecycleTest {

    @Test
    void startAppliesChatServerOverrideOnce() {
        List<String> events = new ArrayList<>();
        GameChatServerOverrideLifecycle lifecycle = new GameChatServerOverrideLifecycle(
            new RecordingGameChatServerOverrideGateway(events)
        );

        lifecycle.start(true);
        lifecycle.start(false);

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("chat:true"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void startSkipsOverrideWhenValueIsNull() {
        List<String> events = new ArrayList<>();
        GameChatServerOverrideLifecycle lifecycle = new GameChatServerOverrideLifecycle(
            new RecordingGameChatServerOverrideGateway(events)
        );

        lifecycle.start(null);

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("chat override failed");
        GameChatServerOverrideLifecycle lifecycle = new GameChatServerOverrideLifecycle(
            new RecordingGameChatServerOverrideGateway(events, failure)
        );

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> lifecycle.start(true));

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start(false);

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("chat:true", "chat:false"), events);
        assertEquals(null, lifecycle.getLastFailure());
    }

    @Test
    void usesChatServerOverrideGatewayCollaborator() {
        assertEquals(GameChatServerOverrideGateway.class, fieldType("chatServerOverrideGateway"));
    }

    private static Class<?> fieldType(String name) {
        try {
            return GameChatServerOverrideLifecycle.class.getDeclaredField(name).getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameChatServerOverrideGateway extends GameChatServerOverrideGateway {

        private final List<String> events;
        private final RuntimeException failure;
        private long currentTimeMillis;

        private RecordingGameChatServerOverrideGateway(List<String> events) {
            this(events, null);
        }

        private RecordingGameChatServerOverrideGateway(List<String> events, RuntimeException failure) {
            this.events = events;
            this.failure = failure;
        }

        @Override
        public void overrideChatServerEnabled(boolean chatServerEnabled) {
            events.add("chat:" + chatServerEnabled);
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
