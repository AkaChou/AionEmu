package com.aionemu.gameserver.world;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.aionemu.gameserver.configs.main.WorldConfig;
import com.aionemu.gameserver.instance.handlers.InstanceHandler;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.Trap;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.DynamicInstance;
import com.aionemu.gameserver.model.instance.InstanceRuntimeState;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.model.team2.league.League;
import com.aionemu.gameserver.model.templates.quest.QuestNpc;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.model.templates.zone.ZoneClassName;
import com.aionemu.gameserver.model.templates.zone.ZoneType;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.world.exceptions.DuplicateAionObjectException;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.world.zone.RegionZone;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 世界地图实例：区域划分与对象管理。
 * World map instance: region partitioning and object management.
 *
 * @author -Nemesiss-
 */
@Slf4j
public abstract class WorldMapInstance {

	/**
	 * 区域边长。
	 * Region edge length.
	 */
	public static final int regionSize = WorldConfig.WORLD_REGION_SIZE;
	/**
	 * 父级世界地图。
	 * Parent world map.
	 */
	private final WorldMap parent;
	/**
	 * 活跃区域表。
	 * Active region map.
	 */
	protected final IntObjectHashMap<MapRegion> regions = new IntObjectHashMap<MapRegion>();

	/**
	 * 本实例内已生成的全部可见对象。
	 * All visible objects spawned in this instance.
	 */
	private final Map<Integer, VisibleObject> worldMapObjects = Collections.synchronizedMap(new LinkedHashMap<Integer, VisibleObject>());

	/**
	 * 本实例内玩家。
	 * Players spawned in this instance.
	 */
	private final Map<Integer, Player> worldMapPlayers = Collections.synchronizedMap(new LinkedHashMap<Integer, Player>());

	/** 已注册对象 ID 集合 / registered object ids */
	private final Set<Integer> registeredObjects = ConcurrentHashMap.newKeySet();

	/** 注册的队伍 / registered player group */
	private PlayerGroup registeredGroup = null;

	/**
	 * 实例 ID（频道）。
	 * Instance id (channel).
	 */
	private int instanceId;

	private DynamicInstance dynamicInstance;
	private InstanceRuntimeState runtimeState = new InstanceRuntimeState();
	private Map<Class<?>, Object> transientStates = new ConcurrentHashMap<>();

	/** 本实例相关任务 ID / quest ids related to this instance */
	private final List<Integer> questIds = new ArrayList<Integer>();

	/** 副本处理器 / instance handler */
	private InstanceHandler instanceHandler;

	/** 区域名到区域实例 / zone name → zone instance */
	private Map<ZoneName, ZoneInstance> zones = new HashMap<ZoneName, ZoneInstance>();

	/** 单人所有者对象 ID / solo owner objectId */
	private Integer soloPlayer;

	/** 注册的联盟 / registered alliance */
	private PlayerAlliance registredAlliance;
	/** 注册的军团联盟 / registered league */
	private League registredLeague;

	/**
	 * 构造地图实例并初始化区域。
	 * Construct a map instance and initialize regions.
	 *
	 * @param parent 父级世界地图 / parent world map
	 * instance id
	 */
	public WorldMapInstance(WorldMap parent, int instanceId) {
		this.parent = parent;
		this.instanceId = instanceId;
		this.zones = GameWorldBootstrapServices.zoneService().getZoneInstancesByWorldId(parent.getMapId());
		initMapRegions();
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
	 * 返回父级世界地图。
	 * Return the parent world map.
	 *
	 * parent map
	 */
	public WorldMap getParent() {
		return parent;
	}

	/**
	 * 返回地图模板。
	 * Return the map template.
	 *
	 * template
	 */
	public WorldMapTemplate getTemplate() {
		return parent.getTemplate();
	}

	/**
	 * 返回包含对象坐标的区域；不存在时由子类创建。
	 * Return the region covering the object; subclasses may create missing regions.
	 *
	 * visible object
	 * map region
	 */
	MapRegion getRegion(VisibleObject object) {
		return getRegion(object.getX(), object.getY(), object.getZ());
	}

	/**
	 * 返回包含给定坐标的区域。
	 * Return the region covering the given coordinates.
	 *
	 * @param x 坐标 X / X coordinate
	 * @param y 坐标 Y / Y coordinate
	 * @param z 坐标 Z / Z coordinate
	 * map region
	 */
	public abstract MapRegion getRegion(float x, float y, float z);

	/**
	 * 创建区域并建立邻接。
	 * Create a region and wire neighbours.
	 *
	 * region id
	 * newly created region
	 */
	protected abstract MapRegion createMapRegion(int regionId);

	/**
	 * 初始化全部地图区域。
	 * Initialize all map regions.
	 */
	protected abstract void initMapRegions();

	/**
	 * 是否个人实例。
	 * Whether this is a personal instance.
	 *
	 * @return 个人实例为 true / true if personal
	 */
	public abstract boolean isPersonal();

	/**
	 * 个人实例所有者 ID。
	 * Personal-instance owner id.
	 *
	 * owner id
	 */
	public abstract int getOwnerId();

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
	 * 将对象加入本实例（含任务 ID 与玩家表）。
	 * Add an object into this instance (including quest ids and player table).
	 *
	 * visible object
	 */
	public void addObject(VisibleObject object) {
		boolean objectStored = false;
		boolean playerStored = false;
		List<Integer> addedQuestIds = Collections.emptyList();
		try {
			addVisibleObject(object);
			objectStored = true;
			addedQuestIds = addQuestIds(object);
			if (object instanceof Player) {
				if (this.getParent().isPossibleFly()) {
					((Player) object).setInsideZoneType(ZoneType.FLY);
				}
				worldMapPlayers.put(object.getObjectId(), (Player) object);
				playerStored = true;
			}
		} catch (RuntimeException | Error e) {
			if (playerStored) {
				worldMapPlayers.remove(object.getObjectId());
			}
			removeQuestIds(addedQuestIds);
			if (objectStored) {
				worldMapObjects.remove(object.getObjectId());
			}
			throw e;
		}
		if (playerStored) {
			InstanceService.onPlayerAdded(this);
		}
	}

	/**
	 * 加入可见对象表；重复 objectId 抛异常。
	 * Add to visible-object table; throws on duplicate objectId.
	 *
	 * visible object
	 */
	private void addVisibleObject(VisibleObject object) {
		synchronized (worldMapObjects) {
			if (worldMapObjects.containsKey(object.getObjectId())) {
				throw new DuplicateAionObjectException("Object with templateId "
						+ String.valueOf(object.getObjectTemplate().getTemplateId()) + " already spawned in the instance "
						+ String.valueOf(this.getMapId()) + " " + String.valueOf(this.getInstanceId()));
			}
			worldMapObjects.put(object.getObjectId(), object);
		}
	}

	/**
	 * 从 NPC 任务数据追加任务 ID。
	 * Append quest ids from NPC quest data.
	 *
	 * visible object
	 *
	 * @param object
	 * @return 本次新增的任务 ID / newly added quest ids
	 */
	private List<Integer> addQuestIds(VisibleObject object) {
		if (!(object instanceof Npc)) {
			return Collections.emptyList();
		}
		QuestNpc data = GameEngineServices.questEngine().getQuestNpc(((Npc) object).getNpcId());
		if (data == null) {
			return Collections.emptyList();
		}
		List<Integer> addedQuestIds = new ArrayList<Integer>();
		synchronized (questIds) {
			for (int id : data.getOnQuestStart()) {
				if (!questIds.contains(id)) {
					questIds.add(id);
					addedQuestIds.add(id);
				}
			}
		}
		return addedQuestIds;
	}

	/**
	 * 回滚刚加入的任务 ID。
	 * Roll back newly added quest ids.
	 *
	 * ids to remove
	 */
	private void removeQuestIds(List<Integer> addedQuestIds) {
		if (addedQuestIds.isEmpty()) {
			return;
		}
		synchronized (questIds) {
			questIds.removeAll(addedQuestIds);
		}
	}

	/**
	 * 从本实例移除对象。
	 * Remove an object from this instance.
	 *
	 * aion object
	 */
	public void removeObject(AionObject object) {
		worldMapObjects.remove(object.getObjectId());
		if (object instanceof Player) {
			if (this.getParent().isPossibleFly()) {
				((Player) object).unsetInsideZoneType(ZoneType.FLY);
			}
			worldMapPlayers.remove(object.getObjectId());
			InstanceService.onPlayerRemoved(this);
		}
	}

	/**
	 * 按 NPC 模板 ID 查找首个 NPC。
	 * Find the first NPC by template id.
	 *
	 * NPC 模板 ID / NPC template id
	 * NPC or null
	 */
	public Npc getNpc(int npcId) {
		for (Iterator<VisibleObject> iter = objectIterator(); iter.hasNext();) {
			VisibleObject obj = iter.next();
			if (obj instanceof Npc) {
				Npc npc = (Npc) obj;
				if (npc.getNpcId() == npcId) {
					return npc;
				}
			}
		}
		return null;
	}

	/**
	 * 本实例内全部玩家列表。
	 * List of all players inside this instance.
	 *
	 * player list
	 */
	public List<Player> getPlayersInside() {
		List<Player> playersInside = new ArrayList<Player>();
		Iterator<Player> players = playerIterator();
		while (players.hasNext()) {
			playersInside.add(players.next());
		}
		return playersInside;
	}

	/**
	 * 按 NPC 模板 ID 查找全部 NPC。
	 * Find all NPCs with the given template id.
	 *
	 * NPC 模板 ID / NPC template id
	 * NPC list
	 */
	public List<Npc> getNpcs(int npcId) {
		List<Npc> npcs = new ArrayList<Npc>();
		for (Iterator<VisibleObject> iter = objectIterator(); iter.hasNext();) {
			VisibleObject obj = iter.next();
			if (obj instanceof Npc) {
				Npc npc = (Npc) obj;
				if (npc.getNpcId() == npcId) {
					npcs.add(npc);
				}
			}
		}
		return npcs;
	}

	/**
	 * 本实例内全部 NPC。
	 * All NPCs in this instance.
	 *
	 * NPC list
	 */
	public List<Npc> getNpcs() {
		List<Npc> npcs = new ArrayList<Npc>();
		for (Iterator<VisibleObject> iter = objectIterator(); iter.hasNext();) {
			VisibleObject obj = iter.next();
			if (obj instanceof Npc) {
				npcs.add((Npc) obj);
			}
		}
		return npcs;
	}

	/**
	 * 本实例内静态门（物理 ID 或真端逻辑 ID → 门）。
	 * Static doors in this instance (physical or retail logical ID → door).
	 *
	 * door map
	 */
	public Map<Integer, StaticDoor> getDoors() {
		Map<Integer, StaticDoor> doors = new HashMap<Integer, StaticDoor>();
		for (Iterator<VisibleObject> iter = objectIterator(); iter.hasNext();) {
			VisibleObject obj = iter.next();
			if (obj instanceof StaticDoor) {
				StaticDoor door = (StaticDoor) obj;
				doors.put(door.getSpawn().getEntityId(), door);
				if (door.getObjectTemplate().getRetailId() > 0) {
					doors.put(door.getObjectTemplate().getRetailId(), door);
				}
			}
		}
		return doors;
	}

	/**
	 * 指定创建者的陷阱列表。
	 * Traps created by the given creature.
	 *
	 * @param p 创建者 / creator
	 * trap list
	 */
	public List<Trap> getTraps(Creature p) {
		List<Trap> traps = new ArrayList<Trap>();
		for (Iterator<VisibleObject> iter = objectIterator(); iter.hasNext();) {
			VisibleObject obj = iter.next();
			if (obj instanceof Trap) {
				Trap t = (Trap) obj;
				if (t.getCreatorId() == p.getObjectId()) {
					traps.add(t);
				}
			}
		}
		return traps;
	}

	/**
	 * 返回实例 ID。
	 * Return the instance id.
	 *
	 * instance id
	 */
	public int getInstanceId() {
		return instanceId;
	}

	public long getInstanceUid() {
		return dynamicInstance == null ? 0 : dynamicInstance.getInstanceUid();
	}

	public DynamicInstance getDynamicInstance() {
		return dynamicInstance;
	}

	public InstanceRuntimeState getRuntimeState() {
		if (runtimeState == null) {
			synchronized (this) {
				if (runtimeState == null) {
					runtimeState = new InstanceRuntimeState();
				}
			}
		}
		return runtimeState;
	}

	public <T> T getTransientState(Class<T> type) {
		Map<Class<?>, Object> states = transientStates;
		return states == null ? null : type.cast(states.get(type));
	}

	public <T> T getOrCreateTransientState(Class<T> type, Supplier<T> factory) {
		if (transientStates == null) {
			synchronized (this) {
				if (transientStates == null) {
					transientStates = new ConcurrentHashMap<>();
				}
			}
		}
		return type.cast(transientStates.computeIfAbsent(type, ignored -> factory.get()));
	}

	public <T> T removeTransientState(Class<T> type) {
		Map<Class<?>, Object> states = transientStates;
		return states == null ? null : type.cast(states.remove(type));
	}

	public void setDynamicInstance(DynamicInstance dynamicInstance, InstanceRuntimeState runtimeState) {
		if (this.dynamicInstance != null) {
			throw new IllegalStateException("Dynamic instance already assigned");
		}
		this.dynamicInstance = dynamicInstance;
		this.runtimeState = runtimeState;
	}

	/**
	 * 是否新手分流实例（instanceId 超过 twinCount）。
	 * Whether this is a beginner overflow instance (id above twinCount).
	 *
	 * @return 新手实例为 true / true if beginner instance
	 */
	public final boolean isBeginnerInstance() {
		if (parent == null) {
			return false;
		}
		if (parent.getTemplate().isInstance()) {
			return false;
		}
		int twinCount = parent.getTemplate().getTwinCount();
		if (twinCount == 0) {
			twinCount = 1;
		}
		return getInstanceId() > twinCount;
	}

	/**
	 * 玩家是否在本实例内。
	 * Whether a player is inside this instance.
	 *
	 * player objectId
	 *
	 * @param objId 若 inside 则为 true / true if inside
	 */
	public boolean isInInstance(int objId) {
		return worldMapPlayers.containsKey(objId);
	}

	/**
	 * 可见对象迭代器。
	 * Iterator over visible objects.
	 *
	 * iterator
	 */
	public Iterator<VisibleObject> objectIterator() {
		return worldMapObjectsSnapshot().iterator();
	}

	/**
	 * 玩家迭代器。
	 * Iterator over players.
	 *
	 * iterator
	 */
	public Iterator<Player> playerIterator() {
		return worldMapPlayersSnapshot().iterator();
	}

	/**
	 * 注册玩家队伍。
	 * Register a player group.
	 *
	 * group
	 */
	public void registerGroup(PlayerGroup group) {
		registeredGroup = group;
		register(group.getTeamId());
	}

	/**
	 * 注册玩家联盟。
	 * Register a player alliance.
	 *
	 * alliance
	 */
	public void registerGroup(PlayerAlliance group) {
		registredAlliance = group;
		register(group.getObjectId());
	}

	/**
	 * 注册军团联盟。
	 * Register a league.
	 *
	 * @param group 军团联盟 / league
	 */
	public void registerGroup(League group) {
		registredLeague = group;
		register(group.getObjectId());
	}

	/**
	 * 返回注册的联盟。
	 * Return the registered alliance.
	 *
	 * alliance
	 */
	public PlayerAlliance getRegistredAlliance() {
		return registredAlliance;
	}

	/**
	 * 返回注册的军团联盟。
	 * Return the registered league.
	 *
	 * league
	 */
	public League getRegistredLeague() {
		return registredLeague;
	}

	/**
	 * 注册对象 ID。
	 * Register an object id.
	 *
	 * object id
	 */
	public void register(int objectId) {
		registeredObjects.add(objectId);
	}

	public void unregister(int objectId) {
		registeredObjects.remove(objectId);
	}

	/**
	 * 对象 ID 是否已注册。
	 * Whether the object id is registered.
	 *
	 * object id
	 *
	 * @param objectId
	 * @return 已注册返回 true / true if registered
	 */
	public boolean isRegistered(int objectId) {
		return registeredObjects.contains(objectId);
	}

	/**
	 * 返回注册的队伍。
	 * Return the registered group.
	 *
	 * group
	 */
	public PlayerGroup getRegisteredGroup() {
		return registeredGroup;
	}

	/**
	 * 本实例玩家数量。
	 * Player count in this instance.
	 *
	 * count
	 */
	public int playersCount() {
		return worldMapPlayers.size();
	}

	/**
	 * 本实例相关任务 ID 只读列表。
	 * Read-only list of quest ids related to this instance.
	 *
	 * quest id list
	 */
	public List<Integer> getQuestIds() {
		synchronized (questIds) {
			return Collections.unmodifiableList(new ArrayList<Integer>(questIds));
		}
	}

	/**
	 * 返回副本处理器。
	 * Return the instance handler.
	 *
	 * handler
	 */
	public final InstanceHandler getInstanceHandler() {
		return instanceHandler;
	}

	/**
	 * 设置副本处理器。
	 * Set the instance handler.
	 *
	 * handler
	 */
	public final void setInstanceHandler(InstanceHandler instanceHandler) {
		this.instanceHandler = instanceHandler;
	}

	/**
	 * 按 objectId 查找玩家。
	 * Find a player by objectId.
	 *
	 * player objectId
	 * player or null
	 */
	public Player getPlayer(Integer object) {
		for (Player player : worldMapPlayersSnapshot()) {
			if (object == player.getObjectId()) {
				return player;
			}
		}
		return null;
	}

	/**
	 * 对全部玩家执行访问者。
	 * Visit all players.
	 *
	 * visitor
	 */
	public void doOnAllPlayers(Visitor<Player> visitor) {
		try {
			for (Player player : worldMapPlayersSnapshot()) {
				if (player != null) {
					visitor.visit(player);
				}
			}
		} catch (Exception ex) {
			log.error(I18n.get("log.cc03391ccf0f", ex), ex);
		}
	}

	/**
	 * 过滤与矩形区域相交的 Zone。
	 * Filter zones intersecting the rectangular region.
	 *
	 * map id
	 * region id
	 * start X
	 * start Y
	 * min Z
	 * max Z
	 * zone array
	 */
	protected ZoneInstance[] filterZones(int mapId, int regionId, float startX, float startY, float minZ, float maxZ) {
		List<ZoneInstance> regionZones = new ArrayList<ZoneInstance>();
		RegionZone regionZone = new RegionZone(startX, startY, minZ, maxZ);

		for (ZoneInstance zoneInstance : zones.values()) {
			if (zoneInstance.getAreaTemplate().intersectsRectangle(regionZone)) {
				regionZones.add(zoneInstance);
			} else if (zoneInstance.getZoneTemplate().getZoneType() == ZoneClassName.DUMMY) {
				log.error(I18n.get("log.2bb533f118b4", regionId, mapId));
			}
		}
		return regionZones.toArray(new ZoneInstance[regionZones.size()]);
	}

	/**
	 * 判断对象是否在指定 Zone 内。
	 * Whether the object is inside the named zone.
	 *
	 * visible object
	 * zone name
	 *
	 * @return 若 inside 则为 true / true if inside
	 */
	public boolean isInsideZone(VisibleObject object, ZoneName zoneName) {
		ZoneInstance zoneTemplate = zones.get(zoneName);
		if (zoneTemplate == null) {
			return false;
		}
		return isInsideZone(object.getPosition(), zoneName);
	}

	/**
	 * 判断位置是否在指定 Zone 内。
	 * Whether the position is inside the named zone.
	 *
	 * @param pos 世界位置 / world position
	 * zone name
	 *
	 * @return 若 inside 则为 true / true if inside
	 */
	public boolean isInsideZone(WorldPosition pos, ZoneName zoneName) {
		MapRegion mapRegion = this.getRegion(pos.getX(), pos.getY(), pos.getZ());
		return mapRegion.isInsideZone(zoneName, pos.getX(), pos.getY(), pos.getZ());
	}

	/**
	 * 设置单人所有者 objectId。
	 * Set solo-owner objectId.
	 *
	 * @param obj objectId
	 */
	public void setSoloPlayerObj(Integer obj) {
		soloPlayer = obj;
	}

	/**
	 * 返回单人所有者 objectId。
	 * Return solo-owner objectId.
	 *
	 * objectId or null
	 */
	public Integer getSoloPlayerObj() {
		return soloPlayer;
	}

	/**
	 * 可见对象快照。
	 * Snapshot of visible objects.
	 *
	 * object list
	 */
	private List<VisibleObject> worldMapObjectsSnapshot() {
		synchronized (worldMapObjects) {
			return new ArrayList<VisibleObject>(worldMapObjects.values());
		}
	}

	/**
	 * 玩家快照。
	 * Snapshot of players.
	 *
	 * player list
	 */
	private List<Player> worldMapPlayersSnapshot() {
		synchronized (worldMapPlayers) {
			return new ArrayList<Player>(worldMapPlayers.values());
		}
	}
}
