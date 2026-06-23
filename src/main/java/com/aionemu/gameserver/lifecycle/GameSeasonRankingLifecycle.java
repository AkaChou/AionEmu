package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.stereotype.Component;

@Component
public class GameSeasonRankingLifecycle {

    private final Runnable sectionPrinter;
    private final Runnable initializer;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameSeasonRankingLifecycle() {
        this(
            () -> Util.printSection(" *** Season Ranking *** "),
            () -> SeasonRankingUpdateService.getInstance().onStart()
        );
    }

    GameSeasonRankingLifecycle(Runnable sectionPrinter, Runnable initializer) {
        this.sectionPrinter = sectionPrinter;
        this.initializer = initializer;
    }

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            sectionPrinter.run();
            initializer.run();
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
