package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.Util;
import org.springframework.stereotype.Component;

@Component
public class GameSpawnLifecycle {

    private final Runnable sectionPrinter;
    private final Runnable spawner;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameSpawnLifecycle() {
        this(() -> Util.printSection(" *** Spawns *** "), SpawnEngine::spawnAll);
    }

    GameSpawnLifecycle(Runnable sectionPrinter, Runnable spawner) {
        this.sectionPrinter = sectionPrinter;
        this.spawner = spawner;
    }

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            sectionPrinter.run();
            spawner.run();
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
