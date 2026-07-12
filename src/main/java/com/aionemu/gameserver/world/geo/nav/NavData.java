package com.aionemu.gameserver.world.geo.nav;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.geoEngine.models.GeoMap;
import com.aionemu.gameserver.geoEngine.scene.NavGeometry;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.utils.ConsoleProgressLineRenderer;

/**
 * 导航网格数据的线程安全懒加载器。
 * 按需加载并缓存导航网格，仅在实际访问时占用内存。
 * Thread-safe lazy loader for navigation mesh data.
 * Loads nav meshes on-demand and caches them for future use.
 * Memory-efficient: only loads maps that are actually accessed.
 *
 * @author Yon (Aion Reconstruction Project)
 */
@Slf4j
public class NavData {

	/** 可选 Spring 单例提供者 / Optional Spring singleton provider */
	private static volatile ObjectProvider<NavData> instanceProvider;

	/** 导航数据目录。 / Navigation data directory. */
	private static final String NAV_DIR = "geo/nav/";

	/** Size of float in bytes / Size of float in bytes */
	private static final int FLOAT_SIZE_BYTES = 4;

	/** Size of int in bytes / Size of int in bytes */
	private static final int INT_SIZE_BYTES = 4;

	/** Number of components per vertex (x, y, z) / Number of components per vertex (x, y, z) */
	private static final int VERTEX_COMPONENTS = 3;

	/** 单个顶点字节数。 / Size of one vertex in bytes. */
	private static final int VERTEX_STRIDE_BYTES = FLOAT_SIZE_BYTES * VERTEX_COMPONENTS;

	/** Headersizeoneintforfloat 次数 / Header size: one int for the float count */
	private static final int HEADER_SIZE_BYTES = INT_SIZE_BYTES;

	/**
	 * 按访问顺序维护的强引用导航地图缓存。
	 * Access-order strong cache of loaded navigation maps.
	 */
	private final Map<Integer, GeoMap> navMaps = Collections.synchronizedMap(new LinkedHashMap<Integer, GeoMap>(16, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Integer, GeoMap> eldest) {
			int maxSize = GeoDataConfig.GEO_NAV_CACHE_SIZE;
			boolean remove = maxSize > 0 && size() > maxSize;
			if (remove) {
				logDebug("Evicted navigation mesh for map {} from strong cache", eldest.getKey());
			}
			return remove;
		}
	});

	/**
	 * 文件索引：仅保存文件引用，启动时填充，不加载实际数据。
	 * File index - stores only file references, not the actual data.
	 * Memory-efficient, populated at startup.
	 */
	private final ConcurrentHashMap<Integer, File> navFiles = new ConcurrentHashMap<>();

	/** 每张地图的加载锁。 / Lock for loading each map once. */
	private final ConcurrentHashMap<Integer, ReentrantLock> mapLocks = new ConcurrentHashMap<>();

	/**
	 * 默认构造。
	 * Default constructor.
	 */
	public NavData() {}

	/**
	 * 判断导航文件索引是否已建立（不代表地图数据已加载）。
	 * Checks if navigation data index exists.
	 * Does NOT indicate if actual map data is loaded.
	 *
	 * @return 索引非空则为 true / true if the index is non-empty
	 */
	boolean isLoaded() {
		return !navFiles.isEmpty();
	}

	/**
	 * 扫描导航文件并建立文件索引，不加载实际地图数据。
	 * Scans for navigation files and builds the file index.
	 * Does NOT load actual map data - lazy loading only.
	 * Fast startup, minimal memory usage.
	 */
	void loadNavMaps() {
		// 若全局禁用寻路则跳过 / Skip if pathfinding is globally disabled
		if (!GeoDataConfig.GEO_NAV_ENABLE) {
			logInfo(I18n.get("console.navigation.scan_disabled"));
			return;
		}

		logInfo(I18n.get("console.navigation.scanning"));
		long startTime = System.currentTimeMillis();
		int fileCount = 0;
		int scannedCount = 0;
		int totalMaps = DataManager.WORLD_MAPS_DATA.size();
		ConsoleProgressLineRenderer progressRenderer = progressRenderer();

		for (WorldMapTemplate map : DataManager.WORLD_MAPS_DATA) {
			int mapId = map.getMapId();
			File navFile = Config.geoFile(NAV_DIR + mapId + ".nav");

			if (navFile.exists() && navFile.isFile()) {
				navFiles.put(mapId, navFile);
				fileCount++;
			}
			progressRenderer.progress(I18n.get("console.progress.navigation_files"), ++scannedCount, totalMaps);
		}

		progressRenderer.finished(I18n.get("console.progress.navigation_files"), totalMaps);

		long duration = System.currentTimeMillis() - startTime;
		logInfo(I18n.get("console.navigation.found", fileCount, duration));
	}

	/**
	 * 返回指定地图的导航网格；线程安全，首次访问时懒加载。
	 * Returns the navigation mesh for the specified map.
	 * Thread-safe, lazy loading: map is loaded on first access.
	 *
	 * map id
	 *
	 * @param worldId
	 * @return 含导航网格的 GeoMap，不可用则为 null / GeoMap with nav mesh, or null if not available
	 */
	public GeoMap getNavMap(int worldId) {
		if (!GeoDataConfig.GEO_NAV_ENABLE) {
			return null;
		}
		return getStrongCachedMap(worldId);
	}

	/**
	 * 从强缓存取地图；未命中则加锁加载。
	 * Fetches a map from the strong cache; loads under lock on miss.
	 *
	 * map id
	 *
	 * @param worldId
	 * @return 导航地图或 null / nav map or null
	 */
	private GeoMap getStrongCachedMap(int worldId) {
		synchronized (navMaps) {
			GeoMap cached = navMaps.get(worldId);
			if (cached != null) {
				return cached;
			}
		}

		ReentrantLock lock = mapLocks.computeIfAbsent(worldId, id -> new ReentrantLock());
		lock.lock();
		try {
			synchronized (navMaps) {
				GeoMap cached = navMaps.get(worldId);
				if (cached != null) {
					return cached;
				}
			}
			GeoMap loaded = loadMap(worldId);
			if (loaded != null) {
				synchronized (navMaps) {
					navMaps.put(worldId, loaded);
				}
			}
			return loaded;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 内部地图加载逻辑；在对应加载锁内每张地图只调用一次。
	 * Internal map loading logic.
	 * Called only once per map while holding its load lock.
	 *
	 * map id
	 *
	 * @param worldId
	 * @return 加载成功的 GeoMap，失败则为 null / loaded GeoMap, or null on failure
	 */
	private GeoMap loadMap(Integer worldId) {
		// 检查该地图文件是否存在 / Check if file exists for this map
		File navFile = navFiles.get(worldId);
		if (navFile == null) {
			logDebug("No navigation file for map {}", worldId);
			return null;
		}

		WorldMapTemplate template = DataManager.WORLD_MAPS_DATA.getTemplate(worldId);
		if (template == null) {
			log.error(I18n.get("log.7d07b80fded2", worldId));
			return null;
		}

		GeoMap geoMap = new GeoMap(String.valueOf(worldId), template.getWorldSize());

		long startTime = System.currentTimeMillis();
		try {
			if (loadNavMesh(worldId, navFile, geoMap)) {
				long duration = System.currentTimeMillis() - startTime;
				logInfo(I18n.get("console.navigation.mesh_loaded", worldId,
						geoMap.getTriangleCount(), duration));
				return geoMap;
			}
		} catch (IOException e) {
			log.error(I18n.get("log.7aeeb17ee6f6", worldId, e.getMessage(), e));
		} catch (Exception e) {
			log.error(I18n.get("log.e8c8626eccba", worldId, e.getMessage(), e));
		}

		return null;
	}

	/**
	 * 解析 .nav 文件并构建导航网格。
	 * Parses .nav file and constructs navigation mesh.
	 *
	 * 导航文件格式 / Navigation file format:
	 * total floats = vertexCount * 3。
	 * vertex positions
	 * triangle count
	 * - 每个三角形 / for each triangle:
	 *   - int[3]: 顶点索引 / vertex indices
	 *   - int[3]: 邻接三角形索引（-1 表示无连接） / adjacent triangle indices (-1 if no connection)
	 *
	 * @param worldId 地图 ID（日志用） / map id for logging
	 * @param navFile 待解析文件 / file to parse
	 * GeoMap to populate
	 * @return 加载成功则为 true / true if loading succeeded
	 * on file read errors。
	 */
	private boolean loadNavMesh(int worldId, File navFile, GeoMap map) throws IOException {
		try (RandomAccessFile raFile = new RandomAccessFile(navFile, "r");
			 FileChannel roChannel = raFile.getChannel();
			 Arena arena = Arena.ofConfined()) {

			ByteBuffer nav = mapReadOnly(roChannel, arena);

			// 校验文件大小 / Validate file size
			if (nav.remaining() < HEADER_SIZE_BYTES) {
				throw new IOException("File too small: missing float count");
			}

			// floatCount 是 float 总数，不是顶点数。 / floatCount is the total number of floats, not vertices.
			int floatCount = nav.getInt();
			if (floatCount <= 0 || floatCount > 3000000) {
				throw new IOException("Invalid float count: " + floatCount);
			}

			// 计算顶点数量（每个顶点 3 个 float：x,y,z） / Calculate vertex count (each vertex has 3 floats: x, y, z)
			int vertexCount = floatCount / VERTEX_COMPONENTS;
			if (vertexCount <= 0 || vertexCount > 1000000) {
				throw new IOException("Invalid vertex count: " + vertexCount);
			}

			// 保存顶点数据起始位置（紧接头部之后） / Save the start position of vertex data (right after the header)
			int vertexDataStart = nav.position();

			// 计算顶点数据总字节大小 / Calculate total vertex data size in bytes
			int vertexDataSize = floatCount * FLOAT_SIZE_BYTES;

			// 校验数据是否足够 / Validate we have enough data
			if (nav.remaining() < vertexDataSize) {
				throw new IOException("Vertex data truncated: need " + vertexDataSize + " bytes, have " + nav.remaining());
			}

			// 暂时跳过顶点数据（将通过 getVertices 访问） / Skip vertex data for now (will be accessed via getVertices)
			nav.position(vertexDataStart + vertexDataSize);

			// 读取三角形数量 / Read triangle count
			if (nav.remaining() < INT_SIZE_BYTES) {
				throw new IOException("Missing triangle count");
			}
			int triangleCount = nav.getInt();
			if (triangleCount <= 0 || triangleCount > 1000000) {
				throw new IOException("Invalid triangle count: " + triangleCount);
			}

			// 计算三角形数据预期大小：每个三角形 6 个 int（3 索引 + 3 连接）。 / Calculate expected triangle data size: each triangle has 6 ints (3 indices + 3 connections)
			int expectedTriangleDataBytes = triangleCount * INT_SIZE_BYTES * 6;
			if (nav.remaining() < expectedTriangleDataBytes) {
				throw new IOException("Triangle data truncated: need " + expectedTriangleDataBytes + " bytes, have " + nav.remaining());
			}

			// 解析三角形 / Parse triangles
			NavGeometry[] triangles = new NavGeometry[triangleCount];
			int[][] connections = new int[triangleCount][3];

			for (int i = 0; i < triangleCount; i++) {
				// 读取顶点索引 / Read vertex indices
				int[] indices = new int[3];
				indices[0] = nav.getInt();
				indices[1] = nav.getInt();
				indices[2] = nav.getInt();

				// 校验索引 / Validate indices
				if (indices[0] < 0 || indices[0] >= vertexCount ||
					indices[1] < 0 || indices[1] >= vertexCount ||
					indices[2] < 0 || indices[2] >= vertexCount) {
					throw new IOException("Invalid vertex index in triangle " + i + ": [" + indices[0] + ", " + indices[1] + ", " + indices[2] + "] max vertex index: " + (vertexCount - 1));
				}

				// 用顶点创建三角形几何 / Create triangle geometry with vertices
				float[] vertices = getVertices(nav, vertexDataStart, indices);
				triangles[i] = new NavGeometry(null, vertices);

				// 读取边连接 / Read edge connections
				for (int edge = 0; edge < 3; edge++) {
					int connection = nav.getInt();
					if (connection < -1 || connection >= triangleCount) {
						throw new IOException("Invalid connection index in triangle " + i + ", edge " + edge + ": " + connection);
					}
					connections[i][edge] = connection;
				}
			}

			// 构建邻接链接 / Build adjacency links
			for (int i = 0; i < triangleCount; i++) {
				if (connections[i][0] != -1) {
					triangles[i].setEdge1(triangles[connections[i][0]]);
				}
				if (connections[i][1] != -1) {
					triangles[i].setEdge2(triangles[connections[i][1]]);
				}
				if (connections[i][2] != -1) {
					triangles[i].setEdge3(triangles[connections[i][2]]);
				}

				triangles[i].updateModelBound();
				map.attachChild(triangles[i]);
			}

			map.updateModelBound();
			logDebug("Successfully loaded {} triangles for map {}", triangleCount, worldId);
		}

		return true;
	}

	/**
	 * 从缓冲区提取顶点坐标。
	 * Extracts vertex coordinates from the buffer.
	 *
	 * @param nav 含顶点数据的映射缓冲区 / mapped buffer containing vertex data
	 * @param vertexDataStart 顶点数据起始位置（文件头之后） / start position of vertex data (after header)
	 * @param indices 待提取的顶点索引（每个三角形 3 个） / indices of vertices to extract (3 per triangle)
	 * @return 顶点坐标数组 [x,y,z, x,y,z, x,y,z]（三角形 9 个 float） / vertex coordinates (9 floats for a triangle)
	 */
	private static float[] getVertices(ByteBuffer nav, int vertexDataStart, int[] indices) {
		float[] vertices = new float[indices.length * VERTEX_COMPONENTS];

		for (int i = 0; i < indices.length; i++) {
			// Calculate offset: start + (index * vertex_stride)
			int vertexOffset = vertexDataStart + (indices[i] * VERTEX_STRIDE_BYTES);

			// 读取 X、Y、Z 坐标 / Read X, Y, Z coordinates
			vertices[i * VERTEX_COMPONENTS] = nav.getFloat(vertexOffset);
			vertices[i * VERTEX_COMPONENTS + 1] = nav.getFloat(vertexOffset + FLOAT_SIZE_BYTES);
			vertices[i * VERTEX_COMPONENTS + 2] = nav.getFloat(vertexOffset + (FLOAT_SIZE_BYTES * 2));
		}

		return vertices;
	}

	/**
	 * 将文件通道只读映射为小端 ByteBuffer。
	 * Maps a file channel read-only into a little-endian ByteBuffer.
	 *
	 * file channel
	 * @param arena 内存作用域 / memory arena
	 * @return 映射缓冲区 / mapped buffer
	 * on mapping failure
	 */
	private static ByteBuffer mapReadOnly(FileChannel channel, Arena arena) throws IOException {
		long size = channel.size();
		if (size > Integer.MAX_VALUE) {
			throw new IOException("Navigation mesh is too large to map into a ByteBuffer: " + size + " bytes");
		}
		MemorySegment segment = channel.map(FileChannel.MapMode.READ_ONLY, 0, size, arena);
		segment.load();
		return segment.asByteBuffer().order(ByteOrder.LITTLE_ENDIAN);
	}

	/**
	 * 从缓存中移除指定地图以释放内存。
	 * Removes a specific map from the cache to free memory.
	 * Useful for memory management in long-running servers.
	 *
	 * @param worldId 待清理的地图 ID / map id to clear
	 */
	public void clearNavMap(int worldId) {
		GeoMap removed;
		synchronized (navMaps) {
			removed = navMaps.remove(worldId);
		}
		if (removed != null) {
			removed.detachAllChildren();
			logDebug("Cleared navigation cache for map {}", worldId);
		}

	}

	/**
	 * 清空全部导航地图缓存。
	 * Clears all navigation maps from cache.
	 */
	public void clearAllNavMaps() {
		synchronized (navMaps) {
			for (GeoMap map : navMaps.values()) {
				if (map != null) {
					map.detachAllChildren();
				}
			}
			navMaps.clear();
		}
		logInfo(I18n.get("console.navigation.cache_cleared"));
	}

	/**
	 * 返回文件索引大小（可用导航网格数量）。
	 * Returns the file index size (number of available nav meshes).
	 *
	 * @return 可用地图数 / available map count
	 */
	public int getAvailableMapCount() {
		return navFiles.size();
	}

	/**
	 * 返回当前已加载的导航网格数量。
	 * Returns the number of currently loaded nav meshes.
	 *
	 * @return 已加载地图数 / loaded map count
	 */
	public int getLoadedMapCount() {
		synchronized (navMaps) {
			return navMaps.size();
		}
	}

	/**
	 * 输出 info 日志。
	 * Logs at info level.
	 *
	 * message
	 * arguments
	 */
	private static void logInfo(String message, Object... arguments) {
		log.info(message, arguments);
	}

	/**
	 * 输出 debug 日志。
	 * Logs at debug level.
	 *
	 * message
	 * arguments
	 */
	private static void logDebug(String message, Object... arguments) {
		log.debug(message, arguments);
	}

	/**
	 * 创建控制台进度渲染器。
	 * Creates a console progress renderer.
	 *
	 * @return 进度渲染器 / progress renderer
	 */
	private static ConsoleProgressLineRenderer progressRenderer() {
		return new ConsoleProgressLineRenderer(System.out, true);
	}

	/**
	 * 获取单例（优先 Spring 提供者）。
	 * Returns the singleton (preferring the Spring provider).
	 *
	 * service instance
	 */
	public static NavData getInstance() {
		ObjectProvider<NavData> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.INSTANCE);
		}
		return SingletonHolder.INSTANCE;
	}

	/**
	 * 注入 Spring 单例提供者。
	 * Injects the Spring singleton provider.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<NavData> provider) {
		instanceProvider = provider;
	}

	/**
	 * 单例持有者。
	 * Singleton holder.
	 */
	private static final class SingletonHolder {
		/** 默认实例。 / Default instance. */
		protected static final NavData INSTANCE = new NavData();
	}
}
