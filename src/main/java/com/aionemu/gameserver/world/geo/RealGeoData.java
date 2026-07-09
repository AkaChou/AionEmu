package com.aionemu.gameserver.world.geo;

import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.geoEngine.GeoWorldLoader;
import com.aionemu.gameserver.geoEngine.models.GeoMap;
import com.aionemu.gameserver.geoEngine.scene.Geometry;
import com.aionemu.gameserver.geoEngine.scene.Mesh;
import com.aionemu.gameserver.geoEngine.scene.Node;
import com.aionemu.gameserver.geoEngine.scene.Spatial;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.utils.ConsoleProgressLineRenderer;

import com.aionemu.commons.utils.collections.IntObjectHashMap;
@Slf4j

public class RealGeoData implements GeoData {
    private final IntObjectHashMap<GeoMap> geoMaps = new IntObjectHashMap<>();

    @Override
    public void loadGeoMaps() {
        final Map<String, Spatial> models = loadMeshes();
        loadWorldMaps(models);
        prebuildCollisionDataAsync(models);
        models.clear();
        log.info("Geodata: {} geo maps loaded!", geoMaps.size());
    }

    protected void loadWorldMaps(final Map<String, Spatial> models) {
        log.info("Loading geo maps..");
        int totalMaps = DataManager.WORLD_MAPS_DATA.size();
        ConsoleProgressLineRenderer progressRenderer = new ConsoleProgressLineRenderer(System.out, true);
        AtomicInteger completedMaps = new AtomicInteger();
        final List<Integer> mapsWithErrors = new ArrayList<>();
        final Set<String> missingMeshes = ConcurrentHashMap.newKeySet();
        List<Callable<Void>> tasks = new ArrayList<>();
        List<GeoMap> maps = new ArrayList<>();

        for (final WorldMapTemplate map : DataManager.WORLD_MAPS_DATA) {
            GeoMap geoMap = new GeoMap(String.valueOf(map.getMapId()), map.getWorldSize());
            maps.add(geoMap);
            geoMaps.put(map.getMapId(), geoMap);
        }
        try {
            GeoWorldLoader.loadTerrains(maps);
        } catch (IOException e) {
            throw new IllegalStateException("Problem loading terrains", e);
        }

        for (final WorldMapTemplate map : DataManager.WORLD_MAPS_DATA) {
            tasks.add(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    int mapId = map.getMapId();
                    GeoMap geoMap = geoMaps.get(mapId);
                    try {
                        GeoWorldLoader.loadWorldObjects(mapId, models, geoMap, missingMeshes);
                    } catch (Throwable t) {
                        log.error("Error loading geo map {}", mapId, t);
                        synchronized (mapsWithErrors) {
                            mapsWithErrors.add(mapId);
                        }
                    }
                    progressRenderer.progress("GeoMaps", completedMaps.incrementAndGet(), totalMaps);
                    return null;
                }
            });
        }

        try {
            List<Future<Void>> futures = GameThreadPoolServices.threadPoolManager().getForkingPool().invokeAll(tasks);
            for (Future<Void> future : futures) {
                future.get();
            }
        } catch (InterruptedException e) {
            log.error("Error during geo map loading", e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            log.error("Error during geo map loading", e);
        }

        progressRenderer.finished("GeoMaps", totalMaps);
        if (!mapsWithErrors.isEmpty()) {
            for (Integer mapId : mapsWithErrors) {
                geoMaps.put(mapId, DummyGeoData.DUMMY_MAP);
            }
            log.warn("Some maps were not loaded correctly and reverted to dummy implementation: {}", mapsWithErrors);
        }
        if (!missingMeshes.isEmpty()) {
            log.warn("{} meshes are missing:\n{}", missingMeshes.size(), missingMeshes.stream().sorted().collect(Collectors.joining("\n")));
        }
    }

    protected Map<String, Spatial> loadMeshes() {
        log.info("Loading meshes..");
        try {
            return GeoWorldLoader.loadMeshs("data/geo/models.mesh");
        } catch (IOException e) {
            throw new IllegalStateException("Problem loading meshes", e);
        }
    }

    // 碰撞树后台预构建；Mesh.collideWith 有懒加载兜底，此步仅为降低运行时首次碰撞卡顿。
    private void prebuildCollisionDataAsync(Map<String, Spatial> models) {
        Set<Mesh> meshes = new HashSet<>();
        for (Spatial s : models.values()) {
            collectMeshes(s, meshes);
        }
        List<Mesh> meshSnapshot = new ArrayList<>(meshes);
        GameThreadPoolServices.threadPoolManager().submitLongRunning(() -> {
            for (Mesh mesh : meshSnapshot) {
                mesh.createCollisionData();
            }
        });
    }

    private void collectMeshes(Spatial s, Set<Mesh> out) {
        if (s instanceof Geometry g) {
            out.add(g.getMesh());
        } else if (s instanceof Node n) {
            for (Spatial child : n.getChildren()) {
                collectMeshes(child, out);
            }
        }
    }

    @Override
    public GeoMap getMap(int worldId) {
        GeoMap geoMap = geoMaps.get(worldId);
        return geoMap != null ? geoMap : DummyGeoData.DUMMY_MAP;
    }
}
