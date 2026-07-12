package com.aionemu.gameserver.geoEngine.models;


import com.aionemu.boot.i18n.I18n;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.collision.CollisionResult;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.collision.IgnoreProperties;
import com.aionemu.gameserver.geoEngine.math.Ray;
import com.aionemu.gameserver.geoEngine.math.Vector2f;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.geoEngine.scene.DespawnableNode;
import com.aionemu.gameserver.geoEngine.scene.DespawnableNode.DespawnableType;
import com.aionemu.gameserver.geoEngine.scene.Geometry;
import com.aionemu.gameserver.geoEngine.scene.Node;
import com.aionemu.gameserver.geoEngine.scene.Spatial;
import com.aionemu.gameserver.world.RegionUtil;
import com.aionemu.gameserver.world.WorldMapType;

/**
 * 世界地图几何根节点，管理分块、地形、可消隐物体与碰撞查询。
 * World-map geometry root managing chunks, terrain, despawnables and collision queries.
 *
 * @author Mr. Poke
 */
@Slf4j
public class GeoMap extends Node {

	/** 碰撞检测时应用的 Z 偏移 / Z offset applied during collision checks */
	public static final float COLLISION_CHECK_Z_OFFSET = 1;
	/** 碰撞回退边界偏移。 / Bound offset when backing off from a contact. */
	private static final float COLLISION_BOUND_OFFSET = 0.5f;
	/** 场景分块边长（世界单位）。 / Scene chunk edge length in world units. */
	private static final int NODE_CHUNK_SIZE = 256;

	/** 映射 ID / Map id */
	private final int mapId;
	/** 地形高度/材质数据。 / Terrain height and material data. */
	private Terrain terrain;
	/** 按区域 ID 索引的分块节点 / Chunk nodes keyed by region id */
	private final Map<Integer, Node> chunkById = new HashMap<Integer, Node>();
	/** 按静态 ID 的可放置对象 / Placeable objects by static id */
	private Map<Integer, DespawnableNode> despawnables = new LinkedHashMap<Integer, DespawnableNode>();
	/** 按城镇 ID 索引的城镇对象 / Town objects by town id*/
	private Map<Integer, List<DespawnableNode>> despawnableTownObjects = new LinkedHashMap<Integer, List<DespawnableNode>>();
	/** 房屋门（地址 → 节点）。 / House doors by address. */
	private Map<Integer, DespawnableNode> despawnableHouseDoors = new LinkedHashMap<Integer, DespawnableNode>();
	/** 按门 ID 的门状态对 [关闭, 打开] / Door state pair [closed, open] by door id */
	private Map<Integer, DespawnableNode[]> despawnableDoors = new LinkedHashMap<Integer, DespawnableNode[]>();

	/**
	 * 以地图名与世界尺寸构造 GeoMap。
	 * Constructs a GeoMap from map name and world size.
	 *
	 * @param name 地图名（通常为数字 ID） / map name (usually numeric id)
	 * @param worldSize 世界尺寸（保留参数） / world size (reserved)
	 */
	public GeoMap(String name, int worldSize) {
		this.mapId = parseMapId(name);
		setCollisionFlags((short) (CollisionIntention.ALL.getId() << 8));
	}

	/**
	 * 从名称解析地图 ID。
	 * Parses map id from the name string.
	 *
	 * map name
	 *
	 * @param name
	 * @return 地图 ID，解析失败为 0 / map id, or 0 on failure
	 */
	private int parseMapId(String name) {
		try {
			return Integer.parseInt(name);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * 返回地图 ID。
	 * Returns the map id.
	 *
	 * map id
	 */
	public int getMapId() {
		return mapId;
	}

	/**
	 * 判断步行者在两点间是否可通行（距离上限 50）。
	 * Whether a walker can pass between two points (distance cap 50).
	 *
	 * @param x 起点 X / origin X
	 * @param y 起点 Y / origin Y
	 * @param z 起点 Z / origin Z
	 * target X
	 * target Y
	 * target Z
	 * @param limit 射线长度上限 / ray limit
	 * instance id
	 *
	 * @return 若 path is clear 则为 true / true if path is clear
	 */
	public boolean canPassWalker(float x, float y, float z, float targetX, float targetY, float targetZ, float limit,
			int instanceId) {
		float x2 = x - targetX;
		float y2 = y - targetY;
		float distance = (float) Math.sqrt(x2 * x2 + y2 * y2);
		if (distance > 50.0f) {
			return false;
		}
		Vector3f pos = new Vector3f(x, y, z);
		Vector3f dir = new Vector3f(targetX, targetY, targetZ);
		dir.subtractLocal(pos).normalizeLocal();
		Ray r = new Ray(pos, dir);
		r.setLimit(limit);
		CollisionResults results = new CollisionResults(CollisionIntention.DEFAULT_COLLISIONS.getId(), true, instanceId);
		int collisions = this.collideWith(r, results);
		return results.size() == 0 && collisions == 0;
	}

	/**
	 * 判断两点间是否可通行（距离上限 65，不跳过第一命中）。
	 * Whether a path is clear between two points (distance cap 65, no first-hit skip).
	 *
	 * @param x 起点 X / origin X
	 * @param y 起点 Y / origin Y
	 * @param z 起点 Z / origin Z
	 * target X
	 * target Y
	 * target Z
	 * @param limit 射线长度上限 / ray limit
	 * instance id
	 *
	 * @return 若 path is clear 则为 true / true if path is clear
	 */
	public boolean canPass(float x, float y, float z, float targetX, float targetY, float targetZ, float limit,
			int instanceId) {
		float x2 = x - targetX;
		float y2 = y - targetY;
		float distance = (float) Math.sqrt(x2 * x2 + y2 * y2);
		if (distance > 65.0f) {
			return false;
		}
		Vector3f pos = new Vector3f(x, y, z);
		Vector3f dir = new Vector3f(targetX, targetY, targetZ);
		dir.subtractLocal(pos).normalizeLocal();
		Ray r = new Ray(pos, dir);
		r.setLimit(limit);
		CollisionResults results = new CollisionResults(CollisionIntention.DEFAULT_COLLISIONS.getId(), false, instanceId);
		int collisions = this.collideWith(r, results);
		return results.size() == 0 && collisions == 0;
	}

	/**
	 * 查询地面高度（无实例上下文）。
	 * Ground height without instance context.
	 *
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * Z height
	 */
	public float getZW(float x, float y) {
		return getZ(x, y);
	}

	/**
	 * 在给定 Z 附近查询地面高度。
	 * Ground height near the given Z for an instance.
	 *
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z 参考 Z / reference Z
	 * instance id
	 * Z height
	 */
	public float getZW(float x, float y, float z, int instanceId) {
		return getZ(x, y, z, instanceId);
	}

	/**
	 * 设置门开闭状态（关态/开态节点互斥激活）。
	 * Sets door open/closed state (closed/open nodes activated mutually exclusive).
	 *
	 * instance id
	 * door id
	 * @param open 是否打开 / whether open
	 */
	public void setDoorState(int instanceId, int doorId, boolean open) {
		DespawnableNode[] states = despawnableDoors.get(doorId);
		if (states == null) {
			if (GeoDataConfig.GEO_ENABLE && !getIgnorableDoorIds().contains(doorId)) {
				log.warn(I18n.get("log.d3d31340f124", doorId, mapId));
			}
			return;
		}
		if (states[0] != null) {
			states[0].setActive(instanceId, !open);
		} else {
			log.warn(I18n.get("log.bb5b8f4b9146", doorId, mapId));
		}
		if (states[1] != null) {
			states[1].setActive(instanceId, open);
		} else {
			log.warn(I18n.get("log.0b8fabd75906", doorId, mapId));
		}
	}

	/**
	 * 返回当前地图可忽略的门 ID 集合（缺 mesh 白名单）。
	 * Door ids that may be missing mesh data on this map.
	 *
	 * ignorable door ids
	 */
	private Set<Integer> getIgnorableDoorIds() {
		if (mapId == 300290000) {
			return Set.of(49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 73);
		}
		WorldMapType world = WorldMapType.getWorld(mapId);
		if (world == null) {
			return Set.of();
		}
		return switch (world) {
			case RENTUS_BASE, OCCUPIED_RENTUS_BASE -> Set.of(145);
			case ABYSSAL_SPLINTER, UNSTABLE_ABYSSAL_SPLINTER -> Set.of(15, 16, 18, 69);
			case ATURAM_SKY_FORTRESS -> Set.of(128, 138, 308, 307);
			case ESOTERRACE -> Set.of(78);
			case RAKSANG_RUINS -> Set.of(219);
			case KAMAR_BATTLEFIELD -> Set.of(5, 144);
			default -> Set.of();
		};
	}

	/**
	 * 激活可放置物体。
	 * Spawns (activates) a placeable object.
	 *
	 * instance id
	 * static object id
	 */
	public void spawnPlaceableObject(int instanceId, int staticId) {
		DespawnableNode node = despawnables.get(staticId);
		if (node != null) {
			node.setActive(instanceId, true);
		}
	}

	/**
	 * 取消可放置物体。
	 * Despawns (deactivates) a placeable object.
	 *
	 * instance id
	 * static object id
	 */
	public void despawnPlaceableObject(int instanceId, int staticId) {
		DespawnableNode node = despawnables.get(staticId);
		if (node != null) {
			node.setActive(instanceId, false);
		}
	}

	/**
	 * 按城镇等级更新城镇物体激活状态。
	 * Updates town-object activation based on town level.
	 *
	 * town id
	 * @param level 城镇等级 / town level
	 */
	public void updateTownToLevel(int townId, int level) {
		List<DespawnableNode> nodes = despawnableTownObjects.get(townId);
		if (nodes == null) {
			return;
		}
		int levelBitMask = 1 << (level - 1);
		for (DespawnableNode node : nodes) {
			node.setActive(1, (node.levelBitMask & levelBitMask) != 0);
		}
	}

	/**
	 * 设置房屋门开闭（打开时节点关闭）。
	 * Sets house door open state (node active when closed).
	 *
	 * instance id
	 * house address
	 * @param open 是否打开 / whether open
	 */
	public void setHouseDoorState(int instanceId, int houseAddress, boolean open) {
		DespawnableNode node = despawnableHouseDoors.get(houseAddress);
		if (node != null) {
			node.setActive(instanceId, !open);
		}
	}

	/**
	 * 附加子节点到分块；可消隐节点先注册。
	 * Attaches a child into its chunk; registers despawnable nodes first.
	 *
	 * @param child 子空间体 / child spatial
	 * always 0
	 */
	@Override
	public int attachChild(Spatial child) {
		if (child instanceof DespawnableNode) {
			registerDespawnable((DespawnableNode) child);
		}
		getOrCreateChunk(child).attachChild(child);
		return 0;
	}

	/**
	 * 按子节点中心获取或创建分块。
	 * Gets or creates the chunk for the child's center.
	 *
	 * @param child 子空间体 / child spatial
	 * chunk node
	 */
	private Node getOrCreateChunk(Spatial child) {
		int chunkId = RegionUtil.get2DRegionId(NODE_CHUNK_SIZE, child.getWorldBound().getCenter().x, child.getWorldBound().getCenter().y);
		Node node = chunkById.get(chunkId);
		if (node == null) {
			node = new Node("");
			node.setCollisionFlags((short) (CollisionIntention.ALL.getId() << 8));
			chunkById.put(chunkId, node);
			super.attachChild(node);
		}
		return node;
	}

	/**
	 * 统计所有分块中的实体数。
	 * Counts entities across all chunks.
	 *
	 * entity count
	 */
	public int getEntityCount() {
		int count = 0;
		for (Node node : chunkById.values()) {
			count += node.getChildren().size();
		}
		return count;
	}

	/**
	 * 按类型注册可消隐节点到对应索引表。
	 * Registers a despawnable node into the matching index by type.
	 *
	 * @param node 可消隐节点 / despawnable node
	 */
	private void registerDespawnable(DespawnableNode node) {
		switch (node.type) {
		case PLACEABLE:
			despawnables.put(node.id, node);
			break;
		case HOUSE_DOOR:
			despawnableHouseDoors.put(node.id, node);
			break;
		case TOWN_OBJECT:
			despawnableTownObjects.computeIfAbsent(node.id, key -> new ArrayList<DespawnableNode>()).add(node);
			break;
		case DOOR_STATE1:
		case DOOR_STATE2:
			DespawnableNode[] states = despawnableDoors.computeIfAbsent(node.id, key -> new DespawnableNode[2]);
			states[node.type == DespawnableType.DOOR_STATE1 ? 0 : 1] = node;
			break;
		default:
			break;
		}
	}

	/**
	 * 设置正方形高度图数据。
	 * Sets square heightmap data.
	 *
	 * height samples
	 */
	public void setTerrainData(short[] terrainData) {
		int size = terrainData.length == 1 ? 1 : (int) Math.sqrt(terrainData.length);
		setTerrainData(terrainData, size, size);
	}

	/**
	 * 设置矩形高度图数据。
	 * Sets rectangular heightmap data.
	 *
	 * height samples
	 * width
	 * height
	 */
	public void setTerrainData(short[] terrainData, int width, int height) {
		if (terrain == null) {
			terrain = new Terrain();
		}
		terrain.setHeightmap(terrainData, width, height);
	}

	/**
	 * 设置地形材质图。
	 * Sets terrain material map data.
	 *
	 * material bytes
	 * width
	 * height
	 */
	public void setTerrainMaterialData(byte[] terrainMaterialData, int width, int height) {
		if (terrain == null) {
			terrain = new Terrain();
		}
		terrain.setMaterials(terrainMaterialData, width, height);
	}

	/**
	 * 是否存在高度图地形。
	 * Whether terrain heightmap is present.
	 *
	 * @return 若 heightmap present 则为 true / true if heightmap present
	 */
	public boolean hasTerrain() {
		return terrain != null && terrain.hasHeightmap();
	}

	/**
	 * 是否存在地形材质图。
	 * Whether terrain materials are present.
	 *
	 * @return 若 materials present 则为 true / true if materials present
	 */
	public boolean hasTerrainMaterials() {
		return terrain != null && terrain.hasMaterials();
	}

	/**
	 * 在 (x,y,z) 采样地形材质，并校验最近碰撞是否为地形本身。
	 * Samples terrain material at (x,y,z) only if the closest hit is the terrain.
	 *
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * instance id
	 *
	 * @return 材质 ID，无效为 0 / material id, or 0
	 */
	public int getTerrainMaterialAt(float x, float y, float z, int instanceId) {
		int matId = terrain == null ? 0 : terrain.getTerrainMaterialAt(x, y);
		if (matId > 0) {
			CollisionResults results = new CollisionResults(CollisionIntention.PHYSICAL.getId(), false, instanceId);
			float zMax = z + 1;
			float zMin = z - 1;
			Vector3f origin = new Vector3f(x, y, zMax);
			Vector3f target = new Vector3f(x, y, zMin);
			target.subtractLocal(origin).normalizeLocal();
			Ray r = new Ray(origin, target);
			r.setLimit(zMax - zMin);
			terrain.collideAtOrigin(r, results);
			CollisionResult terrainCollision = results.getClosestCollision();
			if (terrainCollision != null && (collideWith(r, results) == 0 || results.getClosestCollision().equals(terrainCollision))) {
				return matId;
			}
		}
		return 0;
	}

	/**
	 * 查询地面高度（默认高空向下，无实例）。
	 * Ground height by casting down from a high Z (no instance).
	 *
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @return 高度，失败为 0 / Z, or 0 on miss
	 */
	public float getZ(float x, float y) {
		float z = getZ(x, y, 4000, 0, 1);
		return Float.isNaN(z) ? 0 : z;
	}

	/**
	 * 在参考 Z 附近查询地面高度。
	 * Ground height near a reference Z for an instance.
	 *
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z 参考 Z / reference Z
	 * instance id
	 * @return 高度，失败返回原 z / Z, or original z on miss
	 */
	public float getZ(float x, float y, float z, int instanceId) {
		float geoZ = getZ(x, y, z + 2, z - 100, instanceId);
		return Float.isNaN(geoZ) ? z : geoZ;
	}

	/**
	 * 在 [zMin, zMax] 区间向下投射查询地面高度。
	 * Casts down within [zMin, zMax] to find ground height.
	 *
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * upper Z
	 * lower Z
	 * instance id
	 * Z or NaN
	 */
	public float getZ(float x, float y, float zMax, float zMin, int instanceId) {
		return getZ(x, y, zMax, zMin, instanceId, false);
	}

	/**
	 * 在 [zMin, zMax] 区间向下投射查询地面高度，可选忽略斜面。
	 * Casts down within [zMin, zMax]; optionally invalidates sloping surfaces.
	 *
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * upper Z
	 * lower Z
	 * instance id
	 * @param ignoreSlopingSurface 是否忽略斜面 / ignore sloping surfaces
	 * Z or NaN
	 */
	public float getZ(float x, float y, float zMax, float zMin, int instanceId, boolean ignoreSlopingSurface) {
		CollisionResults results = new CollisionResults(CollisionIntention.PHYSICAL.getId(), false, instanceId);
		results.setInvalidateSlopingSurface(ignoreSlopingSurface);
		Vector3f origin = new Vector3f(x, y, zMax);
		Vector3f target = new Vector3f(x, y, zMin);
		target.subtractLocal(origin).normalizeLocal();
		Ray r = new Ray(origin, target);
		r.setLimit(zMax - zMin);
		collideWith(r, results);
		if (terrain != null) {
			terrain.collideAtOrigin(r, results);
		}
		CollisionResult closestCollision = results.getClosestCollision();
		return closestCollision == null ? Float.NaN : closestCollision.getContactPoint().z;
	}

	/**
	 * 查询从起点到目标的最近碰撞点（无忽略属性）。
	 * Closest collision point from origin toward target (no ignore properties).
	 *
	 * @param x 起点 X / origin X
	 * @param y 起点 Y / origin Y
	 * @param z 起点 Z / origin Z
	 * target X
	 * target Y
	 * target Z
	 * @param changeDirection 是否贴地校正方向 / snap direction to ground
	 * @param fly 是否飞行 / flying
	 * instance id
	 * @param intentions 碰撞意图位 / collision intention flags
	 * @return 最近碰撞或目标点 / contact or target
	 */
	public Vector3f getClosestCollision(float x, float y, float z, float targetX, float targetY, float targetZ,
			boolean changeDirection, boolean fly, int instanceId, byte intentions) {
		return getClosestCollision(x, y, z, targetX, targetY, targetZ, changeDirection, fly, instanceId, intentions, null);
	}

	/**
	 * 查询从起点到目标的最近碰撞点。
	 * Closest collision point from origin toward target.
	 *
	 * @param x 起点 X / origin X
	 * @param y 起点 Y / origin Y
	 * @param z 起点 Z / origin Z
	 * target X
	 * target Y
	 * target Z
	 * @param changeDirection 是否贴地校正方向 / snap direction to ground
	 * @param fly 是否飞行 / flying
	 * instance id
	 * @param intentions 碰撞意图位 / collision intention flags
	 * ignore properties
	 * @return 最近碰撞或目标点 / contact or target
	 */
	public Vector3f getClosestCollision(float x, float y, float z, float targetX, float targetY, float targetZ,
			boolean changeDirection, boolean fly, int instanceId, byte intentions, IgnoreProperties ignoreProperties) {
		Vector3f origin = new Vector3f(x, y, z + COLLISION_CHECK_Z_OFFSET);
		CollisionResult closestCollision = getCollisions(origin, targetX, targetY, targetZ + COLLISION_CHECK_Z_OFFSET, instanceId, intentions, ignoreProperties).getClosestCollision();
		if (closestCollision == null) {
			Vector3f end = new Vector3f(targetX, targetY, targetZ);
			if (!fly && changeDirection) {
				float geoZ = getZ(end.x, end.y, end.z + 1, end.z - 2, instanceId);
				if (!Float.isNaN(geoZ)) {
					end.z = geoZ;
				}
			}
			return end;
		} else if (closestCollision.getDistance() <= COLLISION_BOUND_OFFSET + 0.05f) {
			return new Vector3f(x, y, z);
		}
		Vector3f contactPoint = closestCollision.getContactPoint();
		applyCollisionCheckOffsets(contactPoint, origin, instanceId);
		return contactPoint;
	}

	/**
	 * 对碰撞点施加边界回退与贴地修正。
	 * Applies bound offset and ground snap to a contact point.
	 *
	 * contact point
	 * @param direction 来源方向点 / origin used as direction base
	 * instance id
	 */
	private void applyCollisionCheckOffsets(Vector3f pos, Vector3f direction, int instanceId) {
		applyCollisionCheckOffsets(pos, direction, instanceId, false);
	}

	/**
	 * 对碰撞点施加边界回退与贴地修正。
	 * Applies bound offset and ground snap to a contact point.
	 *
	 * contact point
	 * @param direction 来源方向点 / origin used as direction base
	 * instance id
	 * @param allowNaN 是否允许 NaN 地面高度 / allow NaN ground Z
	 */
	private void applyCollisionCheckOffsets(Vector3f pos, Vector3f direction, int instanceId, boolean allowNaN) {
		if (direction != null) {
			Vector3f dir = pos.subtract(direction).normalizeLocal();
			pos.subtractLocal(dir.multLocal(COLLISION_BOUND_OFFSET));
			float geoZ = getZ(pos.x, pos.y, pos.z, pos.z - COLLISION_CHECK_Z_OFFSET * 3, instanceId);
			if (allowNaN || !Float.isNaN(geoZ)) {
				pos.z = geoZ;
			} else {
				pos.z -= COLLISION_CHECK_Z_OFFSET;
			}
		} else {
			pos.z -= COLLISION_CHECK_Z_OFFSET;
		}
	}

	/**
	 * 沿水平方向逐步探测移动碰撞，贴地前进。
	 * Walks horizontally toward a target, snapping to ground each step.
	 *
	 * @param origin 起点（Z 会被修改） / origin (Z may be mutated)
	 * target X
	 * target Y
	 * instance id
	 * reachable point
	 */
	public Vector3f findMovementCollision(Vector3f origin, float targetX, float targetY, int instanceId) {
		origin.setZ(origin.getZ() + COLLISION_CHECK_Z_OFFSET);
		Vector2f targetXY = new Vector2f(targetX, targetY);
		Vector2f xyOffset = targetXY.subtract(origin.getX(), origin.getY()).normalizeLocal().multLocal(COLLISION_CHECK_Z_OFFSET);
		float nextX = origin.getX() + xyOffset.getX(), nextY = origin.getY() + xyOffset.getY();
		if (xyOffset.getX() >= 0 && nextX > targetX || xyOffset.getX() < 0 && nextX < targetX) {
			nextX = targetX;
		}
		if (xyOffset.getY() >= 0 && nextY > targetY || xyOffset.getY() < 0 && nextY < targetY) {
			nextY = targetY;
		}
		if (origin.getX() != nextX || origin.getY() != nextY) {
			CollisionResult closestCollision = getCollisions(origin, nextX, nextY, origin.getZ(), instanceId, CollisionIntention.DEFAULT_COLLISIONS.getId(), IgnoreProperties.ANY_RACE).getClosestCollision();
			if (closestCollision != null) {
				Vector3f targetPoint = closestCollision.getContactPoint();
				applyCollisionCheckOffsets(targetPoint, origin, instanceId, true);
				if (!Float.isNaN(targetPoint.getZ())) {
					return targetPoint;
				}
			} else {
				float geoZ = getZ(nextX, nextY, origin.getZ(), origin.getZ() - COLLISION_CHECK_Z_OFFSET * 2.5f, instanceId, true);
				if (!Float.isNaN(geoZ)) {
					return findMovementCollision(origin.set(nextX, nextY, geoZ), targetX, targetY, instanceId);
				}
			}
		}
		return origin.setZ(origin.getZ() - COLLISION_CHECK_Z_OFFSET);
	}

	/**
	 * 查询两点间碰撞结果（无忽略属性）。
	 * Collision results between two points (no ignore properties).
	 *
	 * @param x 起点 X / origin X
	 * @param y 起点 Y / origin Y
	 * @param z 起点 Z / origin Z
	 * target X
	 * target Y
	 * target Z
	 * @param changeDirection 是否贴地校正 / snap to ground
	 * @param fly 是否飞行 / flying
	 * instance id
	 * @param intentions 碰撞意图位 / intention flags
	 * collision results
	 */
	public CollisionResults getCollisions(float x, float y, float z, float targetX, float targetY, float targetZ,
			boolean changeDirection, boolean fly, int instanceId, byte intentions) {
		return getCollisions(x, y, z, targetX, targetY, targetZ, changeDirection, fly, instanceId, intentions, null);
	}

	/**
	 * 查询两点间碰撞结果。
	 * Collision results between two points.
	 *
	 * @param x 起点 X / origin X
	 * @param y 起点 Y / origin Y
	 * @param z 起点 Z / origin Z
	 * target X
	 * target Y
	 * target Z
	 * @param changeDirection 是否贴地校正 / snap to ground
	 * @param fly 是否飞行 / flying
	 * instance id
	 * @param intentions 碰撞意图位 / intention flags
	 * ignore properties
	 * collision results
	 */
	public CollisionResults getCollisions(float x, float y, float z, float targetX, float targetY, float targetZ,
			boolean changeDirection, boolean fly, int instanceId, byte intentions, IgnoreProperties ignoreProperties) {
		if (!fly && changeDirection) {
			z = getZ(x, y, z + 2, instanceId);
		}
		return getCollisions(new Vector3f(x, y, z + COLLISION_CHECK_Z_OFFSET), targetX, targetY, targetZ + COLLISION_CHECK_Z_OFFSET,
				instanceId, intentions, ignoreProperties);
	}

	/**
	 * 从原点向量向目标发射射线，收集地形与场景碰撞。
	 * Casts a ray from origin toward the target, collecting terrain and scene hits.
	 *
	 * origin
	 * target X
	 * target Y
	 * target Z
	 * instance id
	 * @param intentions 碰撞意图位 / intention flags
	 * ignore properties
	 * collision results
	 */
	public CollisionResults getCollisions(Vector3f origin, float targetX, float targetY, float targetZ, int instanceId, byte intentions,
			IgnoreProperties ignoreProperties) {
		CollisionResults results = new CollisionResults(intentions, false, instanceId, ignoreProperties);
		Vector3f target = new Vector3f(targetX, targetY, targetZ);
		float limit = origin.distance(target);
		target.subtractLocal(origin).normalizeLocal();
		Ray r = new Ray(origin, target);
		r.setLimit(limit);
		if (terrain != null) {
			terrain.collide(r, targetX, targetY, results);
		}
		collideWith(r, results);
		return results;
	}


	/**
	 * 判断两点间视线是否通畅（无忽略属性）。
	 * Line-of-sight check between two points (no ignore properties).
	 *
	 * @param x 起点 X / origin X
	 * @param y 起点 Y / origin Y
	 * @param z 起点 Z / origin Z
	 * target X
	 * target Y
	 * target Z
	 * @param limit 射线长度上限 / ray limit
	 * instance id
	 *
	 * @return 若 visible 则为 true / true if visible
	 */
	public boolean canSee(float x, float y, float z, float targetX, float targetY, float targetZ, float limit,
			int instanceId) {
		return canSee(x, y, z, targetX, targetY, targetZ, limit, instanceId, null);
	}

	/**
	 * 判断两点间视线是否通畅（距离上限 80）。
	 * Line-of-sight check between two points (distance cap 80).
	 *
	 * @param x 起点 X / origin X
	 * @param y 起点 Y / origin Y
	 * @param z 起点 Z / origin Z
	 * target X
	 * target Y
	 * target Z
	 * @param limit 射线长度上限 / ray limit
	 * instance id
	 * ignore properties
	 *
	 * @return 若 visible 则为 true / true if visible
	 */
	public boolean canSee(float x, float y, float z, float targetX, float targetY, float targetZ, float limit,
			int instanceId, IgnoreProperties ignoreProperties) {
		Vector3f pos = new Vector3f(x, y, z);
		Vector3f dir = new Vector3f(targetX, targetY, targetZ);
		float distance = pos.distance(dir);
		if (distance > 80f) {
			return false;
		}
		dir.subtractLocal(pos).normalizeLocal();
		Ray r = new Ray(pos, dir);
		r.setLimit(limit);
		if (terrain != null && terrain.collide(r, targetX, targetY, null)) {
			return false;
		}
		CollisionResults results = new CollisionResults(
				CollisionIntention.CANT_SEE_COLLISIONS.getId(), true, instanceId, ignoreProperties);
		int collisions = this.collideWith(r, results);
		return (results.size() == 0 && collisions == 0);
	}

	/**
	 * 流式遍历地图下全部 Geometry。
	 * Streams all geometries under this map.
	 *
	 * geometry stream
	 */
	public Stream<Geometry> getGeometries() {
		return getGeometries(getChildren());
	}

	/**
	 * 递归展开节点树中的 Geometry。
	 * Recursively flattens geometries from a spatial list.
	 *
	 * @param spatials 空间体列表 / spatial list
	 * geometry stream
	 */
	private static Stream<Geometry> getGeometries(List<Spatial> spatials) {
		return spatials.stream().flatMap(child -> {
			if (child instanceof Geometry) {
				return Stream.of((Geometry) child);
			}
			if (child instanceof Node) {
				return getGeometries(((Node) child).getChildren());
			}
			return Stream.empty();
		});
	}

	/**
	 * 更新模型包围体，并移除空分块子节点。
	 * Updates model bounds and prunes empty chunk children.
	 */
	@Override
	public void updateModelBound() {
		if (getChildren() != null) {
			Iterator<Spatial> i = getChildren().iterator();
			while (i.hasNext()) {
				Spatial s = i.next();
				if (s instanceof Node && ((Node) s).getChildren().isEmpty()) {
					i.remove();
				}
			}
		}
		super.updateModelBound();
	}
}
