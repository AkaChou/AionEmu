package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.nav.NavService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.stereotype.Component;

@Component
public class GameGeoNavLifecycle {

    private final Runnable sectionPrinter;
    private final Runnable geoInitializer;
    private final Runnable navInitializer;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameGeoNavLifecycle() {
        this(
            () -> Util.printSection(" *** Geodata *** "),
            () -> GeoService.getInstance().initializeGeo(),
            () -> NavService.getInstance().initializeNav()
        );
    }

    GameGeoNavLifecycle(Runnable sectionPrinter, Runnable geoInitializer, Runnable navInitializer) {
        this.sectionPrinter = sectionPrinter;
        this.geoInitializer = geoInitializer;
        this.navInitializer = navInitializer;
    }

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            sectionPrinter.run();
            geoInitializer.run();
            navInitializer.run();
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
