package com.aionemu.gameserver.world.geo.path;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.model.gameobjects.Creature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
public final class PathService implements DisposableBean {

	private static final int MIN_LOCATION_TIMEOUT_MS = 1_000;
	// ponytail: bounded synchronous check; raise only if local waypoints still exhaust it.
	private static final int WAYPOINT_SEARCH_MAX_NODES = 512;
	private static final float MAX_GROUND_GEO_SEGMENT = 49;
	private static final float SPATIAL_CLEARANCE_SAMPLE = 0.5f;
	private static final float GLOBAL_WATER_SAMPLE = 2;
	/** 起终点量化格子（米）；与追击 repath 量级一致。 / Query cell size in meters. */
	private static final float RESULT_CACHE_CELL = 2f;
	/** 路径结果 TTL；对齐 NpcMoveController 500ms replan。 / Result TTL. */
	private static final long RESULT_CACHE_TTL_MS = 500;
	private static final int RESULT_CACHE_MAX = 1024;
	private static volatile ObjectProvider<PathService> instanceProvider;
	private final PathData data = new PathData();
	private final WaterVolumeStore waterVolumes = new WaterVolumeStore();
	private final AtomicLong sequence = new AtomicLong();
	private final Semaphore queueSlots;
	private final LongAdder submitted = new LongAdder();
	private final LongAdder completed = new LongAdder();
	private final LongAdder rejected = new LongAdder();
	private final LongAdder timedOut = new LongAdder();
	private final LongAdder cacheHits = new LongAdder();
	private final LongAdder runNanos = new LongAdder();
	private final ConcurrentHashMap<Long, AtomicLong> obstacleVersions = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<PathCacheKey, PathCacheEntry> resultCache = new ConcurrentHashMap<>();
	private final ThreadPoolExecutor pathfinders;

	public PathService() {
		int workers = workerCount(GeoDataConfig.GEO_PATH_WORKERS, Runtime.getRuntime().availableProcessors());
		int queueCapacity = queueCapacity(GeoDataConfig.GEO_PATH_QUEUE_CAPACITY);
		this.queueSlots = new Semaphore(queueCapacity, true);
		this.pathfinders = new ThreadPoolExecutor(workers, workers, 0, TimeUnit.MILLISECONDS,
				new PriorityBlockingQueue<>(), runnable -> {
					Thread thread = new Thread(runnable, "pathfinder");
					thread.setDaemon(true);
					return thread;
				});
	}

	static int workerCount(int configured, int cpus) {
		if (configured > 0) {
			return Math.max(1, configured);
		}
		return Math.max(1, Math.min(8, Math.max(1, cpus) / 2));
	}

	static int queueCapacity(int configured) {
		return configured > 0 ? configured : 256;
	}

	public void initializePath() {
		if (!GeoDataConfig.GEO_PATH_ENABLE) {
			log.info(I18n.get("log.20e0075b3ab6"));
			return;
		}
		long start = System.currentTimeMillis();
		int waterVolumeCount = waterVolumes.load();
		int count = data.scan();
		log.info(I18n.get("log.c4851e9f54bf", count, System.currentTimeMillis() - start));
		if (waterVolumeCount > 0) {
			log.info("Loaded {} local water volumes", waterVolumeCount);
		}
	}

	@Override
	public void destroy() {
		pathfinders.shutdownNow().forEach(task -> {
			PrioritizedTask pathTask = (PrioritizedTask) task;
			pathTask.cancel(false);
			pathTask.result.cancel(false);
		});
		resultCache.clear();
	}

	public boolean hasPathData(int worldId) {
		return data.hasMap(worldId);
	}

	public float[][] navigateToTarget(Creature owner, Creature target) {
		return await(navigateToTargetAsync(owner, target));
	}

	public CompletableFuture<float[][]> navigateToTargetAsync(Creature owner, Creature target) {
		if (owner == null || target == null || owner.getLifeStats().isAlreadyDead()
				|| owner.getWorldId() != target.getWorldId()) {
			return CompletableFuture.completedFuture(null);
		}
		float targetZ = !usesSpatialPath(owner) && target.isFlying() ? GameWorldServices.geoService().getZ(target) : target.getZ();
		return navigateToTargetAsync(owner, target.getX(), target.getY(), targetZ);
	}

	public CompletableFuture<float[][]> navigateToTargetAsync(Creature owner, float x, float y, float z) {
		return owner == null || owner.getLifeStats().isAlreadyDead()
				? CompletableFuture.completedFuture(null)
				: navigateAsync(owner, x, y, z, 0);
	}

	public float[][] navigateToLocation(Creature owner, float x, float y, float z) {
		return await(navigateToLocationAsync(owner, x, y, z));
	}

	public CompletableFuture<float[][]> navigateToLocationAsync(Creature owner, float x, float y, float z) {
		return owner == null || owner.getLifeStats().isAlreadyDead()
				? CompletableFuture.completedFuture(null)
				: navigateAsync(owner, x, y, z, 1);
	}

	public boolean canReachWaypoint(Creature owner, float x, float y, float z) {
		if (owner == null) {
			return false;
		}
		WaterArea water = owner.isFlying() ? null : waterArea(owner);
		boolean spatial = owner.isFlying() || water != null;
		if (!spatial) {
			PathData.SearchStatus status = groundWaypointStatus(owner, x, y, z);
			if (status != PathData.SearchStatus.INVALID_POSITION) {
				return status == PathData.SearchStatus.FOUND;
			}
		}
		if (!canPass(owner, owner.getX(), owner.getY(), owner.getZ(), x, y, z, spatial)) {
			return false;
		}
		if (spatial) {
			float clearance = Math.max(0.5f, owner.getCollision());
			PathData.HeightProvider terrain = (sampleX, sampleY) ->
					GameWorldServices.geoService().getTerrainZ(owner.getWorldId(), sampleX, sampleY);
			if (water != null && !waterEdge(owner, water, clearance, new HashMap<>()).test(owner.getX(), owner.getY(), owner.getZ(),
					x, y, z)) {
				return false;
			}
			if (!hasTerrainClearance(owner.getX(), owner.getY(), owner.getZ(), x, y, z, clearance, terrain)) {
				return false;
			}
		}
		return true;
	}

	private PathData.SearchStatus groundWaypointStatus(Creature owner, float x, float y, float z) {
		PathData.MapData map;
		try {
			map = data.getMap(owner.getWorldId());
		} catch (IllegalStateException e) {
			return PathData.SearchStatus.INVALID_POSITION;
		}
		if (map == null) {
			return PathData.SearchStatus.INVALID_POSITION;
		}
		PathData.HeightProvider terrain = (sampleX, sampleY) ->
				GameWorldServices.geoService().getTerrainZ(owner.getWorldId(), sampleX, sampleY);
		return map.searchAStar(owner.getX(), owner.getY(), owner.getZ(), x, y, z, WAYPOINT_SEARCH_MAX_NODES, terrain,
				null).status();
	}

	public long obstacleVersion(int worldId, int instanceId) {
		AtomicLong version = obstacleVersions.get(obstacleKey(worldId, instanceId));
		return version == null ? 0 : version.get();
	}

	public void obstacleChanged(int worldId, int instanceId) {
		obstacleVersions.computeIfAbsent(obstacleKey(worldId, instanceId), ignored -> new AtomicLong()).incrementAndGet();
	}

	public void instanceDestroyed(int worldId, int instanceId) {
		obstacleVersions.remove(obstacleKey(worldId, instanceId));
		resultCache.keySet().removeIf(key -> key.worldId() == worldId && key.instanceId() == instanceId);
	}

	public boolean usesSpatialPath(Creature owner) {
		return owner != null && (owner.isFlying() || waterArea(owner) != null);
	}

	public boolean hasPathingData(Creature owner) {
		return owner != null && pathingAvailable(GeoDataConfig.GEO_PATH_ENABLE,
				GeoDataConfig.GEO_ENABLE && usesSpatialPath(owner)
						|| hasGroundPathingData(GeoDataConfig.GEO_ENABLE, data.hasMap(owner.getWorldId())));
	}

	static boolean pathingAvailable(boolean enabled, boolean dataAvailable) {
		return enabled && dataAvailable;
	}

	static boolean hasGroundPathingData(boolean geoEnabled, boolean pathAvailable) {
		return geoEnabled || pathAvailable;
	}

	public Metrics metrics() {
		long done = completed.sum();
		return new Metrics(submitted.sum(), done, rejected.sum(), timedOut.sum(), cacheHits.sum(),
				pathfinders.getQueue().size(), pathfinders.getActiveCount(),
				done == 0 ? 0 : TimeUnit.NANOSECONDS.toMicros(runNanos.sum() / done));
	}

	public static boolean isDefinitivePathFailure(Throwable failure) {
		return failure instanceof IncompletePathSearchException;
	}

	private CompletableFuture<float[][]> navigateAsync(Creature owner, float targetX, float targetY, float targetZ, int priority) {
		PathCacheKey key = pathCacheKey(owner.getWorldId(), owner.getInstanceId(),
				obstacleVersion(owner.getWorldId(), owner.getInstanceId()), usesSpatialPath(owner),
				owner.getX(), owner.getY(), owner.getZ(), targetX, targetY, targetZ);
		PathCacheEntry cached = getFreshCacheEntry(key);
		if (cached != null) {
			cacheHits.increment();
			return CompletableFuture.completedFuture(copyPath(cached.path()));
		}
		return executeAsync(priority, () -> {
			WaterArea water = owner.isFlying() ? null : waterArea(owner);
			float[][] path = owner.isFlying() || water != null
					? findSpatialPath(owner, targetX, targetY, targetZ, water)
					: findGroundPath(owner, targetX, targetY, targetZ);
			putCachedPath(key, path);
			return path;
		});
	}

	static int pathCacheCell(float value) {
		return Math.round(value / RESULT_CACHE_CELL);
	}

	static PathCacheKey pathCacheKey(int worldId, int instanceId, long obstacleVersion, boolean spatial,
			float startX, float startY, float startZ, float targetX, float targetY, float targetZ) {
		return new PathCacheKey(worldId, instanceId, obstacleVersion, spatial,
				pathCacheCell(startX), pathCacheCell(startY), pathCacheCell(startZ),
				pathCacheCell(targetX), pathCacheCell(targetY), pathCacheCell(targetZ));
	}

	private PathCacheEntry getFreshCacheEntry(PathCacheKey key) {
		PathCacheEntry entry = resultCache.get(key);
		if (entry == null) {
			return null;
		}
		if (entry.expiresAt() <= System.currentTimeMillis()) {
			resultCache.remove(key, entry);
			return null;
		}
		return entry;
	}

	private void putCachedPath(PathCacheKey key, float[][] path) {
		// null 也缓存：短时间避免重复对「无路」全量 A*。
		resultCache.put(key, new PathCacheEntry(copyPath(path), System.currentTimeMillis() + RESULT_CACHE_TTL_MS));
		if (resultCache.size() > RESULT_CACHE_MAX) {
			evictResultCache(System.currentTimeMillis());
		}
	}

	private void evictResultCache(long now) {
		resultCache.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
		if (resultCache.size() <= RESULT_CACHE_MAX) {
			return;
		}
		int drop = resultCache.size() - RESULT_CACHE_MAX + RESULT_CACHE_MAX / 4;
		var iterator = resultCache.keySet().iterator();
		while (drop-- > 0 && iterator.hasNext()) {
			iterator.next();
			iterator.remove();
		}
	}

	static float[][] copyPath(float[][] path) {
		if (path == null) {
			return null;
		}
		float[][] copy = new float[path.length][];
		for (int i = 0; i < path.length; i++) {
			copy[i] = path[i].clone();
		}
		return copy;
	}

	float[][] execute(int priority, Callable<float[][]> action) {
		return await(executeAsync(priority, action));
	}

	CompletableFuture<float[][]> executeAsync(int priority, Callable<float[][]> action) {
		if (!queueSlots.tryAcquire()) {
			rejected.increment();
			return CompletableFuture.failedFuture(new RejectedExecutionException("PATH queue is full"));
		}
		CompletableFuture<float[][]> result = new CompletableFuture<>();
		PrioritizedTask task = new PrioritizedTask(priority, sequence.getAndIncrement(), () -> {
			long start = System.nanoTime();
			try {
				return action.call();
			} finally {
				runNanos.add(System.nanoTime() - start);
				completed.increment();
			}
		}, result);
		result.whenComplete((ignored, failure) -> {
			if (result.isCancelled()) {
				task.cancel(true);
				pathfinders.remove(task);
			}
		});
		try {
			submitted.increment();
			pathfinders.execute(task);
		} catch (RejectedExecutionException e) {
			rejected.increment();
			task.cancel(false);
			result.completeExceptionally(e);
		}
		CompletableFuture.delayedExecutor(requestTimeout(priority), TimeUnit.MILLISECONDS).execute(() -> {
			Throwable failure = task.hasStarted() ? new TimeoutException("PATH request timed out")
					: new RejectedExecutionException("PATH request expired in queue");
			if (task.cancel(true)) {
				timedOut.increment();
				result.completeExceptionally(failure);
				pathfinders.remove(task);
			}
		});
		return result;
	}

	static int requestTimeout(int priority) {
		int configured = Math.max(1, GeoDataConfig.GEO_PATH_TIMEOUT_MS);
		return priority == 0 ? configured : Math.max(MIN_LOCATION_TIMEOUT_MS, configured);
	}

	private static float[][] await(CompletableFuture<float[][]> result) {
		try {
			return result.get();
		} catch (InterruptedException e) {
			result.cancel(true);
			Thread.currentThread().interrupt();
			return null;
		} catch (CancellationException | ExecutionException e) {
			return null;
		}
	}

	private float[][] findGroundPath(Creature owner, float targetX, float targetY, float targetZ) {
		float[] start = {owner.getX(), owner.getY(), owner.getZ()};
		float[] target = {targetX, targetY, targetZ};
		SegmentAllowed groundAllowed = (from, to) -> canPass(owner, from[0], from[1], from[2], to[0], to[1], to[2], false);
		PathData.MapData map;
		try {
			map = data.getMap(owner.getWorldId());
		} catch (IllegalStateException e) {
			return GeoDataConfig.GEO_ENABLE ? geoGroundPath(owner, start, target, groundAllowed) : null;
		}
		if (map == null) {
			return GeoDataConfig.GEO_ENABLE ? geoGroundPath(owner, start, target, groundAllowed) : null;
		}
		PathData.HeightProvider terrain = (x, y) -> GameWorldServices.geoService().getTerrainZ(owner.getWorldId(), x, y);
		PathData.SearchResult search = map.searchAStar(owner.getX(), owner.getY(), owner.getZ(), targetX, targetY,
				targetZ, GeoDataConfig.GEO_PATH_MAX_NODES, terrain, null);
		if (search.status() == PathData.SearchStatus.INVALID_POSITION) {
			return GeoDataConfig.GEO_ENABLE ? geoGroundPath(owner, start, target, groundAllowed) : null;
		}
		List<PathData.PathPoint> path = groundPath(search);
		if (path == null) {
			return null;
		}
		SegmentAllowed pathAllowed = (from, to) -> map.canWalkStraight(from[0], from[1], from[2], to[0], to[1],
				to[2], terrain, null);
		return compress(owner, waypoints(path, targetX, targetY), false, null, pathAllowed);
	}

	private static float[][] geoGroundPath(Creature owner, float[] start, float[] target, SegmentAllowed allowed) {
		float[][] direct = directGroundPath(start, target, allowed);
		if (direct != null) {
			return direct;
		}
		Vector3f collision = GameWorldServices.geoService().getClosestCollision(owner, target[0], target[1], target[2], true,
				CollisionIntention.DEFAULT_COLLISIONS.getId());
		return partialGroundPath(start, new float[] {collision.x, collision.y, collision.z}, allowed);
	}

	private static List<PathData.PathPoint> groundPath(PathData.SearchResult result) {
		return switch (result.status()) {
			case FOUND -> result.path();
			case NODE_LIMIT, INTERRUPTED -> throw new IncompletePathSearchException(result.status(), result.processedNodes());
			case NO_PATH, INVALID_POSITION -> null;
		};
	}

	private float[][] findSpatialPath(Creature owner, float targetX, float targetY, float targetZ, WaterArea water) {
		boolean swimming = water != null;
		float clearance = Math.max(0.5f, owner.getCollision());
		int worldSize = GameWorldBootstrapServices.world().getWorldMap(owner.getWorldId()).getWorldSize();
		PathData.HeightProvider terrain = (x, y) -> GameWorldServices.geoService().getTerrainZ(owner.getWorldId(), x, y);
		float targetGround = terrain.get(targetX, targetY);
		if (Float.isFinite(targetGround)) {
			targetZ = Math.max(targetZ, targetGround + clearance);
		}
		if (swimming) {
			float surface = water.surfaceZ(targetX, targetY);
			if (Float.isFinite(surface)) {
				targetZ = Math.min(targetZ, surface - clearance);
			}
		}
		SpatialPathfinder.EdgeAllowed waterEdge = swimming ? waterEdge(owner, water, clearance, new HashMap<>()) : null;
		// 先高度/水体（廉价），最后 canPass 射线，边被挡时少打 geo。
		SpatialPathfinder.EdgeAllowed edgeAllowed = (startX, startY, startZ, endX, endY, endZ) ->
				hasTerrainClearance(startX, startY, startZ, endX, endY, endZ, clearance, terrain)
						&& (waterEdge == null || waterEdge.test(startX, startY, startZ, endX, endY, endZ))
						&& canPass(owner, startX, startY, startZ, endX, endY, endZ, true);
		List<float[]> path = SpatialPathfinder.findProgressive(owner.getX(), owner.getY(), owner.getZ(), targetX, targetY, targetZ,
				Math.max(1, GeoDataConfig.GEO_PATH_SPATIAL_STEP), Math.max(1, GeoDataConfig.GEO_PATH_MAX_NODES),
				worldSize, worldSize,
				(x, y, z) -> {
					float ground = terrain.get(x, y);
					return x >= 0 && y >= 0 && x <= worldSize && y <= worldSize
							&& (!swimming || water.allows(x, y, z, clearance))
							&& (!Float.isFinite(ground) || z >= ground + clearance);
				}, edgeAllowed);
		return path == null ? null : compress(owner, path, true, waterEdge);
	}

	private WaterArea waterArea(Creature owner) {
		float ground = GameWorldServices.geoService().getTerrainZ(owner.getWorldId(), owner.getX(), owner.getY());
		WaterVolumeStore.Volume volume = waterVolumes.find(owner.getWorldId(), owner.getX(), owner.getY(), owner.getZ());
		if (volume != null) {
			float surface = volume.surfaceZ(owner.getX(), owner.getY());
			if (isSubmerged(owner.getZ(), ground, surface)) {
				return new WaterArea(volume, surface);
			}
		}
		float waterLevel = GameWorldBootstrapServices.world().getWorldMap(owner.getWorldId()).getWaterLevel();
		boolean openToSurface = isSubmerged(owner.getZ(), ground, waterLevel)
				&& canPass(owner, owner.getX(), owner.getY(), owner.getZ(), owner.getX(), owner.getY(),
						waterLevel - 0.5f, true);
		return acceptsGlobalWater(owner.getZ(), ground, waterLevel, openToSurface) ? new WaterArea(null, waterLevel) : null;
	}

	static boolean acceptsGlobalWater(float z, float ground, float surface, boolean openToSurface) {
		return openToSurface && isSubmerged(z, ground, surface);
	}

	private static boolean isSubmerged(float z, float ground, float surface) {
		return z < surface - 0.5f && (!Float.isFinite(ground) || z > ground + 0.75f);
	}

	private static SpatialPathfinder.EdgeAllowed waterEdge(Creature owner, WaterArea water, float clearance,
			Map<WaterColumn, Boolean> openColumns) {
		if (water.volume != null) {
			return (startX, startY, startZ, endX, endY, endZ) ->
					water.volume.allowsSegment(startX, startY, startZ, endX, endY, endZ, clearance);
		}
		SpatialPathfinder.PointAllowed openColumn = (x, y, z) -> openColumns.computeIfAbsent(new WaterColumn(x, y, z),
				ignored -> canPass(owner, x, y, z, x, y, water.globalSurface, true));
		return (startX, startY, startZ, endX, endY, endZ) -> allowsGlobalWaterSegment(startX, startY, startZ, endX, endY,
				endZ, water.globalSurface, clearance, openColumn);
	}

	static boolean allowsGlobalWaterSegment(float startX, float startY, float startZ, float endX, float endY, float endZ,
			float surface, float clearance, SpatialPathfinder.PointAllowed openColumn) {
		float dx = endX - startX;
		float dy = endY - startY;
		float dz = endZ - startZ;
		int samples = Math.max(1, (int) Math.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz) / GLOBAL_WATER_SAMPLE));
		for (int i = 0; i <= samples; i++) {
			float amount = (float) i / samples;
			float x = startX + dx * amount;
			float y = startY + dy * amount;
			float z = startZ + dz * amount;
			if (z > surface - clearance || !openColumn.test(x, y, z)) {
				return false;
			}
		}
		return true;
	}

	private static long obstacleKey(int worldId, int instanceId) {
		return Integer.toUnsignedLong(worldId) << 32 | Integer.toUnsignedLong(instanceId);
	}

	static int comparePriority(int priority, long sequence, int otherPriority, long otherSequence) {
		int result = Integer.compare(priority, otherPriority);
		return result != 0 ? result : Long.compare(sequence, otherSequence);
	}

	static List<float[]> waypoints(List<PathData.PathPoint> path, float targetX, float targetY) {
		List<float[]> candidates = new ArrayList<>(path.size());
		for (int i = 1; i < path.size(); i++) {
			PathData.PathPoint point = path.get(i);
			candidates.add(new float[] {point.x(), point.y(), point.z()});
		}
		candidates.add(new float[] {targetX, targetY, path.getLast().z()});
		return candidates;
	}

	private static boolean canPass(Creature owner, float startX, float startY, float startZ, float endX, float endY,
			float endZ, boolean spatial) {
		if (!GeoDataConfig.GEO_ENABLE) {
			return true;
		}
		float dx = endX - startX;
		float dy = endY - startY;
		float dz = endZ - startZ;
		float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		return spatial
				? GameWorldServices.geoService().canPass(owner.getWorldId(), startX, startY, startZ, endX, endY, endZ,
					distance + 0.1f, owner.getInstanceId())
				: GameWorldServices.geoService().canPassWalker(owner.getWorldId(), startX, startY, startZ, endX, endY,
					endZ, distance + 0.1f, owner.getInstanceId());
	}

	static boolean hasTerrainClearance(float startX, float startY, float startZ, float endX, float endY, float endZ,
			float clearance, PathData.HeightProvider terrain) {
		float dx = endX - startX;
		float dy = endY - startY;
		float dz = endZ - startZ;
		int samples = Math.max(1, (int) Math.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz) / SPATIAL_CLEARANCE_SAMPLE));
		for (int i = 0; i <= samples; i++) {
			float amount = (float) i / samples;
			float ground = terrain.get(startX + dx * amount, startY + dy * amount);
			if (Float.isFinite(ground) && startZ + dz * amount < ground + clearance) {
				return false;
			}
		}
		return true;
	}

	private static float[][] compress(Creature owner, List<float[]> points, boolean spatial,
			SpatialPathfinder.EdgeAllowed edgeAllowed) {
		return compress(owner, points, spatial, edgeAllowed, (start, end) -> true);
	}

	private static float[][] compress(Creature owner, List<float[]> points, boolean spatial,
			SpatialPathfinder.EdgeAllowed edgeAllowed, SegmentAllowed pathAllowed) {
		float[] start = {owner.getX(), owner.getY(), owner.getZ()};
		if (!spatial) {
			points = boundedGroundSegments(start, points);
			return simplifyPath(start, points, pathAllowed);
		}
		return simplifyPath(start, points,
				(first, second) -> pathAllowed.test(first, second)
						&& square(first[0] - second[0]) + square(first[1] - second[1]) <= 2_500
						&& canPass(owner, first[0], first[1], first[2], second[0], second[1], second[2], true)
						&& (edgeAllowed == null || edgeAllowed.test(first[0], first[1], first[2], second[0], second[1], second[2])));
	}

	static float[][] simplifyPath(float[] start, List<float[]> points, SegmentAllowed allowed) {
		List<float[]> result = new ArrayList<>(points.size() + 1);
		result.add(start);
		result.addAll(points);
		for (int index = 2; index < result.size();) {
			if (allowed.test(result.get(index - 2), result.get(index))) {
				result.remove(index - 1);
			} else {
				index++;
			}
		}
		for (int index = 1; index < result.size(); index++) {
			if (!allowed.test(result.get(index - 1), result.get(index))) {
				return null;
			}
		}
		result.remove(0);
		return result.toArray(float[][]::new);
	}

	static float[][] directGroundPath(float[] start, float[] target, SegmentAllowed allowed) {
		return simplifyPath(start, boundedGroundSegments(start, List.of(target)), allowed);
	}

	static float[][] partialGroundPath(float[] start, float[] target, SegmentAllowed allowed) {
		return square(target[0] - start[0]) + square(target[1] - start[1]) + square(target[2] - start[2]) <= 0.01f
				? null : directGroundPath(start, target, allowed);
	}

	private static List<float[]> boundedGroundSegments(float[] start, List<float[]> points) {
		List<float[]> result = new ArrayList<>(points.size());
		float[] from = start;
		for (float[] to : points) {
			float deltaX = to[0] - from[0];
			float deltaY = to[1] - from[1];
			float deltaZ = to[2] - from[2];
			int segments = Math.max(1,
					(int) Math.ceil(Math.sqrt(deltaX * deltaX + deltaY * deltaY) / MAX_GROUND_GEO_SEGMENT));
			for (int segment = 1; segment <= segments; segment++) {
				float amount = (float) segment / segments;
				result.add(new float[] {from[0] + deltaX * amount, from[1] + deltaY * amount,
						from[2] + deltaZ * amount});
			}
			from = to;
		}
		return result;
	}

	@FunctionalInterface
	interface SegmentAllowed {
		boolean test(float[] start, float[] end);
	}

	private static float square(float value) {
		return value * value;
	}

	public static PathService getInstance() {
		ObjectProvider<PathService> provider = instanceProvider;
		return provider == null ? Holder.INSTANCE : provider.getIfAvailable(() -> Holder.INSTANCE);
	}

	public static void setInstanceProvider(ObjectProvider<PathService> provider) {
		instanceProvider = provider;
	}

	public record Metrics(long submitted, long completed, long rejected, long timedOut, long cacheHits, int queued,
			int active, long averageMicros) {}

	record PathCacheKey(int worldId, int instanceId, long obstacleVersion, boolean spatial, int startX, int startY,
			int startZ, int targetX, int targetY, int targetZ) {}

	private record PathCacheEntry(float[][] path, long expiresAt) {}

	private record WaterArea(WaterVolumeStore.Volume volume, float globalSurface) {

		private float surfaceZ(float x, float y) {
			return volume == null ? globalSurface : volume.contains(x, y) ? volume.surfaceZ(x, y) : Float.NaN;
		}

		private boolean allows(float x, float y, float z, float clearance) {
			float surface = surfaceZ(x, y);
			return Float.isFinite(surface) && z <= surface - clearance;
		}

		private boolean allowsSegment(float startX, float startY, float startZ, float endX, float endY, float endZ,
				float clearance) {
			if (volume != null) {
				return volume.allowsSegment(startX, startY, startZ, endX, endY, endZ, clearance);
			}
			return startZ <= globalSurface - clearance && endZ <= globalSurface - clearance;
		}
	}

	private record WaterColumn(float x, float y, float z) {}

	private final class PrioritizedTask extends FutureTask<float[][]> implements Comparable<PrioritizedTask> {
		private final int priority;
		private final long sequence;
		private final CompletableFuture<float[][]> result;
		private volatile boolean started;

		private PrioritizedTask(int priority, long sequence, Callable<float[][]> callable,
				CompletableFuture<float[][]> result) {
			super(callable);
			this.priority = priority;
			this.sequence = sequence;
			this.result = result;
		}

		@Override
		public int compareTo(PrioritizedTask other) {
			return comparePriority(priority, sequence, other.priority, other.sequence);
		}

		@Override
		public void run() {
			started = true;
			super.run();
		}

		private boolean hasStarted() {
			return started;
		}

		@Override
		protected void done() {
			try {
				if (!isCancelled()) {
					result.complete(get());
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				result.completeExceptionally(e);
			} catch (ExecutionException e) {
				if (!(e.getCause() instanceof IncompletePathSearchException || e.getCause() instanceof CancellationException)) {
					log.warn("PATH request failed", e.getCause());
				}
				result.completeExceptionally(e.getCause());
			} finally {
				queueSlots.release();
			}
		}
	}

	static final class IncompletePathSearchException extends RuntimeException {

		IncompletePathSearchException(PathData.SearchStatus status, int processedNodes) {
			super("PATH search incomplete: " + status + " after " + processedNodes + " nodes");
		}
	}

	private static final class Holder {
		private static final PathService INSTANCE = new PathService();
	}
}
