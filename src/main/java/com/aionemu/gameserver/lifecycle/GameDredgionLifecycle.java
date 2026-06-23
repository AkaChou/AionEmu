package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.AutoGroupConfig;
import com.aionemu.gameserver.services.instance.AsyunatarService;
import com.aionemu.gameserver.services.instance.DredgionService2;
import com.aionemu.gameserver.utils.Util;
import java.util.function.BooleanSupplier;
import org.springframework.stereotype.Component;

@Component
public class GameDredgionLifecycle {

    private final Runnable sectionPrinter;
    private final BooleanSupplier autoGroupEnabled;
    private final Runnable dredgionInitializer;
    private final Runnable asyunatarInitializer;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameDredgionLifecycle() {
        this(
            () -> Util.printSection(" *** Dredgion *** "),
            () -> AutoGroupConfig.AUTO_GROUP_ENABLED,
            () -> DredgionService2.getInstance().initDredgion(),
            () -> AsyunatarService.getInstance().initAsyunatar()
        );
    }

    GameDredgionLifecycle(
        Runnable sectionPrinter,
        BooleanSupplier autoGroupEnabled,
        Runnable dredgionInitializer,
        Runnable asyunatarInitializer
    ) {
        this.sectionPrinter = sectionPrinter;
        this.autoGroupEnabled = autoGroupEnabled;
        this.dredgionInitializer = dredgionInitializer;
        this.asyunatarInitializer = asyunatarInitializer;
    }

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            sectionPrinter.run();
            if (autoGroupEnabled.getAsBoolean()) {
                dredgionInitializer.run();
            }
            if (autoGroupEnabled.getAsBoolean()) {
                asyunatarInitializer.run();
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
