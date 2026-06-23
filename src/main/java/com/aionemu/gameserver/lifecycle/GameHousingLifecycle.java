package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.services.ChallengeTaskService;
import com.aionemu.gameserver.services.HousingBidService;
import com.aionemu.gameserver.services.TownService;
import com.aionemu.gameserver.utils.Util;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GameHousingLifecycle {

    private final Runnable sectionPrinter;
    private final List<Runnable> initializers;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameHousingLifecycle() {
        this(
            () -> Util.printSection(" *** Housing *** "),
            List.of(
                () -> HousingBidService.getInstance().start(),
                MaintenanceTask::getInstance,
                TownService::getInstance,
                ChallengeTaskService::getInstance
            )
        );
    }

    GameHousingLifecycle(Runnable sectionPrinter, List<Runnable> initializers) {
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
