package com.aionemu.gameserver.lifecycle;

import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.gameserver.ShutdownHook;
import com.aionemu.gameserver.utils.Util;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GameNetworkStartupLifecycle {

    private static final Logger log = LoggerFactory.getLogger(GameNetworkStartupLifecycle.class);

    private final Runnable sectionPrinter;
    private final Runnable miscSectionPrinter;
    private final BooleanSupplier bootEmbedded;
    private final Supplier<Thread> shutdownHookSupplier;
    private final Consumer<Thread> shutdownHookRegistrar;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameNetworkStartupLifecycle() {
        this(
            () -> Util.printSection(" *** Network *** "),
            () -> Util.printSection(" *** Misc *** "),
            AionRuntimeMode::isBootEmbedded,
            ShutdownHook::getInstance,
            Runtime.getRuntime()::addShutdownHook
        );
    }

    GameNetworkStartupLifecycle(
        Runnable sectionPrinter,
        Runnable miscSectionPrinter,
        BooleanSupplier bootEmbedded,
        Supplier<Thread> shutdownHookSupplier,
        Consumer<Thread> shutdownHookRegistrar
    ) {
        this.sectionPrinter = sectionPrinter;
        this.miscSectionPrinter = miscSectionPrinter;
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
            sectionPrinter.run();
            serverStarter.run();
            miscSectionPrinter.run();
            boolean bootEmbeddedMode = bootEmbedded.getAsBoolean();
            log.info(bootEmbeddedMode ? "Network transport started and external server connections scheduled" : "All network servers started successfully");
            if (!bootEmbeddedMode) {
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
