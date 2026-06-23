package com.aionemu.gameserver.lifecycle;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameSystemLifecycle {

    private final GameSystemGateway systemGateway;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private long startupTimeSeconds = -1;
    private Throwable lastFailure;

    public synchronized long start(long serverStartTimeMillis) {
        if (loaded) {
            return startupTimeSeconds;
        }

        long start = System.currentTimeMillis();
        try {
            startupTimeSeconds = systemGateway.start(serverStartTimeMillis);
            loaded = true;
            lastFailure = null;
            return startupTimeSeconds;
        } catch (RuntimeException | Error e) {
            loaded = false;
            startupTimeSeconds = -1;
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

    public synchronized long getStartupTimeSeconds() {
        return startupTimeSeconds;
    }

    public synchronized Throwable getLastFailure() {
        return lastFailure;
    }
}
