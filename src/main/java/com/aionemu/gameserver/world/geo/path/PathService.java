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
import java.util.concurrent.CompletionException;
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
	private static final int MAX_GROUND_SMOOTH_LOOKAHEAD = 128;
	private static final float MAX_GROUND_HEIGHT_DEVIATION = 0.25f;
	private static final float NEAREST_GROUND_PATH_RADIUS = 2;
	private static final float NEAREST_GROUND_PATH_VERTICAL = 0.7f;
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
	private final LongAdder queueExpired = new LongAdder();
	private final LongAdder cacheHits = new LongAdder();
	private final LongAdder found = new LongAdder();
	private final LongAdder noPath = new LongAdder();
	private final LongAdder invalidPosition = new LongAdder();
	private final LongAdder nodeLimit = new LongAdder();
	private final LongAdder interrupted = new LongAdder();
	private final LongAdder cancelled = new LongAdder();
	private final LongAdder failed = new LongAdder();
	private final LongAdder processedNodes = new LongAdder();
	private final LongAdder hierarchicalAttempts = new LongAdder();
	private final LongAdder hierarchicalFound = new LongAdder();
	private final LongAdder hierarchicalFallbacks = new LongAdder();
	private final LongAdder abstractNodes = new LongAdder();
	private final LongAdder queueNanos = new LongAdder();
	private final LongAdder runNanos = new LongAdder();
	private final LongAdder recoverySubmitted = new LongAdder();
	private final LongAdder recoveryFound = new LongAdder();
	private final LongAdder recoveryFailed = new LongAdder();
	private final LongAdder pathsBeforeSmooth = new LongAdder();
	private final LongAdder pathsAfterSmooth = new LongAdder();
	private final LongAdder geoSegmentChecks = new LongAdder();
	private final LongAdder geoSegmentRejected = new LongAdder();
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
		return navigateToTargetAsync(owner, x, y, z, null);
	}

	public CompletableFuture<float[][]> navigateToTargetAsync(Creature owner, float x, float y, float z,
			BlockedSegment blockedSegment) {
		return owner == null || owner.getLifeStats().isAlreadyDead()
				? CompletableFuture.completedFuture(null)
				: navigateAsync(owner, x, y, z, 0, blockedSegment);
	}

	public float[][] navigateToLocation(Creature owner, float x, float y, float z) {
		return await(navigateToLocationAsync(owner, x, y, z));
	}

	public CompletableFuture<float[][]> navigateToLocationAsync(Creature owner, float x, float y, float z) {
		return navigateToLocationAsync(owner, x, y, z, null);
	}

	public CompletableFuture<float[][]> navigateToLocationAsync(Creature owner, float x, float y, float z,
			BlockedSegment blockedSegment) {
		return owner == null || owner.getLifeStats().isAlreadyDead()
				? CompletableFuture.completedFuture(null)
				: navigateAsync(owner, x, y, z, blockedSegment == null ? 1 : 0, blockedSegment);
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
		return canMoveStraight(owner, x, y, z, water, spatial);
	}

	public boolean canMoveStraight(Creature owner, float x, float y, float z) {
		if (owner == null) {
			return false;
		}
		WaterArea water = owner.isFlying() ? null : waterArea(owner);
		return canMoveStraight(owner, x, y, z, water, owner.isFlying() || water != null);
	}

	public int waypointSkipIndex(Creature owner, float[][] path, int lookahead) {
		if (owner == null || path == null || path.length < 2 || lookahead < 1 || usesSpatialPath(owner)) {
			return 0;
		}
		PathData.MapData map;
		try {
			map = data.getMap(owner.getWorldId());
		} catch (IllegalStateException e) {
			return 0;
		}
		if (map == null) {
			return 0;
		}
		float[] start = {owner.getX(), owner.getY(), owner.getZ()};
		PathData.HeightProvider terrain = (x, y) -> GameWorldServices.geoService().getTerrainZ(owner.getWorldId(), x, y);
		SegmentAllowed pathAllowed = (from, to) -> map.canWalkStraight(from[0], from[1], from[2], to[0], to[1],
				to[2], terrain, null);
		SegmentAllowed geoAllowed = (from, to) -> {
			geoSegmentChecks.increment();
			boolean allowed = canPass(owner, from[0], from[1], from[2], to[0], to[1], to[2], false);
			if (!allowed) {
				geoSegmentRejected.increment();
			}
			return allowed;
		};
		return waypointSkipIndex(start, path, lookahead, pathAllowed, geoAllowed);
	}

	public float[] projectGroundStep(Creature owner, float x, float y, float referenceZ) {
		if (owner == null || owner.isFlying()) {
			return null;
		}
		PathData.MapData map;
		try {
			map = data.getMap(owner.getWorldId());
		} catch (IllegalStateException e) {
			return null;
		}
		if (map == null) {
			return null;
		}
		PathData.HeightProvider terrain = terrain(owner.getWorldId());
		PathData.PathPoint projected = map.projectPoint(x, y, referenceZ, terrain);
		if (projected == null || !map.canWalkStraight(owner.getX(), owner.getY(), owner.getZ(), x, y, projected.z(),
				terrain, null)) {
			return null;
		}
		return new float[] {x, y, projected.z()};
	}

	public float[] projectLocalStep(Creature owner, float x, float y, float referenceZ) {
		if (owner == null) {
			return null;
		}
		return usesSpatialPath(owner) ? new float[] {x, y, referenceZ} : projectGroundStep(owner, x, y, referenceZ);
	}

	public float[] projectGroundPoint(Creature context, float x, float y, float referenceZ) {
		if (context == null || context.isFlying()) {
			return null;
		}
		PathData.MapData map;
		try {
			map = data.getMap(context.getWorldId());
		} catch (IllegalStateException e) {
			return null;
		}
		if (map == null) {
			return null;
		}
		PathData.PathPoint projected = map.projectPoint(x, y, referenceZ, terrain(context.getWorldId()));
		return projected == null ? null : new float[] {x, y, projected.z()};
	}

	public float[] nearestGroundPoint(Creature owner, float maxRadius, float maxVerticalDelta) {
		if (owner == null || owner.isFlying()) {
			return null;
		}
		PathData.MapData map;
		try {
			map = data.getMap(owner.getWorldId());
		} catch (IllegalStateException e) {
			return null;
		}
		if (map == null) {
			return null;
		}
		float ownerX = owner.getX();
		float ownerY = owner.getY();
		float ownerZ = owner.getZ();
		PathData.PathPoint point = map.nearestPathPoint(ownerX, ownerY, ownerZ, maxRadius, maxVerticalDelta,
				terrain(owner.getWorldId()), (x, y, z) -> {
					if (square(x - ownerX) + square(y - ownerY) <= 0.01f) {
						return false;
					}
					return canPass(owner, ownerX, ownerY, ownerZ, x, y, z, false);
				});
		return point == null ? null : new float[] {point.x(), point.y(), point.z()};
	}

	private static PathData.HeightProvider terrain(int worldId) {
		return (x, y) -> GameWorldServices.geoService().getTerrainZ(worldId, x, y);
	}

	private boolean canMoveStraight(Creature owner, float x, float y, float z, WaterArea water, boolean spatial) {
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
		return new Metrics(submitted.sum(), done, rejected.sum(), timedOut.sum(), queueExpired.sum(), cacheHits.sum(),
				found.sum(), noPath.sum(), invalidPosition.sum(), nodeLimit.sum(), interrupted.sum(), cancelled.sum(), failed.sum(),
				processedNodes.sum(), hierarchicalAttempts.sum(), hierarchicalFound.sum(), hierarchicalFallbacks.sum(),
				abstractNodes.sum(), recoverySubmitted.sum(), recoveryFound.sum(), recoveryFailed.sum(), pathsBeforeSmooth.sum(),
				pathsAfterSmooth.sum(), geoSegmentChecks.sum(), geoSegmentRejected.sum(),
				pathfinders.getQueue().size(), pathfinders.getActiveCount(),
				done == 0 ? 0 : TimeUnit.NANOSECONDS.toMicros(queueNanos.sum() / done),
				done == 0 ? 0 : TimeUnit.NANOSECONDS.toMicros(runNanos.sum() / done));
	}

	public static boolean isDefinitivePathFailure(Throwable failure) {
		if (failure == null) {
			return false;
		}
		PathResultStatus status = failureStatus(failure);
		return status == PathResultStatus.NO_PATH || status == PathResultStatus.INVALID_POSITION;
	}

	public static PathResultStatus failureStatus(Throwable failure) {
		return resultStatus(null, failure);
	}

	private CompletableFuture<float[][]> navigateAsync(Creature owner, float targetX, float targetY, float targetZ,
			int priority, BlockedSegment blockedSegment) {
		PathRequest request = snapshot(owner, targetX, targetY, targetZ, blockedSegment);
		PathCacheKey key = pathCacheKey(request.worldId(), request.instanceId(), request.obstacleVersion(), request.spatial(),
				request.startX(), request.startY(), request.startZ(), request.targetX(), request.targetY(), request.targetZ());
		if (usesResultCache(request.blockedSegment())) {
			PathCacheEntry cached = getFreshCacheEntry(key);
			if (cached != null) {
				cacheHits.increment();
				return CompletableFuture.completedFuture(copyPath(cached.path()));
			}
		}
		CompletableFuture<float[][]> result = executeAsync(priority, () -> {
			float[][] path = request.spatial() ? findSpatialPath(request) : findGroundPath(owner, request);
			if (usesResultCache(request.blockedSegment())) {
				putCachedPath(key, path);
			}
			return path;
		});
		if (request.blockedSegment() != null) {
			recoverySubmitted.increment();
			result.whenComplete((path, failure) -> {
				if (failure == null && path != null && path.length > 0) {
					recoveryFound.increment();
				} else {
					recoveryFailed.increment();
				}
			});
		}
		return result;
	}

	private PathRequest snapshot(Creature owner, float targetX, float targetY, float targetZ,
			BlockedSegment blockedSegment) {
		int worldId = owner.getWorldId();
		int instanceId = owner.getInstanceId();
		float startX = owner.getX();
		float startY = owner.getY();
		float startZ = owner.getZ();
		boolean flying = owner.isFlying();
		WaterArea water = flying ? null : waterArea(owner, startX, startY, startZ);
		BlockedSegment activeBlockedSegment = blockedSegment != null && blockedSegment.active(System.currentTimeMillis())
				? blockedSegment : null;
		return new PathRequest(worldId, instanceId, obstacleVersion(worldId, instanceId), startX, startY, startZ,
				targetX, targetY, targetZ, flying || water != null, Math.max(0.5f, owner.getCollision()), water,
				activeBlockedSegment);
	}

	static boolean usesResultCache(BlockedSegment blockedSegment) {
		return blockedSegment == null;
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
		long queuedAt = System.nanoTime();
		PrioritizedTask task = new PrioritizedTask(priority, sequence.getAndIncrement(), () -> {
			queueNanos.add(System.nanoTime() - queuedAt);
			long start = System.nanoTime();
			try {
				return action.call();
			} finally {
				runNanos.add(System.nanoTime() - start);
				completed.increment();
			}
		}, result);
		result.whenComplete((path, failure) -> {
			recordResultStatus(resultStatus(path, failure));
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
			boolean started = task.hasStarted();
			if (task.cancel(true)) {
				Throwable failure = started ? new TimeoutException("PATH request timed out")
						: new QueueExpiredException("PATH request expired in queue");
				if (started) {
					timedOut.increment();
				} else {
					queueExpired.increment();
				}
				result.completeExceptionally(failure);
				pathfinders.remove(task);
			}
		});
		return result;
	}

	static PathResultStatus resultStatus(float[][] path, Throwable failure) {
		if (failure == null) {
			return path == null ? PathResultStatus.NO_PATH : PathResultStatus.FOUND;
		}
		if (failure instanceof CompletionException || failure instanceof ExecutionException) {
			return resultStatus(path, failure.getCause());
		}
		if (failure instanceof IncompletePathSearchException incomplete) {
			return switch (incomplete.status()) {
				case NODE_LIMIT -> PathResultStatus.NODE_LIMIT;
				case INTERRUPTED -> PathResultStatus.INTERRUPTED;
				case INVALID_POSITION -> PathResultStatus.INVALID_POSITION;
				case NO_PATH -> PathResultStatus.NO_PATH;
				case FOUND -> PathResultStatus.FAILED;
			};
		}
		if (failure instanceof TimeoutException) {
			return PathResultStatus.TIMEOUT;
		}
		if (failure instanceof QueueExpiredException) {
			return PathResultStatus.QUEUE_EXPIRED;
		}
		if (failure instanceof RejectedExecutionException) {
			return PathResultStatus.REJECTED;
		}
		if (failure instanceof CancellationException) {
			return PathResultStatus.CANCELLED;
		}
		return PathResultStatus.FAILED;
	}

	private void recordResultStatus(PathResultStatus status) {
		switch (status) {
			case FOUND -> found.increment();
			case NO_PATH -> noPath.increment();
			case INVALID_POSITION -> invalidPosition.increment();
			case NODE_LIMIT -> nodeLimit.increment();
			case INTERRUPTED -> interrupted.increment();
			case CANCELLED -> cancelled.increment();
			case FAILED -> failed.increment();
			case TIMEOUT, QUEUE_EXPIRED, REJECTED -> {
				// 调度层在决定具体原因的位置计数，避免同一请求重复累计。
			}
		}
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

	private float[][] findGroundPath(Creature owner, PathRequest request) {
		float[] start = {request.startX(), request.startY(), request.startZ()};
		float[] target = {request.targetX(), request.targetY(), request.targetZ()};
		SegmentAllowed groundAllowed = (from, to) -> canPass(request.worldId(), request.instanceId(), from[0], from[1], from[2],
				to[0], to[1], to[2], false);
		PathData.MapData map;
		try {
			map = data.getMap(request.worldId());
		} catch (IllegalStateException e) {
			return GeoDataConfig.GEO_ENABLE ? geoGroundPath(owner, start, target, groundAllowed) : null;
		}
		if (map == null) {
			return GeoDataConfig.GEO_ENABLE ? geoGroundPath(owner, start, target, groundAllowed) : null;
		}
		PathData.HeightProvider terrain = (x, y) -> GameWorldServices.geoService().getTerrainZ(request.worldId(), x, y);
		PathData.PathPoint pathStart = map.projectPoint(request.startX(), request.startY(), request.startZ(), terrain);
		float[] bridge = null;
		if (pathStart == null) {
			pathStart = map.nearestPathPoint(request.startX(), request.startY(), request.startZ(),
					NEAREST_GROUND_PATH_RADIUS, NEAREST_GROUND_PATH_VERTICAL, terrain,
					(x, y, z) -> groundAllowed.test(start, new float[] {x, y, z}));
			if (pathStart == null) {
				invalidPosition.increment();
				return GeoDataConfig.GEO_ENABLE ? geoGroundPath(owner, start, target, groundAllowed) : null;
			}
			bridge = new float[] {pathStart.x(), pathStart.y(), pathStart.z()};
		}
		PathData.EdgePassability recoveryAllowed = request.blockedSegment() == null ? null
				: request.blockedSegment()::allows;
		PathData.SearchResult search = map.searchAStar(pathStart.x(), pathStart.y(), pathStart.z(), request.targetX(),
				request.targetY(), request.targetZ(), GeoDataConfig.GEO_PATH_MAX_NODES, terrain, recoveryAllowed,
				GeoDataConfig.GEO_PATH_HIERARCHICAL_ENABLE);
		processedNodes.add(search.processedNodes());
		abstractNodes.add(search.abstractNodes());
		if (search.mode() == PathData.SearchMode.HIERARCHICAL) {
			hierarchicalAttempts.increment();
			if (search.status() == PathData.SearchStatus.FOUND) {
				hierarchicalFound.increment();
			}
		} else if (search.mode() == PathData.SearchMode.HIERARCHICAL_FALLBACK) {
			hierarchicalAttempts.increment();
			hierarchicalFallbacks.increment();
		}
		if (search.status() == PathData.SearchStatus.INVALID_POSITION) {
			invalidPosition.increment();
			return GeoDataConfig.GEO_ENABLE ? geoGroundPath(owner, start, target, groundAllowed) : null;
		}
		List<PathData.PathPoint> path = groundPath(search);
		if (path == null) {
			return null;
		}
		SegmentAllowed pathAllowed = (from, to) -> map.canWalkStraight(from[0], from[1], from[2], to[0], to[1],
				to[2], terrain, recoveryAllowed);
		List<float[]> waypoints = waypoints(path, request.targetX(), request.targetY());
		pathsBeforeSmooth.add(waypoints.size() + (bridge == null ? 0 : 1));
		SegmentAllowed geoAllowed = (from, to) -> {
			geoSegmentChecks.increment();
			boolean allowed = groundAllowed.test(from, to);
			if (!allowed) {
				geoSegmentRejected.increment();
			}
			return allowed;
		};
		float[] smoothStart = bridge == null ? start : bridge;
		float[][] result = simplifyGroundPath(smoothStart, waypoints, pathAllowed, geoAllowed);
		if (result != null && bridge != null) {
			result = prependWaypoint(bridge, result);
		}
		if (result != null) {
			pathsAfterSmooth.add(result.length);
		}
		return result;
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

	private float[][] findSpatialPath(PathRequest request) {
		float targetX = request.targetX();
		float targetY = request.targetY();
		float targetZ = request.targetZ();
		WaterArea water = request.water();
		boolean swimming = water != null;
		float clearance = request.clearance();
		int worldSize = GameWorldBootstrapServices.world().getWorldMap(request.worldId()).getWorldSize();
		PathData.HeightProvider terrain = (x, y) -> GameWorldServices.geoService().getTerrainZ(request.worldId(), x, y);
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
		SpatialPathfinder.EdgeAllowed waterEdge = swimming
				? waterEdge(request.worldId(), request.instanceId(), water, clearance, new HashMap<>()) : null;
		// 先高度/水体（廉价），最后 canPass 射线，边被挡时少打 geo。
		SpatialPathfinder.EdgeAllowed edgeAllowed = (startX, startY, startZ, endX, endY, endZ) ->
				hasTerrainClearance(startX, startY, startZ, endX, endY, endZ, clearance, terrain)
						&& (waterEdge == null || waterEdge.test(startX, startY, startZ, endX, endY, endZ))
						&& canPass(request.worldId(), request.instanceId(), startX, startY, startZ, endX, endY, endZ, true);
		List<float[]> path = SpatialPathfinder.findProgressive(request.startX(), request.startY(), request.startZ(), targetX, targetY,
				targetZ,
				Math.max(1, GeoDataConfig.GEO_PATH_SPATIAL_STEP), Math.max(1, GeoDataConfig.GEO_PATH_MAX_NODES),
				worldSize, worldSize,
				(x, y, z) -> {
					float ground = terrain.get(x, y);
					return x >= 0 && y >= 0 && x <= worldSize && y <= worldSize
							&& (!swimming || water.allows(x, y, z, clearance))
							&& (!Float.isFinite(ground) || z >= ground + clearance);
				}, edgeAllowed);
		return path == null ? null : compress(request.worldId(), request.instanceId(),
				new float[] {request.startX(), request.startY(), request.startZ()}, path, true, waterEdge);
	}

	private WaterArea waterArea(Creature owner) {
		return waterArea(owner, owner.getX(), owner.getY(), owner.getZ());
	}

	private WaterArea waterArea(Creature owner, float x, float y, float z) {
		float ground = GameWorldServices.geoService().getTerrainZ(owner.getWorldId(), x, y);
		WaterVolumeStore.Volume volume = waterVolumes.find(owner.getWorldId(), x, y, z);
		if (volume != null) {
			float surface = volume.surfaceZ(x, y);
			if (isSubmerged(z, ground, surface)) {
				return new WaterArea(volume, surface);
			}
		}
		float waterLevel = GameWorldBootstrapServices.world().getWorldMap(owner.getWorldId()).getWaterLevel();
		boolean openToSurface = isSubmerged(z, ground, waterLevel)
				&& canPass(owner, x, y, z, x, y, waterLevel - 0.5f, true);
		return acceptsGlobalWater(z, ground, waterLevel, openToSurface) ? new WaterArea(null, waterLevel) : null;
	}

	static boolean acceptsGlobalWater(float z, float ground, float surface, boolean openToSurface) {
		return openToSurface && isSubmerged(z, ground, surface);
	}

	private static boolean isSubmerged(float z, float ground, float surface) {
		return z < surface - 0.5f && (!Float.isFinite(ground) || z > ground + 0.75f);
	}

	private static SpatialPathfinder.EdgeAllowed waterEdge(Creature owner, WaterArea water, float clearance,
			Map<WaterColumn, Boolean> openColumns) {
		return waterEdge(owner.getWorldId(), owner.getInstanceId(), water, clearance, openColumns);
	}

	private static SpatialPathfinder.EdgeAllowed waterEdge(int worldId, int instanceId, WaterArea water, float clearance,
			Map<WaterColumn, Boolean> openColumns) {
		if (water.volume != null) {
			return (startX, startY, startZ, endX, endY, endZ) ->
					water.volume.allowsSegment(startX, startY, startZ, endX, endY, endZ, clearance);
		}
		SpatialPathfinder.PointAllowed openColumn = (x, y, z) -> openColumns.computeIfAbsent(new WaterColumn(x, y, z),
				ignored -> canPass(worldId, instanceId, x, y, z, x, y, water.globalSurface, true));
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
		return canPass(owner.getWorldId(), owner.getInstanceId(), startX, startY, startZ, endX, endY, endZ, spatial);
	}

	private static boolean canPass(int worldId, int instanceId, float startX, float startY, float startZ, float endX,
			float endY, float endZ, boolean spatial) {
		if (!GeoDataConfig.GEO_ENABLE) {
			return true;
		}
		float dx = endX - startX;
		float dy = endY - startY;
		float dz = endZ - startZ;
		float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		return spatial
				? GameWorldServices.geoService().canPass(worldId, startX, startY, startZ, endX, endY, endZ,
					distance + 0.1f, instanceId)
				: GameWorldServices.geoService().canPassWalker(worldId, startX, startY, startZ, endX, endY,
					endZ, distance + 0.1f, instanceId);
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

	private static float[][] compress(int worldId, int instanceId, float[] start, List<float[]> points, boolean spatial,
			SpatialPathfinder.EdgeAllowed edgeAllowed) {
		return compress(worldId, instanceId, start, points, spatial, edgeAllowed, (from, to) -> true);
	}

	private static float[][] compress(int worldId, int instanceId, float[] start, List<float[]> points, boolean spatial,
			SpatialPathfinder.EdgeAllowed edgeAllowed, SegmentAllowed pathAllowed) {
		if (!spatial) {
			return simplifyGroundPath(start, points, pathAllowed,
					(from, to) -> canPass(worldId, instanceId, from[0], from[1], from[2], to[0], to[1], to[2], false));
		}
		return simplifyPath(start, points,
				(first, second) -> pathAllowed.test(first, second)
						&& square(first[0] - second[0]) + square(first[1] - second[1]) <= 2_500
						&& canPass(worldId, instanceId, first[0], first[1], first[2], second[0], second[1], second[2], true)
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

	static float[][] simplifyGroundPath(float[] start, List<float[]> points, SegmentAllowed pathAllowed,
			SegmentAllowed geoAllowed) {
		List<float[]> result = new ArrayList<>();
		float[] from = start;
		int current = -1;
		while (current + 1 < points.size()) {
			int last = Math.min(points.size() - 1, current + MAX_GROUND_SMOOTH_LOOKAHEAD);
			int selected = -1;
			for (int candidate = last; candidate > current; candidate--) {
				float[] to = points.get(candidate);
				if (square(to[0] - from[0]) + square(to[1] - from[1]) > square(MAX_GROUND_GEO_SEGMENT)
						|| !preservesGroundHeight(from, points, current + 1, candidate)
						|| !pathAllowed.test(from, to)) {
					continue;
				}
				if (geoAllowed.test(from, to)) {
					selected = candidate;
					break;
				}
			}
			if (selected < 0) {
				int next = current + 1;
				if (!pathAllowed.test(from, points.get(next))) {
					return null;
				}
				selected = next;
			}
			from = points.get(selected);
			result.add(from);
			current = selected;
		}
		return result.toArray(float[][]::new);
	}

	static int waypointSkipIndex(float[] start, float[][] path, int lookahead, SegmentAllowed pathAllowed,
			SegmentAllowed geoAllowed) {
		int last = Math.min(Math.max(0, lookahead), path.length - 1);
		for (int index = last; index > 0; index--) {
			if (preservesGroundHeight(start, path, index)
					&& pathAllowed.test(start, path[index]) && geoAllowed.test(start, path[index])) {
				return index;
			}
		}
		return 0;
	}

	private static boolean preservesGroundHeight(float[] from, List<float[]> points, int first, int last) {
		float[] to = points.get(last);
		for (int index = first; index < last; index++) {
			if (!matchesGroundHeight(from, to, points.get(index))) {
				return false;
			}
		}
		return true;
	}

	private static boolean preservesGroundHeight(float[] from, float[][] points, int last) {
		float[] to = points[last];
		for (int index = 0; index < last; index++) {
			if (!matchesGroundHeight(from, to, points[index])) {
				return false;
			}
		}
		return true;
	}

	private static boolean matchesGroundHeight(float[] from, float[] to, float[] point) {
		float dx = to[0] - from[0];
		float dy = to[1] - from[1];
		float lengthSquared = dx * dx + dy * dy;
		if (lengthSquared <= 0.000001f) {
			return false;
		}
		float amount = ((point[0] - from[0]) * dx + (point[1] - from[1]) * dy) / lengthSquared;
		amount = Math.max(0, Math.min(1, amount));
		float expectedZ = from[2] + (to[2] - from[2]) * amount;
		return Math.abs(point[2] - expectedZ) <= MAX_GROUND_HEIGHT_DEVIATION;
	}

	static float[][] prependWaypoint(float[] waypoint, float[][] path) {
		float[][] result = new float[path.length + 1][];
		result[0] = waypoint;
		System.arraycopy(path, 0, result, 1, path.length);
		return result;
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

	public enum PathResultStatus {
		FOUND,
		NO_PATH,
		INVALID_POSITION,
		NODE_LIMIT,
		INTERRUPTED,
		TIMEOUT,
		QUEUE_EXPIRED,
		REJECTED,
		CANCELLED,
		FAILED
	}

	public record Metrics(long submitted, long completed, long rejected, long timedOut, long queueExpired, long cacheHits,
			long found, long noPath, long invalidPosition, long nodeLimit, long interrupted, long cancelled, long failed,
			long processedNodes, long hierarchicalAttempts, long hierarchicalFound, long hierarchicalFallbacks,
			long abstractNodes, long recoverySubmitted, long recoveryFound, long recoveryFailed, long pathsBeforeSmooth,
			long pathsAfterSmooth, long geoSegmentChecks, long geoSegmentRejected, int queued, int active,
			long averageQueueMicros, long averageMicros) {}

	private record PathRequest(int worldId, int instanceId, long obstacleVersion, float startX, float startY, float startZ,
			float targetX, float targetY, float targetZ, boolean spatial, float clearance, WaterArea water,
			BlockedSegment blockedSegment) {}

	public record BlockedSegment(float fromX, float fromY, float fromZ, float toX, float toY, float toZ,
			float radius, long expiresAt) {

		public boolean active(long now) {
			return expiresAt > now;
		}

		boolean allows(float startX, float startY, float startZ, float targetX, float targetY, float targetZ) {
			float dx = toX - fromX;
			float dy = toY - fromY;
			float dz = toZ - fromZ;
			float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (distance <= 0.01f) {
				return true;
			}
			float amount = Math.min(0.5f, distance) / distance;
			float expectedX = fromX + dx * amount;
			float expectedY = fromY + dy * amount;
			float expectedZ = fromZ + dz * amount;
			float startDistance = (float) Math.hypot(startX - fromX, startY - fromY);
			float targetDistance = (float) Math.hypot(targetX - expectedX, targetY - expectedY);
			float forward = (targetX - startX) * dx + (targetY - startY) * dy;
			return startDistance > 0.75f || targetDistance > Math.max(0, radius) || forward <= 0
					|| Math.abs(targetZ - expectedZ) >= 0.7f;
		}
	}

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
		private final PathData.SearchStatus status;

		IncompletePathSearchException(PathData.SearchStatus status, int processedNodes) {
			super("PATH search incomplete: " + status + " after " + processedNodes + " nodes");
			this.status = status;
		}

		PathData.SearchStatus status() {
			return status;
		}
	}

	static final class QueueExpiredException extends RejectedExecutionException {

		QueueExpiredException(String message) {
			super(message);
		}
	}

	private static final class Holder {
		private static final PathService INSTANCE = new PathService();
	}
}
