package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.EventsConfig;
import com.aionemu.gameserver.services.events.PigPoppyEventService;
import com.aionemu.gameserver.services.events.TreasureAbyssService;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import java.util.function.BooleanSupplier;
import org.springframework.stereotype.Component;

@Component
public class GameScheduledServicesLifecycle {

    private final BooleanSupplier pigPoppyEventEnabled;
    private final Runnable pigPoppyScheduler;
    private final BooleanSupplier abyssEventEnabled;
    private final Runnable abyssEventScheduler;
    private final BooleanSupplier imperialTombEnabled;
    private final Runnable imperialTombStarter;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameScheduledServicesLifecycle() {
        this(
            () -> EventsConfig.ENABLE_PIG_POPPY_EVENT,
            PigPoppyEventService::ScheduleCron,
            () -> EventsConfig.ENABLE_ABYSS_EVENT,
            TreasureAbyssService::ScheduleCron,
            () -> EventsConfig.IMPERIAL_TOMB_ENABLE,
            () -> ShugoImperialTombSpawnManager.getInstance().start()
        );
    }

    GameScheduledServicesLifecycle(
        BooleanSupplier pigPoppyEventEnabled,
        Runnable pigPoppyScheduler,
        BooleanSupplier abyssEventEnabled,
        Runnable abyssEventScheduler,
        BooleanSupplier imperialTombEnabled,
        Runnable imperialTombStarter
    ) {
        this.pigPoppyEventEnabled = pigPoppyEventEnabled;
        this.pigPoppyScheduler = pigPoppyScheduler;
        this.abyssEventEnabled = abyssEventEnabled;
        this.abyssEventScheduler = abyssEventScheduler;
        this.imperialTombEnabled = imperialTombEnabled;
        this.imperialTombStarter = imperialTombStarter;
    }

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            if (pigPoppyEventEnabled.getAsBoolean()) {
                pigPoppyScheduler.run();
            }
            if (abyssEventEnabled.getAsBoolean()) {
                abyssEventScheduler.run();
            }
            if (imperialTombEnabled.getAsBoolean()) {
                imperialTombStarter.run();
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
