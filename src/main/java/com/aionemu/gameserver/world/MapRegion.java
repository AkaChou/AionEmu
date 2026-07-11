package com.aionemu.gameserver.world;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.ai2.manager.WalkManager;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.configs.administration.DeveloperConfig;
import com.aionemu.gameserver.configs.main.SiegeConfig;
import com.aionemu.gameserver.configs.main.WorldConfig;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.base.BaseNpc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.siege.SiegeNpc;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.templates.zone.ZoneClassName;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 地图分区：可见对象存放、邻接激活与 Zone 校验。
 * Map region: holds visible objects, neighbour activation and zone validation.
 *
 * @author -Nemesiss-
 */
@Slf4j
public class MapRegion {

	/**
	 * 区域 ID（非世界地图 ID）。
	 * Region id (not the world map id).
	 */
	private final int regionId;
	/**
	 * 所属地图实例。
	 * Parent world-map instance.
	 */
	private final WorldMapInstance parent;
	/**
	 * 邻接区域（含自身）。
	 * Neighbour regions (includes self).
	 */
	private volatile MapRegion[] neighbours = new MapRegion[0];
	/**
	 * 本区域内的可见对象。
	 * Visible objects in this region.
	 */
	private final Map<Integer, VisibleObject> objects = Collections.synchronizedMap(new LinkedHashMap<Integer, VisibleObject>());

	/** 区域内玩家计数 / player count in this region */
	private final AtomicInteger playerCount = new AtomicInteger(0);

	/** 区域是否激活 / whether the region is active */
	private final AtomicBoolean regionActive = new AtomicBoolean(false);

	/** 区域统计 / zone count */
	private final int zoneCount;

	/**
	 * 按类别分组的 Zone 集合。
	 * Zones grouped by category.
	 */
	private Map<Integer, TreeSet<ZoneInstance>> zoneMap;

	/**
	 * 构造地图区域。
	 * Construct a map region.
	 *
	 * @param id 区域 ID / region id
	 * @param parent 父地图实例 / parent map instance
	 * @param zones 关联 Zone 数组 / related zone array
	 */
	MapRegion(int id, WorldMapInstance parent, ZoneInstance[] zones) {
		this.regionId = id;
		this.parent = parent;
		this.zoneCount = zones.length;
		createZoneMap(zones);
		addNeighbourRegion(this);
	}

	/**
	 * 返回世界地图 ID。
	 * Return the world map id.
	 *
	 * map id
	 */
	public Integer getMapId() {
		return getParent().getMapId();
	}

	/**
	 * 返回所属世界。
	 * Return the owning world.
	 *
	 * world
	 */
	public World getWorld() {
		return getParent().getWorld();
	}

	/**
	 * 返回区域 ID（非世界地图 ID）。
	 * Return the region id (not world map id).
	 *
	 * region id
	 */
	public int getRegionId() {
		return regionId;
	}

	/**
	 * 返回父地图实例。
	 * Return the parent map instance.
	 *
	 * parent instance
	 */
	public WorldMapInstance getParent() {
		return parent;
	}

	/**
	 * 返回本区域对象表。
	 * Return the object map of this region.
	 *
	 * object map
	 */
	public Map<Integer, VisibleObject> getObjects() {
		return objects;
	}

	/**
	 * 对象值快照。
	 * Snapshot of object values.
	 *
	 * object list
	 */
	public List<VisibleObject> getObjectsSnapshot() {
		synchronized (objects) {
			return new ArrayList<>(objects.values());
		}
	}

	/**
	 * 本区域内的静态门。
	 * Static doors in this region.
	 *
	 * @return entityId → door。 / entityId → door
	 */
	public Map<Integer, StaticDoor> getDoors() {
		Map<Integer, StaticDoor> doors = new HashMap<Integer, StaticDoor>();
		for (VisibleObject obj : getObjectsSnapshot()) {
			if (obj instanceof StaticDoor) {
				StaticDoor door = (StaticDoor) obj;
				doors.put(door.getSpawn().getEntityId(), door);
			}
		}
		return doors;
	}

	/**
	 * 返回邻接区域数组。
	 * Return neighbour region array.
	 *
	 * neighbours
	 */
	public MapRegion[] getNeighbours() {
		return neighbours;
	}

	/**
	 * 添加邻接区域。
	 * Add a neighbour region.
	 *
	 * neighbour region
	 */
	void addNeighbourRegion(MapRegion neighbour) {
		neighbours = Arrays.copyOf(neighbours, neighbours.length + 1);
		neighbours[neighbours.length - 1] = neighbour;
	}

	/**
	 * 将对象加入本区域；玩家增减触发激活检查。
	 * Add an object to this region; player changes trigger activeness checks.
	 *
	 * visible object
	 */
	void add(VisibleObject object) {
		if (objects.put(object.getObjectId(), object) == null) {
			if (object instanceof Player) {
				checkActiveness(playerCount.incrementAndGet() > 0);
			} else if (DeveloperConfig.SPAWN_CHECK) {
				Iterator<TreeSet<ZoneInstance>> zoneIter = zoneMap.values().iterator();
				while (zoneIter.hasNext()) {
					TreeSet<ZoneInstance> zones = zoneIter.next();
					for (ZoneInstance zone : zones) {
						if (!zone.isInsideCordinate(object.getX(), object.getY(), object.getZ())) {
							continue;
						}
						if (zone.getZoneTemplate().getZoneType() != ZoneClassName.DUMMY) {
							return;
						}
					}
				}
				log.warn(I18n.get("log.55fd6cf19a88", object, object.getX(), object.getY(), object.getZ()));
			}
		}
	}

	/**
	 * 从本区域移除对象。
	 * Remove an object from this region.
	 *
	 * visible object
	 */
	void remove(VisibleObject object) {
		if (objects.remove(object.getObjectId()) != null) {
			if (object instanceof Player) {
				checkActiveness(playerCount.decrementAndGet() > 0);
			}
		}
	}

	/**
	 * 根据是否有玩家决定激活/去激活调度。
	 * Schedule activation/deactivation based on player presence.
	 *
	 * @param active 是否应激活 / whether should be active
	 */
	public final void checkActiveness(boolean active) {
		if (active && regionActive.compareAndSet(false, true)) {
			startActivation();
		} else if (!active) {
			startDeactivation();
		}
	}

	/**
	 * 延迟激活自身与邻接区域的 AI。
	 * Delayed activation of self and neighbour AI.
	 */
	final void startActivation() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

			@Override
			public void run() {
				log.debug("Activating in map {} region {}", getMapId(), regionId);
				MapRegion.this.activateObjects();
				for (MapRegion neighbor : getNeighbours()) {
					neighbor.activate();
				}
			}
		}, 1000);
	}

	/**
	 * 延迟检查邻接是否可去激活。
	 * Delayed check whether neighbours can deactivate.
	 */
	final void startDeactivation() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

			@Override
			public void run() {
				log.debug("Deactivating in map {} region {}", getMapId(), regionId);
				for (MapRegion neighbor : getNeighbours()) {
					if (!neighbor.isNeighboursActive()) {
						neighbor.deactivate();
					}
				}
			}
		}, 60000);
	}

	/**
	 * 激活本区域对象 AI。
	 * Activate AI of objects in this region.
	 */
	public void activate() {
		if (regionActive.compareAndSet(false, true)) {
			activateObjects();
		}
	}

	/**
	 * 向带 AI2 的生物发送 ACTIVATE 事件。
	 * Send ACTIVATE event to creatures with AI2.
	 */
	private final void activateObjects() {
		for (VisibleObject visObject : getObjectsSnapshot()) {
			if (visObject instanceof Creature) {
				Creature creature = (Creature) visObject;
				creature.getAi2().onGeneralEvent(AIEventType.ACTIVATE);
			}
		}
	}

	/**
	 * 去激活本区域对象 AI。
	 * Deactivate AI of objects in this region.
	 */
	public void deactivate() {
		if (regionActive.compareAndSet(true, false)) {
			deactivateObjects();
		}
	}

	/**
	 * 向带 AI2 的生物发送 DEACTIVATE 并停止行走。
	 * Send DEACTIVATE to creatures with AI2 and stop walking.
	 */
	private void deactivateObjects() {
		for (VisibleObject visObject : getObjectsSnapshot()) {
			if (visObject instanceof Creature && !(SiegeConfig.BALAUR_AUTO_ASSAULT && visObject instanceof SiegeNpc || !(visObject instanceof BaseNpc))) { // Tweak
				Creature creature = (Creature) visObject;
				creature.getAi2().onGeneralEvent(AIEventType.DEACTIVATE);

				if (creature instanceof Npc) {
					Npc npc = (Npc) creature;
					if (npc.getAi2() instanceof NpcAI2) {
						WalkManager.stopWalking((NpcAI2) npc.getAi2());
					}
				}
			}
		}
	}

	/**
	 * 区域是否处于激活追踪状态。
	 * Whether the region is considered active (respects WORLD_ACTIVE_TRACE).
	 *
	 * @return 若 active 则为 true / true if active
	 */
	public boolean isMapRegionActive() {
		return !WorldConfig.WORLD_ACTIVE_TRACE || regionActive.get();
	}

	/**
	 * 邻接区域中是否有仍含玩家的激活区。
	 * Whether any neighbour is active and still has players.
	 *
	 * @return 有活跃邻接返回 true / true if a neighbour is active with players
	 */
	boolean isNeighboursActive() {
		for (int i = 0; i < neighbours.length; i++) {
			MapRegion r = neighbours[i];
			if (r != null && r.regionActive.get() && r.playerCount.get() > 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 重校验生物所在全部 Zone 的 enter/leave。
	 * Revalidate enter/leave for all zones covering the creature.
	 *
	 * creature
	 */
	public void revalidateZones(Creature creature) {
		for (Entry<Integer, TreeSet<ZoneInstance>> e : zoneMap.entrySet()) {
			boolean foundZone = false;
			int category = e.getKey();
			TreeSet<ZoneInstance> zones = e.getValue();
			for (ZoneInstance zone : zones) {
				if (!creature.isSpawned() || (category != -1 && foundZone)) {
					zone.onLeave(creature);
					continue;
				}
				boolean result = zone.revalidate(creature);
				if (!result) {
					zone.onLeave(creature);
					continue;
				}
				if (category != -1) {
					foundZone = true;
				}
				zone.onEnter(creature);
			}
		}
	}

	/**
	 * 返回包含该生物的 Zone 列表。
	 * Return zones that contain the creature.
	 *
	 * creature
	 * zone list
	 */
	public List<ZoneInstance> getZones(Creature creature) {
		List<ZoneInstance> z = new ArrayList<ZoneInstance>();
		for (Entry<Integer, TreeSet<ZoneInstance>> e : zoneMap.entrySet()) {
			TreeSet<ZoneInstance> zones = e.getValue();
			for (ZoneInstance zone : zones) {
				if (zone.isInsideCreature(creature)) {
					z.add(zone);
				}
			}
		}
		return z;
	}

	/**
	 * 将死亡事件分发给包含目标的 Zone。
	 * Dispatch death event to zones containing the target.
	 *
	 * attacker
	 * target
	 *
	 * @return 任一 Zone 处理返回 true / true if any zone handled it
	 */
	public boolean onDie(Creature attacker, Creature target) {
		for (Entry<Integer, TreeSet<ZoneInstance>> e : zoneMap.entrySet()) {
			TreeSet<ZoneInstance> zones = e.getValue();
			for (ZoneInstance zone : zones) {
				if (zone.isInsideCreature(target)) {
					if (zone.onDie(attacker, target)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * 判断坐标是否在指定 Zone 内。
	 * Whether coordinates lie inside the named zone.
	 *
	 * zone name
	 *
	 * @param x 坐标 X / X coordinate
	 * @param y 坐标 Y / Y coordinate
	 * @param z 坐标 Z / Z coordinate
	 * @param z 若 inside 则为 true / true if inside
	 */
	public boolean isInsideZone(ZoneName zoneName, float x, float y, float z) {
		for (Entry<Integer, TreeSet<ZoneInstance>> e : zoneMap.entrySet()) {
			TreeSet<ZoneInstance> zones = e.getValue();
			for (ZoneInstance zone : zones) {
				if (zone.getZoneTemplate().getName() != zoneName) {
					continue;
				}
				return zone.isInsideCordinate(x, y, z);
			}
		}
		return false;
	}

	/**
	 * 判断生物是否在指定 Zone 内。
	 * Whether the creature is inside the named zone.
	 *
	 * zone name
	 * creature
	 *
	 * @return 若 inside 则为 true / true if inside
	 */
	public boolean isInsideZone(ZoneName zoneName, Creature creature) {
		for (Entry<Integer, TreeSet<ZoneInstance>> e : zoneMap.entrySet()) {
			TreeSet<ZoneInstance> zones = e.getValue();
			for (ZoneInstance zone : zones) {
				if (zone.getZoneTemplate().getName() != zoneName) {
					continue;
				}
				return zone.isInsideCreature(creature);
			}
		}
		return false;
	}

	/**
	 * 物品使用 Zone 校验（按 xml 名前缀匹配，因实例名不唯一）。
	 * Item-use zone check (prefix-matches xml names because instance names are not unique).
	 *
	 * zone name
	 * creature
	 *
	 * @return 若 inside 则为 true / true if inside
	 */
	public boolean isInsideItemUseZone(ZoneName zoneName, Creature creature) {
		for (Entry<Integer, TreeSet<ZoneInstance>> e : zoneMap.entrySet()) {
			TreeSet<ZoneInstance> zones = e.getValue();
			for (ZoneInstance zone : zones) {
				if (!zone.getZoneTemplate().getXmlName().startsWith(zoneName.toString())) {
					continue;
				}
				if (!zone.isInsideCreature(creature)) {
					continue;
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * 按优先级/类型构建 Zone 分类表。
	 * Build zone category map by priority/type.
	 *
	 * zone array
	 */
	private void createZoneMap(ZoneInstance[] zones) {
		zoneMap = new LinkedHashMap<Integer, TreeSet<ZoneInstance>>();
		for (int i = 0; i < zones.length; i++) {
			ZoneInstance zone = zones[i];
			int category = -1;
			if (zone.getZoneTemplate().getPriority() != 0) {
				category = zone.getZoneTemplate().getZoneType().ordinal();
			}
			TreeSet<ZoneInstance> zoneCategory = zoneMap.get(category);
			if (zoneCategory == null) {
				zoneCategory = new TreeSet<ZoneInstance>();
				zoneMap.put(category, zoneCategory);
			}
			zoneCategory.add(zone);
		}
	}

	/**
	 * 返回 Zone 数量。
	 * Return zone count.
	 *
	 * zone count
	 */
	public int getZoneCount() {
		return zoneCount;
	}
}
