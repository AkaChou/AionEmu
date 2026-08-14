package com.aionemu.gameserver.world;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.PlayerInitialData.LocationData;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.base.BaseNpc;
import com.aionemu.gameserver.model.gameobjects.outpost.OutpostNpc;
import com.aionemu.gameserver.model.gameobjects.player.BindPointPosition;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.siege.SiegeNpc;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.world.container.PlayerContainer;
import com.aionemu.gameserver.world.exceptions.AlreadySpawnedException;
import com.aionemu.gameserver.world.exceptions.DuplicateAionObjectException;
import com.aionemu.gameserver.world.exceptions.WorldMapNotExistException;
import com.aionemu.gameserver.world.knownlist.Visitor;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 游戏世界单例：管理全部地图、玩家与可见对象的生成/位置更新。
 * Game-world singleton: manages all maps, players, and spawn/position updates of visible objects.
 */
@Slf4j
public class World {

	/** Spring 实例提供者 / Spring instance provider */
	private static volatile ObjectProvider<World> instanceProvider;
	/** 在线玩家容器 / online player container */
	private final PlayerContainer allPlayers;
	/** 全部可见对象 / all visible objects */
	private final Map<Integer, VisibleObject> allObjects;
	/** 按据点 ID 索引的攻城 NPC / siege NPCs indexed by siege location id */
	private final IntObjectHashMap<Collection<SiegeNpc>> localSiegeNpcs = new IntObjectHashMap<Collection<SiegeNpc>>();
	/** 按基地 ID 索引的基地 NPC / base NPCs indexed by base id */
	private final IntObjectHashMap<Collection<BaseNpc>> localBaseNpcs = new IntObjectHashMap<Collection<BaseNpc>>();
	/** 按前哨 ID 索引的前哨 NPC / outpost NPCs indexed by outpost id */
	private final IntObjectHashMap<Collection<OutpostNpc>> localOutpostNpcs = new IntObjectHashMap<Collection<OutpostNpc>>();
	/** 全部 NPC / all NPCs */
	private final Map<Integer, Npc> allNpcs;
	/** 全部世界地图 / all world maps */
	private final IntObjectHashMap<WorldMap> worldMaps;

	/**
	 * 构造世界并加载全部地图模板。
	 * Construct the world and load all map templates.
	 */
	public World() {
		Util.printSection(I18n.get("console.section.world"));
		allPlayers = new PlayerContainer();
		allObjects = Collections.synchronizedMap(new LinkedHashMap<Integer, VisibleObject>());
		allNpcs = Collections.synchronizedMap(new LinkedHashMap<Integer, Npc>());
		worldMaps = new IntObjectHashMap<WorldMap>();
		for (WorldMapTemplate template : DataManager.WORLD_MAPS_DATA) {
			worldMaps.put(template.getMapId(), new WorldMap(template, this));
		}
		log.info(I18n.get("log.e9ec75b7b736", worldMaps.size()));
	}

	/**
	 * 返回世界单例。
	 * Return the world singleton.
	 *
	 * @return 世界单例 / the world instance
	 */
	public static World getInstance() {
		ObjectProvider<World> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Inject the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<World> instanceProvider) {
		World.instanceProvider = instanceProvider;
	}

	/**
	 * 将对象登记到世界（玩家/NPC/本地索引）。
	 * Store an object into the world (player/NPC/local indexes).
	 *
	 * @param object 可见对象 / the visible object
	 */
	public void storeObject(VisibleObject object) {
		if (object.getPosition() == null) {
			log.warn(I18n.get("log.4f8199cf634c", object.getObjectTemplate().getTemplateId()));
			return;
		}
		boolean objectStored = false;
		boolean playerStored = false;
		boolean localObjectStored = false;
		boolean npcStored = false;
		try {
			addVisibleObject(object);
			objectStored = true;
			if (object instanceof Player) {
				allPlayers.add((Player) object);
				playerStored = true;
			}
			localObjectStored = addLocalObject(object);
			if (object instanceof Npc) {
				allNpcs.put(object.getObjectId(), (Npc) object);
				npcStored = true;
			}
		} catch (RuntimeException | Error e) {
			if (npcStored) {
				allNpcs.remove(object.getObjectId());
			}
			if (localObjectStored) {
				removeLocalObject(object);
			}
			if (playerStored) {
				allPlayers.remove((Player) object);
			}
			if (objectStored) {
				allObjects.remove(object.getObjectId());
			}
			throw e;
		}
	}

	/**
	 * 加入全局可见对象表；重复 objectId 抛异常。
	 * Add to the global visible-object table; throws on duplicate objectId.
	 *
	 * @param object 可见对象 / the visible object
	 */
	private void addVisibleObject(VisibleObject object) {
		synchronized (allObjects) {
			if (allObjects.containsKey(object.getObjectId())) {
				throw new DuplicateAionObjectException();
			}
			allObjects.put(object.getObjectId(), object);
		}
	}

	/**
	 * 按类型加入本地（攻城/基地/前哨）索引。
	 * Add to local (siege/base/outpost) indexes by type.
	 *
	 * @param object 可见对象 / the visible object
	 * @return 是否写入本地索引 / whether a local index was updated
	 */
	private boolean addLocalObject(VisibleObject object) {
		if (object instanceof SiegeNpc) {
			SiegeNpc siegeNpc = (SiegeNpc) object;
			synchronized (localSiegeNpcs) {
				Collection<SiegeNpc> npcs = localSiegeNpcs.get(siegeNpc.getSiegeId());
				if (npcs == null) {
					npcs = new ArrayList<SiegeNpc>();
					localSiegeNpcs.put(siegeNpc.getSiegeId(), npcs);
				}
				npcs.add(siegeNpc);
			}
			return true;
		} else if (object instanceof BaseNpc) {
			BaseNpc baseNpc = (BaseNpc) object;
			synchronized (localBaseNpcs) {
				Collection<BaseNpc> npcs = localBaseNpcs.get(baseNpc.getBaseId());
				if (npcs == null) {
					npcs = new ArrayList<BaseNpc>();
					localBaseNpcs.put(baseNpc.getBaseId(), npcs);
				}
				npcs.add(baseNpc);
			}
			return true;
		} else if (object instanceof OutpostNpc) {
			OutpostNpc outpostNpc = (OutpostNpc) object;
			synchronized (localOutpostNpcs) {
				Collection<OutpostNpc> npcs = localOutpostNpcs.get(outpostNpc.getOutpostId());
				if (npcs == null) {
					npcs = new ArrayList<OutpostNpc>();
					localOutpostNpcs.put(outpostNpc.getOutpostId(), npcs);
				}
				npcs.add(outpostNpc);
			}
			return true;
		}
		return false;
	}

	/**
	 * 从世界移除对象；Npc 也会从 NPC 表清除。
	 * Remove an object from the world; NPCs are also cleared from the NPC table.
	 *
	 * @param object 可见对象 / the visible object
	 */
	public void removeObject(VisibleObject object) {
		allObjects.remove(object.getObjectId());
		removeLocalObject(object);
		if (object instanceof Npc) {
			allNpcs.remove(object.getObjectId());
		}
		if (object instanceof Player) {
			allPlayers.remove((Player) object);
		}
	}

	/**
	 * 从本地（攻城/基地/前哨）索引移除。
	 * Remove from local (siege/base/outpost) indexes.
	 *
	 * @param object 可见对象 / the visible object
	 */
	private void removeLocalObject(VisibleObject object) {
		if (object instanceof SiegeNpc) {
			SiegeNpc siegeNpc = (SiegeNpc) object;
			synchronized (localSiegeNpcs) {
				Collection<SiegeNpc> locSpawn = localSiegeNpcs.get(siegeNpc.getSiegeId());
				if (!GenericValidator.isBlankOrNull(locSpawn)) {
					locSpawn.remove(siegeNpc);
				}
			}
		} else if (object instanceof BaseNpc) {
			BaseNpc baseNpc = (BaseNpc) object;
			synchronized (localBaseNpcs) {
				Collection<BaseNpc> locSpawn = localBaseNpcs.get(baseNpc.getBaseId());
				if (!GenericValidator.isBlankOrNull(locSpawn)) {
					locSpawn.remove(baseNpc);
				}
			}
		} else if (object instanceof OutpostNpc) {
			OutpostNpc outpostNpc = (OutpostNpc) object;
			synchronized (localOutpostNpcs) {
				Collection<OutpostNpc> locSpawn = localOutpostNpcs.get(outpostNpc.getOutpostId());
				if (!GenericValidator.isBlankOrNull(locSpawn)) {
					locSpawn.remove(outpostNpc);
				}
			}
		}
	}

	/**
	 * 玩家迭代器。
	 * Players iterator.
	 *
	 * @return 玩家迭代器 / player iterator
	 */
	public Iterator<Player> getPlayersIterator() {
		return allPlayers.iterator();
	}

	/**
	 * 指定据点的攻城 NPC 快照。
	 * Snapshot of siege NPCs for a location.
	 *
	 * @param locationId 据点 ID / the location id
	 * @return 攻城 NPC 集合 / the NPC collection
	 */
	public Collection<SiegeNpc> getLocalSiegeNpcs(int locationId) {
		synchronized (localSiegeNpcs) {
			Collection<SiegeNpc> result = localSiegeNpcs.get(locationId);
			return result != null ? new ArrayList<SiegeNpc>(result) : Collections.<SiegeNpc>emptySet();
		}
	}

	/**
	 * 指定基地的基地 NPC 快照。
	 * Snapshot of base NPCs for a location.
	 *
	 * @param locationId 基地 ID / the base id
	 * @return 基地 NPC 集合 / the NPC collection
	 */
	public Collection<BaseNpc> getLocalBaseNpcs(int locationId) {
		synchronized (localBaseNpcs) {
			Collection<BaseNpc> result = localBaseNpcs.get(locationId);
			return result != null ? new ArrayList<BaseNpc>(result) : Collections.<BaseNpc>emptySet();
		}
	}

	/**
	 * 指定前哨的前哨 NPC 快照。
	 * Snapshot of outpost NPCs for a location.
	 *
	 * @param locationId 前哨 ID / the outpost id
	 * @return 前哨 NPC 集合 / the NPC collection
	 */
	public Collection<OutpostNpc> getLocalOutpostNpcs(int locationId) {
		synchronized (localOutpostNpcs) {
			Collection<OutpostNpc> result = localOutpostNpcs.get(locationId);
			return result != null ? new ArrayList<OutpostNpc>(result) : Collections.<OutpostNpc>emptySet();
		}
	}

	/**
	 * 全部 NPC 快照。
	 * Snapshot of all NPCs.
	 *
	 * @return 全部 NPC 集合 / the NPC collection
	 */
	public Collection<Npc> getNpcs() {
		synchronized (allNpcs) {
			return new ArrayList<Npc>(allNpcs.values());
		}
	}

	/**
	 * 按名称查找玩家。
	 * Find a player by name.
	 *
	 * @param name 玩家名 / the player name
	 * @return 玩家或 null / the player or null
	 */
	public Player findPlayer(String name) {
		return allPlayers.get(name);
	}

	/**
	 * 按 objectId 查找玩家。
	 * Find a player by objectId.
	 *
	 * @param objectId 对象 ID / the object id
	 * @return 玩家或 null / the player or null
	 */
	public Player findPlayer(int objectId) {
		return allPlayers.get(objectId);
	}

	/**
	 * 按 objectId 查找可见对象。
	 * Find a visible object by objectId.
	 *
	 * @param objectId 对象 ID / the object id
	 * @return 可见对象或 null / visible object or null
	 */
	public VisibleObject findVisibleObject(int objectId) {
		return allObjects.get(objectId);
	}

	/**
	 * 对象是否已在世界中。
	 * Whether the object is stored in the world.
	 *
	 * @param object 可见对象 / the visible object
	 * @return 在世界中返回 true / true if in world
	 */
	public boolean isInWorld(VisibleObject object) {
		return allObjects.containsKey(object.getObjectId());
	}

	/**
	 * 按地图 ID 返回世界地图；不存在时抛异常。
	 * Return world map by id; throws if missing.
	 *
	 * @param id 地图 ID / map id
	 * @return 世界地图 / the world map
	 */
	public WorldMap getWorldMap(int id) {
		WorldMap map = worldMaps.get(id);
		if (map == null) {
			throw new WorldMapNotExistException("Map: " + id + " not exist!");
		}
		return map;
	}

	/**
	 * 更新对象位置并刷新已知列表。
	 * Update object position and refresh known list.
	 *
	 * @param object 可见对象 / the visible object
	 * @param newX 新 X / the new X
	 * @param newY 新 Y / the new Y
	 * @param newZ 新 Z / the new Z
	 * @param newHeading 新朝向 / the new heading
	 */
	public void updatePosition(VisibleObject object, float newX, float newY, float newZ, byte newHeading) {
		this.updatePosition(object, newX, newY, newZ, newHeading, true);
	}

	/**
	 * 更新对象在同一地图实例内的位置；区域变化时迁移并重校验 Zone。
	 * Update position within the same map instance; migrate regions and revalidate zones on change.
	 *
	 * @param object 可见对象 / the visible object
	 * @param newX 新 X / the new X
	 * @param newY 新 Y / the new Y
	 * @param newZ 新 Z / the new Z
	 * @param newHeading 新朝向 / the new heading
	 * @param updateKnownList 是否刷新已知列表 / whether to update known list
	 */
	public void updatePosition(VisibleObject object, float newX, float newY, float newZ, byte newHeading,
			boolean updateKnownList) {
		if (!object.isSpawned()) {
			return;
		}
		MapRegion oldRegion = object.getActiveRegion();
		if (oldRegion == null) {
			log.warn(I18n.get("log.c4b0d5c0760a", object.getWorldId(), object.getX(), object.getY(), object.getZ()));
			return;
		}
		MapRegion newRegion = oldRegion.getParent().getRegion(newX, newY, newZ);
		if (newRegion == null) {
			log.warn(I18n.get("log.b6b1d45cbd0f", object.getWorldId(), newX, newY, newZ), new Throwable());
			if (object instanceof Creature) {
				((Creature) object).getMoveController().abortMove();
			}
			if (object instanceof Player) {
				Player player = (Player) object;
				float x, y, z;
				int worldId;
				byte h = 0;
				if (player.getBindPoint() != null) {
					BindPointPosition bplist = player.getBindPoint();
					worldId = bplist.getMapId();
					x = bplist.getX();
					y = bplist.getY();
					z = bplist.getZ();
					h = bplist.getHeading();
				} else {
					LocationData locationData = DataManager.PLAYER_INITIAL_DATA
							.getSpawnLocation(player.getCommonData().getRace());
					worldId = locationData.getMapId();
					x = locationData.getX();
					y = locationData.getY();
					z = locationData.getZ();
				}
				setPosition(object, worldId, x, y, z, h);
			}
			return;
		}
		object.getPosition().setXYZH(newX, newY, newZ, newHeading);
		if (newRegion != oldRegion) {
			if (object instanceof Creature) {
				oldRegion.revalidateZones((Creature) object);
				newRegion.revalidateZones((Creature) object);
			}
			oldRegion.remove(object);
			newRegion.add(object);
			object.getPosition().setMapRegion(newRegion);
		}
		if (updateKnownList) {
			object.updateKnownlist();
		}
	}

	/**
	 * 设置对象位置但不生成（已生成则先 despawn）；保留同图实例 ID。
	 * Set position without spawning (despawn first if spawned); keeps instance id on same map.
	 *
	 * @param object 可见对象 / the visible object
	 * @param mapId 地图 ID / the map id
	 * @param x 坐标 X / X coordinate
	 * @param y 坐标 Y / Y coordinate
	 * @param z 坐标 Z / Z coordinate
	 * @param heading 朝向 / heading
	 */
	public void setPosition(VisibleObject object, int mapId, float x, float y, float z, byte heading) {
		int instanceId = 1;
		if (object.getWorldId() == mapId) {
			instanceId = object.getInstanceId();
		}
		this.setPosition(object, mapId, instanceId, x, y, z, heading);
	}

	/**
	 * 设置对象到指定地图实例的位置（不生成）。
	 * Set object position on a specific map instance (without spawning).
	 *
	 * @param object 可见对象 / the visible object
	 * @param mapId 地图 ID / the map id
	 * @param instance 实例 ID / the instance id
	 * @param x 坐标 X / X coordinate
	 * @param y 坐标 Y / Y coordinate
	 * @param z 坐标 Z / Z coordinate
	 * @param heading 朝向 / heading
	 */
	public void setPosition(VisibleObject object, int mapId, int instance, float x, float y, float z, byte heading) {
		if (object.isSpawned()) {
			despawn(object);
		}
		WorldMapInstance instanceMap = getWorldMap(mapId).getWorldMapInstanceById(instance);
		if (instanceMap == null) {
			return;
		}
		object.getPosition().setXYZH(x, y, z, heading);
		object.getPosition().setMapId(mapId);
		MapRegion region = instanceMap.getRegion(object);
		object.getPosition().setMapRegion(region);
	}

	/**
	 * 创建并返回给定参数的 {@link WorldPosition}。
	 * Create and return a {@link WorldPosition} for the given parameters.
	 *
	 * @param mapId 地图 ID / the map id
	 * @param x 坐标 X / X coordinate
	 * @param y 坐标 Y / Y coordinate
	 * @param z 坐标 Z / Z coordinate
	 * @param heading 朝向 / heading
	 * @param instanceId 实例 ID / the instance id
	 * @return 新位置 / the world position
	 */
	public WorldPosition createPosition(int mapId, float x, float y, float z, byte heading, int instanceId) {
		WorldPosition position = new WorldPosition(mapId);
		position.setXYZH(x, y, z, heading);
		position.setMapId(mapId);
		position.setMapRegion(getWorldMap(mapId).getWorldMapInstanceById(instanceId).getRegion(x, y, z));
		return position;
	}

	/**
	 * 玩家预生成：设 ACTIVE、加入区域与实例。
	 * Pre-spawn a player: set ACTIVE, add to region and instance.
	 *
	 * @param object 玩家对象 / the player object
	 */
	public void preSpawn(VisibleObject object) {
		((Player) object).setState(CreatureState.ACTIVE);
		object.getPosition().setIsSpawned(true);
		object.getActiveRegion().getParent().addObject(object);
		object.getActiveRegion().add(object);
		object.getController().onAfterSpawn();
	}

	/**
	 * 在当前位置生成对象，使其可见并更新已知列表。
	 * Spawn the object at its current position so it becomes visible and updates known list.
	 *
	 * @param object 可见对象 / the visible object
	 */
	public void spawn(VisibleObject object) {
		if (object.getPosition().isSpawned()) {
			throw new AlreadySpawnedException();
		}
		object.getController().onBeforeSpawn();
		object.getPosition().setIsSpawned(true);
		object.getActiveRegion().getParent().addObject(object);
		object.getActiveRegion().add(object);
		object.getController().onAfterSpawn();
		object.updateKnownlist();
	}

	/**
	 * 取消生成并使对象不可见（默认清空已知列表）。
	 * Despawn the object and make it invisible (clears known list by default).
	 *
	 * @param object 可见对象 / the visible object
	 */
	public void despawn(VisibleObject object) {
		despawn(object, true);
	}

	/**
	 * 取消生成；可选是否清空已知列表。
	 * Despawn the object; optionally clear known list.
	 *
	 * @param object 可见对象 / the visible object
	 * @param clearKnownlist 是否清空已知列表 / whether to clear known list
	 */
	public void despawn(VisibleObject object, boolean clearKnownlist) {
		MapRegion oldMapRegion = object.getActiveRegion();
		if (object.getActiveRegion() != null) {
			if (object.getActiveRegion().getParent() != null) {
				object.getActiveRegion().getParent().removeObject(object);
			}
			object.getActiveRegion().remove(object);
		}
		object.getPosition().setIsSpawned(false);
		if (oldMapRegion != null && object instanceof Creature) {
			oldMapRegion.revalidateZones((Creature) object);
		}
		if (clearKnownlist) {
			object.clearKnownlist();
		}
	}

	/**
	 * 全部在线玩家。
	 * All online players.
	 *
	 * @return 全部在线玩家 / the player collection
	 */
	public Collection<Player> getAllPlayers() {
		return allPlayers.getAllPlayers();
	}

	/**
	 * 对全部玩家执行访问者。
	 * Visit all players.
	 *
	 * @param visitor 玩家访问者 / the player visitor
	 */
	public void doOnAllPlayers(Visitor<Player> visitor) {
		allPlayers.doOnAllPlayers(visitor);
	}

	/**
	 * 对全部可见对象执行访问者。
	 * Visit all visible objects.
	 *
	 * @param visitor 可见对象访问者 / the visible-object visitor
	 */
	public void doOnAllObjects(Visitor<VisibleObject> visitor) {
		try {
			for (VisibleObject object : allObjectsSnapshot()) {
				if (object != null) {
					visitor.visit(object);
				}
			}
		} catch (Exception ex) {
			log.error(I18n.get("log.e15440de12ca", ex));
		}
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		protected static final World instance = new World();
	}

	/**
	 * 全部可见对象快照。
	 * Snapshot of all visible objects.
	 *
	 * @return 可见对象列表 / the object list
	 */
	private List<VisibleObject> allObjectsSnapshot() {
		synchronized (allObjects) {
			return new ArrayList<VisibleObject>(allObjects.values());
		}
	}
}
