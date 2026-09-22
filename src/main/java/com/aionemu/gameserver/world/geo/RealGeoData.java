package com.aionemu.gameserver.world.geo;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
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
	 * 只读查找快照（加载期重建）。查表走升序 int 数组的二分，避免 {@code Map<Integer,…>} 逐次装箱 ——
	 * 该查表由寻路按节点级频率调用，曾是游戏内分配的第一大来源。
	 * Read-only lookup snapshot (rebuilt while loading). Lookups binary-search an ascending int array to avoid
	 * per-call {@code Integer} boxing, which used to be the top allocation source during gameplay.
	 */
	private volatile WorldMapLookup lookup = WorldMapLookup.EMPTY;

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
	 * @param models 已加载的网格模型表 / loaded mesh model table
	 */
	protected void loadWorldMaps(final Map<String, Spatial> models) {
		log.info(I18n.get("log.b518dba13a24"));
		final List<Integer> mapsWithErrors = new ArrayList<>();
		final Set<String> missingMeshes = ConcurrentHashMap.newKeySet();
		List<Callable<Void>> tasks = new ArrayList<>();
		List<GeoMap> maps = new ArrayList<>();

		for (final WorldMapTemplate map : DataManager.WORLD_MAPS_DATA) {
			GeoMap geoMap = new GeoMap(String.valueOf(map.getMapId()), map.getWorldSize());
			maps.add(geoMap);
			geoMaps.put(map.getMapId(), geoMap);
		}
		rebuildLookupSnapshot();
		try {
			GeoWorldLoader.loadTerrains(maps);
		} catch (IOException e) {
			throw new IllegalStateException("Problem loading terrains", e);
		}

		for (final WorldMapTemplate map : DataManager.WORLD_MAPS_DATA) {
			tasks.add(() -> {
				int mapId = map.getMapId();
				GeoMap geoMap = geoMaps.get(mapId);
				try {
					GeoWorldLoader.loadWorldObjects(mapId, models, geoMap, missingMeshes);
				} catch (Throwable t) {
					log.error(I18n.get("log.55a8c9c96345", mapId), t);
					synchronized (mapsWithErrors) {
						mapsWithErrors.add(mapId);
					}
				}
				return null;
			});
		}

		try {
			List<Future<Void>> futures = GameThreadPoolServices.threadPoolManager().getForkingPool().invokeAll(tasks);
			for (Future<Void> future : futures) {
				future.get();
			}
		} catch (InterruptedException e) {
			log.error(I18n.get("log.21e643902d47"), e);
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			log.error(I18n.get("log.21e643902d47"), e);
		}

		if (!mapsWithErrors.isEmpty()) {
			for (Integer mapId : mapsWithErrors) {
				geoMaps.put(mapId, DummyGeoData.DUMMY_MAP);
			}
			log.warn(I18n.get("log.79a88a7c6348", mapsWithErrors));
			rebuildLookupSnapshot();
		}
		if (!missingMeshes.isEmpty()) {
			log.warn(I18n.get("log.e4c8865c8c9c", missingMeshes.size(), missingMeshes.stream().sorted().collect(Collectors.joining("\n"))));
		}
	}

	/**
	 * 从 {@code geo/models.mesh} 加载全部网格模型。
	 * Loads all mesh models from {@code geo/models.mesh}.
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
	 * @param models 网格模型表 / mesh model table
	 */
	private void prebuildCollisionDataAsync(Map<String, Spatial> models) {
		Set<Mesh> meshes = new HashSet<>();
		for (Spatial s : models.values()) {
			collectMeshes(s, meshes);
		}
		List<Mesh> meshSnapshot = new ArrayList<>(meshes);
		if (meshSnapshot.isEmpty()) {
			return;
		}
		// 碰撞树只读取各自 Mesh 的只读缓冲，按长任务池容量轮转切片并行构建；仍走生命周期线程池，
		// 避免在既有加载任务里嵌套 ForkJoin 池。
		// Collision trees only read each mesh's own read-only buffers; distribute them round-robin
		// across long-running pool slots instead of serializing the whole prebuild on one thread.
		int partitionCount = Math.min(meshSnapshot.size(), Math.max(2, Runtime.getRuntime().availableProcessors()));
		List<List<Mesh>> partitions = partitionRoundRobin(meshSnapshot, partitionCount);
		for (List<Mesh> partition : partitions) {
			GameThreadPoolServices.threadPoolManager().submitLongRunning(() -> {
				for (Mesh mesh : partition) {
					mesh.createCollisionData();
				}
			});
		}
	}

	/**
	 * 将待处理项按轮转法切成指定数量的分片，保证顺序稳定且每项只出现一次。
	 * Splits items into the requested number of partitions in round-robin order, so every item appears
	 * exactly once while keeping a deterministic distribution.
	 * @param items 待处理项 / items to split
	 * @param partitionCount 分片数量（正数）/ number of partitions (positive)
	 * @param <T> 元素类型 / element type
	 * @return 分片列表 / partitions
	 */
	static <T> List<List<T>> partitionRoundRobin(List<T> items, int partitionCount) {
		if (partitionCount <= 0) {
			throw new IllegalArgumentException("partitionCount must be positive");
		}
		List<List<T>> partitions = new ArrayList<>(partitionCount);
		for (int i = 0; i < partitionCount; i++) {
			partitions.add(new ArrayList<>());
		}
		int index = 0;
		for (T item : items) {
			partitions.get(index++ % partitionCount).add(item);
		}
		return partitions;
	}

	/**
	 * 递归收集场景树中的全部网格。
	 * Recursively collects every mesh under a spatial tree.
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
	 * @param worldId 世界 ID / world id
	 * @return 地理地图或哑地图 / geo map or dummy map
	 */
	@Override
	public GeoMap getMap(int worldId) {
		GeoMap geoMap = lookup.find(worldId);
		return geoMap != null ? geoMap : DummyGeoData.DUMMY_MAP;
	}

	/**
	 * 重建只读查找快照；仅在加载期调用（此后 {@link #geoMaps} 不再变更）。
	 * Rebuilds the read-only lookup snapshot; called only while loading, after which {@link #geoMaps} is immutable.
	 */
	private void rebuildLookupSnapshot() {
		List<Integer> sortedIds = new ArrayList<>(geoMaps.keySet());
		Collections.sort(sortedIds);
		int[] ids = new int[sortedIds.size()];
		GeoMap[] maps = new GeoMap[sortedIds.size()];
		for (int i = 0; i < ids.length; i++) {
			int worldId = sortedIds.get(i);
			ids[i] = worldId;
			maps[i] = geoMaps.get(worldId);
		}
		lookup = new WorldMapLookup(ids, maps);
	}

	/**
	 * 在升序世界中查找下标。
	 * Finds the index of the world id in an ascending array.
	 * @param sortedIds 升序世界 ID 数组 / ascending world ids
	 * @param worldId 目标世界 ID / target world id
	 * @return 命中的下标，未命中为 -1 / matching index, or -1 when absent
	 */
	static int indexOfWorldId(int[] sortedIds, int worldId) {
		int low = 0;
		int high = sortedIds.length - 1;
		while (low <= high) {
			int mid = (low + high) >>> 1;
			int candidate = sortedIds[mid];
			if (candidate < worldId) {
				low = mid + 1;
			} else if (candidate > worldId) {
				high = mid - 1;
			} else {
				return mid;
			}
		}
		return -1;
	}

	/**
	 * 世界 ID → 地理地图 的只读查找快照。
	 * Read-only lookup snapshot from world id to geo map.
	 * 升序世界 ID 数组 / ascending world ids
	 * 与 {@code ids} 一一对应的地理地图 / geo maps aligned with {@code ids}
	 */
	record WorldMapLookup(int[] ids, GeoMap[] maps) {

		/** 空快照（加载完成前使用）。 / Empty snapshot used before loading finishes. */
		static final WorldMapLookup EMPTY = new WorldMapLookup(new int[0], new GeoMap[0]);

		/**
		 * 查找世界对应的地理地图。
		 * Looks up the geo map of the world.
		 * @param worldId 世界 ID / world id
		 * @return 地理地图，未命中为 {@code null} / geo map, or {@code null} when absent
		 */
		GeoMap find(int worldId) {
			int index = indexOfWorldId(ids, worldId);
			return index >= 0 ? maps[index] : null;
		}
	}
}
