package com.aionemu.gameserver.lifecycle;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameChatServerOverrideLifecycleTest {

    @Test
    void startAppliesChatServerOverrideOnce() {
        List<String> events = new ArrayList<>();
        GameChatServerOverrideLifecycle lifecycle = new RecordingGameChatServerOverrideLifecycle(events);

        lifecycle.start(true);
        lifecycle.start(false);

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("chat:true"), events);
        assertTrue(lifecycle.getLoadTimeMillis() >= 0);
		assertNull(lifecycle.getLastFailure());
    }

    @Test
    void startSkipsOverrideWhenValueIsNull() {
        List<String> events = new ArrayList<>();
        GameChatServerOverrideLifecycle lifecycle = new RecordingGameChatServerOverrideLifecycle(events);

        lifecycle.start(null);

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of(), events);
    }

    @Test
    void failedStartRecordsFailureAndAllowsRetry() {
        List<String> events = new ArrayList<>();
        IllegalStateException failure = new IllegalStateException("chat override failed");
        GameChatServerOverrideLifecycle lifecycle = new RecordingGameChatServerOverrideLifecycle(events, failure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> lifecycle.start(true));

        assertSame(failure, thrown);
        assertSame(failure, lifecycle.getLastFailure());
        assertFalse(lifecycle.isLoaded());

        lifecycle.start(false);

        assertTrue(lifecycle.isLoaded());
        assertEquals(List.of("chat:true", "chat:false"), events);
		assertNull(lifecycle.getLastFailure());
    }

    private static final class RecordingGameChatServerOverrideLifecycle extends GameChatServerOverrideLifecycle {

        private final List<String> events;
        private final RuntimeException failure;

        private RecordingGameChatServerOverrideLifecycle(List<String> events) {
            this(events, null);
        }

        private RecordingGameChatServerOverrideLifecycle(List<String> events, RuntimeException failure) {
            this.events = events;
            this.failure = failure;
        }

        @Override
        protected void overrideChatServerEnabled(boolean chatServerEnabled) {
            events.add("chat:" + chatServerEnabled);
            if (failure != null && events.size() == 1) {
                throw failure;
            }
        }
    }
}
