package com.aionemu.gameserver.lifecycle;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameHtmlLifecycle {

    private final GameHtmlGateway htmlGateway;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            htmlGateway.start();
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
