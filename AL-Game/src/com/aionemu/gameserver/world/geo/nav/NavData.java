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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.geoEngine.models.GeoMap;
import com.aionemu.gameserver.geoEngine.scene.NavGeometry;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;

/**
 * Thread-safe lazy loader for navigation mesh data.
 * Loads nav meshes on-demand and caches them for future use.
 * Memory-efficient: only loads maps that are actually accessed.
 * 
 * @author Yon (Aion Reconstruction Project)
 */
public class NavData {

    private static final Logger LOG = LoggerFactory.getLogger(NavData.class);

    /** Navigation data directory */
    private static final String NAV_DIR = "./data/nav/";
    
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
     * Thread-safe cache of loaded navigation maps.
     * Uses ConcurrentHashMap for lock-free reads and atomic lazy loading.
     */
    private final ConcurrentHashMap<Integer, GeoMap> navMaps = new ConcurrentHashMap<>();
    
    /**
     * File index - stores only file references, not the actual data.
     * Memory-efficient, populated at startup.
     */
    private final ConcurrentHashMap<Integer, File> navFiles = new ConcurrentHashMap<>();
    
    /**
     * Optional: SoftReference-based cache for memory-sensitive environments.
     * Can be enabled via config.
     */
    private final ConcurrentHashMap<Integer, java.lang.ref.SoftReference<GeoMap>> softNavMaps = GeoDataConfig.GEO_NAV_SOFT_CACHE ? new ConcurrentHashMap<>() : null;
    
    /**
     * Lock for map loading when computeIfAbsent can't be used
     * (e.g., for soft reference cache)
     */
    private final ConcurrentHashMap<Integer, ReentrantLock> mapLocks = new ConcurrentHashMap<>();

    private NavData() {}

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
            LOG.info("Navigation system is disabled, skipping file scan.");
            return;
        }
        
        LOG.info("Scanning for navigation files...");
        long startTime = System.currentTimeMillis();
        int fileCount = 0;
        
        for (WorldMapTemplate map : DataManager.WORLD_MAPS_DATA) {
            int mapId = map.getMapId();
            File navFile = new File(NAV_DIR, mapId + ".nav");
            
            if (navFile.exists() && navFile.isFile()) {
                navFiles.put(mapId, navFile);
                fileCount++;
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        LOG.info("Found {} navigation files, took {} ms", fileCount, duration);
    }

    /**
     * Returns the navigation mesh for the specified map.
     * Thread-safe, lazy loading: map is loaded on first access.
     * 
     * @param worldId Map ID
     * @return GeoMap with nav mesh, or null if not available
     */
    public GeoMap getNavMap(int worldId) {
        // Fast path: config check
        if (!GeoDataConfig.GEO_NAV_ENABLE) {
            return null;
        }
        
        // Fast path: soft reference cache if enabled
        if (softNavMaps != null) {
            java.lang.ref.SoftReference<GeoMap> ref = softNavMaps.get(worldId);
            if (ref != null) {
                GeoMap map = ref.get();
                if (map != null) {
                    return map;
                }
                // Reference was cleared, remove stale entry
                softNavMaps.remove(worldId);
            }
        }
        
        // Standard cache with atomic lazy loading
        return navMaps.computeIfAbsent(worldId, this::loadMap);
    }

    /**
     * Internal map loading logic.
     * Called only once per map via computeIfAbsent.
     */
    private GeoMap loadMap(Integer worldId) {
        // Check if file exists for this map
        File navFile = navFiles.get(worldId);
        if (navFile == null) {
            LOG.debug("No navigation file for map {}", worldId);
            return null;
        }
        
        WorldMapTemplate template = DataManager.WORLD_MAPS_DATA.getTemplate(worldId);
        if (template == null) {
            LOG.error("World map template not found for ID: {}", worldId);
            return null;
        }
        
        GeoMap geoMap = new GeoMap(String.valueOf(worldId), template.getWorldSize());
        
        long startTime = System.currentTimeMillis();
        try {
            if (loadNavMesh(worldId, navFile, geoMap)) {
                long duration = System.currentTimeMillis() - startTime;
                LOG.info("Loaded navigation mesh for map {} ({} triangles), took {} ms", worldId, geoMap.getChildren() != null ? geoMap.getChildren().size() : 0, duration);
                
                // Also store in soft cache if enabled
                if (softNavMaps != null) {
                    softNavMaps.put(worldId, new java.lang.ref.SoftReference<>(geoMap));
                }
                
                return geoMap;
            }
        } catch (IOException e) {
            LOG.error("Failed to load navigation file for map {}: {}", worldId, e.getMessage(), e);
        } catch (Exception e) {
            LOG.error("Unexpected error loading navigation for map {}: {}", worldId, e.getMessage(), e);
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
             FileChannel roChannel = raFile.getChannel()) {
            
            MappedByteBuffer nav = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int) roChannel.size());
            nav.load();
            nav.order(ByteOrder.LITTLE_ENDIAN);
            
            try {
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
                    if (connections[i][0] != -1 && connections[i][0] < triangleCount) 
                        triangles[i].setEdge1(triangles[connections[i][0]]);
                    if (connections[i][1] != -1 && connections[i][1] < triangleCount) 
                        triangles[i].setEdge2(triangles[connections[i][1]]);
                    if (connections[i][2] != -1 && connections[i][2] < triangleCount) 
                        triangles[i].setEdge3(triangles[connections[i][2]]);
                    
                    triangles[i].updateModelBound();
                    map.attachChild(triangles[i]);
                }
                
                map.updateModelBound();
                LOG.debug("Successfully loaded {} triangles for map {}", triangleCount, worldId);
                
            } finally {
                // Always release native buffer, even on error
                releaseDirectBuffer(nav);
            }
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
    private static float[] getVertices(MappedByteBuffer nav, int vertexDataStart, int[] indices) {
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

    /**
     * Safely releases a direct byte buffer's native memory.
     * Uses reflection to avoid a compile-time dependency on Unsafe.
     */
    private static void releaseDirectBuffer(Buffer buffer) {
        if (!(buffer instanceof ByteBuffer) || !buffer.isDirect()) {
            return;
        }
        
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Object unsafe = unsafeField.get(null);
            Method invokeCleaner = unsafeClass.getMethod("invokeCleaner", ByteBuffer.class);
            invokeCleaner.invoke(unsafe, (ByteBuffer) buffer);
        } catch (Exception e) {
            LOG.debug("Failed to release direct buffer: {}", e.getMessage());
        }
    }

    /**
     * Removes a specific map from the cache to free memory.
     * Useful for memory management in long-running servers.
     * 
     * @param worldId Map ID to clear
     */
    public void clearNavMap(int worldId) {
        GeoMap removed = navMaps.remove(worldId);
        if (removed != null) {
            removed.detachAllChildren();
            LOG.debug("Cleared navigation cache for map {}", worldId);
        }
        
        if (softNavMaps != null) {
            softNavMaps.remove(worldId);
        }
    }

    /**
     * Clears all navigation maps from cache.
     */
    public void clearAllNavMaps() {
        navMaps.clear();
        if (softNavMaps != null) {
            softNavMaps.clear();
        }
        LOG.info("Cleared all navigation caches");
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
        return navMaps.size();
    }

    /**
     * Singleton holder.
     */
    public static NavData getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static final class SingletonHolder {
        protected static final NavData INSTANCE = new NavData();
    }
}
