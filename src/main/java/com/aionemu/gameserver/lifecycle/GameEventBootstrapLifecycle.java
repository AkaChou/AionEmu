package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.events.AtreianPassportService;
import com.aionemu.gameserver.services.events.EventWindowService;
import com.aionemu.gameserver.services.events.ShugoSweepService;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.toypet.MinionService;
import com.aionemu.gameserver.utils.Util;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GameEventBootstrapLifecycle {

    private final List<Runnable> bootstrappers;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameEventBootstrapLifecycle() {
        this(List.of(
            () -> Util.printSection(" *** Luna Shop System *** "),
            () -> LunaShopService.getInstance().init(),
            () -> Util.printSection(" *** Minion System *** "),
            () -> MinionService.getInstance().init(),
            () -> Util.printSection(" *** Shugo Sweep System *** "),
            () -> ShugoSweepService.getInstance().initShugoSweep(),
            () -> Util.printSection(" *** Atreian Passport System *** "),
            () -> AtreianPassportService.getInstance().onStart(),
            () -> Util.printSection(" *** Event Window System *** "),
            () -> EventWindowService.getInstance().initialize()
        ));
    }

    GameEventBootstrapLifecycle(List<Runnable> bootstrappers) {
        this.bootstrappers = List.copyOf(bootstrappers);
    }

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            bootstrappers.forEach(Runnable::run);
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
