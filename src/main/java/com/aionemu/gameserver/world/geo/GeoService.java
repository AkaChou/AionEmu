/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 */
package com.aionemu.gameserver.world.geo;

import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.ai2.poll.AIQuestion;
import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.collision.IgnoreProperties;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.geoEngine.scene.Spatial;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.utils.MathUtil;
@Slf4j

public class GeoService {
	private static volatile ObjectProvider<GeoService> instanceProvider;
	private static final List<Integer> npcsExclude = new ArrayList<>();
	private GeoData geoData;

	public static List<Integer> getNpcsExclude() {
		return npcsExclude;
	}

	public static final GeoService getInstance() {
		ObjectProvider<GeoService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	public static void setInstanceProvider(ObjectProvider<GeoService> instanceProvider) {
		GeoService.instanceProvider = instanceProvider;
	}

	public void initializeGeo() {
		switch (this.getConfiguredGeoType()) {
		case GEO_MESHES: {
			this.geoData = new RealGeoData();
			break;
		}
		case NO_GEO: {
			this.geoData = new DummyGeoData();
		}
		}
		log.info("Configured Geo type: ");

		log.info("Geodata format: aion-server models.mesh + per-map geo/png data");

		this.geoData.loadGeoMaps();
	}

	public void setDoorState(int worldId, int instanceId, int doorId, boolean isOpened) {
		if (GeoDataConfig.GEO_ENABLE) {
			this.geoData.getMap(worldId).setDoorState(instanceId, doorId, isOpened);
		}
	}

	public Spatial getGeometry(int worldId, String name) {
		return geoData.getMap(worldId).getChild(name);
	}

	public void spawnPlaceableObject(int worldId, int instanceId, int staticId) {
		if (GeoDataConfig.GEO_ENABLE) {
			this.geoData.getMap(worldId).spawnPlaceableObject(instanceId, staticId);
		}
	}

	public void despawnPlaceableObject(int worldId, int instanceId, int staticId) {
		if (GeoDataConfig.GEO_ENABLE) {
			this.geoData.getMap(worldId).despawnPlaceableObject(instanceId, staticId);
		}
	}

	public void updateTownToLevel(int worldId, int townId, int level) {
		if (GeoDataConfig.GEO_ENABLE) {
			this.geoData.getMap(worldId).updateTownToLevel(townId, level);
		}
	}

	public void setHouseDoorState(int worldId, int instanceId, int houseAddress, boolean open) {
		if (GeoDataConfig.GEO_ENABLE) {
			this.geoData.getMap(worldId).setHouseDoorState(instanceId, houseAddress, open);
		}
	}

	public boolean worldHasTerrainMaterials(int worldId) {
		return GeoDataConfig.GEO_MATERIALS_ENABLE && this.geoData.getMap(worldId).hasTerrainMaterials();
	}

	public int getTerrainMaterialAt(int worldId, float x, float y, float z, int instanceId) {
		return GeoDataConfig.GEO_MATERIALS_ENABLE ? this.geoData.getMap(worldId).getTerrainMaterialAt(x, y, z, instanceId) : 0;
	}

	public float getZAfterMoveBehind(int worldId, float x, float y, float z, int instanceId) {
		if (GeoDataConfig.GEO_ENABLE) {
			return this.getZ(worldId, x, y, z, 0.0f, instanceId);
		}
		return this.getZ(worldId, x, y, z, 0.5f, instanceId);
	}

	public float getZ(VisibleObject object) {
		float z = object.getZ();
		float newZ = this.geoData.getMap(object.getWorldId()).getZ(object.getX(), object.getY(), z + 2, z - 100, object.getInstanceId());
		if (Float.isNaN(newZ)) {
			newZ = z;
		}
		if (GeoDataConfig.GEO_ENABLE) {
			newZ += 0.001f;
		}
		return newZ;
	}

	public float getZ(int worldId, float x, float y, float z, float defaultUp, int instanceId) {
		float newZ = this.geoData.getMap(worldId).getZ(x, y, z + 2, z - 100, instanceId);
		if (Float.isNaN(newZ)) {
			newZ = defaultUp == 100.0f ? z : z + defaultUp;
		}
		if (GeoDataConfig.GEO_ENABLE && defaultUp != 100.0f) {
			newZ += 0.001f;
		}
		return newZ;
	}

	public float getZW(int worldId, float x, float y, float z, float defaultUp, int instanceId) {
		float newZ = this.geoData.getMap(worldId).getZ(x, y, z + 2, z - 100, instanceId);
		if (Float.isNaN(newZ)) {
			newZ = defaultUp == 100.0f ? z : z + defaultUp;
		}
		if (GeoDataConfig.GEO_ENABLE && defaultUp != 100.0f) {
			newZ += 0.001f;
		}
		return newZ;
	}

	public float getZ(int worldId, float x, float y) {
		float newZ = this.geoData.getMap(worldId).getZ(x, y);
		if (GeoDataConfig.GEO_ENABLE) {
			newZ += 0.001f;
		}
		return newZ;
	}

	public float getZW(int worldId, float x, float y) {
		float newZ = this.geoData.getMap(worldId).getZW(x, y);
		if (GeoDataConfig.GEO_ENABLE) {
			newZ += 0.001f;
		}
		return newZ;
	}

	public CollisionResults getCollisions(VisibleObject object, float x, float y, float z, boolean changeDirection, byte intentions) {
		return getCollisions(object, x, y, z, changeDirection, intentions, null);
	}

	public CollisionResults getCollisions(VisibleObject object, float x, float y, float z, boolean changeDirection, byte intentions,
			IgnoreProperties ignoreProperties) {
		return this.geoData.getMap(object.getWorldId()).getCollisions(object.getX(), object.getY(), object.getZ() - 0.6f, x, y, z,
				changeDirection, true, object.getInstanceId(), intentions, ignoreProperties);
	}

	public boolean canSee(VisibleObject object, VisibleObject target) {

    if (object == null || target == null) {
        log.warn("GeoService.canSee(): object or target is null. object={}, target={}", object, target);
        return false;
    }

    if (!object.isSpawned() || !target.isSpawned()) {
        return false;
    }

    // 1. 提前返回优化 / Early return optimization
    if (!GeoDataConfig.CANSEE_ENABLE) {
        return true;
    }
    
    int worldId = object.getWorldId();
    if (worldId == 301110000 || worldId == 301360000) {
        return true;
    }
    
    float x = object.getX();
    float y = object.getY();
    float targetX = target.getX();
    float targetY = target.getY();
    if (object instanceof Npc && ((Npc) object).getAi2().ask(AIQuestion.CONSIDER_BOUNDS_IN_CAN_SEE_CHECK_WHEN_ATTACKING).isPositive()) {
        double rad = Math.toRadians(MathUtil.calculateAngleFrom(object, target));
        float bound = Math.max(object.getObjectTemplate().getBoundRadius().getFront(), object.getObjectTemplate().getBoundRadius().getSide());
        x += (float) (Math.cos(rad) * bound);
        y += (float) (Math.sin(rad) * bound);
    }
    if (target instanceof Npc && ((Npc) target).getAi2().ask(AIQuestion.CONSIDER_BOUNDS_IN_CAN_SEE_CHECK_WHEN_ATTACKED).isPositive()) {
        double rad = Math.toRadians(MathUtil.calculateAngleFrom(target, object));
        float bound = Math.max(target.getObjectTemplate().getBoundRadius().getFront(), target.getObjectTemplate().getBoundRadius().getSide());
        targetX += (float) (Math.cos(rad) * bound);
        targetY += (float) (Math.sin(rad) * bound);
    }

    float distance = (float) MathUtil.getDistance(x, y, object.getZ(), targetX, targetY, target.getZ());
    float limit = distance - target.getObjectTemplate().getBoundRadius().getCollision();
    
    if (limit <= 0.0f) {
        return true;
    }
    
	Race race = object instanceof Creature creature ? creature.getRace() : null;
	int staticId = target.getSpawn() == null ? 0 : target.getSpawn().getStaticId();
	IgnoreProperties ignoreProperties = race == null && staticId == 0 ? null : IgnoreProperties.of(race, staticId);
    return this.geoData.getMap(worldId).canSee(
        x, y, object.getZ() + getSeeCheckOffset(object),
        targetX, targetY, target.getZ() + getSeeCheckOffset(target),
        limit, object.getInstanceId(), ignoreProperties);
   }

    public boolean canPass(VisibleObject object, VisibleObject target) {
    if (object == null || target == null) {
        log.warn("GeoService.canPass(): object или target равен null. object={}, target={}",  object, target);
        return false;
    }

    if (!object.isSpawned() || !target.isSpawned()) {
        return false;
    }

    // 1. 减少重复计算 / Reduce redundant calculations
    float distance = (float) MathUtil.getDistance(object, target);
    float targetCollision = target.getObjectTemplate().getBoundRadius().getCollision();
    float limit = distance - targetCollision;

    if (limit <= 0.0f) {
        return true;
    }
    
    // 2. 提取重复计算 / Extract repeated calculations
    return this.geoData.getMap(object.getWorldId()).canPass(
        object.getX(), object.getY(), object.getZ() + getSeeCheckOffset(object),
        target.getX(), target.getY(), target.getZ() + getSeeCheckOffset(target),
        limit, object.getInstanceId());
    }

	private float getSeeCheckOffset(VisibleObject object) {
		if (object instanceof Player player && player.isTransformed() && DataManager.NPC_DATA != null) {
			NpcTemplate template = DataManager.NPC_DATA.getNpcTemplate(player.getTransformModel().getModelId());
			if (template != null) {
				return template.getBoundRadius().getUpper();
			}
		}
		float height = object.getObjectTemplate().getBoundRadius().getUpper();
		return height > 2.5f ? height / 2 : 1.25f;
	}

	public boolean canSee(int worldId, float x, float y, float z, float x1, float y1, float z1, float limit, int instanceId) {
		return canSee(worldId, x, y, z, x1, y1, z1, limit, instanceId, null);
	}

	public boolean canSee(int worldId, float x, float y, float z, float x1, float y1, float z1, float limit, int instanceId,
			IgnoreProperties ignoreProperties) {
		if (worldId == 301110000 || worldId == 301360000) {
			return true;
		}
		return this.geoData.getMap(worldId).canSee(x, y, z, x1, y1, z1, limit, instanceId, ignoreProperties);
	}

	public boolean canPass(int worldId, float x, float y, float z, float x1, float y1, float z1, float limit, int instanceId) {
		return this.geoData.getMap(worldId).canPass(x, y, z, x1, y1, z1, limit, instanceId);
	}

	public boolean canPassWalker(int worldId, float x, float y, float z, float x1, float y1, float z1, float limit, int instanceId) {
		return this.geoData.getMap(worldId).canPassWalker(x, y, z, x1, y1, z1, limit, instanceId);
	}

	public boolean isGeoOn() {
		return GeoDataConfig.GEO_ENABLE;
	}

	public Vector3f getClosestCollision(Creature object, float x, float y, float z, boolean changeDirection, byte intentions) {
		return getClosestCollision(object, x, y, z, changeDirection, intentions, null);
	}

	public Vector3f getClosestCollision(Creature object, float x, float y, float z, boolean changeDirection, byte intentions,
			IgnoreProperties ignoreProperties) {
		if ((intentions & CollisionIntention.PHYSICAL.getId()) != 0) {
			intentions |= CollisionIntention.PHYSICAL_SEE_THROUGH.getId();
		}
		return this.geoData.getMap(object.getWorldId()).getClosestCollision(object.getX(), object.getY(), object.getZ() - 0.6f, x, y, z,
				changeDirection, object.isInFlyingState(), object.getInstanceId(), intentions, ignoreProperties);
	}

	public Vector3f findMovementCollision(Creature creature, float directionAngle, float maxDistance) {
		double rad = Math.toRadians(directionAngle);
		float xOffset = (float) (Math.cos(rad) * maxDistance);
		float yOffset = (float) (Math.sin(rad) * maxDistance);
		if (creature.isInFlyingState()) {
			return this.geoData.getMap(creature.getWorldId()).getClosestCollision(creature.getX(), creature.getY(), creature.getZ(),
					creature.getX() + xOffset, creature.getY() + yOffset, creature.getZ(), false, true, creature.getInstanceId(),
					CollisionIntention.DEFAULT_COLLISIONS.getId(), IgnoreProperties.ANY_RACE);
		}
		Vector3f start = new Vector3f(creature.getX(), creature.getY(), creature.getZ());
		return this.geoData.getMap(creature.getWorldId()).findMovementCollision(start, start.getX() + xOffset, start.getY() + yOffset,
				creature.getInstanceId());
	}

	public GeoType getConfiguredGeoType() {
		if (GeoDataConfig.GEO_ENABLE) {
			return GeoType.GEO_MESHES;
		}
		return GeoType.NO_GEO;
	}

	private static final class SingletonHolder {
		protected static final GeoService instance = new GeoService();
		private SingletonHolder() {
		}
	}
}
