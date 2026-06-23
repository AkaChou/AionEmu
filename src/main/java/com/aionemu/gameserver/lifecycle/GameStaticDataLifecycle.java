package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.dataholders.DataManager;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class GameStaticDataLifecycle {

    private final Supplier<DataManager> dataManagerSupplier;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameStaticDataLifecycle() {
        this(DataManager::getInstance);
    }

    GameStaticDataLifecycle(Supplier<DataManager> dataManagerSupplier) {
        this.dataManagerSupplier = dataManagerSupplier;
    }

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            dataManagerSupplier.get();
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
