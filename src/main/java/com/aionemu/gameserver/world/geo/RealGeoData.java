package com.aionemu.gameserver.world.geo;

import com.aionemu.boot.i18n.I18n;
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

/**
 * 真实地理数据实现：并行加载地形与世界物体，并异步预构建碰撞树。
 * Real geo-data implementation that parallel-loads terrain and world objects and pre-builds collision trees asynchronously.
 */
@Slf4j
public class RealGeoData implements GeoData {

	/** 世界 ID → 地理地图。 / World id → geo map. */
	private final IntObjectHashMap<GeoMap> geoMaps = new IntObjectHashMap<>();

	/**
	 * 加载网格、世界地图，并异步预构建碰撞数据。
	 * Loads meshes and world maps, then pre-builds collision data asynchronously.
	 */
	@Override
	public void loadGeoMaps() {
		final Map<String, Spatial> models = loadMeshes();
		loadWorldMaps(models);
		prebuildCollisionDataAsync(models);
		models.clear();
		log.info(I18n.get("log.dbf54601a186", geoMaps.size()));
	}

	/**
	 * 按世界模板并行加载地形与物体到各地理地图。
	 * Parallel-loads terrain and world objects into each geo map for every world template.
	 *
	 * @param models 已加载的网格模型表 / loaded mesh model table
	 */
	protected void loadWorldMaps(final Map<String, Spatial> models) {
		log.info(I18n.get("log.b518dba13a24"));
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
						log.error(I18n.get("log.55a8c9c96345", mapId, t));
						synchronized (mapsWithErrors) {
							mapsWithErrors.add(mapId);
						}
					}
					progressRenderer.progress(I18n.get("console.progress.geo_maps"), completedMaps.incrementAndGet(), totalMaps);
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
			log.error(I18n.get("log.21e643902d47", e));
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			log.error(I18n.get("log.21e643902d47", e));
		}

		progressRenderer.finished(I18n.get("console.progress.geo_maps"), totalMaps);
		if (!mapsWithErrors.isEmpty()) {
			for (Integer mapId : mapsWithErrors) {
				geoMaps.put(mapId, DummyGeoData.DUMMY_MAP);
			}
			log.warn(I18n.get("log.79a88a7c6348", mapsWithErrors));
		}
		if (!missingMeshes.isEmpty()) {
			log.warn(I18n.get("log.e4c8865c8c9c", missingMeshes.size(), missingMeshes.stream().sorted().collect(Collectors.joining("\n"))));
		}
	}

	/**
	 * 从 {@code geo/models.mesh} 加载全部网格模型。
	 * Loads all mesh models from {@code geo/models.mesh}.
	 *
	 * @return 名称 → 空间节点映射 / name → spatial map
	 */
	protected Map<String, Spatial> loadMeshes() {
		log.info(I18n.get("log.f40bcb01bf83"));
		try {
			return GeoWorldLoader.loadMeshs("geo/models.mesh");
		} catch (IOException e) {
			throw new IllegalStateException("Problem loading meshes", e);
		}
	}

	/**
	 * 碰撞树后台预构建；Mesh.collideWith 有懒加载兜底，此步仅为降低运行时首次碰撞卡顿。
	 * Background pre-build of collision trees; Mesh.collideWith has a lazy fallback — this only reduces first-hit hitching at runtime.
	 *
	 * @param models 网格模型表 / mesh model table
	 */
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

	/**
	 * 递归收集场景树中的全部网格。
	 * Recursively collects every mesh under a spatial tree.
	 *
	 * @param s 当前节点 / current spatial
	 * @param out 输出集合 / output set
	 */
	private void collectMeshes(Spatial s, Set<Mesh> out) {
		if (s instanceof Geometry g) {
			out.add(g.getMesh());
		} else if (s instanceof Node n) {
			for (Spatial child : n.getChildren()) {
				collectMeshes(child, out);
			}
		}
	}

	/**
	 * 按世界 ID 获取地理地图；缺失时回退到哑地图。
	 * Returns the geo map for the world id, falling back to the dummy map when missing.
	 *
	 * 世界 ID / world id
	 *
	 * @param worldId
	 * @return 地理地图或哑地图 / geo map or dummy map
	 */
	@Override
	public GeoMap getMap(int worldId) {
		GeoMap geoMap = geoMaps.get(worldId);
		return geoMap != null ? geoMap : DummyGeoData.DUMMY_MAP;
	}
}
