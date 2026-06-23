package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.GameServer;
import java.util.function.LongSupplier;
import org.springframework.stereotype.Component;

@Component
public class GameStartupLogLifecycle {

    private final LongSupplier currentTimeMillis;
    private final Runnable startupLogger;
    private boolean loaded;
    private long startupTimeMillis = -1;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameStartupLogLifecycle() {
        this(System::currentTimeMillis, () -> GameServer.log.info("GameServer starting..."));
    }

    GameStartupLogLifecycle(LongSupplier currentTimeMillis, Runnable startupLogger) {
        this.currentTimeMillis = currentTimeMillis;
        this.startupLogger = startupLogger;
    }

    public synchronized long start() {
        if (loaded) {
            return startupTimeMillis;
        }

        long start = System.currentTimeMillis();
        try {
            startupTimeMillis = currentTimeMillis.getAsLong();
            startupLogger.run();
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
