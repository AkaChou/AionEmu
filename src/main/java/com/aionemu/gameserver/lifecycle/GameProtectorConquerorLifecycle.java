package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.ProtectorConquerorService;
import org.springframework.stereotype.Component;

@Component
public class GameProtectorConquerorLifecycle {

    private final Runnable initializer;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameProtectorConquerorLifecycle() {
        this(() -> ProtectorConquerorService.getInstance().initSystem());
    }

    GameProtectorConquerorLifecycle(Runnable initializer) {
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
