package com.aionemu.gameserver.world.geo.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class PathServiceCompressionTest {

	@Test
	void removesRetailIntermediatePointsWhenTheirNeighborsCanConnect() {
		float[] start = {0, 0, 0};
		float[] first = {0, 1, 0};
		float[] second = {1, 1, 0};
		float[] target = {2, 0, 0};

		float[][] result = PathService.simplifyPath(start, List.of(first, second, target),
				(from, to) -> from == first && to == target || from == start && to == first || from == target
						|| from == first && to == second || from == second && to == target);

		assertEquals(2, result.length);
		assertEquals(first, result[0]);
		assertEquals(target, result[1]);
	}

	@Test
	void projectsTheDestinationOntoTheSelectedPathLayer() {
		List<PathData.PathPoint> path = List.of(
				new PathData.PathPoint(1, 1, 10),
				new PathData.PathPoint(2, 2, 25));

		List<float[]> waypoints = PathService.waypoints(path, 2.4f, 2.6f);

		assertEquals(2, waypoints.size());
		assertEquals(25, waypoints.getFirst()[2]);
		assertEquals(2.4f, waypoints.getLast()[0]);
		assertEquals(2.6f, waypoints.getLast()[1]);
		assertEquals(25, waypoints.getLast()[2]);
	}

	@Test
	void rejectsAPathWhenAnAdjacentSegmentIsNotTraversable() {
		float[] start = {0, 0, 0};
		float[] first = {1, 0, 0};
		float[] target = {2, 0, 0};

		assertEquals(null, PathService.simplifyPath(start, List.of(first, target),
				(from, to) -> from == start && to == first));
	}

	@Test
	void groundSmoothingFallsBackWhenGeoRejectsTheLongestPathSegment() {
		float[] start = {0, 0, 10};
		float[] first = {1, 0, 11};
		float[] second = {1, 1, 12};
		float[] target = {2, 1, 13};

		float[][] result = PathService.simplifyGroundPath(start, List.of(first, second, target),
				(from, to) -> true, (from, to) -> from != start || to != target);

		assertEquals(2, result.length);
		assertEquals(second, result[0]);
		assertEquals(target, result[1]);
		assertEquals(12, result[0][2]);
		assertEquals(13, result[1][2]);
	}

	@Test
	void groundSmoothingKeepsRawAStarStepsWhenGeoRejectsEveryShortcut() {
		float[] start = {0, 0, 10};
		float[] first = {1, 0, 11};
		float[] second = {2, 0, 12};

		float[][] result = PathService.simplifyGroundPath(start, List.of(first, second),
				(from, to) -> true, (from, to) -> false);

		assertEquals(2, result.length);
		assertEquals(first, result[0]);
		assertEquals(second, result[1]);
	}

	@Test
	void groundSmoothingKeepsSlopeBreaksButStillCompressesLinearSlopes() {
		float[] start = {0, 0, 0};
		float[] crest = {1, 0, 1};
		float[] flat = {2, 0, 1};

		float[][] brokenSlope = PathService.simplifyGroundPath(start, List.of(crest, flat),
				(from, to) -> true, (from, to) -> true);
		float[][] linearSlope = PathService.simplifyGroundPath(start,
				List.of(new float[] {1, 0, 0.5f}, flat), (from, to) -> true, (from, to) -> true);

		assertEquals(2, brokenSlope.length);
		assertEquals(crest, brokenSlope[0]);
		assertEquals(1, linearSlope.length);
		assertEquals(flat, linearSlope[0]);
	}

	@Test
	void waypointSkipChecksAtMostThreePointsAndOnlyGeoChecksPathCandidates() {
		float[] start = {0, 0, 0};
		float[][] path = {{1, 0, 0}, {2, 0, 0}, {3, 0, 0}, {4, 0, 0}, {5, 0, 0}};
		AtomicInteger pathChecks = new AtomicInteger();
		AtomicInteger geoChecks = new AtomicInteger();

		int index = PathService.waypointSkipIndex(start, path, 3, (from, to) -> {
			pathChecks.incrementAndGet();
			return to != path[3];
		}, (from, to) -> {
			geoChecks.incrementAndGet();
			return true;
		});

		assertEquals(2, index);
		assertEquals(2, pathChecks.get());
		assertEquals(1, geoChecks.get());
	}

	@Test
	void waypointSkipFallsBackToANearerPathPointWhenGeoBlocksTheFarthestOne() {
		float[] start = {0, 0, 0};
		float[][] path = {{1, 0, 0}, {2, 0, 0}, {3, 0, 0}, {4, 0, 0}};

		int index = PathService.waypointSkipIndex(start, path, 3, (from, to) -> true,
				(from, to) -> to != path[3]);

		assertEquals(2, index);
	}

	@Test
	void waypointSkipDoesNotCutThroughASlopeBreak() {
		float[] start = {0, 0, 0};
		float[][] path = {{1, 0, 1}, {2, 0, 1}, {3, 0, 1}};

		assertEquals(0, PathService.waypointSkipIndex(start, path, 2,
				(from, to) -> true, (from, to) -> true));
	}

	@Test
	void nearestNodeBridgeIsPrependedBeforeTheAStarRoute() {
		float[] bridge = {0.25f, 0.25f, 1};
		float[][] route = {{0.75f, 0.25f, 1}, {1.25f, 0.25f, 1}};

		float[][] result = PathService.prependWaypoint(bridge, route);

		assertEquals(3, result.length);
		assertEquals(bridge, result[0]);
		assertEquals(route[0], result[1]);
		assertEquals(route[1], result[2]);
	}

	@Test
	void fallsBackToBoundedGeoSegmentsOutsidePathData() {
		float[][] result = PathService.directGroundPath(new float[] {0, 0, 0}, new float[] {120, 0, 12},
				(start, end) -> end[0] - start[0] <= 50);

		assertEquals(3, result.length);
		assertEquals(40, result[0][0]);
		assertEquals(80, result[1][0]);
		assertEquals(120, result[2][0]);
		assertEquals(null, PathService.directGroundPath(new float[] {0, 0, 0}, new float[] {120, 0, 12},
				(start, end) -> end[0] <= 40));
	}

	@Test
	void returnsOnlyAUsablePartialGroundPath() {
		float[] start = {0, 0, 0};

		assertEquals(1, PathService.partialGroundPath(start, new float[] {10, 0, 0}, (from, to) -> true).length);
		assertEquals(null, PathService.partialGroundPath(start, new float[] {0.05f, 0, 0}, (from, to) -> true));
		assertEquals(null, PathService.partialGroundPath(start, new float[] {10, 0, 0}, (from, to) -> false));
	}

	@Test
	void geoKeepsGroundPathingAvailableWithoutPathData() {
		assertTrue(PathService.hasGroundPathingData(true, false));
		assertTrue(PathService.hasGroundPathingData(false, true));
		assertFalse(PathService.hasGroundPathingData(false, false));
	}

	@Test
	void groundPathSearchDoesNotRetryAStarWithGeoPassability() throws IOException {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/world/geo/path/PathService.java"));
		String method = source.substring(source.indexOf("private float[][] findGroundPath"),
				source.indexOf("private static float[][] geoGroundPath"));

		assertEquals(1, method.split("searchAStar\\(", -1).length - 1);
		assertTrue(method.contains("request.blockedSegment()::allows"));
		assertFalse(method.contains("EdgePassability geo"));
	}

	@Test
	void asynchronousSearchUsesTheSameImmutableEndpointsAsItsCacheKey() throws IOException {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/world/geo/path/PathService.java"));
		String navigation = source.substring(source.indexOf("private CompletableFuture<float[][]> navigateAsync"),
				source.indexOf("static int pathCacheCell"));
		String ground = source.substring(source.indexOf("private float[][] findGroundPath"),
				source.indexOf("private static float[][] geoGroundPath"));
		String spatial = source.substring(source.indexOf("private float[][] findSpatialPath"),
				source.indexOf("private WaterArea waterArea"));
		String compactSpatial = spatial.replaceAll("\\s+", " ");

		assertTrue(navigation.contains("PathRequest request = snapshot"));
		assertTrue(navigation.contains("pathCacheKey(request.worldId()"));
		assertTrue(ground.contains("map.projectPoint(request.startX(), request.startY(), request.startZ()"));
		assertTrue(ground.contains("map.searchAStar(pathStart.x(), pathStart.y(), pathStart.z()"));
		assertTrue(compactSpatial.contains("SpatialPathfinder.findProgressive(request.startX(), request.startY(), request.startZ()"));
		assertFalse(ground.contains("owner.getX()"));
		assertFalse(spatial.contains("owner.getX()"));
	}

	@Test
	void groundWaypointUsesBoundedPathSearchBeforeStraightFallback() throws IOException {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/world/geo/path/PathService.java"));
		String method = source.substring(source.indexOf("public boolean canReachWaypoint"),
				source.indexOf("public long obstacleVersion"));

		assertTrue(method.indexOf("groundWaypointStatus") < method.indexOf("canMoveStraight("));
		assertTrue(method.contains("status != PathData.SearchStatus.INVALID_POSITION"));
		assertTrue(method.contains("WAYPOINT_SEARCH_MAX_NODES"));
		assertFalse(method.contains(", 1, terrain"));
	}

	@Test
	void pathCacheKeyQuantizesEndpointsToTwoMeterCells() {
		// 2m 格：[-1,1)→0，[1,3)→1
		var a = PathService.pathCacheKey(1, 0, 3L, false, 0.4f, 0.4f, 0.4f, 10.4f, 10.4f, 1f);
		var b = PathService.pathCacheKey(1, 0, 3L, false, 0.9f, 0.9f, 0.9f, 9.6f, 9.6f, 1f);
		var c = PathService.pathCacheKey(1, 0, 4L, false, 0.4f, 0.4f, 0.4f, 10.4f, 10.4f, 1f);
		assertEquals(a, b);
		assertEquals(0, PathService.pathCacheCell(0.4f));
		assertEquals(1, PathService.pathCacheCell(1.4f));
		assertFalse(a.equals(c));
	}

	@Test
	void recoveryBlocksOnlyTheFailedFirstEdgeOnTheSameLayer() {
		PathService.BlockedSegment blocked = new PathService.BlockedSegment(0.25f, 0.25f, 1,
				2.25f, 0.25f, 1, 0.35f, 2_000);

		assertTrue(blocked.active(1_000));
		assertFalse(blocked.allows(0.25f, 0.25f, 1, 0.75f, 0.25f, 1));
		assertTrue(blocked.allows(0.25f, 0.25f, 1, 0.75f, 0.75f, 1));
		assertTrue(blocked.allows(0.25f, 0.25f, 3, 0.75f, 0.25f, 3));
	}

	@Test
	void recoveryRequestsBypassTheSharedResultCache() {
		PathService.BlockedSegment blocked = new PathService.BlockedSegment(0, 0, 0, 1, 0, 0, 0.35f, 2_000);

		assertTrue(PathService.usesResultCache(null));
		assertFalse(PathService.usesResultCache(blocked));
	}

	@Test
	void pathingFeatureFlagMustBeEnabled() {
		assertFalse(PathService.pathingAvailable(false, true));
		assertTrue(PathService.pathingAvailable(true, true));
	}

	@Test
	void checksTerrainClearanceAlongTheWholeSpatialSegment() {
		assertFalse(PathService.hasTerrainClearance(0, 0, 2, 2, 0, 2, 0.5f,
				(x, y) -> x > 0.9f && x < 1.1f ? 1.75f : 0));
	}

	@Test
	void acceptsGlobalWaterOnlyWhenTheSurfaceIsOpenAboveTheNpc() {
		assertTrue(PathService.acceptsGlobalWater(5, 3, 10, true));
		assertFalse(PathService.acceptsGlobalWater(5, 3, 10, false));
		assertFalse(PathService.acceptsGlobalWater(2, 3, 10, true));
	}

	@Test
	void checksOpenWaterColumnsAlongTheWholeGlobalWaterSegment() {
		assertFalse(PathService.allowsGlobalWaterSegment(0, 0, 5, 4, 0, 5, 10, 0.5f,
				(x, y, z) -> x < 1.5f || x > 2.5f));
		assertTrue(PathService.allowsGlobalWaterSegment(0, 0, 5, 4, 0, 5, 10, 0.5f,
				(x, y, z) -> true));
		assertFalse(PathService.allowsGlobalWaterSegment(0, 0, 5, 4, 0, 10, 10, 0.5f,
				(x, y, z) -> true));
	}
}
