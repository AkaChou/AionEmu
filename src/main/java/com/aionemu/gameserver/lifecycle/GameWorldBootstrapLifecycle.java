package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.RoadService;
import com.aionemu.gameserver.services.teleport.HotspotTeleportService;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.zone.ZoneService;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GameWorldBootstrapLifecycle {

    private final List<Runnable> bootstrappers;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameWorldBootstrapLifecycle() {
        this(List.of(
            () -> Util.printSection(" *** IDFactory *** "),
            () -> IDFactory.getInstance(),
            () -> Util.printSection(" *** Zone *** "),
            () -> ZoneService.getInstance().load(null),
            () -> HotspotTeleportService.getInstance(),
            () -> RoadService.getInstance(),
            () -> World.getInstance()
        ));
    }

    GameWorldBootstrapLifecycle(List<Runnable> bootstrappers) {
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
