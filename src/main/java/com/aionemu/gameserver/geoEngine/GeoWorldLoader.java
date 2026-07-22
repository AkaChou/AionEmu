package com.aionemu.gameserver.geoEngine;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.zip.GZIPInputStream;
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
 * 地理数据加载器：网格、世界物体、地形高度与材质。
 * Geo-data loader for meshes, world objects, terrain height and materials.
 *
 * @author Mr. Poke
 */
@Slf4j
public class GeoWorldLoader {

	/** 地理数据目录。 / Geo data directory. */
	private static final String GEO_DIR = "geo/";
	static final Set<Integer> TERRAIN_DISABLED_MAPS = Set.of(
			110020000, 110070000, 120020000, 130090000, 140010000,
			210080000, 210110000, 220090000, 220120000,
			300010000, 300060000, 300070000, 300080000, 300090000, 300100000, 300110000, 300120000,
			300130000, 300140000, 300150000, 300160000, 300190000, 300210000, 300230000, 300240000,
			300241000, 300440000, 300460000, 300520000, 300630000, 300700000, 300800000,
			301270000, 301340000, 301390000, 301510000, 301520000, 301540000, 301550000, 301560000,
			301570000, 301580000, 301600000, 301610000, 301620000, 301630000, 301631000, 301650000,
			301690000, 302320000, 302370000, 302390000, 302400000, 302420000,
			310010000, 310020000, 310030000, 310040000, 310050000, 310060000, 310070000, 310080000,
			310090000, 310100000, 310110000, 310120000,
			320010000, 320020000, 320030000, 320040000, 320050000, 320060000, 320070000, 320080000,
			320090000, 320100000, 320120000, 320130000, 320140000,
			400010000, 400020000, 400040000, 400050000, 400060000);

	/**
	 * 从网格二进制文件加载命名 Spatial 模型表。
	 * Loads a name→Spatial model table from a mesh binary file.
	 *
	 * relative geo path
	 *
	 * @param fileName
	 * @return 小写名称到模型的映射 / lowercase name to model map
	 * @return
	 * @throws IOException 读文件失败 / on I/O failure
	 */
	public static Map<String, Spatial> loadMeshs(String fileName) throws IOException {
		Map<String, Spatial> geoms = new HashMap<String, Spatial>();
		File geoFile = Config.geoFile(fileName);
		try (Arena arena = Arena.ofConfined()) {
			ByteBuffer geo;
			if (geoFile.getName().endsWith(".gz")) {
				try (InputStream input = new GZIPInputStream(new BufferedInputStream(new FileInputStream(geoFile)))) {
					geo = ByteBuffer.wrap(input.readAllBytes()).order(ByteOrder.BIG_ENDIAN);
				}
			} else {
				try (RandomAccessFile raFile = new RandomAccessFile(geoFile, "r"); FileChannel roChannel = raFile.getChannel()) {
					geo = mapReadOnly(roChannel, arena).order(ByteOrder.BIG_ENDIAN);
				}
			}
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

	/**
	 * 加载指定世界的放置物体到 GeoMap。
	 * Loads placed world objects for a world into the GeoMap.
	 *
	 * 世界 ID / world id
	 *
	 * @param models 已加载模型表 / loaded model table
	 * @param map 目标地图 / target geo map
	 * @param missingMeshes 缺失 mesh 名称收集器 / collector for missing mesh names
	 * @param missingMeshes
	 *
	 * @throws IOException 读文件失败 / on I/O failure
	 */
	public static void loadWorldObjects(int worldId, Map<String, Spatial> models, GeoMap map, Set<String> missingMeshes) throws IOException {
		File geoFile = Config.geoFile(GEO_DIR + worldId + ".geo.gz");
		if (!geoFile.isFile()) {
			geoFile = Config.geoFile(GEO_DIR + worldId + ".geo");
		}
		if (!geoFile.isFile()) {
			return;
		}
		try (InputStream fileInput = new BufferedInputStream(new FileInputStream(geoFile));
			 InputStream input = geoFile.getName().endsWith(".gz") ? new GZIPInputStream(fileInput) : fileInput) {
			ByteBuffer geo = ByteBuffer.wrap(input.readAllBytes()).order(ByteOrder.BIG_ENDIAN);

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
						log.warn(I18n.get("log.5c6e2bc4186d", name, worldId));
					}
				}
			}
		}
		map.updateModelBound();
	}

	/**
	 * 从 geo 目录 PNG 加载各地图高度图与材质图。
	 * Loads height and material PNGs from the geo directory into maps.
	 *
	 * @param maps 地图集合 / geo maps
	 * on I/O failure
	 */
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
				if (TERRAIN_DISABLED_MAPS.contains(mapId)) {
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

	/**
	 * 文件名是否为单地图直接地形（无逗号别名）。
	 * Whether the file name is a direct single-map terrain (no comma aliases).
	 *
	 * file name
	 *
	 * @param fileName
	 * @return 是否直接地形文件 / true if direct terrain
	 */
	private static boolean isDirectTerrainFile(String fileName) {
		String suffix = fileName.endsWith("_materials.png") ? "_materials.png" : ".png";
		String stem = fileName.substring(0, fileName.length() - suffix.length());
		return stem.indexOf(',') == -1;
	}

	/**
	 * 将普通节点包装为可消隐节点。
	 * Wraps a node as a DespawnableNode with type/id/level metadata.
	 *
	 * source node
	 * @param type 消隐类型 ID / despawnable type id
	 * @param id 实体 ID / entity id
	 * @param level 城镇等级 / town level
	 * @return 可消隐节点 / despawnable node
	 * on clone failure
	 */
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

	/**
	 * 加载同城镇更高等级实体变体。
	 * Loads higher-level town entity variants sharing the same placement.
	 *
	 * @param map 目标地图 / target map
	 * model table
	 * @param townEntity 当前城镇实体 / current town entity
	 * model name
	 * rotation matrix
	 * location
	 * scale
	 * @param level 当前等级 / current level
	 * 世界 ID / world id
	 * on clone failure
	 */
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

	/**
	 * 从光栅读取高度 short 数组（行列转置存储）。
	 * Reads height shorts from a raster (transposed storage order).
	 *
	 * image raster
	 * width
	 * height
	 * height data
	 */
	private static short[] readHeightData(Raster raster, int width, int height) {
		short[] terrainData = new short[width * height];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				terrainData[y + x * height] = (short) raster.getSample(y, x, 0);
			}
		}
		return terrainData;
	}

	/**
	 * 从光栅读取材质 byte 数组（行列转置存储）。
	 * Reads material bytes from a raster (transposed storage order).
	 *
	 * image raster
	 * width
	 * height
	 * material data
	 */
	private static byte[] readMaterialData(Raster raster, int width, int height) {
		byte[] materialData = new byte[width * height];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				materialData[y + x * height] = (byte) raster.getSample(y, x, 0);
			}
		}
		return materialData;
	}

	/**
	 * 附加到地图并为材质子节点创建区域。
	 * Attaches a spatial to the map and creates material zones for children.
	 *
	 * @param map 目标地图 / target map
	 * source spatial
	 * rotation
	 * location
	 * scale
	 * 世界 ID / world id
	 * @return 克隆后的节点 / attached clone
	 * on clone failure
	 */
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

	/**
	 * 克隆节点并设置变换后附加到地图。
	 * Clones the node, applies transform and attaches it to the map.
	 *
	 * @param map 目标地图 / target map
	 * source spatial
	 * rotation
	 * location
	 * scale
	 * clone
	 * on clone failure
	 */
	private static Spatial attachChild(GeoMap map, Spatial node, Matrix3f matrix, Vector3f location, Vector3f scale)
			throws CloneNotSupportedException {
		Spatial nodeClone = node.clone();
		nodeClone.setTransform(matrix, location, scale);
		nodeClone.updateModelBound();
		map.attachChild(nodeClone);
		return nodeClone;
	}

	/**
	 * 为带材质意图的节点创建材质区域模板。
	 * Creates a material zone template for nodes with material collision intention.
	 *
	 * spatial
	 * 世界 ID / world id
	 * @param childNumber 子序号（0 表示无后缀） / child number (0 = no suffix)
	 */
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
				// 用于覆盖 / for override
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
	 * 空间哈希（论文 VMV03，表大小 700001）。
	 * Spatial hash from the VMV03 paper; table size 700001.
	 * <p>
	 * 参考 http://www.beosil.com/download/CollisionDetectionHashing_VMV03.pdf
	 * See http://www.beosil.com/download/CollisionDetectionHashing_VMV03.pdf
	 *
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * hash
	 */
	private static int getVectorHash(float x, float y, float z) {
		long xIntBits = Float.floatToIntBits(x);
		long yIntBits = Float.floatToIntBits(y);
		long zIntBits = Float.floatToIntBits(z);
		return (int) ((xIntBits * 73856093 ^ yIntBits * 19349669 ^ zIntBits * 83492791) % 700001);
	}

	/**
	 * 将文件通道只读映射为 ByteBuffer（小端视图，调用方可改序）。
	 * Memory-maps a file channel read-only as a ByteBuffer (little-endian view; caller may reorder).
	 *
	 * file channel
	 * foreign Arena
	 * mapped buffer
	 * on map failure or oversized file。
	 */
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
