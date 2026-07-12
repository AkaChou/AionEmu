package com.aionemu.gameserver.world;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.world.zone.ZoneAttributes;

/**
 * 游戏内一张地图及其全部实例的容器。
 * Container for one in-game map and all of its instances.
 *
 * @author -Nemesiss-
 */
public class WorldMap {

	/** 地图模板 / map template */
	private WorldMapTemplate worldMapTemplate;

	/** 下一个实例 ID 分配器 / next instance-id allocator */
	private AtomicInteger nextInstanceId = new AtomicInteger(0);
	/**
	 * 实例表（instanceId → 实例）。
	 * Instance table (instanceId → instance).
	 */
	private final Map<Integer, WorldMapInstance> instances = Collections.synchronizedMap(new LinkedHashMap<Integer, WorldMapInstance>());

	/** 所属世界 / owning world */
	private World world;
	/** 运行时世界选项位掩码 / runtime world-option bitmask */
	private int worldOptions;

	/**
	 * 根据模板创建地图并初始化默认/双生实例。
	 * Create a map from template and initialize default/twin instances.
	 *
	 * map template
	 * @param world 所属世界 / owning world
	 */
	public WorldMap(WorldMapTemplate worldMapTemplate, World world) {
		this.world = world;
		this.worldMapTemplate = worldMapTemplate;
		this.worldOptions = worldMapTemplate.getFlags();

		if (worldMapTemplate.getTwinCount() != 0) {
			for (int i = 1; i <= worldMapTemplate.getTwinCount(); i++) {
				int nextId = getNextInstanceId();
				addInstance(nextId, WorldMapInstanceFactory.createWorldMapInstance(this, nextId));
			}
		} else {
			int nextId = getNextInstanceId();
			addInstance(nextId, WorldMapInstanceFactory.createWorldMapInstance(this, nextId));
		}
	}

	/**
	 * 地图名称。
	 * Map name.
	 *
	 * name
	 */
	public String getName() {
		return worldMapTemplate.getName();
	}

	/**
	 * 水位高度。
	 * Water level height.
	 *
	 * water level
	 */
	public int getWaterLevel() {
		return worldMapTemplate.getWaterLevel();
	}

	/**
	 * 坠落死亡高度。
	 * Death-fall height.
	 *
	 * death level
	 */
	public int getDeathLevel() {
		return worldMapTemplate.getDeathLevel();
	}

	/**
	 * 世界大类。
	 * World type category.
	 *
	 * @return {@link WorldType}
	 */
	public WorldType getWorldType() {
		return worldMapTemplate.getWorldType();
	}

	/**
	 * 世界尺寸（边长）。
	 * World size (edge length).
	 *
	 * size
	 */
	public int getWorldSize() {
		return worldMapTemplate.getWorldSize();
	}

	/**
	 * 地图 ID。
	 * Map id.
	 *
	 * map id
	 */
	public Integer getMapId() {
		return worldMapTemplate.getMapId();
	}

	/**
	 * 是否允许飞行。
	 * Whether flying is allowed.
	 *
	 * flying allowed
	 */
	public boolean isPossibleFly() {
		return (worldOptions & ZoneAttributes.FLY.getId()) != 0;
	}

	/**
	 * 是否排除 Buff 效果。
	 * Whether buffs are excluded.
	 *
	 * except buff
	 */
	public boolean isExceptBuff() {
		return worldMapTemplate.isExceptBuff();
	}

	/**
	 * 是否允许滑翔。
	 * Whether gliding is allowed.
	 *
	 * gliding allowed
	 */
	public boolean canGlide() {
		return (worldOptions & ZoneAttributes.GLIDE.getId()) != 0;
	}

	/**
	 * 是否允许放置 Kisk。
	 * Whether placing a kisk is allowed.
	 *
	 * kisk allowed
	 */
	public boolean canPutKisk() {
		return (worldOptions & ZoneAttributes.BIND.getId()) != 0;
	}

	/**
	 * 是否允许召回。
	 * Whether recall is allowed.
	 *
	 * recall allowed
	 */
	public boolean canRecall() {
		return (worldOptions & ZoneAttributes.RECALL.getId()) != 0;
	}

	/**
	 * 是否允许骑乘。
	 * Whether riding is allowed.
	 *
	 * riding allowed
	 */
	public boolean canRide() {
		return (worldOptions & ZoneAttributes.RIDE.getId()) != 0;
	}

	/**
	 * 是否允许飞行骑乘。
	 * Whether fly-ride is allowed.
	 *
	 * @return 允许飞行骑乘 / fly-ride allowed
	 */
	public boolean canFlyRide() {
		return (worldOptions & ZoneAttributes.FLY_RIDE.getId()) != 0;
	}

	/**
	 * 是否允许 PvP。
	 * Whether PvP is allowed.
	 *
	 * PvP allowed
	 */
	public boolean isPvpAllowed() {
		return (worldOptions & ZoneAttributes.PVP_ENABLED.getId()) != 0;
	}

	/**
	 * 是否允许同种族决斗。
	 * Whether same-race duels are allowed.
	 *
	 * @return 允许同族决斗 / same-race duel allowed
	 */
	public boolean isSameRaceDuelsAllowed() {
		return (worldOptions & ZoneAttributes.DUEL_SAME_RACE_ENABLED.getId()) != 0;
	}

	/**
	 * 是否允许异种族决斗。
	 * Whether other-race duels are allowed.
	 *
	 * @return 允许异族决斗 / other-race duel allowed
	 */
	public boolean isOtherRaceDuelsAllowed() {
		return (worldOptions & ZoneAttributes.DUEL_OTHER_RACE_ENABLED.getId()) != 0;
	}

	/**
	 * 开启一项世界选项。
	 * Enable a world option flag.
	 *
	 * option
	 */
	public void setWorldOption(ZoneAttributes option) {
		worldOptions |= option.getId();
	}

	/**
	 * 关闭一项世界选项。
	 * Disable a world option flag.
	 *
	 * option
	 */
	public void removeWorldOption(ZoneAttributes option) {
		worldOptions &= ~option.getId();
	}

	/**
	 * 判断运行时选项是否相对模板发生了覆盖。
	 * Whether the runtime option differs from the template flag.
	 *
	 * option
	 *
	 * @param option
	 * @return 已覆盖返回 true / true if overridden
	 */
	public boolean hasOverridenOption(ZoneAttributes option) {
		if ((worldMapTemplate.getFlags() & option.getId()) == 0) {
			return (worldOptions & option.getId()) != 0;
		}
		return (worldOptions & option.getId()) == 0;
	}

	/**
	 * 配置的实例（双生）数量。
	 * Configured instance (twin) count.
	 *
	 * instance count
	 */
	public int getInstanceCount() {
		int twinCount = worldMapTemplate.getTwinCount();
		return twinCount > 0 ? twinCount : 1;
	}

	/**
	 * 返回主实例（当前固定取 instanceId=1）。
	 * Return the main instance (currently always instanceId=1).
	 *
	 * @return 主地图实例 / main world map instance
	 */
	public WorldMapInstance getMainWorldMapInstance() {
		return getWorldMapInstance(1);
	}

	/**
	 * 按实例 ID 返回实例；双生数量校验失败时抛异常。
	 * Return instance by id; throws if id exceeds twin count.
	 *
	 * instance id
	 * world map instance
	 */
	public WorldMapInstance getWorldMapInstanceById(int instanceId) {
		if (worldMapTemplate.getTwinCount() != 0) {
			if (instanceId > worldMapTemplate.getTwinCount()) {
				throw new IllegalArgumentException(
						"WorldMapInstance " + getMapId() + " has lower instances count than " + instanceId);
			}
		}
		return getWorldMapInstance(instanceId);
	}

	/**
	 * 按实例 ID 取实例；0 视为 1。
	 * Get instance by id; 0 is treated as 1.
	 *
	 * instance id
	 * world map instance
	 */
	private WorldMapInstance getWorldMapInstance(int instanceId) {
		// instanceId 为计数，部分代码仍用 0 表示默认副本 / instanceId is a count, some code still uses 0 for the default instance
		if (instanceId == 0) {
			instanceId = 1;
		}
		return instances.get(instanceId);
	}

	/**
	 * 按实例 ID 移除实例；0 视为 1。
	 * Remove instance by id; 0 is treated as 1.
	 *
	 * instance id
	 */
	public void removeWorldMapInstance(int instanceId) {
		// instanceId 为计数，部分代码仍用 0 表示默认副本 / instanceId is a count, some code still uses 0 for the default instance
		if (instanceId == 0) {
			instanceId = 1;
		}
		instances.remove(instanceId);
	}

	/**
	 * 将实例加入本地图；0 视为 1。
	 * Add an instance to this map; 0 is treated as 1.
	 *
	 * instance id
	 * map instance
	 */
	public void addInstance(int instanceId, WorldMapInstance instance) {
		// instanceId 为计数，部分代码仍用 0 表示默认副本 / instanceId is a count, some code still uses 0 for the default instance
		if (instanceId == 0) {
			instanceId = 1;
		}
		instances.put(instanceId, instance);
	}

	/**
	 * 返回所属世界。
	 * Return the owning world.
	 *
	 * world
	 */
	public World getWorld() {
		return world;
	}

	/**
	 * 返回地图模板。
	 * Return the map template.
	 *
	 * template
	 */
	public final WorldMapTemplate getTemplate() {
		return worldMapTemplate;
	}

	/**
	 * 分配并返回下一个实例 ID。
	 * Allocate and return the next instance id.
	 *
	 * new instance id
	 */
	public int getNextInstanceId() {
		return nextInstanceId.incrementAndGet();
	}

	/**
	 * 是否副本类型地图。
	 * Whether this is an instance-type map.
	 *
	 * @return 副本地图为 true / true if instance type
	 */
	public boolean isInstanceType() {
		return worldMapTemplate.isInstance();
	}

	/**
	 * 实例迭代器。
	 * Iterator over instances.
	 *
	 * iterator
	 */
	public Iterator<WorldMapInstance> iterator() {
		return instancesSnapshot().iterator();
	}

	/**
	 * 当前全部可用实例 ID。
	 * All currently available instance ids.
	 *
	 * instance id collection
	 */
	public Collection<Integer> getAvailableInstanceIds() {
		synchronized (instances) {
			return new ArrayList<Integer>(instances.keySet());
		}
	}

	/**
	 * 当前全部实例快照。
	 * Snapshot of all current instances.
	 *
	 * instance collection
	 */
	public Collection<WorldMapInstance> getInstances() {
		return instancesSnapshot();
	}

	/**
	 * 掉落区域类型。
	 * World drop type.
	 *
	 * @return {@link WorldDropType}
	 */
	public WorldDropType getWorldDropType() {
		return worldMapTemplate.getWorldDropType();
	}

	/**
	 * 实例值快照。
	 * Snapshot of instance values.
	 *
	 * instance list
	 */
	private List<WorldMapInstance> instancesSnapshot() {
		synchronized (instances) {
			return new ArrayList<WorldMapInstance>(instances.values());
		}
	}
}
