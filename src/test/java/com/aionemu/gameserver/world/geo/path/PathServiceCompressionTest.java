package com.aionemu.gameserver.world.geo.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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
