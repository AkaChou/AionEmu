package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.ServerCommandProcessor;
import com.aionemu.gameserver.configs.main.GSConfig;
import java.util.function.BooleanSupplier;
import org.springframework.stereotype.Component;

@Component
public class GameAdminPanelLifecycle {

    private final BooleanSupplier adminPanelEnabled;
    private final Runnable adminPanelStarter;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameAdminPanelLifecycle() {
        this(
            () -> GSConfig.SERVER_YAADMINPANEL_SWITCH_ON,
            () -> new ServerCommandProcessor().startAdminPanel()
        );
    }

    GameAdminPanelLifecycle(BooleanSupplier adminPanelEnabled, Runnable adminPanelStarter) {
        this.adminPanelEnabled = adminPanelEnabled;
        this.adminPanelStarter = adminPanelStarter;
    }

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            if (adminPanelEnabled.getAsBoolean()) {
                adminPanelStarter.run();
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
