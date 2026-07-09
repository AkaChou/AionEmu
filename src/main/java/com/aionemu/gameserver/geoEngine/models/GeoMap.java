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
package com.aionemu.gameserver.geoEngine.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mr. Poke
 */
public class GeoMap extends Node {

	private static final Logger log = LoggerFactory.getLogger(GeoMap.class);
	public static final float COLLISION_CHECK_Z_OFFSET = 1;
	private static final float COLLISION_BOUND_OFFSET = 0.5f;
	private static final int NODE_CHUNK_SIZE = 256;

	private final int mapId;
	private Terrain terrain;
	private final Map<Integer, Node> chunkById = new HashMap<Integer, Node>();
	private Map<Integer, DespawnableNode> despawnables = new LinkedHashMap<Integer, DespawnableNode>();
	private Map<Integer, List<DespawnableNode>> despawnableTownObjects = new LinkedHashMap<Integer, List<DespawnableNode>>();
	private Map<Integer, DespawnableNode> despawnableHouseDoors = new LinkedHashMap<Integer, DespawnableNode>();
	private Map<Integer, DespawnableNode[]> despawnableDoors = new LinkedHashMap<Integer, DespawnableNode[]>();

	/**
	 * @param name
	 */
	public GeoMap(String name, int worldSize) {
		this.mapId = parseMapId(name);
		setCollisionFlags((short) (CollisionIntention.ALL.getId() << 8));
	}

	private int parseMapId(String name) {
		try {
			return Integer.parseInt(name);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public int getMapId() {
		return mapId;
	}

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

	public float getZW(float x, float y) {
		return getZ(x, y);
	}

	public float getZW(float x, float y, float z, int instanceId) {
		return getZ(x, y, z, instanceId);
	}

	public void setDoorState(int instanceId, int doorId, boolean open) {
		DespawnableNode[] states = despawnableDoors.get(doorId);
		if (states == null) {
			if (GeoDataConfig.GEO_ENABLE && !getIgnorableDoorIds().contains(doorId)) {
				log.warn("No geometry found for door " + doorId + " in world " + mapId);
			}
			return;
		}
		if (states[0] != null) {
			states[0].setActive(instanceId, !open);
		} else {
			log.warn("Door state 1 not available for door " + doorId + " in world " + mapId);
		}
		if (states[1] != null) {
			states[1].setActive(instanceId, open);
		} else {
			log.warn("Door state 2 not available for door " + doorId + " in world " + mapId);
		}
	}

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

	public void spawnPlaceableObject(int instanceId, int staticId) {
		DespawnableNode node = despawnables.get(staticId);
		if (node != null) {
			node.setActive(instanceId, true);
		}
	}

	public void despawnPlaceableObject(int instanceId, int staticId) {
		DespawnableNode node = despawnables.get(staticId);
		if (node != null) {
			node.setActive(instanceId, false);
		}
	}

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

	public void setHouseDoorState(int instanceId, int houseAddress, boolean open) {
		DespawnableNode node = despawnableHouseDoors.get(houseAddress);
		if (node != null) {
			node.setActive(instanceId, !open);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * aionjHungary.geoEngine.scene.Node#attachChild(aionjHungary.geoEngine.scene.
	 * Spatial)
	 */
	@Override
	public int attachChild(Spatial child) {
		if (child instanceof DespawnableNode) {
			registerDespawnable((DespawnableNode) child);
		}
		getOrCreateChunk(child).attachChild(child);
		return 0;
	}

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

	public int getEntityCount() {
		int count = 0;
		for (Node node : chunkById.values()) {
			count += node.getChildren().size();
		}
		return count;
	}

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
	 * @param terrainData The terrainData to set.
	 */
	public void setTerrainData(short[] terrainData) {
		int size = terrainData.length == 1 ? 1 : (int) Math.sqrt(terrainData.length);
		setTerrainData(terrainData, size, size);
	}

	public void setTerrainData(short[] terrainData, int width, int height) {
		if (terrain == null) {
			terrain = new Terrain();
		}
		terrain.setHeightmap(terrainData, width, height);
	}

	public void setTerrainMaterialData(byte[] terrainMaterialData, int width, int height) {
		if (terrain == null) {
			terrain = new Terrain();
		}
		terrain.setMaterials(terrainMaterialData, width, height);
	}

	public boolean hasTerrain() {
		return terrain != null && terrain.hasHeightmap();
	}

	public boolean hasTerrainMaterials() {
		return terrain != null && terrain.hasMaterials();
	}

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

	public float getZ(float x, float y) {
		float z = getZ(x, y, 4000, 0, 1);
		return Float.isNaN(z) ? 0 : z;
	}

	public float getZ(float x, float y, float z, int instanceId) {
		float geoZ = getZ(x, y, z + 2, z - 100, instanceId);
		return Float.isNaN(geoZ) ? z : geoZ;
	}

	public float getZ(float x, float y, float zMax, float zMin, int instanceId) {
		return getZ(x, y, zMax, zMin, instanceId, false);
	}

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

	public Vector3f getClosestCollision(float x, float y, float z, float targetX, float targetY, float targetZ,
			boolean changeDirection, boolean fly, int instanceId, byte intentions) {
		return getClosestCollision(x, y, z, targetX, targetY, targetZ, changeDirection, fly, instanceId, intentions, null);
	}

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

	private void applyCollisionCheckOffsets(Vector3f pos, Vector3f direction, int instanceId) {
		applyCollisionCheckOffsets(pos, direction, instanceId, false);
	}

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

	public CollisionResults getCollisions(float x, float y, float z, float targetX, float targetY, float targetZ,
			boolean changeDirection, boolean fly, int instanceId, byte intentions) {
		return getCollisions(x, y, z, targetX, targetY, targetZ, changeDirection, fly, instanceId, intentions, null);
	}

	public CollisionResults getCollisions(float x, float y, float z, float targetX, float targetY, float targetZ,
			boolean changeDirection, boolean fly, int instanceId, byte intentions, IgnoreProperties ignoreProperties) {
		if (!fly && changeDirection) {
			z = getZ(x, y, z + 2, instanceId);
		}
		return getCollisions(new Vector3f(x, y, z + COLLISION_CHECK_Z_OFFSET), targetX, targetY, targetZ + COLLISION_CHECK_Z_OFFSET,
				instanceId, intentions, ignoreProperties);
	}

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


	public boolean canSee(float x, float y, float z, float targetX, float targetY, float targetZ, float limit,
			int instanceId) {
		return canSee(x, y, z, targetX, targetY, targetZ, limit, instanceId, null);
	}

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

	public Stream<Geometry> getGeometries() {
		return getGeometries(getChildren());
	}

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see aionjHungary.geoEngine.scene.Spatial#updateModelBound()
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
