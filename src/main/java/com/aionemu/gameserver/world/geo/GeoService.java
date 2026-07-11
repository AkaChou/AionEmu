package com.aionemu.gameserver.world.geo;


import com.aionemu.boot.i18n.I18n;
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

/**
 * 地理服务：统一入口，提供高度、视线、通行与碰撞查询。
 * Geo service entry point providing height, LOS, passability and collision queries.
 */
@Slf4j
public class GeoService {
	/** 可选 Spring 单例提供者 / Optional Spring singleton provider */
	private static volatile ObjectProvider<GeoService> instanceProvider;
	/** NPC ids excluded from geo handling / NPC ids excluded from geo handling */
	private static final List<Integer> npcsExclude = new ArrayList<>();
	/** 当前使用的地理数据实现。 / Active geo-data implementation. */
	private GeoData geoData;

	/**
	 * 返回排除地理处理的 NPC 列表。
	 * Returns the NPC exclusion list for geo handling.
	 *
	 * exclusion list
	 */
	public static List<Integer> getNpcsExclude() {
		return npcsExclude;
	}

	/**
	 * 获取地理服务单例（优先 Spring 提供者）。
	 * Returns the geo-service singleton (preferring the Spring provider).
	 *
	 * service instance
	 */
	public static final GeoService getInstance() {
		ObjectProvider<GeoService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * 注入 Spring 单例提供者。
	 * Injects the Spring singleton provider.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<GeoService> instanceProvider) {
		GeoService.instanceProvider = instanceProvider;
	}

	/**
	 * 按配置初始化地理数据并加载全部地图。
	 * Initializes geo data according to configuration and loads all maps.
	 */
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
		log.info(I18n.get("log.f0cb8ab09daf"));

		log.info(I18n.get("log.dfefaa9da2af"));

		this.geoData.loadGeoMaps();
	}

	/**
	 * 设置指定实例中门的开合状态。
	 * Sets the open/closed state of a door in the given instance.
	 *
	 * 世界 ID / world id
	 * instance id
	 * door id
	 * whether open
	 */
	public void setDoorState(int worldId, int instanceId, int doorId, boolean isOpened) {
		if (GeoDataConfig.GEO_ENABLE) {
			this.geoData.getMap(worldId).setDoorState(instanceId, doorId, isOpened);
		}
	}

	/**
	 * 按名称获取世界中的几何节点。
	 * Returns a named geometry node from the world map.
	 *
	 * 世界 ID / world id
	 * @param name 节点名称 / node name
	 * spatial node
	 */
	public Spatial getGeometry(int worldId, String name) {
		return geoData.getMap(worldId).getChild(name);
	}

	/**
	 * 在实例中生成可放置物碰撞体。
	 * Spawns placeable-object collision for the instance.
	 *
	 * 世界 ID / world id
	 * instance id
	 * static object id
	 */
	public void spawnPlaceableObject(int worldId, int instanceId, int staticId) {
		if (GeoDataConfig.GEO_ENABLE) {
			this.geoData.getMap(worldId).spawnPlaceableObject(instanceId, staticId);
		}
	}

	/**
	 * 在实例中销毁可放置物碰撞体。
	 * Despawns placeable-object collision for the instance.
	 *
	 * 世界 ID / world id
	 * instance id
	 * static object id
	 */
	public void despawnPlaceableObject(int worldId, int instanceId, int staticId) {
		if (GeoDataConfig.GEO_ENABLE) {
			this.geoData.getMap(worldId).despawnPlaceableObject(instanceId, staticId);
		}
	}

	/**
	 * 更新城镇地理等级。
	 * Updates the geo level of a town.
	 *
	 * 世界 ID / world id
	 * town id
	 * @param level 目标等级 / target level
	 */
	public void updateTownToLevel(int worldId, int townId, int level) {
		if (GeoDataConfig.GEO_ENABLE) {
			this.geoData.getMap(worldId).updateTownToLevel(townId, level);
		}
	}

	/**
	 * 设置房屋门开合状态。
	 * Sets the open/closed state of a house door.
	 *
	 * 世界 ID / world id
	 * instance id
	 * house address
	 * @param open 是否打开 / whether open
	 */
	public void setHouseDoorState(int worldId, int instanceId, int houseAddress, boolean open) {
		if (GeoDataConfig.GEO_ENABLE) {
			this.geoData.getMap(worldId).setHouseDoorState(instanceId, houseAddress, open);
		}
	}

	/**
	 * 判断世界是否具备地形材质数据。
	 * Whether the world has terrain material data.
	 *
	 * 世界 ID / world id
	 *
	 * @param worldId 若 materials are available 则为 true / true if materials are available
	 */
	public boolean worldHasTerrainMaterials(int worldId) {
		return GeoDataConfig.GEO_MATERIALS_ENABLE && this.geoData.getMap(worldId).hasTerrainMaterials();
	}

	/**
	 * 获取指定坐标的地形材质 ID。
	 * Returns the terrain material id at the given coordinates.
	 *
	 * 世界 ID / world id
	 * @param x X 坐标 / x coordinate
	 * @param y Y 坐标 / y coordinate
	 * @param z Z 坐标 / z coordinate
	 * instance id
	 *
	 * @return 材质 ID，禁用时为 0 / material id, or 0 when disabled
	 */
	public int getTerrainMaterialAt(int worldId, float x, float y, float z, int instanceId) {
		return GeoDataConfig.GEO_MATERIALS_ENABLE ? this.geoData.getMap(worldId).getTerrainMaterialAt(x, y, z, instanceId) : 0;
	}

	/**
	 * 移动后根据地理高度校正 Z（禁用地理时使用 0.5 抬升）。
	 * Corrects Z after a move from geo height (uses a 0.5 lift when geo is off).
	 *
	 * 世界 ID / world id
	 * @param x X 坐标 / x coordinate
	 * @param y Y 坐标 / y coordinate
	 * @param z 参考高度 / reference height
	 * instance id
	 * @return 校正后高度 / corrected height
	 */
	public float getZAfterMoveBehind(int worldId, float x, float y, float z, int instanceId) {
		if (GeoDataConfig.GEO_ENABLE) {
			return this.getZ(worldId, x, y, z, 0.0f, instanceId);
		}
		return this.getZ(worldId, x, y, z, 0.5f, instanceId);
	}

	/**
	 * 按可见对象当前位置采样地表高度。
	 * Samples ground height at the visible object's current position.
	 *
	 * visible object
	 * ground height
	 */
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

	/**
	 * 采样指定坐标的地表高度；无效时按 defaultUp 回退。
	 * Samples ground height at the coordinates; falls back via defaultUp when invalid.
	 *
	 * 世界 ID / world id
	 * @param x X 坐标 / x coordinate
	 * @param y Y 坐标 / y coordinate
	 * @param z 参考高度 / reference height
	 * @param defaultUp 无效时抬升量（100 表示保持原高度） / lift when invalid (100 keeps original z)
	 * instance id
	 * ground height
	 */
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

	/**
	 * 采样地表高度（水下兼容路径）。
	 * Samples ground height (water-compatible path).
	 *
	 * 世界 ID / world id
	 * @param x X 坐标 / x coordinate
	 * @param y Y 坐标 / y coordinate
	 * @param z 参考高度 / reference height
	 * @param defaultUp 无效时抬升量 / lift when invalid
	 * instance id
	 * ground height
	 */
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

	/**
	 * 仅按 XY 采样地表高度。
	 * Samples ground height from X/Y only.
	 *
	 * 世界 ID / world id
	 * @param x X 坐标 / x coordinate
	 * @param y Y 坐标 / y coordinate
	 * ground height
	 */
	public float getZ(int worldId, float x, float y) {
		float newZ = this.geoData.getMap(worldId).getZ(x, y);
		if (GeoDataConfig.GEO_ENABLE) {
			newZ += 0.001f;
		}
		return newZ;
	}

	/**
	 * 仅按 XY 采样地表高度（水下兼容路径）。
	 * Samples ground height from X/Y only (water-compatible path).
	 *
	 * 世界 ID / world id
	 * @param x X 坐标 / x coordinate
	 * @param y Y 坐标 / y coordinate
	 * ground height
	 */
	public float getZW(int worldId, float x, float y) {
		float newZ = this.geoData.getMap(worldId).getZW(x, y);
		if (GeoDataConfig.GEO_ENABLE) {
			newZ += 0.001f;
		}
		return newZ;
	}

	/**
	 * 查询对象到目标点的碰撞结果。
	 * Queries collision results from the object to a target point.
	 *
	 * origin object
	 * @param x 目标 X / target x
	 * @param y 目标 Y / target y
	 * @param z 目标 Z / target z
	 * @param changeDirection 是否允许改向 / whether direction may change
	 * @param intentions 碰撞意图掩码 / collision intention mask
	 * collision results
	 */
	public CollisionResults getCollisions(VisibleObject object, float x, float y, float z, boolean changeDirection, byte intentions) {
		return getCollisions(object, x, y, z, changeDirection, intentions, null);
	}

	/**
	 * 查询对象到目标点的碰撞结果（带忽略属性）。
	 * Queries collision results from the object to a target point (with ignore properties).
	 *
	 * origin object
	 * @param x 目标 X / target x
	 * @param y 目标 Y / target y
	 * @param z 目标 Z / target z
	 * @param changeDirection 是否允许改向 / whether direction may change
	 * @param intentions 碰撞意图掩码 / collision intention mask
	 * ignore properties
	 * collision results
	 */
	public CollisionResults getCollisions(VisibleObject object, float x, float y, float z, boolean changeDirection, byte intentions,
			IgnoreProperties ignoreProperties) {
		return this.geoData.getMap(object.getWorldId()).getCollisions(object.getX(), object.getY(), object.getZ() - 0.6f, x, y, z,
				changeDirection, true, object.getInstanceId(), intentions, ignoreProperties);
	}

	/**
	 * 判断两对象之间是否视线畅通。
	 * Whether line of sight is clear between two objects.
	 *
	 * observer
	 * target
	 *
	 * @return 若 visible 则为 true / true if visible
	 */
	public boolean canSee(VisibleObject object, VisibleObject target) {

    if (object == null || target == null) {
        log.warn(I18n.get("log.f1725ab36627", object, target));
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

	/**
	 * 判断两对象之间是否可通行。
	 * Whether the path is passable between two objects.
	 *
	 * origin object
	 * target object
	 *
	 * @return 可通行则为 true / true if passable
	 */
    public boolean canPass(VisibleObject object, VisibleObject target) {
    if (object == null || target == null) {
        log.warn(I18n.get("log.1cc87552264e", object, target));
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

	/**
	 * 计算视线检测用的垂直偏移（考虑变身与碰撞高度）。
	 * Computes the vertical offset used for LOS checks (accounts for transform and bound height).
	 *
	 * checked object
	 * vertical offset
	 */
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

	/**
	 * 判断两点之间视线是否畅通。
	 * Whether line of sight is clear between two points.
	 *
	 * 世界 ID / world id
	 * @param x 起点 X / start x
	 * @param y 起点 Y / start y
	 * @param z 起点 Z / start z
	 * @param x1 终点 X / end x
	 * @param y1 终点 Y / end y
	 * @param z1 终点 Z / end z
	 * @param limit 检测距离上限 / ray-length limit
	 * instance id
	 *
	 * @return 若 visible 则为 true / true if visible
	 */
	public boolean canSee(int worldId, float x, float y, float z, float x1, float y1, float z1, float limit, int instanceId) {
		return canSee(worldId, x, y, z, x1, y1, z1, limit, instanceId, null);
	}

	/**
	 * 判断两点之间视线是否畅通（带忽略属性）。
	 * Whether line of sight is clear between two points (with ignore properties).
	 *
	 * 世界 ID / world id
	 * @param x 起点 X / start x
	 * @param y 起点 Y / start y
	 * @param z 起点 Z / start z
	 * @param x1 终点 X / end x
	 * @param y1 终点 Y / end y
	 * @param z1 终点 Z / end z
	 * @param limit 检测距离上限 / ray-length limit
	 * instance id
	 * ignore properties
	 *
	 * @return 若 visible 则为 true / true if visible
	 */
	public boolean canSee(int worldId, float x, float y, float z, float x1, float y1, float z1, float limit, int instanceId,
			IgnoreProperties ignoreProperties) {
		if (worldId == 301110000 || worldId == 301360000) {
			return true;
		}
		return this.geoData.getMap(worldId).canSee(x, y, z, x1, y1, z1, limit, instanceId, ignoreProperties);
	}

	/**
	 * 判断两点之间是否可通行。
	 * Whether the path is passable between two points.
	 *
	 * 世界 ID / world id
	 * @param x 起点 X / start x
	 * @param y 起点 Y / start y
	 * @param z 起点 Z / start z
	 * @param x1 终点 X / end x
	 * @param y1 终点 Y / end y
	 * @param z1 终点 Z / end z
	 * @param limit 检测距离上限 / ray-length limit
	 * instance id
	 *
	 * @return 可通行则为 true / true if passable
	 */
	public boolean canPass(int worldId, float x, float y, float z, float x1, float y1, float z1, float limit, int instanceId) {
		return this.geoData.getMap(worldId).canPass(x, y, z, x1, y1, z1, limit, instanceId);
	}

	/**
	 * 判断行走者两点之间是否可通行。
	 * Whether a walker path is passable between two points.
	 *
	 * 世界 ID / world id
	 * @param x 起点 X / start x
	 * @param y 起点 Y / start y
	 * @param z 起点 Z / start z
	 * @param x1 终点 X / end x
	 * @param y1 终点 Y / end y
	 * @param z1 终点 Z / end z
	 * @param limit 检测距离上限 / ray-length limit
	 * instance id
	 *
	 * @return 可通行则为 true / true if passable
	 */
	public boolean canPassWalker(int worldId, float x, float y, float z, float x1, float y1, float z1, float limit, int instanceId) {
		return this.geoData.getMap(worldId).canPassWalker(x, y, z, x1, y1, z1, limit, instanceId);
	}

	/**
	 * 地理数据是否开启。
	 * Whether geo data is enabled.
	 *
	 * @return 若 enabled 则为 true / true if enabled
	 */
	public boolean isGeoOn() {
		return GeoDataConfig.GEO_ENABLE;
	}

	/**
	 * 获取到目标点最近碰撞位置。
	 * Returns the closest collision point toward the target.
	 *
	 * origin creature
	 *
	 * @param x 目标 X / target x
	 * @param y 目标 Y / target y
	 * @param z 目标 Z / target z
	 * @param changeDirection 是否允许改向 / whether direction may change
	 * @param intentions 碰撞意图掩码 / collision intention mask
	 * @param intentions @return 最近碰撞点 / closest collision point
	 */
	public Vector3f getClosestCollision(Creature object, float x, float y, float z, boolean changeDirection, byte intentions) {
		return getClosestCollision(object, x, y, z, changeDirection, intentions, null);
	}

	/**
	 * 获取到目标点最近碰撞位置（带忽略属性）。
	 * Returns the closest collision point toward the target (with ignore properties).
	 *
	 * origin creature
	 * @param x 目标 X / target x
	 * @param y 目标 Y / target y
	 * @param z 目标 Z / target z
	 * @param changeDirection 是否允许改向 / whether direction may change
	 * @param intentions 碰撞意图掩码 / collision intention mask
	 * ignore properties
	 * @return 最近碰撞点 / closest collision point
	 */
	public Vector3f getClosestCollision(Creature object, float x, float y, float z, boolean changeDirection, byte intentions,
			IgnoreProperties ignoreProperties) {
		if ((intentions & CollisionIntention.PHYSICAL.getId()) != 0) {
			intentions |= CollisionIntention.PHYSICAL_SEE_THROUGH.getId();
		}
		return this.geoData.getMap(object.getWorldId()).getClosestCollision(object.getX(), object.getY(), object.getZ() - 0.6f, x, y, z,
				changeDirection, object.isInFlyingState(), object.getInstanceId(), intentions, ignoreProperties);
	}

	/**
	 * 沿给定方向寻找移动碰撞点。
	 * Finds the movement collision point along the given direction.
	 *
	 * moving creature
	 * direction angle in degrees
	 * max distance
	 * collision point
	 */
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

	/**
	 * 返回当前配置对应的地理模式。
	 * Returns the geo mode matching the current configuration.
	 *
	 * geo type
	 */
	public GeoType getConfiguredGeoType() {
		if (GeoDataConfig.GEO_ENABLE) {
			return GeoType.GEO_MESHES;
		}
		return GeoType.NO_GEO;
	}

	/**
	 * 单例持有者。
	 * Singleton holder.
	 */
	private static final class SingletonHolder {
		/** 默认实例。 / Default instance. */
		protected static final GeoService instance = new GeoService();
		private SingletonHolder() {
		}
	}
}
