package com.aionemu.gameserver.services.instance;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameHousingServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import com.aionemu.gameserver.configs.main.AutoGroupConfig;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.main.InstanceConfig;
import com.aionemu.gameserver.configs.main.MembershipConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.model.team2.league.League;
import com.aionemu.gameserver.model.templates.InstanceCooltime;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.network.aion.SystemMessageId;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.spawnengine.StaticDoorSpawnManager;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMap;
import com.aionemu.gameserver.world.WorldMap2DInstance;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldMapInstanceFactory;
import com.aionemu.gameserver.world.WorldMapType;
import com.aionemu.gameserver.world.zone.ZoneInstance;

/**
 * 副本生命周期服务，负责创建/销毁、注册、进出及空副本回收。
 * Instance lifecycle service — create/destroy, registration, enter/leave and empty-instance recycle.
 *
 * @author G-Robson26
 */
@Slf4j

public class InstanceService {
	/** 映射 IDwhereinstancemobsuseaggro / Map IDs where instance mobs use aggro */
	private static final List<Integer> instanceAggro = new ArrayList<Integer>();
	/** 待空副本重置的实例集合（弱引用）。 / Instances pending empty-reset (weak refs). */
	private static final Set<WorldMapInstance> pendingResets = Collections
			.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

	/**
	 * 加载副本仇恨地图配置。
	 * Loads instance aggro map configuration.
	 */
	public static void load() {
		instanceAggro.clear();
		for (String s : CustomConfig.INSTANCES_MOB_AGGRO.split(",")) {
			instanceAggro.add(Integer.parseInt(s));
		}
	}

	/**
	 * 创建下一可用副本实例（可指定所有者）。
	 * Creates the next available instance, optionally owned.
	 *
	 * world map id
	 *
	 * @param ownerId 所有者对象 ID（个人副本） / owner object id (personal instance)
	 * @param ownerId
	 * @return 新建的世界地图实例 / newly created world map instance
	 */
	public synchronized static WorldMapInstance getNextAvailableInstance(int worldId, int ownerId) {
		WorldMap map = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(worldId);
		if (!map.isInstanceType()) {
			throw new UnsupportedOperationException("Invalid call for next available instance  of " + worldId);
		}
		int nextInstanceId = map.getNextInstanceId();
		log.info(I18n.get("log.a698e49af03a", worldId, nextInstanceId, ownerId));
		WorldMapInstance worldMapInstance = WorldMapInstanceFactory.createWorldMapInstance(map, nextInstanceId,
				ownerId);
		map.addInstance(nextInstanceId, worldMapInstance);
		SpawnEngine.spawnInstance(worldId, worldMapInstance.getInstanceId(), (byte) 0, ownerId);
		GameEngineServices.instanceEngine().onInstanceCreate(worldMapInstance);
		if (map.isInstanceType()) {
			startInstanceChecker(worldMapInstance);
		}
		return worldMapInstance;
	}

	/**
	 * 创建下一可用副本实例（无所有者）。
	 * Creates the next available instance without an owner.
	 *
	 * world map id
	 *
	 * @param worldId
	 * @return 新建的世界地图实例 / newly created world map instance
	 */
	public synchronized static WorldMapInstance getNextAvailableInstance(int worldId) {
		return getNextAvailableInstance(worldId, 0);
	}

	/**
	 * 销毁副本：踢出玩家、删除对象并通知处理器。
	 * Destroys an instance: ejects players, deletes objects, notifies the handler.
	 *
	 * @param instance 要销毁的副本 / instance to destroy
	 */
	public static void destroyInstance(WorldMapInstance instance) {
		pendingResets.remove(instance);
		if (instance.getEmptyInstanceTask() != null) {
			instance.getEmptyInstanceTask().cancel(false);
		}
		int worldId = instance.getMapId();
		WorldMap map = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(worldId);
		if (!map.isInstanceType()) {
			return;
		}
		int instanceId = instance.getInstanceId();
		map.removeWorldMapInstance(instanceId);
		log.info(I18n.get("log.e1c9d831d4ea", worldId, instanceId));
		Iterator<VisibleObject> it = instance.objectIterator();
		while (it.hasNext()) {
			VisibleObject obj = it.next();
			if (obj instanceof Player) {
				Player player = (Player) obj;
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(SystemMessageId.LEAVE_INSTANCE_NOT_PARTY));
				moveToExitPoint((Player) obj);
			} else {
				obj.getController().onDelete();
			}
		}
		instance.getInstanceHandler().onInstanceDestroy();
		if (instance instanceof WorldMap2DInstance) {
			WorldMap2DInstance w2d = (WorldMap2DInstance) instance;
			if (w2d.isPersonal()) {
				GameHousingServices.housingService().onInstanceDestroy(w2d.getOwnerId());
			}
		}
	}

	/**
	 * 将玩家注册到副本。
	 * Registers a player with the instance.
	 *
	 * instance
	 * 玩家 / player
	 */
	public static void registerPlayerWithInstance(WorldMapInstance instance, Player player) {
		Integer obj = player.getObjectId();
		instance.register(obj);
		instance.setSoloPlayerObj(obj);
	}

	/**
	 * 将队伍注册到副本。
	 * Registers a player group with the instance.
	 *
	 * instance
	 * group
	 */
	public static void registerGroupWithInstance(WorldMapInstance instance, PlayerGroup group) {
		instance.registerGroup(group);
	}

	/**
	 * 将联盟注册到副本。
	 * Registers a player alliance with the instance.
	 *
	 * instance
	 * alliance
	 */
	public static void registerAllianceWithInstance(WorldMapInstance instance, PlayerAlliance group) {
		instance.registerGroup(group);
	}

	/**
	 * 将军团联赛注册到副本。
	 * Registers a league with the instance.
	 *
	 * instance
	 * league
	 */
	public static void registerLeagueWithInstance(WorldMapInstance instance, League group) {
		instance.registerGroup(group);
	}

	/**
	 * 按对象 ID 查找已注册的副本。
	 * Finds a registered instance by object id.
	 *
	 * world map id
	 * registered object id
	 *
	 * @return 已注册副本，未找到则为 null / registered instance, or null
	 */
	public static WorldMapInstance getRegisteredInstance(int worldId, int objectId) {
		Iterator<WorldMapInstance> iterator = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(worldId).iterator();
		while (iterator.hasNext()) {
			WorldMapInstance instance = iterator.next();
			if (instance.isRegistered(objectId)) {
				return instance;
			}
		}
		return null;
	}

	/**
	 * 按所有者查找个人副本。
	 * Finds a personal instance by owner id.
	 *
	 * world map id
	 * owner id
	 *
	 * @return 个人副本，未找到则为 null / personal instance, or null
	 */
	public static WorldMapInstance getPersonalInstance(int worldId, int ownerId) {
		if (ownerId == 0) {
			return null;
		}
		Iterator<WorldMapInstance> iterator = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(worldId).iterator();
		while (iterator.hasNext()) {
			WorldMapInstance instance = iterator.next();
			if (instance.isPersonal() && instance.getOwnerId() == ownerId) {
				return instance;
			}
		}
		return null;
	}

	/**
	 * 获取新手副本（若已注册且为新手实例）。
	 * Returns the beginner instance if registered and beginner-type.
	 *
	 * world map id
	 * registered id
	 *
	 * @return 新手副本，否则 null / beginner instance, or null
	 */
	public static WorldMapInstance getBeginnerInstance(int worldId, int registeredId) {
		WorldMapInstance instance = getRegisteredInstance(worldId, registeredId);
		if (instance == null) {
			return null;
		}
		return instance.isBeginnerInstance() ? instance : null;
	}

	/**
	 * 解析玩家最近一次副本注册所用的查找 ID。
	 * Resolves the lookup id last used for the player's instance registration.
	 *
	 * 玩家 / player
	 * lookup id
	 */
	private static int getLastRegisteredId(Player player) {
		int lookupId;
		boolean isPersonal = WorldMapType.getWorld(player.getWorldId()).isPersonal();
		if (player.isInGroup2()) {
			lookupId = player.getPlayerGroup2().getTeamId();
		} else if (player.isInAlliance2()) {
			lookupId = player.getPlayerAlliance2().getTeamId();
			if (player.isInLeague()) {
				lookupId = player.getPlayerAlliance2().getLeague().getObjectId();
			}
		} else if (isPersonal && player.getCommonData().getWorldOwnerId() != 0) {
			lookupId = player.getCommonData().getWorldOwnerId();
		} else {
			lookupId = player.getObjectId();
		}
		return lookupId;
	}

	/**
	 * 玩家登录时恢复副本位置；无效则传送至出口。
	 * Restores instance position on login; moves to exit if invalid.
	 *
	 * @param player 玩家 / player
	 */
	public static void onPlayerLogin(Player player) {
		int worldId = player.getWorldId();
		int lookupId = getLastRegisteredId(player);
		WorldMapInstance beginnerInstance = getBeginnerInstance(worldId, lookupId);
		if (beginnerInstance != null) {
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().setPosition(player, worldId, beginnerInstance.getInstanceId(), player.getX(),
					player.getY(), player.getZ(), player.getHeading());
		}
		WorldMapTemplate worldTemplate = DataManager.WORLD_MAPS_DATA.getTemplate(worldId);
		if (worldTemplate.isInstance()) {
			boolean isPersonal = WorldMapType.getWorld(player.getWorldId()).isPersonal();
			WorldMapInstance registeredInstance = isPersonal ? getPersonalInstance(worldId, lookupId)
					: getRegisteredInstance(worldId, lookupId);
			if (isPersonal) {
				if (registeredInstance == null) {
					registeredInstance = getNextAvailableInstance(player.getWorldId(), lookupId);
				}
				if (!registeredInstance.isRegistered(player.getObjectId())) {
					registerPlayerWithInstance(registeredInstance, player);
				}
			}
			if (registeredInstance != null) {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().setPosition(player, worldId, registeredInstance.getInstanceId(), player.getX(),
						player.getY(), player.getZ(), player.getHeading());
				player.getPosition().getWorldMapInstance().getInstanceHandler().onPlayerLogin(player);
				return;
			}
			moveToExitPoint(player);
		}
	}

	/**
	 * 将玩家传送到当前副本出口点。
	 * Teleports the player to the instance exit point.
	 *
	 * @param player 玩家 / player
	 */
	public static void moveToExitPoint(Player player) {
		TeleportService2.moveToInstanceExit(player, player.getWorldId(), player.getRace());
	}

	/**
	 * 判断指定世界与实例 ID 的副本是否仍存在。
	 * Whether the instance still exists for the given world and instance id.
	 *
	 * world map id
	 * instance id
	 * whether it exists
	 */
	public static boolean isInstanceExist(int worldId, int instanceId) {
		return com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(worldId).getWorldMapInstanceById(instanceId) != null;
	}

	/**
	 * 副本在玩家离开后是否为空，可安排重置。
	 * Whether the instance is empty and eligible for reset after leave.
	 *
	 * instance
	 * whether empty
	 */
	static boolean isEmptyForResetAfterLeave(WorldMapInstance instance) {
		return instance.playersCount() == 0;
	}

	/**
	 * 若副本为空则安排延迟销毁。
	 * Schedules delayed destroy when the instance is empty.
	 *
	 * instance
	 */
	private static void scheduleResetIfEmpty(final WorldMapInstance instance) {
		if (instance.getEmptyInstanceTask() != null) {
			instance.getEmptyInstanceTask().cancel(false);
		}
		pendingResets.add(instance);
		instance.setEmptyInstanceTask(GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				pendingResets.remove(instance);
				if (isInstanceExist(instance.getMapId(), instance.getInstanceId()) && isEmptyForResetAfterLeave(instance)) {
					destroyInstance(instance);
				}
			}
		}, getScheduledDestroyDelayMillis(instance)));
	}

	/**
	 * 启动副本空闲检查（创建后即调度重置任务）。
	 * Starts empty-instance checking by scheduling a reset task.
	 *
	 * instance
	 */
	private static void startInstanceChecker(WorldMapInstance worldMapInstance) {
		scheduleResetIfEmpty(worldMapInstance);
	}

	/**
	 * 玩家进入后取消空副本销毁任务并刷新缩放。
	 * Cancels empty-destroy task and refreshes scaler when a player is added.
	 *
	 * instance
	 */
	public static void onPlayerAdded(WorldMapInstance instance) {
		if (instance.getParent().isInstanceType()) {
			pendingResets.remove(instance);
			if (instance.getEmptyInstanceTask() != null) {
				instance.getEmptyInstanceTask().cancel(false);
				instance.setEmptyInstanceTask(null);
			}
			InstanceScaler.onPlayersChanged(instance);
		}
	}

	/**
	 * 重新加载所有待销毁空副本的延迟任务。
	 * Reloads delayed destroy tasks for all pending empty instances.
	 */
	public static void reloadDestroyTasks() {
		List<WorldMapInstance> instances;
		synchronized (pendingResets) {
			instances = new ArrayList<>(pendingResets);
		}
		for (WorldMapInstance instance : instances) {
			if (isInstanceExist(instance.getMapId(), instance.getInstanceId()) && isEmptyForResetAfterLeave(instance)) {
				scheduleResetIfEmpty(instance);
			} else {
				pendingResets.remove(instance);
			}
		}
	}

	/**
	 * 玩家离开后刷新缩放；若无人则调度销毁。
	 * Refreshes scaler on leave; schedules destroy when no players remain.
	 *
	 * instance
	 */
	public static void onPlayerRemoved(WorldMapInstance instance) {
		if (instance.getParent().isInstanceType()) {
			InstanceScaler.onPlayersChanged(instance);
			if (instance.playersCount() == 0) {
				scheduleResetIfEmpty(instance);
			}
		}
	}

	/**
	 * 玩家登出时通知副本处理器。
	 * Notifies the instance handler when a player logs out.
	 *
	 * @param player 玩家 / player
	 */
	public static void onLogOut(Player player) {
		player.getPosition().getWorldMapInstance().getInstanceHandler().onPlayerLogOut(player);
	}

	/**
	 * 玩家进入副本：更新区域/任务、通知处理器并清理非本图归属物品。
	 * On enter: update zone/quests, notify handler, drop non-owned-world items.
	 *
	 * 玩家 / player
	 */
	public static void onEnterInstance(Player player) {
		player.getController().updateZone();
		player.getController().updateNearbyQuests();
		player.getPosition().getWorldMapInstance().getInstanceHandler().onEnterInstance(player);
		GameCoreGameplayServices.autoGroupService().onEnterInstance(player);
		for (Item item : player.getInventory().getItems()) {
			if (item.getItemTemplate().getOwnershipWorld() == 0) {
				continue;
			}
			if (item.getItemTemplate().getOwnershipWorld() != player.getWorldId()) {
				player.getInventory().decreaseByObjectId(item.getObjectId(), item.getItemCount());
			}
		}
	}

	/**
	 * 玩家离开副本：通知处理器、清理本图归属物品并处理自动组队。
	 * On leave: notify handler, drop owned-world items, handle auto-group.
	 *
	 * @param player 玩家 / player
	 */
	public static void onLeaveInstance(Player player) {
		WorldMapInstance instance = player.getPosition().getWorldMapInstance();
		instance.getInstanceHandler().onLeaveInstance(player);
		for (Item item : player.getInventory().getItems()) {
			if (item.getItemTemplate().getOwnershipWorld() == player.getWorldId()) {
				player.getInventory().decreaseByObjectId(item.getObjectId(), item.getItemCount());
			}
		}
		if (AutoGroupConfig.AUTO_GROUP_ENABLED) {
			GameCoreGameplayServices.autoGroupService().onLeaveInstance(player);
		}
	}

	/**
	 * 玩家进入区域时转发至副本处理器。
	 * Forwards zone enter to the instance handler.
	 *
	 * 玩家 / player
	 * zone
	 */
	public static void onEnterZone(Player player, ZoneInstance zone) {
		player.getPosition().getWorldMapInstance().getInstanceHandler().onEnterZone(player, zone);
	}

	/**
	 * 玩家开门时转发至副本处理器。
	 * Forwards door open to the instance handler.
	 *
	 * 玩家 / player
	 * door id
	 */
	public static void onOpenDoor(Player player, int door) {
		player.getPosition().getWorldMapInstance().getInstanceHandler().onOpenDoor(player, door);
	}

	/**
	 * 玩家离开区域时转发至副本处理器。
	 * Forwards zone leave to the instance handler.
	 *
	 * 玩家 / player
	 * zone
	 */
	public static void onLeaveZone(Player player, ZoneInstance zone) {
		player.getPosition().getWorldMapInstance().getInstanceHandler().onLeaveZone(player, zone);
	}

	/**
	 * 判断地图是否启用副本仇恨。
	 * Whether the map uses instance-mob aggro.
	 *
	 * map id
	 * whether aggro is enabled
	 */
	public static boolean isAggro(int mapId) {
		return instanceAggro.contains(mapId);
	}

	/**
	 * 计算玩家在指定地图的冷却倍率。
	 * Computes the instance cooldown rate for a player on a map.
	 *
	 * 玩家 / player
	 * map id
	 * cooldown rate
	 */
	public static int getInstanceRate(Player player, int mapId) {
		return player.havePermission(MembershipConfig.INSTANCES_COOLDOWN) && !InstanceConfig.isCooldownExcluded(mapId)
				? InstanceConfig.COOLDOWN_RATE : 1;
	}

	/**
	 * 获取空副本销毁延迟（毫秒）。
	 * Returns empty-instance destroy delay in milliseconds.
	 *
	 * @param soloInstance 是否单人副本 / whether solo instance
	 * @return 延迟毫秒数 / delay in ms
	 */
	static long getDestroyDelayMillis(boolean soloInstance) {
		return (soloInstance ? InstanceConfig.SOLO_DESTROY_DELAY_SECONDS : InstanceConfig.DESTROY_DELAY_SECONDS) * 1000L;
	}

	/**
	 * 获取调度用销毁延迟，至少 1 秒。
	 * Returns scheduled destroy delay, at least 1 second.
	 *
	 * @param soloInstance 是否单人副本 / whether solo instance
	 * @return 延迟毫秒数 / delay in ms
	 */
	static long getScheduledDestroyDelayMillis(boolean soloInstance) {
		return Math.max(1000L, getDestroyDelayMillis(soloInstance));
	}

	/**
	 * 按副本类型返回调度销毁延迟。
	 * Returns scheduled destroy delay for the given instance.
	 *
	 * instance
	 *
	 * @param instance
	 * @return 延迟毫秒数 / delay in ms
	 */
	private static long getScheduledDestroyDelayMillis(WorldMapInstance instance) {
		return getScheduledDestroyDelayMillis(isSoloInstance(instance));
	}

	/**
	 * 判断是否为单人副本（最大人数为 1，或无组队注册的单人登记）。
	 * Whether the instance is solo (max 1, or solo-registered without groups).
	 *
	 * instance
	 * whether solo
	 */
	private static boolean isSoloInstance(WorldMapInstance instance) {
		int maxPlayers = getMaxPlayers(instance.getMapId());
		return maxPlayers == 1 || maxPlayers == 0 && instance.getSoloPlayerObj() != null
				&& instance.getRegisteredGroup() == null && instance.getRegistredAlliance() == null && instance.getRegistredLeague() == null;
	}

	/**
	 * 获取地图允许的最大玩家数（光/暗侧取较大值）。
	 * Max players allowed on the map (greater of light/dark caps).
	 *
	 * map id
	 *
	 * @param mapId
	 * @return 最大人数，无模板则为 0 / max players, or 0 if no template
	 */
	static int getMaxPlayers(int mapId) {
		InstanceCooltime template = DataManager.INSTANCE_COOLTIME_DATA.getInstanceCooltimeByWorldId(mapId);
		if (template == null) {
			return 0;
		}
		return Math.max(valueOrZero(template.getMaxMemberLight()), valueOrZero(template.getMaxMemberDark()));
	}

	/**
	 * 将可能为 null 的 Integer 转为 0。
	 * Converts a nullable Integer to 0 when null.
	 *
	 * value
	 * non-null int
	 */
	private static int valueOrZero(Integer value) {
		return value == null ? 0 : value;
	}

	/**
	 * 创建下一可用战场（BG）副本实例。
	 * Creates the next available battleground instance.
	 *
	 * world map id
	 *
	 * @param worldId
	 * @return 新建的世界地图实例 / newly created world map instance
	 */
	public synchronized static WorldMapInstance getNextBgInstance(int worldId) {
		WorldMap map = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(worldId);
		int nextInstanceId = map.getNextInstanceId();
		WorldMapInstance worldMapInstance = WorldMapInstanceFactory.createWorldMapInstance(map, nextInstanceId);
		map.addInstance(nextInstanceId, worldMapInstance);
		StaticDoorSpawnManager.spawnTemplate(worldId, worldMapInstance.getInstanceId());
		if (map.isInstanceType()) {
			startInstanceChecker(worldMapInstance);
		}
		return worldMapInstance;
	}
}
