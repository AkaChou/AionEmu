package com.aionemu.gameserver.lifecycle;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameChatServerOverrideLifecycle {

    private final GameChatServerOverrideGateway chatServerOverrideGateway;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public synchronized void start(Boolean chatServerEnabledOverride) {
        if (loaded) {
            return;
        }

        long start = chatServerOverrideGateway.currentTimeMillis();
        try {
            if (chatServerEnabledOverride != null) {
                chatServerOverrideGateway.overrideChatServerEnabled(chatServerEnabledOverride);
            }
            loaded = true;
            lastFailure = null;
        } catch (RuntimeException | Error e) {
            loaded = false;
            lastFailure = e;
            throw e;
        } finally {
            loadTimeMillis = chatServerOverrideGateway.currentTimeMillis() - start;
        }
    }

    public synchronized boolean isLoaded() {
        return loaded;
    }

    public synchronized long getLoadTimeMillis() {
        return loadTimeMillis;
    }

    public synchronized Throwable getLastFailure() {
        return lastFailure;
    }
}
