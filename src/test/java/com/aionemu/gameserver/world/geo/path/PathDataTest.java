package com.aionemu.gameserver.world.geo.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import com.aionemu.gameserver.configs.main.GeoDataConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PathDataTest {

	@TempDir
	Path directory;

	@Test
	void loadsGzipPathAndRebuildsInvalidCache() throws Exception {
		Path geo = directory.resolve("geo/path");
		Path cache = directory.resolve("cache");
		Files.createDirectories(geo);
		byte[] data = flatLinkedPath();
		Files.write(geo.resolve("1.idx"), index(data, 137));
		try (GZIPOutputStream output = new GZIPOutputStream(Files.newOutputStream(geo.resolve("1.path.gz")))) {
			output.write(data);
		}
		String oldGeo = System.getProperty("aion.game.geo.dir");
		String oldCache = System.getProperty("aion.game.cache.dir");
		boolean oldEnabled = GeoDataConfig.GEO_PATH_ENABLE;
		try {
			System.setProperty("aion.game.geo.dir", directory.resolve("geo").toString());
			System.setProperty("aion.game.cache.dir", cache.toString());
			GeoDataConfig.GEO_PATH_ENABLE = true;
			PathData paths = new PathData();
			assertEquals(1, paths.scan());
			assertNotNull(paths.getMap(1));
			Path cached = cache.resolve("path/1.path");
			assertArrayEquals(data, Files.readAllBytes(cached));

			Files.write(cached, new byte[data.length]);
			paths = new PathData();
			assertEquals(1, paths.scan());
			assertNotNull(paths.getMap(1));
			assertArrayEquals(data, Files.readAllBytes(cached));
		} finally {
			restore("aion.game.geo.dir", oldGeo);
			restore("aion.game.cache.dir", oldCache);
			GeoDataConfig.GEO_PATH_ENABLE = oldEnabled;
		}
	}

	@Test
	void stopsAdvertisingAMapAfterItsCompressedDataFailsToLoad() throws Exception {
		Path geo = directory.resolve("geo/path");
		Files.createDirectories(geo);
		byte[] data = flatLinkedPath();
		Files.write(geo.resolve("1.idx"), index(data, 137));
		Files.writeString(geo.resolve("1.path.gz"), "not gzip");
		String oldGeo = System.getProperty("aion.game.geo.dir");
		boolean oldEnabled = GeoDataConfig.GEO_PATH_ENABLE;
		try {
			System.setProperty("aion.game.geo.dir", directory.resolve("geo").toString());
			GeoDataConfig.GEO_PATH_ENABLE = true;
			PathData paths = new PathData();
			assertEquals(1, paths.scan());
			assertThrows(IllegalStateException.class, () -> paths.getMap(1));
			assertFalse(paths.hasMap(1));
		} finally {
			restore("aion.game.geo.dir", oldGeo);
			GeoDataConfig.GEO_PATH_ENABLE = oldEnabled;
		}
	}

	@Test
	void interruptedColdLoadDoesNotDisableTheMap() throws Exception {
		Path geo = directory.resolve("geo/path");
		Files.createDirectories(geo);
		byte[] data = flatLinkedPath();
		Files.write(geo.resolve("1.idx"), index(data, 137));
		try (GZIPOutputStream output = new GZIPOutputStream(Files.newOutputStream(geo.resolve("1.path.gz")))) {
			output.write(data);
		}
		String oldGeo = System.getProperty("aion.game.geo.dir");
		String oldCache = System.getProperty("aion.game.cache.dir");
		boolean oldEnabled = GeoDataConfig.GEO_PATH_ENABLE;
		try {
			System.setProperty("aion.game.geo.dir", directory.resolve("geo").toString());
			System.setProperty("aion.game.cache.dir", directory.resolve("cache").toString());
			GeoDataConfig.GEO_PATH_ENABLE = true;
			PathData paths = new PathData();
			assertEquals(1, paths.scan());

			Thread.currentThread().interrupt();
			assertThrows(CancellationException.class, () -> paths.getMap(1));
			assertTrue(Thread.interrupted());
			assertTrue(paths.hasMap(1));
			assertNotNull(paths.getMap(1));
		} finally {
			Thread.interrupted();
			restore("aion.game.geo.dir", oldGeo);
			restore("aion.game.cache.dir", oldCache);
			GeoDataConfig.GEO_PATH_ENABLE = oldEnabled;
		}
	}

	@Test
	void followsRetailLinkMasksAroundBlockedEdge() throws Exception {
		Path path = directory.resolve("1.path");
		Path index = directory.resolve("1.idx");
		byte[] data = flatLinkedPath();
		Files.write(path, data);
		Files.write(index, index(data, 137));

		PathData.MapData map = PathData.MapData.load(path.toFile(), index.toFile());
		List<PathData.PathPoint> result = map.findPath(0.25f, 0.25f, 1, 1.25f, 0.25f, 1, 100,
				(x, y) -> Float.NaN);

		assertNotNull(result);
		assertEquals(4, result.size());
		assertEquals(new PathData.PathPoint(0.25f, 0.75f, 1), result.get(1));
		assertFalse(map.canWalkStraight(0.25f, 0.25f, 1, 1.25f, 0.25f, 1,
				(x, y) -> Float.NaN, null));
		assertTrue(map.canWalkStraight(0.25f, 0.25f, 1, 0.25f, 0.75f, 1,
				(x, y) -> Float.NaN, null));
	}

	@Test
	void reusesThreadLocalAStarWorkspace() throws Exception {
		Path path = directory.resolve("1.path");
		Path index = directory.resolve("1.idx");
		byte[] data = flatLinkedPath();
		Files.write(path, data);
		Files.write(index, index(data, 137));
		PathData.MapData map = PathData.MapData.load(path.toFile(), index.toFile());

		map.findPath(0.25f, 0.25f, 1, 1.25f, 0.25f, 1, 100, (x, y) -> Float.NaN);
		Object workspace = currentSearchWorkspace();
		Object firstNode = ((Object[]) field(workspace, "nodes"))[0];
		Object firstSearchNode = ((Object[]) field(workspace, "searchNodes"))[0];
		Object firstOpenNode = ((Object[]) field(workspace, "openNodes"))[0];
		Object openQueue = field(workspace, "open");

		map.findPath(0.25f, 0.25f, 1, 1.25f, 0.25f, 1, 100, (x, y) -> Float.NaN);

		assertSame(firstNode, ((Object[]) field(workspace, "nodes"))[0]);
		assertSame(firstSearchNode, ((Object[]) field(workspace, "searchNodes"))[0]);
		assertSame(firstOpenNode, ((Object[]) field(workspace, "openNodes"))[0]);
		assertSame(openQueue, field(workspace, "open"));
	}

	@Test
	void projectsAndFindsTheNearestReachablePointOnTheSameLayer() throws Exception {
		Path path = directory.resolve("1.path");
		Path index = directory.resolve("1.idx");
		byte[] data = flatLinkedPath((byte) 0xff);
		Files.write(path, data);
		Files.write(index, index(data, 137));
		PathData.MapData map = PathData.MapData.load(path.toFile(), index.toFile());

		assertEquals(new PathData.PathPoint(0.25f, 0.25f, 1),
				map.projectPoint(0.25f, 0.25f, 1.5f, (x, y) -> Float.NaN));
		assertNull(map.projectPoint(0.25f, 0.25f, 2, (x, y) -> Float.NaN));
		PathData.PathPoint nearest = map.nearestPathPoint(0.25f, 0.25f, 1, 2, 0.7f,
				(x, y) -> Float.NaN, (x, y, z) -> x > 0.5f);

		assertNotNull(nearest);
		assertEquals(0.75f, nearest.x());
		assertEquals(1, nearest.z());
	}

	@Test
	void distinguishesNodeBudgetExhaustionFromNoPath() throws Exception {
		Path path = directory.resolve("1.path");
		Path index = directory.resolve("1.idx");
		byte[] data = flatLinkedPath();
		Files.write(path, data);
		Files.write(index, index(data, 137));
		PathData.MapData map = PathData.MapData.load(path.toFile(), index.toFile());

		PathData.SearchResult result = map.searchAStar(0.25f, 0.25f, 1, 15.75f, 15.75f, 1, 1,
				(x, y) -> Float.NaN, null);

		assertEquals(PathData.SearchStatus.NODE_LIMIT, result.status());
		assertEquals(1, result.processedNodes());
	}

	@Test
	void shortStraightRoutesSkipAStar() throws Exception {
		Path path = directory.resolve("1.path");
		Path index = directory.resolve("1.idx");
		byte[] data = flatLinkedPath((byte) 0xff);
		Files.write(path, data);
		Files.write(index, index(data, 137));
		PathData.MapData map = PathData.MapData.load(path.toFile(), index.toFile());

		PathData.SearchResult result = map.searchAStar(0.25f, 0.25f, 1, 15.75f, 15.75f, 1, 1,
				(x, y) -> Float.NaN, null);

		assertEquals(PathData.SearchStatus.FOUND, result.status());
		assertEquals(PathData.SearchMode.DIRECT, result.mode());
		assertEquals(0, result.processedNodes());
		assertEquals(List.of(new PathData.PathPoint(0.25f, 0.25f, 1),
				new PathData.PathPoint(15.75f, 15.75f, 1)), result.path());
	}

	@Test
	void preservesIntermediateHeightsOnStraightRoutes() throws Exception {
		Path path = directory.resolve("1.path");
		Path index = directory.resolve("1.idx");
		byte[] data = terrainLinkedPath();
		Files.write(path, data);
		Files.write(index, index(data, 137));
		PathData.MapData map = PathData.MapData.load(path.toFile(), index.toFile());
		PathData.HeightProvider terrain = (x, y) -> x > 1 && x < 1.5f ? 2 : 1;

		assertFalse(map.canWalkStraight(0.25f, 0.25f, 1, 2.25f, 0.25f, 1, terrain, null));
		PathData.SearchResult result = map.searchAStar(0.25f, 0.25f, 1, 2.25f, 0.25f, 1, 100, terrain, null);

		assertEquals(PathData.SearchStatus.FOUND, result.status());
		assertTrue(result.path().stream().anyMatch(point -> point.z() == 2));
	}

	@Test
	void preservesSmallTerrainCurvesThatWouldSinkTheModel() throws Exception {
		Path path = directory.resolve("1.path");
		Path index = directory.resolve("1.idx");
		byte[] data = terrainLinkedPath();
		Files.write(path, data);
		Files.write(index, index(data, 137));
		PathData.MapData map = PathData.MapData.load(path.toFile(), index.toFile());
		PathData.HeightProvider terrain = (x, y) -> x > 1 && x < 1.5f ? 1.15f : 1;

		assertFalse(map.canWalkStraight(0.25f, 0.25f, 1, 2.25f, 0.25f, 1, terrain, null));
		PathData.SearchResult result = map.searchAStar(0.25f, 0.25f, 1, 2.25f, 0.25f, 1, 100, terrain, null);
		assertTrue(result.path().stream().anyMatch(point -> point.z() == 1.15f));
	}

	@Test
	void groundSearchFindsDetoursBeyondOneHundredMeters() throws Exception {
		Path path = directory.resolve("1.path");
		Path index = directory.resolve("1.idx");
		byte[] data = flatBlocksPath(16);
		int[] offsets = new int[16];
		for (int i = 0; i < offsets.length; i++) {
			offsets[i] = 137 + i * 23;
		}
		Files.write(path, data);
		Files.write(index, index(data, 256, 16, 1, 137, 0, offsets));
		PathData.MapData map = PathData.MapData.load(path.toFile(), index.toFile());

		PathData.EdgePassability passability = (startX, startY, startZ, targetX, targetY, targetZ) ->
				!(startX == 0.25f && startY == 0.25f && targetX == 0.75f && targetY == 0.25f);
		PathData.SearchResult result = map.searchAStar(0.25f, 0.25f, 1, 100.25f, 0.25f, 1, 50_000,
				(x, y) -> 1, passability);

		assertEquals(PathData.SearchStatus.FOUND, result.status());
		assertTrue(result.processedNodes() > 0);
	}

	@Test
	void hierarchicalBlockCorridorReducesLongRetailSearches() throws Exception {
		int columns = 12;
		int rows = 5;
		Path path = directory.resolve("1.path");
		Path index = directory.resolve("1.idx");
		byte[] data = flatBlockGridPath(columns, rows, true, true);
		Files.write(path, data);
		Files.write(index, index(data, columns * 16, rows * 16, 1, 137, 0,
				blockOffsets(columns * rows)));
		PathData.MapData map = PathData.MapData.load(path.toFile(), index.toFile());
		float targetX = columns * 16 - 0.25f;

		PathData.SearchResult lowLevel = map.searchAStar(0.25f, 0.25f, 1, targetX, 0.25f, 1,
				50_000, (x, y) -> 1, null, false);
		PathData.SearchResult hierarchical = map.searchAStar(0.25f, 0.25f, 1, targetX, 0.25f, 1,
				50_000, (x, y) -> 1, null, true);

		assertEquals(PathData.SearchStatus.FOUND, hierarchical.status());
		assertEquals(PathData.SearchMode.HIERARCHICAL, hierarchical.mode(),
				"hierarchicalNodes=" + hierarchical.processedNodes() + " lowLevelNodes=" + lowLevel.processedNodes());
		assertTrue(hierarchical.abstractNodes() > 0);
		assertTrue(lowLevel.status() != PathData.SearchStatus.FOUND
				|| hierarchical.processedNodes() * 2 < lowLevel.processedNodes());
	}

	@Test
	void hierarchicalCorridorFallsBackWhenRuntimeObstacleNeedsAWiderDetour() throws Exception {
		int columns = 16;
		int rows = 5;
		Path path = directory.resolve("1.path");
		Path index = directory.resolve("1.idx");
		byte[] data = flatBlockGridPath(columns, rows, false, false);
		Files.write(path, data);
		Files.write(index, index(data, columns * 16, rows * 16, 1, 137, 0,
				blockOffsets(columns * rows)));
		PathData.MapData map = PathData.MapData.load(path.toFile(), index.toFile());
		PathData.EdgePassability wallWithDistantGap = (startX, startY, startZ, targetX, targetY, targetZ) ->
				!crosses(startX, targetX, 128) || Math.max(startY, targetY) >= 48;

		PathData.SearchResult result = map.searchAStar(0.25f, 0.25f, 1, columns * 16 - 0.25f, 0.25f, 1,
				50_000, (x, y) -> 1, wallWithDistantGap, true);

		assertEquals(PathData.SearchStatus.FOUND, result.status());
		assertEquals(PathData.SearchMode.HIERARCHICAL_FALLBACK, result.mode());
		assertTrue(result.processedNodes() > 8_192);
	}

	@Test
	void hierarchicalSearchReportsFallbackWhenTheAbstractGraphHasNoRoute() throws Exception {
		int columns = 9;
		int rows = 2;
		Path path = directory.resolve("1.path");
		Path index = directory.resolve("1.idx");
		byte[] data = flatBlockGridPath(columns, rows, true, false);
		Files.write(path, data);
		Files.write(index, index(data, columns * 16, rows * 16, 1, 137, 0,
				blockOffsets(columns * rows)));
		PathData.MapData map = PathData.MapData.load(path.toFile(), index.toFile());

		PathData.SearchResult result = map.searchAStar(0.25f, 0.25f, 1, columns * 16 - 0.25f, 0.25f, 1,
				100, (x, y) -> 1, null, true);

		assertEquals(PathData.SearchStatus.NODE_LIMIT, result.status());
		assertEquals(PathData.SearchMode.HIERARCHICAL_FALLBACK, result.mode());
		assertTrue(result.abstractNodes() > 0);
	}

	@Test
	void movesDiagonallyOnOpenRetailGrid() throws Exception {
		Path path = directory.resolve("1.path");
		Path index = directory.resolve("1.idx");
		byte[] data = flatLinkedPath((byte) 0xff);
		Files.write(path, data);
		Files.write(index, index(data, 137));

		PathData.MapData map = PathData.MapData.load(path.toFile(), index.toFile());
		List<PathData.PathPoint> result = map.findPath(0.25f, 0.25f, 1, 0.75f, 0.75f, 1, 10,
				(x, y) -> Float.NaN);

		assertEquals(List.of(new PathData.PathPoint(0.25f, 0.25f, 1),
				new PathData.PathPoint(0.75f, 0.75f, 1)), result);
	}

	@Test
	void doesNotCutDiagonalCornerWhenEitherOrthogonalEdgeIsBlocked() throws Exception {
		Path path = directory.resolve("1.path");
		Path index = directory.resolve("1.idx");
		byte[] data = flatLinkedPath();
		Files.write(path, data);
		Files.write(index, index(data, 137));

		PathData.MapData map = PathData.MapData.load(path.toFile(), index.toFile());
		List<PathData.PathPoint> result = map.findPath(0.25f, 0.25f, 1, 0.75f, 0.75f, 1, 10,
				(x, y) -> Float.NaN);

		assertEquals(List.of(new PathData.PathPoint(0.25f, 0.25f, 1),
				new PathData.PathPoint(0.25f, 0.75f, 1), new PathData.PathPoint(0.75f, 0.75f, 1)), result);
	}

	@Test
	void doesNotUseADiagonalRejectedByRuntimeCollision() throws Exception {
		Path path = directory.resolve("1.path");
		Path index = directory.resolve("1.idx");
		byte[] data = flatLinkedPath((byte) 0xff);
		Files.write(path, data);
		Files.write(index, index(data, 137));

		PathData.MapData map = PathData.MapData.load(path.toFile(), index.toFile());
		List<PathData.PathPoint> result = map.findPath(0.25f, 0.25f, 1, 0.75f, 0.75f, 1, 10,
				(x, y) -> Float.NaN,
				(startX, startY, startZ, targetX, targetY, targetZ) -> startX == targetX || startY == targetY);

		assertNotNull(result);
		assertEquals(3, result.size());
		for (int i = 1; i < result.size(); i++) {
			PathData.PathPoint start = result.get(i - 1);
			PathData.PathPoint end = result.get(i);
			assertTrue(start.x() == end.x() || start.y() == end.y());
		}
	}

	@Test
	void runtimeOrthogonalCollisionDoesNotOverrideStaticDiagonalLinks() throws Exception {
		Path path = directory.resolve("1.path");
		Path index = directory.resolve("1.idx");
		byte[] data = flatLinkedPath((byte) 0xff);
		Files.write(path, data);
		Files.write(index, index(data, 137));

		PathData.MapData map = PathData.MapData.load(path.toFile(), index.toFile());
		List<PathData.PathPoint> result = map.findPath(0.25f, 0.25f, 1, 0.75f, 0.75f, 1, 10,
				(x, y) -> Float.NaN,
				(startX, startY, startZ, targetX, targetY, targetZ) -> startX != targetX && startY != targetY);

		assertEquals(List.of(new PathData.PathPoint(0.25f, 0.25f, 1),
				new PathData.PathPoint(0.75f, 0.75f, 1)), result);
	}

	@Test
	void usesFirstMatchingRetailLayerInsteadOfClosestHeight() throws Exception {
		Path path = directory.resolve("1.path");
		Path index = directory.resolve("1.idx");
		byte[] data = layeredFlatPath();
		Files.write(path, data);
		Files.write(index, index(data, 137));

		PathData.MapData map = PathData.MapData.load(path.toFile(), index.toFile());
		List<PathData.PathPoint> result = map.findPath(0.25f, 0.25f, 1.1f, 0.75f, 0.25f, 1.1f, 10,
				(x, y) -> Float.NaN);

		assertEquals(List.of(new PathData.PathPoint(0.25f, 0.25f, 1.5f),
				new PathData.PathPoint(0.75f, 0.25f, 1.5f)), result);
	}

	@Test
	void reroutesAroundRuntimeBlockedEdge() throws Exception {
		Path path = directory.resolve("1.path");
		Path index = directory.resolve("1.idx");
		byte[] data = flatLinkedPath((byte) 0xff);
		Files.write(path, data);
		Files.write(index, index(data, 137));

		PathData.MapData map = PathData.MapData.load(path.toFile(), index.toFile());
		PathService.BlockedSegment blocked = new PathService.BlockedSegment(0.25f, 0.25f, 1,
				1.25f, 0.25f, 1, 0.35f, Long.MAX_VALUE);
		List<PathData.PathPoint> result = map.findPath(0.25f, 0.25f, 1, 1.25f, 0.25f, 1, 100,
				(x, y) -> Float.NaN, blocked::allows);

		assertNotNull(result);
		assertEquals(new PathData.PathPoint(0.75f, 0.75f, 1), result.get(1));
	}

	@Test
	void supportsConcurrentNpcPathQueries() throws Exception {
		Path path = directory.resolve("1.path");
		Path index = directory.resolve("1.idx");
		byte[] data = flatLinkedPath();
		Files.write(path, data);
		Files.write(index, index(data, 137));
		PathData.MapData map = PathData.MapData.load(path.toFile(), index.toFile());

		try (var executor = Executors.newFixedThreadPool(16)) {
			List<java.util.concurrent.Callable<List<PathData.PathPoint>>> queries = new java.util.ArrayList<>();
			for (int i = 0; i < 1_000; i++) {
				queries.add(() -> map.findPath(0.25f, 0.25f, 1, 15.75f, 0.25f, 1, 100,
						(x, y) -> Float.NaN));
			}
			for (var result : executor.invokeAll(queries, 30, TimeUnit.SECONDS)) {
				assertNotNull(result.get());
			}
		}
	}

	@Test
	void supportsCoordinatesAcrossTheFullRetailWorldSize() throws Exception {
		Path path = directory.resolve("1.path");
		Path index = directory.resolve("1.idx");
		byte[] data = flatLinkedPath();
		Files.write(path, data);
		Files.write(index, index(data, 137));

		PathData.MapData map = PathData.MapData.load(path.toFile(), index.toFile());
		List<PathData.PathPoint> result = map.findPath(15.25f, 0.25f, 1, 15.75f, 0.25f, 1, 10,
				(x, y) -> Float.NaN);

		assertNotNull(result);
		assertEquals(2, result.size());
	}

	@Test
	void crossesRetailPortalBetweenBlocks() throws Exception {
		Path path = directory.resolve("1.path");
		Path index = directory.resolve("1.idx");
		byte[] data = portalPath();
		Files.write(path, data);
		Files.write(index, index(data, 32, 16, 1, 137, 1, 141, 164));

		PathData.MapData map = PathData.MapData.load(path.toFile(), index.toFile());
		PathData.SearchResult result = map.searchAStar(15.75f, 0.25f, 1, 16.25f, 0.25f, 1, 10,
				(x, y) -> Float.NaN, null);

		assertEquals(PathData.SearchStatus.FOUND, result.status());
		assertEquals(2, result.path().size());
	}

	@Test
	void followsComplexEdgesAcrossLayersWithinBlock() throws Exception {
		Path path = directory.resolve("1.path");
		Path index = directory.resolve("1.idx");
		byte[] data = layeredComplexPath();
		Files.write(path, data);
		Files.write(index, index(data, 16, 16, 33, 169, 0, 169));

		PathData.MapData map = PathData.MapData.load(path.toFile(), index.toFile());
		PathData.SearchResult result = map.searchAStar(0.25f, 0.25f, 1, 1.25f, 0.25f, 2, 10,
				(x, y) -> Float.NaN, null);

		assertEquals(PathData.SearchStatus.FOUND, result.status());
		assertEquals(new PathData.PathPoint(1.25f, 0.25f, 2), result.path().get(result.path().size() - 1));
	}

	@Test
	void rejectsRetailEdgesWithTwentyMeterTraversalCost() throws Exception {
		Path path = directory.resolve("1.path");
		Path index = directory.resolve("1.idx");
		byte[] data = layeredComplexPath(100, 2100);
		Files.write(path, data);
		Files.write(index, index(data, 16, 16, 33, 169, 0, 169));
		PathData.MapData map = PathData.MapData.load(path.toFile(), index.toFile());

		assertEquals(PathData.SearchStatus.NO_PATH,
				map.searchAStar(0.25f, 0.25f, 1, 1.25f, 0.25f, 21, 10, (x, y) -> Float.NaN, null).status());
	}

	private static byte[] flatLinkedPath() {
		return flatLinkedPath((byte) 0xfe);
	}

	private static byte[] flatLinkedPath(byte firstCellLinks) {
		ByteBuffer buffer = ByteBuffer.allocate(673).order(ByteOrder.LITTLE_ENDIAN);
		buffer.position(16);
		buffer.putInt(0x00060005);
		buffer.position(128);
		buffer.putInt(1).put((byte) 0); // node table
		buffer.putInt(0); // portals
		buffer.put((byte) 1).put((byte) 1); // one sector, flat with links
		buffer.put((byte) 0x0f);
		for (int i = 0; i < 4; i++) {
			buffer.putInt(-1); // outer edge blocked
		}
		buffer.putInt(100);
		byte[] links = new byte[512];
		Arrays.fill(links, (byte) 0xff);
		links[0] = firstCellLinks;
		buffer.put(links);
		return buffer.array();
	}

	private static byte[] terrainLinkedPath() {
		ByteBuffer buffer = ByteBuffer.allocate(669).order(ByteOrder.LITTLE_ENDIAN);
		buffer.position(16);
		buffer.putInt(0x00060005);
		buffer.position(128);
		buffer.putInt(1).put((byte) 0); // node table
		buffer.putInt(0); // portals
		buffer.put((byte) 1).put((byte) 3); // one terrain-height sector with links
		buffer.put((byte) 0x0f);
		for (int i = 0; i < 4; i++) {
			buffer.putInt(-1);
		}
		byte[] links = new byte[512];
		Arrays.fill(links, (byte) 0xff);
		buffer.put(links);
		return buffer.array();
	}

	private static byte[] flatBlocksPath(int blocks) {
		ByteBuffer buffer = ByteBuffer.allocate(137 + blocks * 23).order(ByteOrder.LITTLE_ENDIAN);
		buffer.position(16);
		buffer.putInt(0x00060005);
		buffer.position(128);
		buffer.putInt(1).put((byte) 0); // node table
		buffer.putInt(0); // portals
		for (int i = 0; i < blocks; i++) {
			putFlatSectorBlock(buffer, 0x0f, i + 1 < blocks ? 0 : -1, -1, i > 0 ? 0 : -1, -1);
		}
		return buffer.array();
	}

	private static byte[] flatBlockGridPath(int columns, int rows, boolean wall, boolean bottomGap) {
		ByteBuffer buffer = ByteBuffer.allocate(137 + columns * rows * 23).order(ByteOrder.LITTLE_ENDIAN);
		buffer.position(16);
		buffer.putInt(0x00060005);
		buffer.position(128);
		buffer.putInt(1).put((byte) 0);
		buffer.putInt(0);
		for (int row = 0; row < rows; row++) {
			for (int column = 0; column < columns; column++) {
				boolean gap = bottomGap && row + 1 == rows;
				boolean eastWall = wall && !gap && column == columns / 2 - 1;
				boolean westWall = wall && !gap && column == columns / 2;
				putFlatSectorBlock(buffer, 0x0f,
						column + 1 < columns && !eastWall ? 0 : -1,
						row + 1 < rows ? 0 : -1,
						column > 0 && !westWall ? 0 : -1,
						row > 0 ? 0 : -1);
			}
		}
		return buffer.array();
	}

	private static int[] blockOffsets(int blocks) {
		int[] offsets = new int[blocks];
		for (int i = 0; i < offsets.length; i++) {
			offsets[i] = 137 + i * 23;
		}
		return offsets;
	}

	private static boolean crosses(float start, float target, float coordinate) {
		return start < coordinate && target >= coordinate || target < coordinate && start >= coordinate;
	}

	private static byte[] portalPath() {
		ByteBuffer buffer = ByteBuffer.allocate(187).order(ByteOrder.LITTLE_ENDIAN);
		buffer.position(16);
		buffer.putInt(0x00060005);
		buffer.position(128);
		buffer.putInt(1).put((byte) 0); // node table
		buffer.putInt(1).putInt(128); // one portal to layer 0
		putFlatSectorBlock(buffer, 0x0e, 0, -1, -1, -1);
		putFlatSectorBlock(buffer, 0x0f, -1, -1, -1, -1);
		return buffer.array();
	}

	private static byte[] layeredFlatPath() {
		ByteBuffer buffer = ByteBuffer.allocate(182).order(ByteOrder.LITTLE_ENDIAN);
		buffer.position(16);
		buffer.putInt(0x00060005);
		buffer.position(128);
		buffer.putInt(1).put((byte) 0); // node table
		buffer.putInt(0); // portals
		buffer.put((byte) 2);
		for (int height : new int[] {100, 150}) {
			buffer.put((byte) 0).put((byte) 0x0f);
			for (int direction = 0; direction < 4; direction++) {
				buffer.putInt(-1);
			}
			buffer.putInt(height);
		}
		return buffer.array();
	}

	private static void putFlatSectorBlock(ByteBuffer buffer, int boundaryMask, int east, int north, int west,
			int south) {
		buffer.put((byte) 1).put((byte) 0).put((byte) boundaryMask);
		buffer.putInt(east).putInt(north).putInt(west).putInt(south).putInt(100);
	}

	private static byte[] layeredComplexPath() {
		return layeredComplexPath(100, 200);
	}

	private static byte[] layeredComplexPath(int startHeight, int targetHeight) {
		ByteBuffer buffer = ByteBuffer.allocate(184).order(ByteOrder.LITTLE_ENDIAN);
		buffer.position(16);
		buffer.putInt(0x00060005);
		buffer.position(128);
		buffer.putInt(33);
		buffer.putInt(startHeight).putShort((short) 0).putShort((short) 0).put((byte) 2).putInt(13 << 7);
		buffer.putInt(targetHeight).putShort((short) 1).putShort((short) 0).put((byte) 1).putShort((short) 11);
		buffer.putInt(targetHeight).putShort((short) 2).putShort((short) 0).put((byte) 0);
		buffer.putInt(0); // portals
		buffer.put((byte) 2);
		buffer.put((byte) 16).putInt(0).putShort((short) 1); // layer 1
		buffer.put((byte) 16).putInt(13).putShort((short) 2); // layer 0
		return buffer.array();
	}

	private static byte[] index(byte[] path, int blockOffset) throws Exception {
		return index(path, 16, 16, 1, 137, 0, blockOffset);
	}

	private static byte[] index(byte[] path, int width, int height, int nodeSize, int portalOffset,
			int portalCount, int... blockOffsets) throws Exception {
		ByteBuffer buffer = ByteBuffer.allocate(84 + blockOffsets.length * 4).order(ByteOrder.LITTLE_ENDIAN);
		buffer.put(new byte[] {'A', 'I', 'P', 'X'}).putInt(1);
		buffer.putInt(width).putInt(height).putInt((width + 15) / 16).putInt((height + 15) / 16);
		buffer.putInt(132).putInt(nodeSize).putInt(portalOffset).putInt(portalCount);
		buffer.putLong(path.length).put(MessageDigest.getInstance("SHA-256").digest(path));
		buffer.putInt(blockOffsets.length);
		for (int blockOffset : blockOffsets) {
			buffer.putInt(blockOffset);
		}
		return buffer.array();
	}

	private static Object currentSearchWorkspace() throws Exception {
		Field workspace = PathData.MapData.class.getDeclaredField("SEARCH_WORKSPACE");
		workspace.setAccessible(true);
		return ((ThreadLocal<?>) workspace.get(null)).get();
	}

	private static Object field(Object target, String name) throws Exception {
		Field field = target.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}

	private static void restore(String key, String value) {
		if (value == null) {
			System.clearProperty(key);
		} else {
			System.setProperty(key, value);
		}
	}
}
