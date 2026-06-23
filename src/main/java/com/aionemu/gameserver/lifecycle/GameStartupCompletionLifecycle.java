package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.GameServer;
import java.util.function.BiConsumer;
import org.springframework.stereotype.Component;

@Component
public class GameStartupCompletionLifecycle {

    private final BiConsumer<String, Object> logger;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameStartupCompletionLifecycle() {
        this((message, value) -> {
            if (value == null) {
                GameServer.log.info(message);
            } else {
                GameServer.log.info(message, value);
            }
        });
    }

    GameStartupCompletionLifecycle(BiConsumer<String, Object> logger) {
        this.logger = logger;
    }

    public synchronized void start(long startupTime) {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            logger.accept("=== Server initialization COMPLETE ===", null);
            logger.accept("Total initialization time: {} seconds", startupTime);
            logger.accept("Server is now ready to accept connections", null);
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
