package com.aionemu.gameserver.lifecycle;

import org.springframework.stereotype.Component;

@Component
public class GameSystemPropertiesLifecycle {

    private final Runnable propertyInitializer;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameSystemPropertiesLifecycle() {
        this(() -> {
            System.setProperty("file.encoding", "UTF-8");
            System.setProperty("java.net.preferIPv4Stack", "true");
            System.setProperty("java.net.preferIPv6Addresses", "false");
        });
    }

    GameSystemPropertiesLifecycle(Runnable propertyInitializer) {
        this.propertyInitializer = propertyInitializer;
    }

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            propertyInitializer.run();
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
