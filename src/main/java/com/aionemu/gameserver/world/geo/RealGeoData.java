package com.aionemu.gameserver.world.geo;

import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.geoEngine.GeoWorldLoader;
import com.aionemu.gameserver.geoEngine.models.GeoMap;
import com.aionemu.gameserver.geoEngine.scene.Spatial;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.utils.ConsoleProgressLineRenderer;

import com.aionemu.commons.utils.collections.IntObjectHashMap;
@Slf4j

public class RealGeoData implements GeoData {
    private final IntObjectHashMap<GeoMap> geoMaps = new IntObjectHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Override
    public void loadGeoMaps() {
        final Map<String, Spatial> models = loadMeshes();
        loadWorldMaps(models);
        models.clear();
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        log.info("Geodata: {} geo maps loaded!", geoMaps.size());
    }

    protected void loadWorldMaps(final Map<String, Spatial> models) {
        log.info("Loading geo maps..");
        int totalMaps = DataManager.WORLD_MAPS_DATA.size();
        ConsoleProgressLineRenderer progressRenderer = new ConsoleProgressLineRenderer(System.out, true);
        AtomicInteger completedMaps = new AtomicInteger();
        final List<Integer> mapsWithErrors = new ArrayList<>();
        List<Callable<Void>> tasks = new ArrayList<>();

        for (final WorldMapTemplate map : DataManager.WORLD_MAPS_DATA) {
            tasks.add(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    int mapId = map.getMapId();
                    int worldSize = map.getWorldSize();
                    GeoMap geoMap = new GeoMap(String.valueOf(mapId), worldSize);
                    try {
                        if (GeoWorldLoader.loadWorld(mapId, models, geoMap)) {
                            synchronized (geoMaps) {
                                geoMaps.put(mapId, geoMap);
                            }
                        }
                    } catch (Throwable t) {
                        synchronized (mapsWithErrors) {
                            mapsWithErrors.add(mapId);
                            geoMaps.put(mapId, DummyGeoData.DUMMY_MAP);
                        }
                    }
                    progressRenderer.progress("GeoMaps", completedMaps.incrementAndGet(), totalMaps);
                    return null;
                }
            });
        }

        try {
            List<Future<Void>> futures = executorService.invokeAll(tasks);
            for (Future<Void> future : futures) {
                future.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error during geo map loading", e);
        }

        progressRenderer.finished("GeoMaps", totalMaps);
        if (!mapsWithErrors.isEmpty()) {
            log.warn("Some maps were not loaded correctly and reverted to dummy implementation: {}", mapsWithErrors);
        }
    }

    protected Map<String, Spatial> loadMeshes() {
        log.info("Loading meshes..");
        try {
            return GeoWorldLoader.loadMeshs("data/geo/meshs.geo");
        } catch (IOException e) {
            throw new IllegalStateException("Problem loading meshes", e);
        }
    }

    @Override
    public GeoMap getMap(int worldId) {
        GeoMap geoMap = geoMaps.get(worldId);
        return geoMap != null ? geoMap : DummyGeoData.DUMMY_MAP;
    }
}
