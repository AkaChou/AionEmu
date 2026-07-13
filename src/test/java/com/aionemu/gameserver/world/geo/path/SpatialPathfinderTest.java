package com.aionemu.gameserver.world.geo.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.aionemu.gameserver.geoEngine.models.GeoMap;
import org.junit.jupiter.api.Test;

class SpatialPathfinderTest {

	@Test
	void routesAboveAThreeDimensionalBarrier() {
		List<float[]> path = SpatialPathfinder.find(0, 0, 0, 4, 0, 0, 1, 500,
				(x, y, z) -> true,
				(startX, startY, startZ, endX, endY, endZ) -> !crossesWall(startX, startZ, endX, endZ));

		assertNotNull(path);
		assertTrue(path.stream().anyMatch(point -> point[2] > 1));
		assertEquals(4, path.getLast()[0]);
	}

	@Test
	void respectsWaterVolumeBounds() {
		List<float[]> path = SpatialPathfinder.find(0, 0, -2, 3, 0, -2, 1, 100,
				(x, y, z) -> z <= -1 && z >= -4,
				(startX, startY, startZ, endX, endY, endZ) -> true);

		assertNotNull(path);
		assertTrue(path.stream().allMatch(point -> point[2] <= -1 && point[2] >= -4));
	}

	@Test
	void expandsSearchForObstaclesWiderThanTheInitialDetour() {
		SpatialPathfinder.EdgeAllowed aroundWideWall = (startX, startY, startZ, endX, endY, endZ) ->
				!crossesWideWall(startX, startY, endX, endY);

		List<float[]> initial = SpatialPathfinder.find(0, 0, 0, 80, 0, 0, 4, 5000, 24, 12,
				(x, y, z) -> true, aroundWideWall);
		List<float[]> expanded = SpatialPathfinder.findProgressive(0, 0, 0, 80, 0, 0, 4, 15000,
				(x, y, z) -> true, aroundWideWall);

		assertEquals(null, initial);
		assertNotNull(expanded);
		assertEquals(80, expanded.getLast()[0]);
	}

	@Test
	void expandsSearchBeyondTheFormerNinetySixMeterLimit() {
		SpatialPathfinder.EdgeAllowed aroundHugeWall = (startX, startY, startZ, endX, endY, endZ) ->
				!crossesHugeWall(startX, startY, endX, endY);

		List<float[]> path = SpatialPathfinder.findProgressive(0, 1024, 0, 80, 1024, 0, 4, 12000, 2048, 48,
				(x, y, z) -> x >= 0 && y >= 0 && x <= 2048 && y <= 2048 && z == 0, aroundHugeWall);

		assertNotNull(path);
		assertTrue(path.stream().anyMatch(point -> Math.abs(point[1] - 1024) > 900));
		assertEquals(80, path.getLast()[0]);
	}

	@Test
	void keepsTheFullNodeBudgetForTheFineGridStage() {
		List<float[]> path = SpatialPathfinder.findProgressive(0, 0, 0, 150, 0, 0, 1, 300,
				(x, y, z) -> y == 0 && z == 0,
				(startX, startY, startZ, endX, endY, endZ) -> Math.abs(endX - startX) <= 1.01f);

		assertNotNull(path);
		assertEquals(150, path.getLast()[0]);
	}

	@Test
	void doesNotTakeTheDirectPathThroughHeightmapTerrain() {
		GeoMap map = new GeoMap("1", 10);
		map.setTerrainData(new short[] {
				0, 0, 0, 0,
				0, 0, 0, 0,
				0, 320, 320, 0,
				0, 0, 0, 0,
				0, 0, 0, 0
		}, 5, 4);

		List<float[]> path = SpatialPathfinder.find(1, 3, 2, 7, 3, 2, 1, 1000,
				(x, y, z) -> {
					float ground = map.getTerrainPathHeight(x, y);
					return !Float.isFinite(ground) || z >= ground + 0.5f;
				}, (startX, startY, startZ, endX, endY, endZ) -> PathService.hasTerrainClearance(startX, startY,
						startZ, endX, endY, endZ, 0.5f, map::getTerrainPathHeight));

		assertNotNull(path);
		assertTrue(path.size() > 1);
	}

	private static boolean crossesWall(float startX, float startZ, float endX, float endZ) {
		return Math.min(startX, endX) < 2 && Math.max(startX, endX) >= 2 && Math.max(startZ, endZ) <= 1;
	}

	private static boolean crossesWideWall(float startX, float startY, float endX, float endY) {
		return Math.min(startX, endX) < 60 && Math.max(startX, endX) >= 20
				&& Math.max(Math.abs(startY), Math.abs(endY)) <= 30;
	}

	private static boolean crossesHugeWall(float startX, float startY, float endX, float endY) {
		return Math.min(startX, endX) < 60 && Math.max(startX, endX) >= 20
				&& Math.min(startY, endY) <= 1924 && Math.max(startY, endY) >= 124;
	}
}
