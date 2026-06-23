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
            chatEnabled -> events.add("chat:" + chatEnabled)
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
            chatEnabled -> events.add("chat:" + chatEnabled)
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
            chatEnabled -> {
                events.add("chat:" + chatEnabled);
                if (events.size() == 1) {
                    throw failure;
                }
            }
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
}
