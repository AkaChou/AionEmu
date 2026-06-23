package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.GSConfig;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GameChatServerOverrideLifecycle {

    private final Consumer<Boolean> overrideAction;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameChatServerOverrideLifecycle() {
        this(chatServerEnabled -> {
            GSConfig.ENABLE_CHAT_SERVER = chatServerEnabled;
            log.info("Chat Server connection overridden by boot configuration: {}", chatServerEnabled);
        });
    }

    GameChatServerOverrideLifecycle(Consumer<Boolean> overrideAction) {
        this.overrideAction = overrideAction;
    }

    public synchronized void start(Boolean chatServerEnabledOverride) {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            if (chatServerEnabledOverride != null) {
                overrideAction.accept(chatServerEnabledOverride);
            }
            loaded = true;
            lastFailure = null;
        } catch (RuntimeException | Error e) {
            loaded = false;
            lastFailure = e;
            throw e;
        } finally {
            loadTimeMillis = System.currentTimeMillis() - start;
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
