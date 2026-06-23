package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.FFAConfig;
import com.aionemu.gameserver.configs.main.PvPModConfig;
import com.aionemu.gameserver.services.events.BGService;
import com.aionemu.gameserver.services.events.BanditService;
import com.aionemu.gameserver.services.events.FFAService;
import com.aionemu.gameserver.services.events.LadderService;
import java.util.function.BooleanSupplier;
import org.springframework.stereotype.Component;

@Component
public class GameCustomEventsLifecycle {

    private final BooleanSupplier ffaEnabled;
    private final Runnable ffaInitializer;
    private final BooleanSupplier battlegroundEnabled;
    private final Runnable ladderInitializer;
    private final Runnable battlegroundInitializer;
    private final Runnable banditInitializer;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameCustomEventsLifecycle() {
        this(
            () -> FFAConfig.FFA_ENABLED,
            FFAService::getInstance,
            () -> PvPModConfig.BG_ENABLED,
            LadderService::getInstance,
            BGService::getInstance,
            () -> BanditService.getInstance().onInit()
        );
    }

    GameCustomEventsLifecycle(
        BooleanSupplier ffaEnabled,
        Runnable ffaInitializer,
        BooleanSupplier battlegroundEnabled,
        Runnable ladderInitializer,
        Runnable battlegroundInitializer,
        Runnable banditInitializer
    ) {
        this.ffaEnabled = ffaEnabled;
        this.ffaInitializer = ffaInitializer;
        this.battlegroundEnabled = battlegroundEnabled;
        this.ladderInitializer = ladderInitializer;
        this.battlegroundInitializer = battlegroundInitializer;
        this.banditInitializer = banditInitializer;
    }

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            if (ffaEnabled.getAsBoolean()) {
                ffaInitializer.run();
            }
            if (battlegroundEnabled.getAsBoolean()) {
                ladderInitializer.run();
                battlegroundInitializer.run();
            }
            banditInitializer.run();
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
