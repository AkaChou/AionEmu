/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.geoEngine;

import lombok.extern.slf4j.Slf4j;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.geoEngine.bounding.BoundingVolume;
import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.math.Matrix3f;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.geoEngine.models.GeoMap;
import com.aionemu.gameserver.geoEngine.scene.Geometry;
import com.aionemu.gameserver.geoEngine.scene.DespawnableNode;
import com.aionemu.gameserver.geoEngine.scene.Mesh;
import com.aionemu.gameserver.geoEngine.scene.Node;
import com.aionemu.gameserver.geoEngine.scene.Spatial;
import com.aionemu.gameserver.geoEngine.scene.VertexBuffer;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.model.templates.materials.MaterialTemplate;
import com.aionemu.gameserver.world.zone.ZoneName;

/**
 * @author Mr. Poke
 */
@Slf4j
public class GeoWorldLoader {

	private static final String GEO_DIR = "geo/";

	public static Map<String, Spatial> loadMeshs(String fileName) throws IOException {
		Map<String, Spatial> geoms = new HashMap<String, Spatial>();
		File geoFile = Config.geoFile(fileName);
		try (RandomAccessFile raFile = new RandomAccessFile(geoFile, "r");
			 FileChannel roChannel = raFile.getChannel();
			 Arena arena = Arena.ofConfined()) {
			ByteBuffer geo = mapReadOnly(roChannel, arena).order(ByteOrder.BIG_ENDIAN);
			while (geo.hasRemaining()) {
				int namelenght = Short.toUnsignedInt(geo.getShort());
				byte[] nameByte = new byte[namelenght];
				geo.get(nameByte);
				String name = new String(nameByte).intern();
				boolean hasAliases = name.indexOf('|') >= 0;
				Node node = new Node();
				byte intentions = 0;
				byte singleChildMaterialId = -1;
				int modelCount = Byte.toUnsignedInt(geo.get());
				for (int c = 0; c < modelCount; c++) {
					Mesh m = new Mesh();

					int vectorCount = Short.toUnsignedInt(geo.getShort()) * 3;

					ByteBuffer floatBuffer = ByteBuffer.allocateDirect(vectorCount * 4);
					FloatBuffer vertices = floatBuffer.asFloatBuffer();
					for (int x = 0; x < vectorCount; x++) {
						vertices.put(geo.getFloat());
					}

					int triangles = Short.toUnsignedInt(geo.getShort()) * 3;
					int indexSize = Byte.toUnsignedInt(geo.get());
					ByteBuffer shortBuffer = ByteBuffer.allocateDirect(triangles * 2);
					ShortBuffer indexes = shortBuffer.asShortBuffer();
					for (int x = 0; x < triangles; x++) {
						indexes.put((short) (indexSize == 1 ? Byte.toUnsignedInt(geo.get()) : Short.toUnsignedInt(geo.getShort())));
					}

					Geometry geom;
					int materialId = Byte.toUnsignedInt(geo.get());
					int collisionIntentions = Byte.toUnsignedInt(geo.get());
					m.setCollisionFlags((short) (collisionIntentions << 8 | materialId));
					intentions |= m.getIntentions();
					m.setBuffer(VertexBuffer.Type.Position, 3, vertices);
					m.setBuffer(VertexBuffer.Type.Index, 3, indexes);
					// ponytail: 碰撞树不再同步构建，改由 RealGeoData 后台并行预构建，避免阻塞启动

					MaterialTemplate mtl = DataManager.MATERIAL_DATA == null ? null : DataManager.MATERIAL_DATA.getTemplate(m.getMaterialId());
					geom = new Geometry(null, m);
					if (mtl != null || m.getMaterialId() == 11) {
						node.setName(name);
					}
					if (modelCount == 1) {
						geom.setName(name);
						singleChildMaterialId = geom.getMaterialId();
					} else {
						geom.setName(("child" + c + "_" + name).intern());
					}
					node.attachChild(geom);
					if (!hasAliases) {
						geoms.put(geom.getName().toLowerCase().intern(), geom);
					}
				}
				node.setCollisionFlags((short) (intentions << 8 | singleChildMaterialId & 0xFF));
				if (!node.getChildren().isEmpty()) {
					if (!hasAliases) {
						geoms.put(name.toLowerCase().intern(), node);
					} else {
						try {
							for (String alias : name.split("\\|")) {
								Node clone = node.clone();
								if (clone.getName() != null) {
									clone.setName(alias);
								}
								Spatial child = clone.getChild(name);
								if (child != null) {
									child.setName(alias);
								}
								geoms.put(alias.toLowerCase().intern(), clone);
							}
						} catch (CloneNotSupportedException e) {
							throw new IOException("Failed to clone aliased geo model " + name, e);
						}
					}
				}
			}
		}
		return geoms;

	}

	public static void loadWorldObjects(int worldId, Map<String, Spatial> models, GeoMap map, Set<String> missingMeshes) throws IOException {
		File geoFile = Config.geoFile(GEO_DIR + worldId + ".geo");
		if (!geoFile.exists()) {
			return;
		}
		try (RandomAccessFile raFile = new RandomAccessFile(geoFile, "r");
			 FileChannel roChannel = raFile.getChannel();
			 Arena arena = Arena.ofConfined()) {
			ByteBuffer geo = mapReadOnly(roChannel, arena).order(ByteOrder.BIG_ENDIAN);

			while (geo.hasRemaining()) {
				int nameLength = Short.toUnsignedInt(geo.getShort());
				byte[] nameByte = new byte[nameLength];
				geo.get(nameByte);
				String name = new String(nameByte);
				Vector3f loc = new Vector3f(geo.getFloat(), geo.getFloat(), geo.getFloat());
				float[] matrix = new float[9];
				for (int i = 0; i < 9; i++) {
					matrix[i] = geo.getFloat();
				}

				Vector3f scale = new Vector3f(geo.getFloat(), geo.getFloat(), geo.getFloat());
				byte type = geo.get();
				int id = Short.toUnsignedInt(geo.getShort());
				byte level = geo.get();

				Matrix3f matrix3f = new Matrix3f();
				matrix3f.set(matrix);
				Spatial node = models.get(name.toLowerCase().intern());
				if (node != null) {
					try {
					if (type > 0 && node instanceof Node) {
						node = despawnable((Node) node, type, id, level);
					}
					Spatial nodeClone = attachToMapAndCreateZones(map, node, matrix3f, loc, scale, worldId);
					if (nodeClone instanceof DespawnableNode
							&& ((DespawnableNode) nodeClone).type == DespawnableNode.DespawnableType.TOWN_OBJECT) {
						loadTownLevelEntities(map, models, (DespawnableNode) nodeClone, name, matrix3f, loc, scale, level,
								worldId);
					}
					} catch (CloneNotSupportedException e) {
						throw new IOException("Could not clone geo node " + name + " in world " + worldId, e);
					}
				} else {
					missingMeshes.add(name);
					if (missingMeshes.size() == 1) {
						log.warn("Missing geo mesh {} in world {}", name, worldId);
					}
				}
			}
		}
		map.updateModelBound();
	}

	public static void loadTerrains(Collection<GeoMap> maps) throws IOException {
		File geoDir = Config.geoFile(GEO_DIR);
		File[] files = geoDir.listFiles((dir, name) -> name.endsWith(".png"));
		if (files == null) {
			return;
		}
		Arrays.sort(files, (a, b) -> Boolean.compare(isDirectTerrainFile(a.getName()), isDirectTerrainFile(b.getName())));
		for (File file : files) {
			BufferedImage image = ImageIO.read(file);
			if (image == null) {
				throw new IOException("Unsupported terrain PNG: " + file);
			}
			int width = image.getWidth();
			int height = image.getHeight();
			Raster raster = image.getRaster();
			boolean material = file.getName().endsWith("_materials.png");
			String stem = file.getName().substring(0, file.getName().length() - (material ? "_materials.png".length() : ".png".length()));
			for (String token : stem.split(",")) {
				int mapId;
				try {
					mapId = Integer.parseInt(token);
				} catch (NumberFormatException e) {
					continue;
				}
				for (GeoMap map : maps) {
					if (map.getMapId() == mapId) {
						if (material) {
							map.setTerrainMaterialData(readMaterialData(raster, width, height), width, height);
						} else {
							map.setTerrainData(readHeightData(raster, width, height), width, height);
						}
						break;
					}
				}
			}
		}
	}

	private static boolean isDirectTerrainFile(String fileName) {
		String suffix = fileName.endsWith("_materials.png") ? "_materials.png" : ".png";
		String stem = fileName.substring(0, fileName.length() - suffix.length());
		return stem.indexOf(',') == -1;
	}

	private static DespawnableNode despawnable(Node node, byte type, int id, byte level) throws CloneNotSupportedException {
		DespawnableNode despawnable = new DespawnableNode();
		despawnable.copyFrom(node);
		despawnable.type = DespawnableNode.DespawnableType.getById(type);
		despawnable.id = id;
		if (despawnable.type == DespawnableNode.DespawnableType.TOWN_OBJECT) {
			if (level > 8) {
				throw new IllegalArgumentException(level + " doesn't fit in town level bit mask");
			}
			despawnable.levelBitMask = level < 1 ? 0 : (byte) (1 << (level - 1));
		} else if (level != 0) {
			throw new IllegalArgumentException("Unexpected town level " + level + " for " + despawnable.type);
		}
		return despawnable;
	}

	private static void loadTownLevelEntities(GeoMap map, Map<String, Spatial> models, DespawnableNode townEntity,
			String name, Matrix3f matrix, Vector3f loc, Vector3f scale, byte level, int worldId)
			throws CloneNotSupportedException {
		for (int townLevel = level + 1; townLevel <= 5; townLevel++) {
			String townEntityName = name.replace("_01.cgf", "_0" + townLevel + ".cgf");
			Spatial model = models.get(townEntityName.toLowerCase().intern());
			if (model instanceof Node) {
				DespawnableNode townNode = new DespawnableNode();
				townNode.copyFrom((Node) model);
				townNode.type = townEntity.type;
				townNode.id = townEntity.id;
				townNode.levelBitMask = (byte) (1 << (townLevel - 1));
				attachToMapAndCreateZones(map, townNode, matrix, loc, scale, worldId);
			} else {
				townEntity.levelBitMask |= (byte) (1 << (townLevel - 1));
			}
		}
	}

	private static short[] readHeightData(Raster raster, int width, int height) {
		short[] terrainData = new short[width * height];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				terrainData[y + x * height] = (short) raster.getSample(y, x, 0);
			}
		}
		return terrainData;
	}

	private static byte[] readMaterialData(Raster raster, int width, int height) {
		byte[] materialData = new byte[width * height];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				materialData[y + x * height] = (byte) raster.getSample(y, x, 0);
			}
		}
		return materialData;
	}

	private static Spatial attachToMapAndCreateZones(GeoMap map, Spatial node, Matrix3f matrix, Vector3f location,
			Vector3f scale, int worldId) throws CloneNotSupportedException {
		Spatial nodeClone = attachChild(map, node, matrix, location, scale);
		if (nodeClone instanceof Node) {
			List<Spatial> children = ((Node) nodeClone).getChildren();
			for (int c = 0; c < children.size(); c++) {
				createZone(children.get(c), worldId, children.size() == 1 ? 0 : c + 1);
			}
		} else {
			createZone(nodeClone, worldId, 0);
		}
		return nodeClone;
	}

	private static Spatial attachChild(GeoMap map, Spatial node, Matrix3f matrix, Vector3f location, Vector3f scale)
			throws CloneNotSupportedException {
		Spatial nodeClone = node.clone();
		nodeClone.setTransform(matrix, location, scale);
		nodeClone.updateModelBound();
		map.attachChild(nodeClone);
		return nodeClone;
	}

	private static void createZone(Spatial node, int worldId, int childNumber) {
		if (GeoDataConfig.GEO_MATERIALS_ENABLE && (node.getIntentions() & CollisionIntention.MATERIAL.getId()) != 0) {
			BoundingVolume bv = node.getWorldBound();
			int regionId = getVectorHash(bv.getCenter().x, bv.getCenter().y, bv.getCenter().z);
			int index = Math.max(node.getName().lastIndexOf('\\'), node.getName().lastIndexOf('/'));
			int dotIndex = node.getName().lastIndexOf('.');
			String zoneName = node.getName().substring(index + 1, dotIndex).toUpperCase();
			if (childNumber > 0) {
				zoneName += "_CHILD" + childNumber;
			}
			String existingName = zoneName + "_" + regionId + "_" + worldId;
			if (ZoneName.getId(existingName) != ZoneName.getId(ZoneName.NONE)) {
				// for override
				zoneName += "_" + regionId;
				node.setName(zoneName);
				GameWorldBootstrapServices.zoneService().createMaterialZoneTemplate(node, worldId, node.getMaterialId(), true);
			} else {
				node.setName(zoneName);
				GameWorldBootstrapServices.zoneService().createMaterialZoneTemplate(node, regionId, worldId, node.getMaterialId());
			}
		}
	}

	/**
	 * Hash formula from paper
	 * http://www.beosil.com/download/CollisionDetectionHashing_VMV03.pdf Hash table
	 * size 700001, the higher value, more precision
	 */
	private static int getVectorHash(float x, float y, float z) {
		long xIntBits = Float.floatToIntBits(x);
		long yIntBits = Float.floatToIntBits(y);
		long zIntBits = Float.floatToIntBits(z);
		return (int) ((xIntBits * 73856093 ^ yIntBits * 19349669 ^ zIntBits * 83492791) % 700001);
	}

	private static ByteBuffer mapReadOnly(FileChannel channel, Arena arena) throws IOException {
		long size = channel.size();
		if (size > Integer.MAX_VALUE) {
			throw new IOException("Geo file is too large to map into a ByteBuffer: " + size + " bytes");
		}
		MemorySegment segment = channel.map(FileChannel.MapMode.READ_ONLY, 0, size, arena);
		segment.load();
		return segment.asByteBuffer().order(ByteOrder.LITTLE_ENDIAN);
	}
}
