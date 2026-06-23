package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.configs.main.GSConfig;
import java.util.function.BooleanSupplier;
import org.springframework.stereotype.Component;

@Component
public class GameRatioLimitLifecycle {

    private final BooleanSupplier ratioLimitationEnabled;
    private final Runnable ratioHookRegistrar;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameRatioLimitLifecycle() {
        this(
            () -> GSConfig.ENABLE_RATIO_LIMITATION,
            GameServer::registerRatioLimitStartupHook
        );
    }

    GameRatioLimitLifecycle(BooleanSupplier ratioLimitationEnabled, Runnable ratioHookRegistrar) {
        this.ratioLimitationEnabled = ratioLimitationEnabled;
        this.ratioHookRegistrar = ratioHookRegistrar;
    }

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            if (ratioLimitationEnabled.getAsBoolean()) {
                ratioHookRegistrar.run();
            }
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
