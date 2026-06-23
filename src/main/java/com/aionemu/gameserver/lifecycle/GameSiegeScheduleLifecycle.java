package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.BaseService;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.utils.Util;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GameSiegeScheduleLifecycle {

    private final Runnable sectionPrinter;
    private final List<Runnable> initializers;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameSiegeScheduleLifecycle() {
        this(
            () -> Util.printSection(" *** Sieges *** "),
            List.of(
                () -> SiegeService.getInstance().initSieges(),
                () -> BaseService.getInstance().initBases()
            )
        );
    }

    GameSiegeScheduleLifecycle(Runnable sectionPrinter, List<Runnable> initializers) {
        this.sectionPrinter = sectionPrinter;
        this.initializers = List.copyOf(initializers);
    }

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            sectionPrinter.run();
            initializers.forEach(Runnable::run);
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
