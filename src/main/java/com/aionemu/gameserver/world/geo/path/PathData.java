package com.aionemu.gameserver.world.geo.path;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPInputStream;

/**
 * 路径数据仓库：扫描、加载并缓存各世界的地图路径数据。
 * Path data store: scans, loads and caches map path data per world.
 */
@Slf4j
public final class PathData {

	private static final String PATH_DIR = "geo/path";
	private static final int INDEX_MAGIC = 0x58504941; // AIPX
	private static final int INDEX_VERSION = 1;
	private static final int PATH_VERSION_MAJOR = 6;
	private final Map<Integer, PathFiles> files = new ConcurrentHashMap<>();
	private final Map<Integer, File> materialized = new ConcurrentHashMap<>();
	private final Map<Integer, MapData> maps = new LinkedHashMap<>(16, 0.75f, true);
	private final Map<Integer, ReentrantLock> locks = new ConcurrentHashMap<>();
	private final Set<Integer> failedMaps = ConcurrentHashMap.newKeySet();

	public int scan() {
		files.clear();
		materialized.clear();
		failedMaps.clear();
		File directory = Config.geoFile(PATH_DIR);
		File[] indexes = directory.listFiles((dir, name) -> name.endsWith(".idx"));
		if (indexes == null) {
			return 0;
		}
		for (File index : indexes) {
			String stem = index.getName().substring(0, index.getName().length() - 4);
			try {
				int worldId = Integer.parseInt(stem);
				File compressed = new File(directory, stem + ".path.gz");
				if (compressed.isFile()) {
					files.put(worldId, new PathFiles(compressed, index));
				}
			} catch (NumberFormatException ignored) {
			}
		}
		return files.size();
	}

	public boolean hasMap(int worldId) {
		return files.containsKey(worldId) && !failedMaps.contains(worldId);
	}

	public MapData getMap(int worldId) {
		if (!GeoDataConfig.GEO_PATH_ENABLE) {
			return null;
		}
		if (failedMaps.contains(worldId)) {
			throw new IllegalStateException("PATH map is unavailable: " + worldId);
		}
		synchronized (maps) {
			MapData cached = maps.get(worldId);
			if (cached != null) {
				return cached;
			}
		}
		PathFiles source = files.get(worldId);
		if (source == null) {
			return null;
		}
		ReentrantLock lock = locks.computeIfAbsent(worldId, ignored -> new ReentrantLock());
		try {
			lock.lockInterruptibly();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw interruptedLoad(worldId, e);
		}
		try {
			synchronized (maps) {
				MapData cached = maps.get(worldId);
				if (cached != null) {
					return cached;
				}
			}
			try {
				MapData loaded = loadMap(worldId, source);
				synchronized (maps) {
					maps.put(worldId, loaded);
					evictIfNeeded();
				}
				if (Thread.currentThread().isInterrupted()) {
					throw interruptedLoad(worldId, null);
				}
				return loaded;
			} catch (CancellationException e) {
				throw e;
			} catch (ClosedByInterruptException e) {
				throw interruptedLoad(worldId, e);
			} catch (IOException | RuntimeException e) {
				failedMaps.add(worldId);
				log.error(I18n.get("log.ec9434700078", worldId, e.getMessage()), e);
				throw new IllegalStateException("Failed to load PATH map " + worldId, e);
			}
		} finally {
			lock.unlock();
		}
	}

	private MapData loadMap(int worldId, PathFiles source) throws IOException {
		boolean interrupted = false;
		try {
			for (int attempt = 0; ; attempt++) {
				try {
					File path = materialized.get(worldId);
					if (path == null) {
						path = materialize(source);
						materialized.put(worldId, path);
					}
					return MapData.load(path, source.index());
				} catch (ClosedByInterruptException e) {
					interrupted = true;
					Thread.interrupted();
					if (attempt > 0) {
						throw e;
					}
				}
			}
		} finally {
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private static CancellationException interruptedLoad(int worldId, Exception cause) {
		CancellationException failure = new CancellationException("PATH map load interrupted: " + worldId);
		if (cause != null) {
			failure.initCause(cause);
		}
		return failure;
	}

	private static File materialize(PathFiles source) throws IOException {
		PathMetadata metadata = PathMetadata.read(source.index());
		String name = source.compressed().getName();
		File cache = Config.cacheFile("path/" + name.substring(0, name.length() - 3));
		if (matches(cache, metadata)) {
			return cache;
		}
		Path parent = cache.toPath().toAbsolutePath().getParent();
		Files.createDirectories(parent);
		Path temporary = Files.createTempFile(parent, cache.getName() + ".", ".tmp");
		MessageDigest digest = sha256();
		try {
			try (InputStream input = new DigestInputStream(new GZIPInputStream(Files.newInputStream(source.compressed().toPath())), digest)) {
				Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
			}
			if (Files.size(temporary) != metadata.size() || !MessageDigest.isEqual(digest.digest(), metadata.sha256())) {
				throw new IOException("Compressed path differs from index: " + source.compressed());
			}
			try {
				Files.move(temporary, cache.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temporary, cache.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
			return cache;
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	private static boolean matches(File path, PathMetadata metadata) throws IOException {
		if (!path.isFile() || path.length() != metadata.size()) {
			return false;
		}
		MessageDigest digest = sha256();
		try (InputStream input = new DigestInputStream(Files.newInputStream(path.toPath()), digest)) {
			input.transferTo(OutputStream.nullOutputStream());
		}
		return MessageDigest.isEqual(digest.digest(), metadata.sha256());
	}

	private static MessageDigest sha256() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private void evictIfNeeded() {
		int limit = GeoDataConfig.GEO_PATH_CACHE_SIZE;
		while (limit > 0 && maps.size() > limit) {
			maps.remove(maps.keySet().iterator().next());
		}
	}

	int loadedMapCount() {
		synchronized (maps) {
			return maps.size();
		}
	}

	@FunctionalInterface
	/**
	 * 指定坐标处的地面高度提供者。
	 * Provides ground height at the given coordinates.
	 */
	public interface HeightProvider {
		float get(float x, float y);
	}

	/**
	 * 线段可通过性判定。
	 * Decides whether a segment is passable.
	 */
	@FunctionalInterface
	public interface EdgePassability {
		boolean canPass(float startX, float startY, float startZ, float targetX, float targetY, float targetZ);
	}

	/**
	 * 点可通过性判定。
	 * Decides whether a point is passable.
	 */
	@FunctionalInterface
	public interface PointPassability {
		boolean canPass(float x, float y, float z);
	}

	/** 路径途经点 / Path waypoint */
	public record PathPoint(float x, float y, float z) {}

	/** 搜索结果状态 / Search result status */
	public enum SearchStatus {
		/** 已找到路径 / Path found */
		FOUND,
		/** 无可行路径 / No path */
		NO_PATH,
		/** 节点预算耗尽 / Node budget exhausted */
		NODE_LIMIT,
		/** 搜索被中断 / Search interrupted */
		INTERRUPTED,
		/** 起点或终点无效 / Invalid start or target */
		INVALID_POSITION
	}

	/** 搜索模式 / Search mode */
	public enum SearchMode {
		/** 直线直达 / Direct */
		DIRECT,
		/** 底层网格搜索 / Low-level grid search */
		LOW_LEVEL,
		/** 分层搜索 / Hierarchical search */
		HIERARCHICAL,
		/** 分层搜索回退 / Hierarchical fallback */
		HIERARCHICAL_FALLBACK
	}

	/**
	 * 搜索结果：状态、路径点、处理节点数与搜索模式。
	 * Search result: status, waypoints, processed node count and search mode.
	 */
	public record SearchResult(SearchStatus status, List<PathPoint> path, int processedNodes, SearchMode mode,
			int abstractNodes) {

		private static SearchResult direct(List<PathPoint> path) {
			return new SearchResult(SearchStatus.FOUND, path, 0, SearchMode.DIRECT, 0);
		}

		private static SearchResult found(List<PathPoint> path, int processedNodes) {
			return new SearchResult(SearchStatus.FOUND, path, processedNodes, SearchMode.LOW_LEVEL, 0);
		}

		private static SearchResult failed(SearchStatus status, int processedNodes) {
			return new SearchResult(status, null, processedNodes, SearchMode.LOW_LEVEL, 0);
		}

		private SearchResult hierarchical(SearchMode searchMode, int extraProcessedNodes, int processedAbstractNodes) {
			return new SearchResult(status, path, processedNodes + extraProcessedNodes, searchMode,
					abstractNodes + processedAbstractNodes);
		}
	}

	private record PathFiles(File compressed, File index) {}

	private record PathMetadata(long size, byte[] sha256) {

		private static PathMetadata read(File index) throws IOException {
			byte[] bytes = Files.readAllBytes(index.toPath());
			ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
			if (data.remaining() < 84 || data.getInt(0) != INDEX_MAGIC || data.getInt(4) != INDEX_VERSION) {
				throw new IOException("Unsupported path index: " + index);
			}
			long size = data.getLong(40);
			if (size < 1 || size > Integer.MAX_VALUE) {
				throw new IOException("Invalid path size in index: " + index);
			}
			return new PathMetadata(size, Arrays.copyOfRange(bytes, 48, 80));
		}
	}

	/**
	 * 单张地图的路径数据：分层块（Block）与扇区（Sector）结构。
	 * Path data for one map: hierarchical block and sector structure.
	 */
	public static final class MapData {

		private static final int MAX_PROCESSED_NODES = 49_999;
		private static final int MAX_PATH_POINTS = 20_000;
		private static final int HIERARCHICAL_MIN_BLOCK_DISTANCE = 8;
		private static final int HIERARCHICAL_MAX_ABSTRACT_NODES = 2_048;
		private static final int HIERARCHICAL_FINE_MAX_NODES = 8_192;
		private static final float MAX_ADJACENT_HEIGHT_DELTA = 1.5f;
		private static final float MAX_STRAIGHT_HEIGHT_DEVIATION = 0.10f;
		private static final int[] DX = {1, 0, -1, 0};
		private static final int[] DY = {0, 1, 0, -1};
		private static final int[] DIAGONAL_FIRST = {0, 1, 0, 2};
		private static final int[] DIAGONAL_SECOND = {1, 2, 3, 3};
		private static final Comparator<OpenNode> OPEN_NODE_ORDER = Comparator.comparingDouble(OpenNode::score)
				.thenComparing(Comparator.comparingLong(OpenNode::sequence).reversed());
		private static final Comparator<BlockOpenNode> BLOCK_OPEN_NODE_ORDER = Comparator.comparingDouble(BlockOpenNode::score)
				.thenComparing(Comparator.comparingLong(BlockOpenNode::sequence).reversed());
		// ponytail: 每线程保留峰值工作区；只有实测极端搜索长期占内存时才增加容量上限。
		private static final ThreadLocal<SearchWorkspace> SEARCH_WORKSPACE = ThreadLocal.withInitial(SearchWorkspace::new);
		private final ByteBuffer data;
		private final int width;
		private final int height;
		private final int blockColumns;
		private final int blockRows;
		private final int nodeTableOffset;
		private final int nodeTableSize;
		private final int portalOffset;
		private final int portalCount;
		private final int[] blockOffsets;
		private final AtomicReferenceArray<Block> blocks;
		private final Map<Integer, int[]> blockNeighbors = new ConcurrentHashMap<>();

		private MapData(ByteBuffer data, int width, int height, int blockColumns, int blockRows,
				int nodeTableOffset, int nodeTableSize, int portalOffset, int portalCount, int[] blockOffsets) {
			this.data = data;
			this.width = width;
			this.height = height;
			this.blockColumns = blockColumns;
			this.blockRows = blockRows;
			this.nodeTableOffset = nodeTableOffset;
			this.nodeTableSize = nodeTableSize;
			this.portalOffset = portalOffset;
			this.portalCount = portalCount;
			this.blockOffsets = blockOffsets;
			this.blocks = new AtomicReferenceArray<>(blockOffsets.length);
		}

		static MapData load(File path, File index) throws IOException {
			byte[] indexBytes = Files.readAllBytes(index.toPath());
			ByteBuffer idx = ByteBuffer.wrap(indexBytes).order(ByteOrder.LITTLE_ENDIAN);
			if (idx.remaining() < 84 || idx.getInt(0) != INDEX_MAGIC || idx.getInt(4) != INDEX_VERSION) {
				throw new IOException("Unsupported path index: " + index);
			}
			int width = positive(idx.getInt(8), "width");
			int height = positive(idx.getInt(12), "height");
			int columns = positive(idx.getInt(16), "block columns");
			int rows = positive(idx.getInt(20), "block rows");
			int nodeOffset = positive(idx.getInt(24), "node table offset");
			int nodeSize = positive(idx.getInt(28), "node table size");
			int portalOffset = positive(idx.getInt(32), "portal table offset");
			int portalCount = idx.getInt(36);
			long expectedSize = idx.getLong(40);
			int blockCount = idx.getInt(80);
			if (portalCount < 0 || blockCount != columns * rows || indexBytes.length != 84L + blockCount * 4L) {
				throw new IOException("Invalid path index dimensions: " + index);
			}
			long size = path.length();
			if (size != expectedSize || size > Integer.MAX_VALUE) {
				throw new IOException("Path file size differs from index: " + path);
			}
			ByteBuffer data;
			try (FileChannel channel = FileChannel.open(path.toPath(), StandardOpenOption.READ)) {
				data = channel.map(FileChannel.MapMode.READ_ONLY, 0, size).order(ByteOrder.LITTLE_ENDIAN);
			}
			if ((data.getInt(16) >>> 16) != PATH_VERSION_MAJOR || nodeOffset + nodeSize > size
					|| portalOffset + portalCount * 4L > size) {
				throw new IOException("Invalid path data header: " + path);
			}
			int[] offsets = new int[blockCount];
			int previous = portalOffset + portalCount * 4;
			for (int i = 0; i < offsets.length; i++) {
				int offset = idx.getInt(84 + i * 4);
				if (offset < previous || offset >= size) {
					throw new IOException("Invalid block offset " + offset + " in " + index);
				}
				offsets[i] = offset;
				previous = offset;
			}
			return new MapData(data, width, height, columns, rows, nodeOffset, nodeSize, portalOffset,
					portalCount, offsets);
		}

		private static int positive(int value, String name) throws IOException {
			if (value < 1) {
				throw new IOException("Invalid " + name + ": " + value);
			}
			return value;
		}

		public List<PathPoint> findPath(float startX, float startY, float startZ, float targetX, float targetY,
				float targetZ, int maxNodes, HeightProvider terrain) {
			return findPath(startX, startY, startZ, targetX, targetY, targetZ, maxNodes, terrain, null);
		}

		public List<PathPoint> findPath(float startX, float startY, float startZ, float targetX, float targetY,
				float targetZ, int maxNodes, HeightProvider terrain, EdgePassability passability) {
			return searchAStar(startX, startY, startZ, targetX, targetY, targetZ, maxNodes, terrain, passability).path();
		}

		SearchResult searchAStar(float startX, float startY, float startZ, float targetX, float targetY,
				float targetZ, int maxNodes, HeightProvider terrain, EdgePassability passability) {
			return searchAStar(startX, startY, startZ, targetX, targetY, targetZ, maxNodes, terrain, passability, false);
		}

		SearchResult searchAStar(float startX, float startY, float startZ, float targetX, float targetY,
				float targetZ, int maxNodes, HeightProvider terrain, EdgePassability passability, boolean hierarchical) {
			workspace().resetNodes();
			Node start = findNode(startX, startY, startZ, terrain);
			Node target = findNode(targetX, targetY, targetZ, terrain);
			if (start == null || target == null) {
				return SearchResult.failed(SearchStatus.INVALID_POSITION, 0);
			}
			float deltaX = targetX - startX;
			float deltaY = targetY - startY;
			float deltaZ = targetZ - startZ;
			float distanceSquared = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
			if (canWalkStraight(start, target, terrain, passability)) {
				return SearchResult.direct(List.of(point(start), point(target)));
			}
			float searchRadiusSquared = Math.max(2_500, 2 * distanceSquared);
			int budget = Math.min(MAX_PROCESSED_NODES, Math.max(1, maxNodes));
			if (hierarchical && usesHierarchicalSearch(start, target)) {
				BlockPath corridor = findBlockPath(start.sector().block.id(), target.sector().block.id());
				if (corridor.status() == SearchStatus.INTERRUPTED) {
					return SearchResult.failed(SearchStatus.INTERRUPTED, 0)
							.hierarchical(SearchMode.HIERARCHICAL, 0, corridor.processedNodes());
				}
				if (corridor.blocks() != null) {
					SearchResult refined = refineBlockPath(start, target, corridor.blocks(),
							Math.min(budget, HIERARCHICAL_FINE_MAX_NODES), terrain, passability);
					if (refined.status() == SearchStatus.FOUND || refined.status() == SearchStatus.INTERRUPTED) {
						return refined.hierarchical(SearchMode.HIERARCHICAL, 0, corridor.processedNodes());
					}
					SearchResult fallback = searchLowLevel(start, target, startX, startY, startZ, targetX, targetY,
							targetZ, searchRadiusSquared, budget, terrain, passability, null);
					return fallback.hierarchical(SearchMode.HIERARCHICAL_FALLBACK, refined.processedNodes(),
							corridor.processedNodes());
				}
				SearchResult fallback = searchLowLevel(start, target, startX, startY, startZ, targetX, targetY,
						targetZ, searchRadiusSquared, budget, terrain, passability, null);
				return fallback.hierarchical(SearchMode.HIERARCHICAL_FALLBACK, 0, corridor.processedNodes());
			}
			return searchLowLevel(start, target, startX, startY, startZ, targetX, targetY, targetZ,
					searchRadiusSquared, budget, terrain, passability, null);
		}

		private SearchResult searchLowLevel(Node start, Node target, float startX, float startY, float startZ,
				float targetX, float targetY, float targetZ, float searchRadiusSquared, int budget,
				HeightProvider terrain, EdgePassability passability, BitSet allowedBlocks) {
			SearchWorkspace workspace = workspace();
			workspace.beginLowLevelSearch();
			Map<Long, SearchNode> visited = workspace.visited;
			PriorityQueue<OpenNode> open = workspace.open;
			try {
				long sequence = 0;
				SearchNode first = workspace.searchNode(start, null, 0, distance(start, target));
				visited.put(start.key(), first);
				open.add(workspace.openNode(start.key(), 0, first.score, sequence++));
				int processed = 0;
				while (!open.isEmpty() && processed < budget && !Thread.currentThread().isInterrupted()) {
					OpenNode queued = open.poll();
					SearchNode current = visited.get(queued.key());
					if (current == null || current.closed || Float.compare(current.cost, queued.cost()) != 0) {
						continue;
					}
					processed++;
					current.closed = true;
					if (current.node.key() == target.key()) {
						List<PathPoint> path = reconstruct(current);
						return path == null ? SearchResult.failed(SearchStatus.NODE_LIMIT, processed)
								: SearchResult.found(path, processed);
					}
					for (int direction = 0; direction < 8; direction++) {
						Node neighbor = step(current.node, direction, terrain, passability);
						if (neighbor == null || allowedBlocks != null && !allowedBlocks.get(neighbor.sector().block.id())
								|| !withinSearchArea(neighbor, startX, startY, startZ, targetX, targetY, targetZ,
										searchRadiusSquared)) {
							continue;
						}
						float cost = current.cost + distance(current.node, neighbor);
						SearchNode known = visited.get(neighbor.key());
						if (known != null && known.closed) {
							continue;
						}
						if (known == null || cost + 0.5f < known.cost) {
							float score = cost + distance(neighbor, target);
							if (known == null) {
								known = workspace.searchNode(neighbor, current, cost, score);
								visited.put(neighbor.key(), known);
							} else {
								known.parent = current;
								known.cost = cost;
								known.score = score;
							}
							open.add(workspace.openNode(neighbor.key(), cost, score, sequence++));
						}
					}
				}
				if (Thread.currentThread().isInterrupted()) {
					return SearchResult.failed(SearchStatus.INTERRUPTED, processed);
				}
				return SearchResult.failed(open.isEmpty() ? SearchStatus.NO_PATH : SearchStatus.NODE_LIMIT, processed);
			} finally {
				workspace.endLowLevelSearch();
			}
		}

		private boolean usesHierarchicalSearch(Node start, Node target) {
			int startBlock = start.sector().block.id();
			int targetBlock = target.sector().block.id();
			int deltaColumn = Math.abs(startBlock % blockColumns - targetBlock % blockColumns);
			int deltaRow = Math.abs(startBlock / blockColumns - targetBlock / blockColumns);
			return Math.max(deltaColumn, deltaRow) >= HIERARCHICAL_MIN_BLOCK_DISTANCE;
		}

		private BlockPath findBlockPath(int startBlock, int targetBlock) {
			SearchWorkspace workspace = workspace();
			workspace.beginBlockSearch();
			Map<Integer, BlockSearchNode> visited = workspace.blockVisited;
			PriorityQueue<BlockOpenNode> open = workspace.blockOpen;
			try {
				long sequence = 0;
				BlockSearchNode first = workspace.blockSearchNode(startBlock, null, 0,
						blockDistance(startBlock, targetBlock));
				visited.put(startBlock, first);
				open.add(workspace.blockOpenNode(startBlock, 0, first.score, sequence++));
				int processed = 0;
				while (!open.isEmpty() && processed < HIERARCHICAL_MAX_ABSTRACT_NODES
						&& !Thread.currentThread().isInterrupted()) {
					BlockOpenNode queued = open.poll();
					BlockSearchNode current = visited.get(queued.blockId());
					if (current == null || current.closed || Float.compare(current.cost, queued.cost()) != 0) {
						continue;
					}
					processed++;
					current.closed = true;
					if (current.blockId == targetBlock) {
						return new BlockPath(SearchStatus.FOUND, reconstructBlocks(current), processed);
					}
					for (int neighborId : blockNeighbors(current.blockId)) {
						float cost = current.cost + 1;
						BlockSearchNode known = visited.get(neighborId);
						if (known != null && (known.closed || cost >= known.cost)) {
							continue;
						}
						float score = cost + blockDistance(neighborId, targetBlock);
						if (known == null) {
							known = workspace.blockSearchNode(neighborId, current, cost, score);
							visited.put(neighborId, known);
						} else {
							known.parent = current;
							known.cost = cost;
							known.score = score;
						}
						open.add(workspace.blockOpenNode(neighborId, cost, score, sequence++));
					}
				}
				SearchStatus status = Thread.currentThread().isInterrupted() ? SearchStatus.INTERRUPTED
						: open.isEmpty() ? SearchStatus.NO_PATH : SearchStatus.NODE_LIMIT;
				return new BlockPath(status, null, processed);
			} finally {
				workspace.endBlockSearch();
			}
		}

		private int[] blockNeighbors(int blockId) {
			return blockNeighbors.computeIfAbsent(blockId, this::readBlockNeighbors);
		}

		private int[] readBlockNeighbors(int blockId) {
			Block block = block(blockId);
			int column = blockId % blockColumns;
			int row = blockId / blockColumns;
			int[] result = new int[4];
			int count = 0;
			for (int direction = 0; direction < 4; direction++) {
				int targetColumn = column + DX[direction];
				int targetRow = row + DY[direction];
				if (targetColumn < 0 || targetRow < 0 || targetColumn >= blockColumns || targetRow >= blockRows) {
					continue;
				}
				for (Sector sector : block.sectors()) {
					if (sector.hasBoundaryLink(direction)) {
						result[count++] = targetRow * blockColumns + targetColumn;
						break;
					}
				}
			}
			return Arrays.copyOf(result, count);
		}

		private SearchResult refineBlockPath(Node start, Node target, int[] blockPath, int budget,
				HeightProvider terrain, EdgePassability passability) {
			List<PathPoint> result = new ArrayList<>();
			result.add(point(start));
			Node current = start;
			int processed = 0;
			for (int index = 1; index < blockPath.length; index++) {
				int remaining = budget - processed;
				if (remaining <= 0) {
					return SearchResult.failed(SearchStatus.NODE_LIMIT, processed);
				}
				List<PortalStep> portals = boundaryPortals(blockPath[index - 1], blockPath[index], terrain,
						passability);
				PortalSearchResult segment = searchToAnyPortal(current, blockPath[index - 1], portals, remaining,
						terrain, passability);
				processed += segment.processedNodes();
				if (segment.status() != SearchStatus.FOUND) {
					return SearchResult.failed(segment.status(), processed);
				}
				if (!appendPath(result, segment.path()) || result.size() == MAX_PATH_POINTS) {
					return SearchResult.failed(SearchStatus.NODE_LIMIT, processed);
				}
				current = segment.portal().target();
				result.add(point(current));
			}

			if (current.key() == target.key()) {
				return SearchResult.found(result, processed);
			}
			if (canWalkStraight(current, target, terrain, passability)) {
				if (result.size() == MAX_PATH_POINTS) {
					return SearchResult.failed(SearchStatus.NODE_LIMIT, processed);
				}
				result.add(point(target));
				return SearchResult.found(result, processed);
			}
			int remaining = budget - processed;
			if (remaining <= 0) {
				return SearchResult.failed(SearchStatus.NODE_LIMIT, processed);
			}
			BitSet targetBlock = new BitSet(blockOffsets.length);
			targetBlock.set(blockPath[blockPath.length - 1]);
			float distanceSquared = square(target.x() - current.x()) + square(target.y() - current.y())
					+ square(target.z() - current.z());
			SearchResult last = searchLowLevel(current, target, current.x(), current.y(), current.z(), target.x(),
					target.y(), target.z(), Math.max(2_500, 2 * distanceSquared), remaining, terrain, passability,
					targetBlock);
			processed += last.processedNodes();
			if (last.status() != SearchStatus.FOUND) {
				return SearchResult.failed(last.status(), processed);
			}
			if (!appendPath(result, last.path())) {
				return SearchResult.failed(SearchStatus.NODE_LIMIT, processed);
			}
			return SearchResult.found(result, processed);
		}

		private PortalSearchResult searchToAnyPortal(Node start, int blockId, List<PortalStep> portals, int budget,
				HeightProvider terrain, EdgePassability passability) {
			if (portals.isEmpty()) {
				return PortalSearchResult.failed(SearchStatus.NO_PATH, 0);
			}
			SearchWorkspace workspace = workspace();
			workspace.beginPortalSearch();
			Map<Long, PortalStep> goals = workspace.portalGoals;
			Map<Long, SearchNode> visited = workspace.visited;
			PriorityQueue<OpenNode> open = workspace.open;
			try {
				for (PortalStep portal : portals) {
					goals.putIfAbsent(portal.source().key(), portal);
				}
				PortalStep immediate = goals.get(start.key());
				if (immediate != null) {
					return PortalSearchResult.found(List.of(point(start)), immediate, 0);
				}
				long sequence = 0;
				float firstScore = portalDistance(start, portals);
				SearchNode first = workspace.searchNode(start, null, 0, firstScore);
				visited.put(start.key(), first);
				open.add(workspace.openNode(start.key(), 0, firstScore, sequence++));
				int processed = 0;
				while (!open.isEmpty() && processed < budget && !Thread.currentThread().isInterrupted()) {
					OpenNode queued = open.poll();
					SearchNode current = visited.get(queued.key());
					if (current == null || current.closed || Float.compare(current.cost, queued.cost()) != 0) {
						continue;
					}
					processed++;
					current.closed = true;
					PortalStep reached = goals.get(current.node.key());
					if (reached != null) {
						List<PathPoint> path = reconstruct(current);
						return path == null ? PortalSearchResult.failed(SearchStatus.NODE_LIMIT, processed)
								: PortalSearchResult.found(path, reached, processed);
					}
					for (int direction = 0; direction < 8; direction++) {
						Node neighbor = step(current.node, direction, terrain, passability);
						if (neighbor == null || neighbor.sector().block.id() != blockId) {
							continue;
						}
						float cost = current.cost + distance(current.node, neighbor);
						SearchNode known = visited.get(neighbor.key());
						if (known != null && (known.closed || cost + 0.5f >= known.cost)) {
							continue;
						}
						float score = cost + portalDistance(neighbor, portals);
						if (known == null) {
							known = workspace.searchNode(neighbor, current, cost, score);
							visited.put(neighbor.key(), known);
						} else {
							known.parent = current;
							known.cost = cost;
							known.score = score;
						}
						open.add(workspace.openNode(neighbor.key(), cost, score, sequence++));
					}
				}
				if (Thread.currentThread().isInterrupted()) {
					return PortalSearchResult.failed(SearchStatus.INTERRUPTED, processed);
				}
				return PortalSearchResult.failed(open.isEmpty() ? SearchStatus.NO_PATH : SearchStatus.NODE_LIMIT, processed);
			} finally {
				workspace.endPortalSearch();
			}
		}

		private static float portalDistance(Node node, List<PortalStep> portals) {
			float result = Float.POSITIVE_INFINITY;
			for (PortalStep portal : portals) {
				result = Math.min(result, distance(node, portal.source()));
			}
			return result;
		}

		private List<PortalStep> boundaryPortals(int blockId, int targetBlockId, HeightProvider terrain,
				EdgePassability passability) {
			int direction = blockDirection(blockId, targetBlockId);
			if (direction < 0) {
				return List.of();
			}
			Block block = block(blockId);
			List<PortalStep> result = new ArrayList<>();
			for (Sector sector : block.sectors()) {
				if (sector.type == 16) {
					for (int offset : sector.nodeOffsets()) {
						addBoundaryPortal(result, sector.complexNode(offset), direction, targetBlockId, terrain,
								passability);
					}
					continue;
				}
				int blockColumn = blockId % blockColumns;
				int blockRow = blockId / blockColumns;
				for (int coordinate = 0; coordinate < 32; coordinate++) {
					int gridX = switch (direction) {
						case 0 -> blockColumn * 32 + 31;
						case 2 -> blockColumn * 32;
						default -> blockColumn * 32 + coordinate;
					};
					int gridY = switch (direction) {
						case 1 -> blockRow * 32 + 31;
						case 3 -> blockRow * 32;
						default -> blockRow * 32 + coordinate;
					};
					addBoundaryPortal(result, sector.simpleNode(gridX, gridY, terrain), direction, targetBlockId,
							terrain, passability);
				}
			}
			return result;
		}

		private void addBoundaryPortal(List<PortalStep> result, Node source, int direction, int targetBlockId,
				HeightProvider terrain, EdgePassability passability) {
			if (source == null) {
				return;
			}
			Node target = direction < 4 ? neighbor(source, direction, terrain) : null;
			if (target != null && target.sector().block.id() == targetBlockId && edgeAllowed(source, target, passability)) {
				result.add(new PortalStep(source, target));
			}
		}

		private int blockDirection(int blockId, int targetBlockId) {
			int column = blockId % blockColumns;
			int row = blockId / blockColumns;
			int targetColumn = targetBlockId % blockColumns;
			int targetRow = targetBlockId / blockColumns;
			for (int direction = 0; direction < 4; direction++) {
				if (column + DX[direction] == targetColumn && row + DY[direction] == targetRow) {
					return direction;
				}
			}
			return -1;
		}

		private static boolean appendPath(List<PathPoint> target, List<PathPoint> segment) {
			if (segment == null) {
				return false;
			}
			for (int index = 1; index < segment.size(); index++) {
				if (target.size() == MAX_PATH_POINTS) {
					return false;
				}
				target.add(segment.get(index));
			}
			return true;
		}

		private float blockDistance(int first, int second) {
			int deltaColumn = first % blockColumns - second % blockColumns;
			int deltaRow = first / blockColumns - second / blockColumns;
			return (float) Math.hypot(deltaColumn, deltaRow);
		}

		private static int[] reconstructBlocks(BlockSearchNode end) {
			int count = 0;
			for (BlockSearchNode node = end; node != null; node = node.parent) {
				count++;
			}
			int[] result = new int[count];
			for (BlockSearchNode node = end; node != null; node = node.parent) {
				result[--count] = node.blockId;
			}
			return result;
		}

		boolean canWalkStraight(float startX, float startY, float startZ, float targetX, float targetY,
				float targetZ, HeightProvider terrain, EdgePassability passability) {
			workspace().resetNodes();
			Node current = findNode(startX, startY, startZ, terrain);
			Node target = findNode(targetX, targetY, targetZ, terrain);
			return current != null && target != null && canWalkStraight(current, target, terrain, passability);
		}

		PathPoint projectPoint(float x, float y, float z, HeightProvider terrain) {
			workspace().resetNodes();
			Node node = findNode(x, y, z, terrain);
			return node == null ? null : point(node);
		}

		PathPoint nearestPathPoint(float x, float y, float z, float maxRadius, float maxVerticalDelta,
				HeightProvider terrain, PointPassability passability) {
			workspace().resetNodes();
			int centerX = (int) (x * 2);
			int centerY = (int) (y * 2);
			int radius = Math.max(0, (int) Math.ceil(Math.max(0, maxRadius) * 2));
			float radiusSquared = maxRadius * maxRadius;
			Node best = null;
			float bestDistance = Float.POSITIVE_INFINITY;
			for (int offsetX = -radius; offsetX <= radius; offsetX++) {
				for (int offsetY = -radius; offsetY <= radius; offsetY++) {
					int gridX = centerX + offsetX;
					int gridY = centerY + offsetY;
					float pointX = gridX * 0.5f + 0.25f;
					float pointY = gridY * 0.5f + 0.25f;
					float distance = square(pointX - x) + square(pointY - y);
					if (distance > radiusSquared || distance > bestDistance) {
						continue;
					}
					Block block = block(gridX, gridY);
					if (block == null) {
						continue;
					}
					for (Sector sector : block.sectors) {
						Node node = sector.find(gridX, gridY, z, terrain, maxVerticalDelta);
						if (node != null && (passability == null || passability.canPass(node.x(), node.y(), node.z()))) {
							best = node;
							bestDistance = distance;
						}
					}
				}
			}
			return best == null ? null : point(best);
		}

		private boolean canWalkStraight(Node current, Node target, HeightProvider terrain, EdgePassability passability) {
			Node start = current;
			int lineX = target.gridX() - start.gridX();
			int lineY = target.gridY() - start.gridY();
			float lineLengthSquared = lineX * lineX + lineY * lineY;
			int deltaX = Math.abs(target.gridX() - current.gridX());
			int deltaY = Math.abs(target.gridY() - current.gridY());
			int stepX = Integer.compare(target.gridX(), current.gridX());
			int stepY = Integer.compare(target.gridY(), current.gridY());
			int error = deltaX - deltaY;
			while (current.gridX() != target.gridX() || current.gridY() != target.gridY()) {
				int doubledError = error * 2;
				int moveX = 0;
				int moveY = 0;
				if (doubledError > -deltaY) {
					error -= deltaY;
					moveX = stepX;
				}
				if (doubledError < deltaX) {
					error += deltaX;
					moveY = stepY;
				}
				Node next = step(current, direction(moveX, moveY), terrain, passability);
				if (next == null || distance(current, next) >= 5) {
					return false;
				}
				current = next;
				float amount = ((current.gridX() - start.gridX()) * lineX
						+ (current.gridY() - start.gridY()) * lineY) / lineLengthSquared;
				float expectedZ = start.z() + (target.z() - start.z()) * amount;
				if (Math.abs(current.z() - expectedZ) > MAX_STRAIGHT_HEIGHT_DEVIATION) {
					return false;
				}
			}
			return current.key() == target.key();
		}

		private static PathPoint point(Node node) {
			return new PathPoint(node.x(), node.y(), node.z());
		}

		private Node step(Node source, int direction, HeightProvider terrain, EdgePassability passability) {
			Node next = direction < 4 ? neighbor(source, direction, terrain)
					: diagonalNeighbor(source, direction - 4, terrain, passability);
			if (next == null || Math.abs(source.z() - next.z()) > MAX_ADJACENT_HEIGHT_DELTA
					|| distance(source, next) >= 20) {
				return null;
			}
			return direction < 4 && !edgeAllowed(source, next, passability) ? null : next;
		}

		private static int direction(int dx, int dy) {
			if (dy == 0) {
				return dx > 0 ? 0 : 2;
			}
			if (dx == 0) {
				return dy > 0 ? 1 : 3;
			}
			if (dx > 0) {
				return dy > 0 ? 4 : 6;
			}
			return dy > 0 ? 5 : 7;
		}

		private static List<PathPoint> reconstruct(SearchNode end) {
			List<PathPoint> reverse = new ArrayList<>();
			for (SearchNode node = end; node != null; node = node.parent) {
				if (reverse.size() == MAX_PATH_POINTS) {
					return null;
				}
				reverse.add(new PathPoint(node.node.x(), node.node.y(), node.node.z()));
			}
			List<PathPoint> result = new ArrayList<>(reverse.size());
			for (int i = reverse.size() - 1; i >= 0; i--) {
				result.add(reverse.get(i));
			}
			return result;
		}

		private Node findNode(float x, float y, float z, HeightProvider terrain) {
			int gridX = (int) (x * 2);
			int gridY = (int) (y * 2);
			Block block = block(gridX, gridY);
			if (block == null) {
				return null;
			}
			for (Sector sector : block.sectors) {
				Node node = sector.find(gridX, gridY, z, terrain);
				if (node != null) {
					return node;
				}
			}
			return null;
		}

		private Node diagonalNeighbor(Node source, int diagonal, HeightProvider terrain, EdgePassability passability) {
			Node first = neighbor(source, DIAGONAL_FIRST[diagonal], terrain);
			Node second = neighbor(source, DIAGONAL_SECOND[diagonal], terrain);
			if (first == null || second == null) {
				return null;
			}
			Node firstTarget = neighbor(first, DIAGONAL_SECOND[diagonal], terrain);
			Node secondTarget = neighbor(second, DIAGONAL_FIRST[diagonal], terrain);
			if (firstTarget == null || secondTarget == null || firstTarget.key() != secondTarget.key()
					|| !edgeAllowed(source, firstTarget, passability)) {
				return null;
			}
			return firstTarget;
		}

		private static boolean edgeAllowed(Node start, Node end, EdgePassability passability) {
			return end != null && (passability == null || passability.canPass(start.x(), start.y(), start.z(), end.x(), end.y(),
					end.z()));
		}

		private Node neighbor(Node source, int direction, HeightProvider terrain) {
			return source.complexOffset() >= 0
					? complexNeighbor(source, direction, terrain)
					: simpleNeighbor(source, direction, terrain);
		}

		private Node simpleNeighbor(Node source, int direction, HeightProvider terrain) {
			Sector sector = source.sector();
			int x = source.gridX();
			int y = source.gridY();
			int targetX = x + DX[direction];
			int targetY = y + DY[direction];
			if ((x >> 5) == (targetX >> 5) && (y >> 5) == (targetY >> 5)) {
				if (!sector.hasLink(x, y, direction)) {
					return null;
				}
				return sector.simpleNode(targetX, targetY, terrain);
			}
			int coordinate = (direction & 1) == 0 ? y & 31 : x & 31;
			if ((sector.boundaryMask & 1 << direction) != 0) {
				if (((sector.boundaries[direction] >>> coordinate) & 1) != 0) {
					return null;
				}
				return targetNode(targetX, targetY, 0, -1, source.z(), terrain);
			}
			long portalIndex = Integer.toUnsignedLong(sector.boundaries[direction]) + coordinate;
			if (portalIndex >= portalCount) {
				return null;
			}
			int packed = data.getInt(portalOffset + (int) portalIndex * 4);
			return packed == 0 ? null : targetNode(targetX, targetY, packed & 0x7f, packed >>> 7, source.z(), terrain);
		}

		private Node complexNeighbor(Node source, int direction, HeightProvider terrain) {
			int packed = source.sector().edge(source.complexOffset(), direction);
			if (packed == 0) {
				return null;
			}
			int targetX = source.gridX() + DX[direction];
			int targetY = source.gridY() + DY[direction];
			return targetNode(targetX, targetY, packed & 0x7f, packed >>> 7, source.z(), terrain);
		}

		private Node targetNode(int x, int y, int layer, int complexOffset, float sourceZ, HeightProvider terrain) {
			Block block = block(x, y);
			if (block == null || layer < 0 || layer >= block.sectors.length) {
				return null;
			}
			Sector sector = block.sectors[layer];
			if (sector.type == 16) {
				Node node = complexOffset >= 0 ? sector.complexNode(complexOffset) : sector.find(x, y, sourceZ, terrain);
				return node != null && node.gridX() == x && node.gridY() == y ? node : null;
			}
			return sector.simpleNode(x, y, terrain);
		}

		private Block block(int gridX, int gridY) {
			if (gridX < 0 || gridY < 0 || gridX >= (long) width * 2 || gridY >= (long) height * 2) {
				return null;
			}
			int column = gridX >> 5;
			int row = gridY >> 5;
			if (column >= blockColumns || row >= blockRows) {
				return null;
			}
			int id = row * blockColumns + column;
			return block(id);
		}

		private Block block(int id) {
			Block cached = blocks.get(id);
			if (cached != null) {
				return cached;
			}
			Block loaded = readBlock(id);
			return blocks.compareAndSet(id, null, loaded) ? loaded : blocks.get(id);
		}

		private Block readBlock(int id) {
			int cursor = blockOffsets[id];
			int count = data.get(cursor++) & 0xff;
			Sector[] sectors = new Sector[count];
			Block block = new Block(id, sectors);
			for (int fileOrder = 0; fileOrder < count; fileOrder++) {
				int type = data.get(cursor++) & 0xff;
				int layer = count - fileOrder - 1;
				if (type == 16) {
					int base = data.getInt(cursor);
					int nodes = data.getShort(cursor + 4);
					cursor += 6;
					if (base < 0 || nodes < 0) {
						throw new IllegalArgumentException("Invalid complex sector at block " + id);
					}
					sectors[layer] = new Sector(block, layer, type, 0, new int[4], -1, -1, base, nodes);
					continue;
				}
				if (type > 15 || cursor + 17 > data.limit()) {
					throw new IllegalArgumentException("Invalid sector type " + type + " at block " + id);
				}
				int boundaryMask = data.get(cursor++) & 0xff;
				int[] boundaries = new int[4];
				for (int i = 0; i < 4; i++, cursor += 4) {
					boundaries[i] = data.getInt(cursor);
				}
				int payload = cursor;
				cursor += payloadSize(type);
				int links = (type & 1) == 0 ? -1 : cursor;
				if (links >= 0) {
					cursor += 512;
				}
				if (cursor > data.limit()) {
					throw new IllegalArgumentException("Truncated sector at block " + id);
				}
				sectors[layer] = new Sector(block, layer, type, boundaryMask, boundaries, payload, links, -1, 0);
			}
			return block;
		}

		private static int payloadSize(int type) {
			return switch (type & ~1) {
				case 0 -> 4;
				case 2 -> 0;
				case 4 -> 4096;
				case 6 -> 128;
				case 8 -> 264;
				case 10 -> 568;
				case 12 -> 1028;
				case 14 -> 2052;
				default -> throw new IllegalArgumentException("Unknown sector type " + type);
			};
		}

		private static float distance(Node first, Node second) {
			float x = first.x() - second.x();
			float y = first.y() - second.y();
			float z = first.z() - second.z();
			return (float) Math.sqrt(x * x + y * y + z * z);
		}

		private static float square(float value) {
			return value * value;
		}

		private static boolean withinSearchArea(Node node, float startX, float startY, float startZ, float targetX,
				float targetY, float targetZ, float radiusSquared) {
			float x = node.x() - startX;
			float y = node.y() - startY;
			float z = node.z() - startZ;
			if (x * x + y * y + z * z <= radiusSquared) {
				return true;
			}
			x = node.x() - targetX;
			y = node.y() - targetY;
			z = node.z() - targetZ;
			return x * x + y * y + z * z <= radiusSquared;
		}

		private final class Sector {
			private final Block block;
			private final int layer;
			private final int type;
			private final int boundaryMask;
			private final int[] boundaries;
			private final int payload;
			private final int links;
			private final int complexBase;
			private final int complexCount;
			private volatile int[] complexOffsets;

			private Sector(Block block, int layer, int type, int boundaryMask, int[] boundaries, int payload,
					int links, int complexBase, int complexCount) {
				this.block = block;
				this.layer = layer;
				this.type = type;
				this.boundaryMask = boundaryMask;
				this.boundaries = boundaries;
				this.payload = payload;
				this.links = links;
				this.complexBase = complexBase;
				this.complexCount = complexCount;
			}

			private Node find(int x, int y, float z, HeightProvider terrain) {
				return find(x, y, z, terrain, 0.7f);
			}

			private Node find(int x, int y, float z, HeightProvider terrain, float maxVerticalDelta) {
				if (type != 16) {
					Node node = simpleNode(x, y, terrain);
					return node != null && Math.abs(node.z() - z) < maxVerticalDelta ? node : null;
				}
				Node best = null;
				float difference = maxVerticalDelta;
				for (int offset : nodeOffsets()) {
					Node node = complexNode(offset);
					if (node != null && node.gridX() == x && node.gridY() == y) {
						float current = Math.abs(node.z() - z);
						if (current < difference) {
							best = node;
							difference = current;
						}
					}
				}
				return best;
			}

			private Node simpleNode(int x, int y, HeightProvider terrain) {
				if (type == 16 || block(x, y) != block) {
					return null;
				}
				float z = heightAt(x, y, terrain);
				if (!Float.isFinite(z)) {
					return null;
				}
				long key = (long) block.id << 24 | (long) layer << 10 | (y & 31) * 32L | x & 31;
				return workspace().node(this, x, y, -1, key, x * 0.5f + 0.25f, y * 0.5f + 0.25f, z);
			}

			private Node complexNode(int offset) {
				if (offset < 0 || offset + 9 > nodeTableSize) {
					return null;
				}
				int position = nodeTableOffset + offset;
				int x = Short.toUnsignedInt(data.getShort(position + 4));
				int y = Short.toUnsignedInt(data.getShort(position + 6));
				float z = data.getInt(position) / 100f;
				long key = Long.MIN_VALUE | Integer.toUnsignedLong(offset);
				return workspace().node(this, x, y, offset, key, x * 0.5f + 0.25f, y * 0.5f + 0.25f, z);
			}

			private int[] nodeOffsets() {
				int[] cached = complexOffsets;
				if (cached != null) {
					return cached;
				}
				synchronized (this) {
					if (complexOffsets == null) {
						int[] offsets = new int[complexCount];
						int cursor = complexBase;
						for (int i = 0; i < offsets.length; i++) {
							offsets[i] = cursor;
							if (cursor < 0 || cursor + 9 > nodeTableSize) {
								throw new IllegalArgumentException("Complex node outside node table");
							}
							int descriptor = data.get(nodeTableOffset + cursor + 8) & 0xff;
							cursor += 9;
							for (int direction = 0; direction < 4; direction++) {
								int mode = descriptor >>> (direction * 2) & 3;
								cursor += mode == 1 ? 2 : mode == 2 ? 4 : 0;
							}
						}
						complexOffsets = offsets;
					}
					return complexOffsets;
				}
			}

			private int edge(int offset, int wantedDirection) {
				if (offset < 0 || offset + 9 > nodeTableSize) {
					return 0;
				}
				int position = nodeTableOffset + offset;
				int descriptor = data.get(position + 8) & 0xff;
				int cursor = position + 9;
				for (int direction = 0; direction < 4; direction++) {
					int mode = descriptor >>> (direction * 2) & 3;
					if (direction == wantedDirection) {
						return switch (mode) {
							case 1 -> (complexBase + Short.toUnsignedInt(data.getShort(cursor))) << 7 | layer;
							case 2 -> data.getInt(cursor);
							default -> 0;
						};
					}
					cursor += mode == 1 ? 2 : mode == 2 ? 4 : 0;
				}
				return 0;
			}

			private boolean hasBoundaryLink(int direction) {
				if (type == 16) {
					for (int offset : nodeOffsets()) {
						Node node = complexNode(offset);
						int localX = node == null ? -1 : node.gridX() & 31;
						int localY = node == null ? -1 : node.gridY() & 31;
						boolean onBoundary = switch (direction) {
							case 0 -> localX == 31;
							case 1 -> localY == 31;
							case 2 -> localX == 0;
							case 3 -> localY == 0;
							default -> false;
						};
						if (onBoundary && edge(offset, direction) != 0) {
							return true;
						}
					}
					return false;
				}
				if ((boundaryMask & 1 << direction) != 0) {
					return boundaries[direction] != -1;
				}
				long portalIndex = Integer.toUnsignedLong(boundaries[direction]);
				for (int coordinate = 0; coordinate < 32 && portalIndex + coordinate < portalCount; coordinate++) {
					if (data.getInt(portalOffset + (int) (portalIndex + coordinate) * 4) != 0) {
						return true;
					}
				}
				return false;
			}

			private boolean hasLink(int x, int y, int direction) {
				if (links < 0) {
					return true;
				}
				int localX = x & 31;
				int value = data.get(links + (y & 31) * 16 + localX / 2) & 0xff;
				int mask = (localX & 1) == 0 ? value & 0xf : value >>> 4;
				return (mask & 1 << direction) != 0;
			}

			private float heightAt(int x, int y, HeightProvider terrain) {
				int localX = x & 31;
				int localY = y & 31;
				int cell = localY * 32 + localX;
				float terrainHeight = Float.NaN;
				return switch (type & ~1) {
					case 0 -> data.getInt(payload) / 100f;
					case 2 -> terrain.get(x * 0.5f + 0.25f, y * 0.5f + 0.25f);
					case 4 -> decodedHeight(data.getInt(payload + cell * 4));
					case 6 -> ((data.get(payload + localY * 4 + localX / 8) >>> (localX & 7)) & 1) != 0
							? Float.NaN : terrain.get(x * 0.5f + 0.25f, y * 0.5f + 0.25f);
					case 8 -> {
						int code = data.get(payload + 8 + localY * 8 + localX / 4) >>> ((localX & 3) * 2) & 3;
						yield code < 2 ? data.getInt(payload + code * 4) / 100f
								: code == 2 ? terrain.get(x * 0.5f + 0.25f, y * 0.5f + 0.25f) : Float.NaN;
					}
					case 10 -> {
						int value = data.get(payload + 56 + localY * 16 + localX / 2) & 0xff;
						int code = (localX & 1) == 0 ? value & 0xf : value >>> 4;
						yield code < 14 ? data.getInt(payload + code * 4) / 100f
								: code == 14 ? terrain.get(x * 0.5f + 0.25f, y * 0.5f + 0.25f) : Float.NaN;
					}
					case 12 -> {
						int value = data.get(payload + 4 + cell) & 0xff;
						yield value < 0xfe ? (data.getInt(payload) + value) / 100f
								: value == 0xfe ? terrain.get(x * 0.5f + 0.25f, y * 0.5f + 0.25f) : Float.NaN;
					}
					case 14 -> {
						int value = Short.toUnsignedInt(data.getShort(payload + 4 + cell * 2));
						yield value < 0xfffe ? (data.getInt(payload) + value) / 100f
								: value == 0xfffe ? terrain.get(x * 0.5f + 0.25f, y * 0.5f + 0.25f) : Float.NaN;
					}
					default -> terrainHeight;
				};
			}

			private static float decodedHeight(int value) {
				return value == Integer.MAX_VALUE ? Float.NaN : value / 100f;
			}
		}

		private static SearchWorkspace workspace() {
			return SEARCH_WORKSPACE.get();
		}

		private record Block(int id, Sector[] sectors) {}

		private static final class Node {
			private Sector sector;
			private int gridX;
			private int gridY;
			private int complexOffset;
			private long key;
			private float x;
			private float y;
			private float z;

			private Node reset(Sector sector, int gridX, int gridY, int complexOffset, long key,
					float x, float y, float z) {
				this.sector = sector;
				this.gridX = gridX;
				this.gridY = gridY;
				this.complexOffset = complexOffset;
				this.key = key;
				this.x = x;
				this.y = y;
				this.z = z;
				return this;
			}

			private Sector sector() {
				return sector;
			}

			private int gridX() {
				return gridX;
			}

			private int gridY() {
				return gridY;
			}

			private int complexOffset() {
				return complexOffset;
			}

			private long key() {
				return key;
			}

			private float x() {
				return x;
			}

			private float y() {
				return y;
			}

			private float z() {
				return z;
			}
		}

		private static final class SearchNode {
			private Node node;
			private SearchNode parent;
			private float cost;
			private float score;
			private boolean closed;

			private SearchNode reset(Node node, SearchNode parent, float cost, float score) {
				this.node = node;
				this.parent = parent;
				this.cost = cost;
				this.score = score;
				this.closed = false;
				return this;
			}
		}

		private static final class OpenNode {
			private long key;
			private float cost;
			private float score;
			private long sequence;

			private OpenNode reset(long key, float cost, float score, long sequence) {
				this.key = key;
				this.cost = cost;
				this.score = score;
				this.sequence = sequence;
				return this;
			}

			private long key() {
				return key;
			}

			private float cost() {
				return cost;
			}

			private float score() {
				return score;
			}

			private long sequence() {
				return sequence;
			}
		}

		private static final class BlockSearchNode {
			private int blockId;
			private BlockSearchNode parent;
			private float cost;
			private float score;
			private boolean closed;

			private BlockSearchNode reset(int blockId, BlockSearchNode parent, float cost, float score) {
				this.blockId = blockId;
				this.parent = parent;
				this.cost = cost;
				this.score = score;
				this.closed = false;
				return this;
			}
		}

		private static final class BlockOpenNode {
			private int blockId;
			private float cost;
			private float score;
			private long sequence;

			private BlockOpenNode reset(int blockId, float cost, float score, long sequence) {
				this.blockId = blockId;
				this.cost = cost;
				this.score = score;
				this.sequence = sequence;
				return this;
			}

			private int blockId() {
				return blockId;
			}

			private float cost() {
				return cost;
			}

			private float score() {
				return score;
			}

			private long sequence() {
				return sequence;
			}
		}

		private static final class SearchWorkspace {
			private Node[] nodes = new Node[256];
			private SearchNode[] searchNodes = new SearchNode[128];
			private OpenNode[] openNodes = new OpenNode[256];
			private BlockSearchNode[] blockSearchNodes = new BlockSearchNode[32];
			private BlockOpenNode[] blockOpenNodes = new BlockOpenNode[64];
			private int nodeIndex;
			private int searchNodeIndex;
			private int openNodeIndex;
			private int blockSearchNodeIndex;
			private int blockOpenNodeIndex;
			private final Map<Long, SearchNode> visited = new HashMap<>();
			private final PriorityQueue<OpenNode> open = new PriorityQueue<>(OPEN_NODE_ORDER);
			private final Map<Long, PortalStep> portalGoals = new HashMap<>();
			private final Map<Integer, BlockSearchNode> blockVisited = new HashMap<>();
			private final PriorityQueue<BlockOpenNode> blockOpen = new PriorityQueue<>(BLOCK_OPEN_NODE_ORDER);

			private void resetNodes() {
				nodeIndex = 0;
			}

			private Node node(Sector sector, int gridX, int gridY, int complexOffset, long key,
					float x, float y, float z) {
				if (nodeIndex == nodes.length) {
					nodes = Arrays.copyOf(nodes, nodes.length * 2);
				}
				Node node = nodes[nodeIndex];
				if (node == null) {
					node = nodes[nodeIndex] = new Node();
				}
				nodeIndex++;
				return node.reset(sector, gridX, gridY, complexOffset, key, x, y, z);
			}

			private void beginLowLevelSearch() {
				visited.clear();
				open.clear();
				searchNodeIndex = 0;
				openNodeIndex = 0;
			}

			private void endLowLevelSearch() {
				visited.clear();
				open.clear();
			}

			private void beginPortalSearch() {
				beginLowLevelSearch();
				portalGoals.clear();
			}

			private void endPortalSearch() {
				portalGoals.clear();
				endLowLevelSearch();
			}

			private SearchNode searchNode(Node node, SearchNode parent, float cost, float score) {
				if (searchNodeIndex == searchNodes.length) {
					searchNodes = Arrays.copyOf(searchNodes, searchNodes.length * 2);
				}
				SearchNode result = searchNodes[searchNodeIndex];
				if (result == null) {
					result = searchNodes[searchNodeIndex] = new SearchNode();
				}
				searchNodeIndex++;
				return result.reset(node, parent, cost, score);
			}

			private OpenNode openNode(long key, float cost, float score, long sequence) {
				if (openNodeIndex == openNodes.length) {
					openNodes = Arrays.copyOf(openNodes, openNodes.length * 2);
				}
				OpenNode result = openNodes[openNodeIndex];
				if (result == null) {
					result = openNodes[openNodeIndex] = new OpenNode();
				}
				openNodeIndex++;
				return result.reset(key, cost, score, sequence);
			}

			private void beginBlockSearch() {
				blockVisited.clear();
				blockOpen.clear();
				blockSearchNodeIndex = 0;
				blockOpenNodeIndex = 0;
			}

			private void endBlockSearch() {
				blockVisited.clear();
				blockOpen.clear();
			}

			private BlockSearchNode blockSearchNode(int blockId, BlockSearchNode parent, float cost, float score) {
				if (blockSearchNodeIndex == blockSearchNodes.length) {
					blockSearchNodes = Arrays.copyOf(blockSearchNodes, blockSearchNodes.length * 2);
				}
				BlockSearchNode result = blockSearchNodes[blockSearchNodeIndex];
				if (result == null) {
					result = blockSearchNodes[blockSearchNodeIndex] = new BlockSearchNode();
				}
				blockSearchNodeIndex++;
				return result.reset(blockId, parent, cost, score);
			}

			private BlockOpenNode blockOpenNode(int blockId, float cost, float score, long sequence) {
				if (blockOpenNodeIndex == blockOpenNodes.length) {
					blockOpenNodes = Arrays.copyOf(blockOpenNodes, blockOpenNodes.length * 2);
				}
				BlockOpenNode result = blockOpenNodes[blockOpenNodeIndex];
				if (result == null) {
					result = blockOpenNodes[blockOpenNodeIndex] = new BlockOpenNode();
				}
				blockOpenNodeIndex++;
				return result.reset(blockId, cost, score, sequence);
			}
		}

		private record BlockPath(SearchStatus status, int[] blocks, int processedNodes) {}

		private record PortalStep(Node source, Node target) {}

		private record PortalSearchResult(SearchStatus status, List<PathPoint> path, PortalStep portal,
				int processedNodes) {

			private static PortalSearchResult found(List<PathPoint> path, PortalStep portal, int processedNodes) {
				return new PortalSearchResult(SearchStatus.FOUND, path, portal, processedNodes);
			}

			private static PortalSearchResult failed(SearchStatus status, int processedNodes) {
				return new PortalSearchResult(status, null, null, processedNodes);
			}
		}

	}
}
