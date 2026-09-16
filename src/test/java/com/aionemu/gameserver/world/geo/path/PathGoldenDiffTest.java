package com.aionemu.gameserver.world.geo.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 寻路结果的 golden 差分守护：固定地图 + 固定起终点，把搜索的「状态/模式/节点数/路径点序列」逐字比对，
 * 并在同一线程上「深搜 → 短搜」交替后再次比对，防止工作区复用与容量策略改动改变走位结果。
 *
 * <p>Golden diff guard for path search results: with a fixed map and fixed start/goal pairs it compares status,
 * mode, node counts and the exact waypoint sequence, then repeats the run after a deep search on the same thread so
 * that workspace reuse and capacity changes can never alter where NPCs walk.</p>
 *
 * <p>录制模式：{@code mvn -Dtest=PathGoldenDiffTest -Dpath.golden.record=true test} 会把当前实现的
 * 结果写入 {@code src/test/resources/aion/geo/path-golden.txt}；平时只读取比对。
 * Record mode writes the snapshot with {@code -Dpath.golden.record=true}; otherwise the snapshot is only read.</p>
 */
class PathGoldenDiffTest {

	private static final String GOLDEN_RESOURCE = "/aion/geo/path-golden.txt";
	private static final Path GOLDEN_SOURCE = Path.of("src/test/resources/aion/geo/path-golden.txt");
	private static final int COLUMNS = 8;
	private static final int ROWS = 4;
	private static final int WIDTH = COLUMNS * 16;
	private static final int HEIGHT = ROWS * 16;

	@TempDir
	Path directory;

	@Test
	void recordedPathResultsStayIdenticalAcrossWorkspaceReuse() throws Exception {
		PathData.MapData openMap = loadMap(flatBlockGridPath(COLUMNS, ROWS, true, true));
		PathData.MapData sealedMap = loadMap(flatBlockGridPath(COLUMNS, ROWS, true, false));

		String first = record(openMap, sealedMap);

		// 同一线程上先跑一次深搜（长距离绕墙），再重放同一组用例：
		// 复用工作区/节点池不得改变任何一条结果（这是容量策略改动的核心护栏）。
		// Run a deep search first, then replay the same cases: workspace and node-pool reuse must not change a thing.
		openMap.searchAStar(0.25f, 0.25f, 1, WIDTH - 0.25f, HEIGHT - 0.25f, 1, 50_000, terrain(), null);
		String second = record(openMap, sealedMap);

		assertEquals(first, second, "工作区复用后同一组用例的结果必须逐字一致");
		assertGolden(first);
	}

	/** 依次执行固定用例并生成规范化的文本快照。 / Runs the fixed cases and renders the canonical snapshot. */
	private String record(PathData.MapData openMap, PathData.MapData sealedMap) {
		StringBuilder out = new StringBuilder();
		for (Case testCase : cases()) {
			PathData.MapData map = testCase.sealed() ? sealedMap : openMap;
			PathData.SearchResult result = map.searchAStar(testCase.startX(), testCase.startY(), testCase.startZ(),
					testCase.targetX(), testCase.targetY(), testCase.targetZ(), testCase.maxNodes(), terrain(), null);
			out.append(testCase.name()).append('|').append(result.status()).append('|').append(result.mode())
					.append('|').append("processed=").append(result.processedNodes())
					.append('|').append("abstract=").append(result.abstractNodes())
					.append('|').append("path=").append(render(result.path())).append('\n');
		}
		return out.toString();
	}

	private void assertGolden(String actual) throws Exception {
		if (Boolean.getBoolean("path.golden.record")) {
			Files.createDirectories(GOLDEN_SOURCE.getParent());
			Files.writeString(GOLDEN_SOURCE, actual, StandardCharsets.UTF_8);
			return;
		}
		String expected;
		try (InputStream input = getClass().getResourceAsStream(GOLDEN_RESOURCE)) {
			assertNotNull(input, GOLDEN_RESOURCE + " 缺失：先用 -Dpath.golden.record=true 录制");
			expected = new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
		assertEquals(expected, actual, "寻路结果相对 golden 快照发生变化");
	}

	private static List<Case> cases() {
		float farX = WIDTH - 0.25f;
		float farY = HEIGHT - 0.25f;
		return List.of(
				// 短距离直连 / short direct hop inside one sector
				new Case("short", 0.25f, 0.25f, 1, 2.25f, 0.25f, 1, 50_000, false),
				// 绕墙：墙挡住第 3/4 列，必须从最后一行绕过去 / wall between columns 3 and 4, detour via the last row
				new Case("around-wall", 0.25f, 0.25f, 1, farX, 0.25f, 1, 50_000, false),
				// 对角长距离（穿墙缺口 + 跨多个块）/ long diagonal across blocks
				new Case("diagonal", 0.25f, 0.25f, 1, farX, farY, 1, 50_000, false),
				// 同一个格子内的零距离查询 / zero-distance query inside one cell
				new Case("same-cell", 0.25f, 0.25f, 1, 0.25f, 0.25f, 1, 50_000, false),
				// 节点预算耗尽 / node budget exhausted
				new Case("node-limit", 0.25f, 0.25f, 1, farX, 0.25f, 1, 5, false),
				// 完全封死的墙：无路 / sealed wall: no path
				new Case("no-path", 0.25f, 0.25f, 1, farX, 0.25f, 1, 50_000, true));
	}

	/** 台阶式高度：每 8 世界单位抬升 0.5，用于验证路径点 z 序列。 / Step heights every 8 units; guards the z sequence. */
	private static PathData.HeightProvider terrain() {
		return (x, y) -> 1f + (float) ((int) (x / 8f)) * 0.5f;
	}

	private static String render(List<PathData.PathPoint> path) {
		if (path == null) {
			return "null";
		}
		List<String> points = new ArrayList<>(path.size());
		for (PathData.PathPoint point : path) {
			points.add("(" + point.x() + "," + point.y() + "," + point.z() + ")");
		}
		return points.toString();
	}

	private PathData.MapData loadMap(byte[] data) throws Exception {
		Path path = directory.resolve("1.path");
		Path index = directory.resolve("1.idx");
		Files.write(index, index(data, WIDTH, HEIGHT, 1, 137, 0, blockOffsets(COLUMNS * ROWS)));
		try (GZIPOutputStream output = new GZIPOutputStream(Files.newOutputStream(path))) {
			output.write(data);
		}
		return PathData.MapData.load(new PathData.PathFiles(path.toFile(), index.toFile()));
	}

	/**
	 * 一条固定用例。
	 * One fixed search case.
	 */
	private record Case(String name, float startX, float startY, float startZ, float targetX, float targetY,
			float targetZ, int maxNodes, boolean sealed) {
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

	private static void putFlatSectorBlock(ByteBuffer buffer, int boundaryMask, int east, int north, int west,
			int south) {
		buffer.put((byte) 1).put((byte) 0).put((byte) boundaryMask);
		buffer.putInt(east).putInt(north).putInt(west).putInt(south).putInt(100);
	}

	private static int[] blockOffsets(int blocks) {
		int[] offsets = new int[blocks];
		for (int i = 0; i < offsets.length; i++) {
			offsets[i] = 137 + i * 23;
		}
		return offsets;
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
}
