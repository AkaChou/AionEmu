/**
 * This file is part of the Aion Reconstruction Project Server.
 *
 * The Aion Reconstruction Project Server is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * The Aion Reconstruction Project Server is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with the Aion Reconstruction Project Server. If not see
 * <http://www.gnu.org/licenses/>.
 *
 * @AionReconstructionProjectTeam
 */
package com.aionemu.gameserver.world.geo.nav;

import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.ref.SoftReference;
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
 * Thread-safe lazy loader for navigation mesh data.
 * Loads nav meshes on-demand and caches them for future use.
 * Memory-efficient: only loads maps that are actually accessed.
 * 
 * @author Yon (Aion Reconstruction Project)
 */
@Slf4j
public class NavData {

    private static volatile ObjectProvider<NavData> instanceProvider;

    /** Navigation data directory */
    private static final String NAV_DIR = "./data/geo/nav/";
    
    /** Size of float in bytes */
    private static final int FLOAT_SIZE_BYTES = 4;
    
    /** Size of int in bytes */
    private static final int INT_SIZE_BYTES = 4;
    
    /** Number of components per vertex (x, y, z) */
    private static final int VERTEX_COMPONENTS = 3;
    
    /** Size of one vertex in bytes */
    private static final int VERTEX_STRIDE_BYTES = FLOAT_SIZE_BYTES * VERTEX_COMPONENTS;
    
    /** Header size: one int for float count (legacy format) */
    private static final int HEADER_SIZE_BYTES = INT_SIZE_BYTES;

    /**
     * Access-order strong cache of loaded navigation maps.
     */
    private final Map<Integer, GeoMap> navMaps = Collections.synchronizedMap(new LinkedHashMap<Integer, GeoMap>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, GeoMap> eldest) {
            int maxSize = GeoDataConfig.GEO_NAV_CACHE_SIZE;
            boolean remove = maxSize > 0 && size() > maxSize;
            if (remove && eldest.getValue() != null) {
                eldest.getValue().detachAllChildren();
                logDebug("Evicted navigation mesh for map {} from strong cache", eldest.getKey());
            }
            return remove;
        }
    });
    
    /**
     * File index - stores only file references, not the actual data.
     * Memory-efficient, populated at startup.
     */
    private final ConcurrentHashMap<Integer, File> navFiles = new ConcurrentHashMap<>();
    
    /**
     * SoftReference-based cache for memory-sensitive environments.
     * Used only when enabled via config.
     */
    private final ConcurrentHashMap<Integer, SoftReference<GeoMap>> softNavMaps = new ConcurrentHashMap<>();
    
    /**
     * Lock for map loading when computeIfAbsent can't be used
     * (e.g., for soft reference cache)
     */
    private final ConcurrentHashMap<Integer, ReentrantLock> mapLocks = new ConcurrentHashMap<>();

    public NavData() {}

    /**
     * Checks if navigation data index exists.
     * Does NOT indicate if actual map data is loaded.
     */
    boolean isLoaded() {
        return !navFiles.isEmpty();
    }

    /**
     * Scans for navigation files and builds the file index.
     * Does NOT load actual map data - lazy loading only.
     * Fast startup, minimal memory usage.
     */
    void loadNavMaps() {
        // Skip if pathfinding is globally disabled
        if (!GeoDataConfig.GEO_NAV_ENABLE) {
            logInfo("Navigation system is disabled, skipping file scan.");
            return;
        }
        
        logInfo("Scanning for navigation files...");
        long startTime = System.currentTimeMillis();
        int fileCount = 0;
        int scannedCount = 0;
        int totalMaps = DataManager.WORLD_MAPS_DATA.size();
        ConsoleProgressLineRenderer progressRenderer = progressRenderer();
        
        for (WorldMapTemplate map : DataManager.WORLD_MAPS_DATA) {
            int mapId = map.getMapId();
            File navFile = Config.dataFile(NAV_DIR + mapId + ".nav");
            
            if (navFile.exists() && navFile.isFile()) {
                navFiles.put(mapId, navFile);
                fileCount++;
            }
            progressRenderer.progress("NavigationFiles", ++scannedCount, totalMaps);
        }

        progressRenderer.finished("NavigationFiles", totalMaps);
        
        long duration = System.currentTimeMillis() - startTime;
        logInfo("Found {} navigation files, took {} ms", fileCount, duration);

        if (!GeoDataConfig.GEO_NAV_LAZY_LOAD) {
            preloadNavMaps();
        }
    }

    /**
     * Returns the navigation mesh for the specified map.
     * Thread-safe, lazy loading: map is loaded on first access.
     * 
     * @param worldId Map ID
     * @return GeoMap with nav mesh, or null if not available
     */
    public GeoMap getNavMap(int worldId) {
        return getNavMap(worldId, true);
    }

    private GeoMap getNavMap(int worldId, boolean logLoadInfo) {
        // Fast path: config check
        if (!GeoDataConfig.GEO_NAV_ENABLE) {
            return null;
        }

        if (GeoDataConfig.GEO_NAV_SOFT_CACHE) {
            return getSoftCachedMap(worldId, logLoadInfo);
        }

        return getStrongCachedMap(worldId, logLoadInfo);
    }

    private GeoMap getStrongCachedMap(int worldId, boolean logLoadInfo) {
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
            GeoMap loaded = loadMap(worldId, logLoadInfo);
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

    private GeoMap getSoftCachedMap(int worldId, boolean logLoadInfo) {
        GeoMap cached = getSoftReference(worldId);
        if (cached != null) {
            return cached;
        }

        ReentrantLock lock = mapLocks.computeIfAbsent(worldId, id -> new ReentrantLock());
        lock.lock();
        try {
            cached = getSoftReference(worldId);
            if (cached != null) {
                return cached;
            }
            GeoMap loaded = loadMap(worldId, logLoadInfo);
            if (loaded != null) {
                softNavMaps.put(worldId, new SoftReference<>(loaded));
            }
            return loaded;
        } finally {
            lock.unlock();
        }
    }

    private GeoMap getSoftReference(int worldId) {
        SoftReference<GeoMap> ref = softNavMaps.get(worldId);
        if (ref == null) {
            return null;
        }
        GeoMap map = ref.get();
        if (map == null) {
            softNavMaps.remove(worldId, ref);
        }
        return map;
    }

    private void preloadNavMaps() {
        int loaded = 0;
        long startTime = System.currentTimeMillis();
        logInfo("Loading navigation meshes..");
        int totalMaps = navFiles.size();
        int processedMaps = 0;
        ConsoleProgressLineRenderer progressRenderer = progressRenderer();
        for (Integer worldId : navFiles.keySet()) {
            if (getNavMap(worldId, false) != null) {
                loaded++;
            }
            progressRenderer.progress("NavigationMeshes", ++processedMaps, totalMaps);
        }
        progressRenderer.finished("NavigationMeshes", totalMaps);
        logInfo("Preloaded {} navigation meshes, took {} ms", loaded, System.currentTimeMillis() - startTime);
    }

    /**
     * Internal map loading logic.
     * Called only once per map via computeIfAbsent.
     */
    private GeoMap loadMap(Integer worldId, boolean logLoadInfo) {
        // Check if file exists for this map
        File navFile = navFiles.get(worldId);
        if (navFile == null) {
            logDebug("No navigation file for map {}", worldId);
            return null;
        }
        
        WorldMapTemplate template = DataManager.WORLD_MAPS_DATA.getTemplate(worldId);
        if (template == null) {
            log.error("World map template not found for ID: {}", worldId);
            return null;
        }
        
        GeoMap geoMap = new GeoMap(String.valueOf(worldId), template.getWorldSize());
        
        long startTime = System.currentTimeMillis();
        try {
            if (loadNavMesh(worldId, navFile, geoMap)) {
                long duration = System.currentTimeMillis() - startTime;
                if (logLoadInfo) {
                    logInfo("Loaded navigation mesh for map {} ({} triangles), took {} ms", worldId, geoMap.getChildren() != null ? geoMap.getChildren().size() : 0, duration);
                } else {
                    logDebug("Loaded navigation mesh for map {} ({} triangles), took {} ms", worldId, geoMap.getChildren() != null ? geoMap.getChildren().size() : 0, duration);
                }
                return geoMap;
            }
        } catch (IOException e) {
            log.error("Failed to load navigation file for map {}: {}", worldId, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error loading navigation for map {}: {}", worldId, e.getMessage(), e);
        }
        
        return null;
    }

    /**
     * Parses .nav file and constructs navigation mesh.
     * 
     * Legacy file format:
     * - int: floatCount (total number of floats = vertexCount * 3)
     * - float[vertexCount * 3]: vertex positions (x,y,z)
     * - int: triangleCount
     * - for each triangle:
     *   - int[3]: vertex indices
     *   - int[3]: adjacent triangle indices (-1 if no connection)
     * 
     * @param worldId Map ID for logging
     * @param navFile File to parse
     * @param map GeoMap to populate
     * @return true if loading succeeded
     * @throws IOException on file read errors
     */
    private boolean loadNavMesh(int worldId, File navFile, GeoMap map) throws IOException {
        try (RandomAccessFile raFile = new RandomAccessFile(navFile, "r");
             FileChannel roChannel = raFile.getChannel();
             Arena arena = Arena.ofConfined()) {
            
            ByteBuffer nav = mapReadOnly(roChannel, arena);

            // Validate file size
            if (nav.remaining() < HEADER_SIZE_BYTES) {
                throw new IOException("File too small: missing float count");
            }

            // Read floatCount (legacy format - total number of floats, not vertices)
            int floatCount = nav.getInt();
            if (floatCount <= 0 || floatCount > 3000000) {
                throw new IOException("Invalid float count: " + floatCount);
            }

            // Calculate vertex count (each vertex has 3 floats: x, y, z)
            int vertexCount = floatCount / VERTEX_COMPONENTS;
            if (vertexCount <= 0 || vertexCount > 1000000) {
                throw new IOException("Invalid vertex count: " + vertexCount);
            }

            // Save the start position of vertex data (right after the header)
            int vertexDataStart = nav.position();

            // Calculate total vertex data size in bytes
            int vertexDataSize = floatCount * FLOAT_SIZE_BYTES;

            // Validate we have enough data
            if (nav.remaining() < vertexDataSize) {
                throw new IOException("Vertex data truncated: need " + vertexDataSize + " bytes, have " + nav.remaining());
            }

            // Skip vertex data for now (will be accessed via getVertices)
            nav.position(vertexDataStart + vertexDataSize);

            // Read triangle count
            if (nav.remaining() < INT_SIZE_BYTES) {
                throw new IOException("Missing triangle count");
            }
            int triangleCount = nav.getInt();
            if (triangleCount <= 0 || triangleCount > 1000000) {
                throw new IOException("Invalid triangle count: " + triangleCount);
            }

            // Calculate expected triangle data size: each triangle has 6 ints (3 indices + 3 connections)
            int expectedTriangleDataBytes = triangleCount * INT_SIZE_BYTES * 6;
            if (nav.remaining() < expectedTriangleDataBytes) {
                throw new IOException("Triangle data truncated: need " + expectedTriangleDataBytes + " bytes, have " + nav.remaining());
            }

            // Parse triangles
            NavGeometry[] triangles = new NavGeometry[triangleCount];
            int[][] connections = new int[triangleCount][3];

            for (int i = 0; i < triangleCount; i++) {
                // Read vertex indices
                int[] indices = new int[3];
                indices[0] = nav.getInt();
                indices[1] = nav.getInt();
                indices[2] = nav.getInt();

                // Validate indices
                if (indices[0] < 0 || indices[0] >= vertexCount ||
                    indices[1] < 0 || indices[1] >= vertexCount ||
                    indices[2] < 0 || indices[2] >= vertexCount) {
                    throw new IOException("Invalid vertex index in triangle " + i + ": [" + indices[0] + ", " + indices[1] + ", " + indices[2] + "] max vertex index: " + (vertexCount - 1));
                }

                // Create triangle geometry with vertices
                float[] vertices = getVertices(nav, vertexDataStart, indices);
                triangles[i] = new NavGeometry(null, vertices);

                // Read edge connections
                connections[i][0] = nav.getInt();
                connections[i][1] = nav.getInt();
                connections[i][2] = nav.getInt();
            }

            // Build adjacency links
            for (int i = 0; i < triangleCount; i++) {
                if (connections[i][0] != -1 && connections[i][0] < triangleCount) {
                    triangles[i].setEdge1(triangles[connections[i][0]]);
                }
                if (connections[i][1] != -1 && connections[i][1] < triangleCount) {
                    triangles[i].setEdge2(triangles[connections[i][1]]);
                }
                if (connections[i][2] != -1 && connections[i][2] < triangleCount) {
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
     * Extracts vertex coordinates from the buffer.
     * 
     * @param nav Mapped buffer containing vertex data
     * @param vertexDataStart Start position of vertex data in buffer (after header)
     * @param indices Indices of vertices to extract (3 indices per triangle)
     * @return Array of vertex coordinates [x,y,z, x,y,z, x,y,z] (9 floats for a triangle)
     */
    private static float[] getVertices(ByteBuffer nav, int vertexDataStart, int[] indices) {
        float[] vertices = new float[indices.length * VERTEX_COMPONENTS];
        
        for (int i = 0; i < indices.length; i++) {
            // Calculate offset: start + (index * vertex_stride)
            int vertexOffset = vertexDataStart + (indices[i] * VERTEX_STRIDE_BYTES);
            
            // Read X, Y, Z coordinates
            vertices[i * VERTEX_COMPONENTS] = nav.getFloat(vertexOffset);
            vertices[i * VERTEX_COMPONENTS + 1] = nav.getFloat(vertexOffset + FLOAT_SIZE_BYTES);
            vertices[i * VERTEX_COMPONENTS + 2] = nav.getFloat(vertexOffset + (FLOAT_SIZE_BYTES * 2));
        }
        
        return vertices;
    }

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
     * Removes a specific map from the cache to free memory.
     * Useful for memory management in long-running servers.
     * 
     * @param worldId Map ID to clear
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

        SoftReference<GeoMap> ref = softNavMaps.remove(worldId);
        GeoMap softMap = ref == null ? null : ref.get();
        if (softMap != null && softMap != removed) {
            softMap.detachAllChildren();
        }
    }

    /**
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
        for (SoftReference<GeoMap> ref : softNavMaps.values()) {
            GeoMap map = ref.get();
            if (map != null) {
                map.detachAllChildren();
            }
        }
        softNavMaps.clear();
        logInfo("Cleared all navigation caches");
    }

    /**
     * Returns the file index size (number of available nav meshes).
     */
    public int getAvailableMapCount() {
        return navFiles.size();
    }

    /**
     * Returns the number of currently loaded nav meshes.
     */
    public int getLoadedMapCount() {
        if (!GeoDataConfig.GEO_NAV_SOFT_CACHE) {
            synchronized (navMaps) {
                return navMaps.size();
            }
        }
        int count = 0;
        for (Map.Entry<Integer, SoftReference<GeoMap>> entry : softNavMaps.entrySet()) {
            if (entry.getValue().get() == null) {
                softNavMaps.remove(entry.getKey(), entry.getValue());
            } else {
                count++;
            }
        }
        return count;
    }

    private static void logInfo(String message, Object... arguments) {
        if (GeoDataConfig.GEO_NAV_LOG_LEVEL >= 1) {
            log.info(message, arguments);
        }
    }

    private static void logDebug(String message, Object... arguments) {
        if (GeoDataConfig.GEO_NAV_LOG_LEVEL >= 2) {
            log.debug(message, arguments);
        }
    }

    private static boolean showProgress() {
        return GeoDataConfig.GEO_NAV_LOG_LEVEL >= 1;
    }

    private static ConsoleProgressLineRenderer progressRenderer() {
        return new ConsoleProgressLineRenderer(System.out, showProgress());
    }

    /**
     * Singleton holder.
     */
    public static NavData getInstance() {
        ObjectProvider<NavData> provider = instanceProvider;
        if (provider != null) {
            return provider.getIfAvailable(() -> SingletonHolder.INSTANCE);
        }
        return SingletonHolder.INSTANCE;
    }

    public static void setInstanceProvider(ObjectProvider<NavData> provider) {
        instanceProvider = provider;
    }

    private static final class SingletonHolder {
        protected static final NavData INSTANCE = new NavData();
    }
}
