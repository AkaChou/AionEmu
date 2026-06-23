package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.DatabaseCleaningService;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GameCleaningLifecycle {

    private final List<Runnable> cleaners;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameCleaningLifecycle() {
        this(List.of(
            DatabaseCleaningService::getInstance,
            AbyssRankCleaningService::getInstance
        ));
    }

    GameCleaningLifecycle(List<Runnable> cleaners) {
        this.cleaners = List.copyOf(cleaners);
    }

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            cleaners.forEach(Runnable::run);
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
