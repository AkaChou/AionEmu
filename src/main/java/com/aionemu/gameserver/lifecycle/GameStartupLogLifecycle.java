package com.aionemu.gameserver.lifecycle;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameStartupLogLifecycle {

    private final GameStartupLogGateway startupLogGateway;
    private boolean loaded;
    private long startupTimeMillis = -1;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public synchronized long start() {
        if (loaded) {
            return startupTimeMillis;
        }

        long start = System.currentTimeMillis();
        try {
            startupTimeMillis = startupLogGateway.start();
            loaded = true;
            lastFailure = null;
            return startupTimeMillis;
        } catch (RuntimeException | Error e) {
            loaded = false;
            startupTimeMillis = -1;
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
