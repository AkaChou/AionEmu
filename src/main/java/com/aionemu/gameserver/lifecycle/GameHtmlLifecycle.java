package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.cache.HTMLCache;
import org.springframework.stereotype.Component;

@Component
public class GameHtmlLifecycle {

    private final Runnable initializer;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameHtmlLifecycle() {
        this(HTMLCache::getInstance);
    }

    GameHtmlLifecycle(Runnable initializer) {
        this.initializer = initializer;
    }

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            initializer.run();
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
