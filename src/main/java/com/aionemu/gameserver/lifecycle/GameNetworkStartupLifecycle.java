package com.aionemu.gameserver.lifecycle;

import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.gameserver.ShutdownHook;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class GameNetworkStartupLifecycle {

    private final BooleanSupplier bootEmbedded;
    private final Supplier<Thread> shutdownHookSupplier;
    private final Consumer<Thread> shutdownHookRegistrar;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameNetworkStartupLifecycle() {
        this(
            AionRuntimeMode::isBootEmbedded,
            ShutdownHook::getInstance,
            Runtime.getRuntime()::addShutdownHook
        );
    }

    GameNetworkStartupLifecycle(
        BooleanSupplier bootEmbedded,
        Supplier<Thread> shutdownHookSupplier,
        Consumer<Thread> shutdownHookRegistrar
    ) {
        this.bootEmbedded = bootEmbedded;
        this.shutdownHookSupplier = shutdownHookSupplier;
        this.shutdownHookRegistrar = shutdownHookRegistrar;
    }

    public synchronized void start(Runnable serverStarter) {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            serverStarter.run();
            if (!bootEmbedded.getAsBoolean()) {
                shutdownHookRegistrar.accept(shutdownHookSupplier.get());
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
