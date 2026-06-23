package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.GameServer;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class GameUtilityServicesLifecycle {

    private final Consumer<GameThreadPoolLifecycle> initializer;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameUtilityServicesLifecycle() {
        this(GameServer::initializeUtilityServicesAndConfig);
    }

    GameUtilityServicesLifecycle(Consumer<GameThreadPoolLifecycle> initializer) {
        this.initializer = initializer;
    }

    public synchronized void start(GameThreadPoolLifecycle threadPoolLifecycle) {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            initializer.accept(threadPoolLifecycle);
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
