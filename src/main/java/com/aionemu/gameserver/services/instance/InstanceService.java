package com.aionemu.gameserver.services.instance;

import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.ai.RetailAreaEngine;
import com.aionemu.gameserver.ai.RetailDynamicAreaEngine;
import com.aionemu.gameserver.ai.RetailWindstreamEngine;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;

import com.aionemu.gameserver.lifecycle.GameHousingServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.lifecycle.GameWorldServices;

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
import com.aionemu.gameserver.questEngine.runtime.QuestRuntimeResources;
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
 * @author G-Robson26
 */
@Slf4j

public class InstanceService {
	/** 映射 IDwhereinstancemobsuseaggro / Map IDs where instance mobs use aggro */
	private static final List<Integer> instanceAggro = new ArrayList<>();
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
	 * @param worldId 世界地图 ID / world map id
	 * @param ownerId 所有者对象 ID（个人副本） / owner object id (personal instance)
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
		SpawnEngine.spawnInstance(worldId, worldMapInstance.getInstanceId(), 0, 0, ownerId);
		GameEngineServices.instanceEngine().onInstanceCreate(worldMapInstance);
		if (map.isInstanceType()) {
			startInstanceChecker(worldMapInstance);
		}
		return worldMapInstance;
	}

	/**
	 * 创建下一可用副本实例（无所有者）。
	 * Creates the next available instance without an owner.
	 * @param worldId 世界地图 ID / world map id
	 * @return 新建的世界地图实例 / newly created world map instance
	 */
	public synchronized static WorldMapInstance getNextAvailableInstance(int worldId) {
		return getNextAvailableInstance(worldId, 0);
	}

	/**
	 * 重置指定玩家拥有或其队伍登记的全部副本实例，并返回实际销毁数量。
	 * Resets all instances owned by the given player or registered to their team, and returns the number destroyed.
	 * <p>处理范围包括：
	 * 1. 玩家拥有的个人实例（isPersonal 为 true，ownerId 匹配）；
	 * 2. 玩家个人单独进入并登记的副本（无队伍/联盟/军团登记，且 soloPlayerObj 匹配），包括单人进入的组队副本；
	 * 3. 玩家当前所在队伍、联盟或军团所注册的副本实例。
	 * Scope covers:
	 * 1. Personal instances owned by the player (isPersonal true, ownerId matches);
	 * 2. Instances entered and registered by the player alone (no group/alliance/league, soloPlayerObj matches), including group instances entered solo;
	 * 3. Instances registered to the player's current group, alliance, or league.</p>
	 * @param player 玩家 / player
	 * @return 已销毁的副本数量 / number of destroyed instances
	 */
	public synchronized static int resetPlayerInstances(Player player) {
		if (player == null) {
			throw new IllegalArgumentException("player must not be null");
		}
		int playerObjectId = player.getObjectId();
		if (playerObjectId == 0) {
			return 0;
		}

		List<WorldMapInstance> instancesToReset = new ArrayList<>();
		World world = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world();
		for (WorldMapTemplate worldTemplate : DataManager.WORLD_MAPS_DATA) {
			if (!worldTemplate.isInstance()) {
				continue;
			}
			for (WorldMapInstance instance : world.getWorldMap(worldTemplate.getMapId()).getInstances()) {
				if (isPlayerInstance(instance, player)) {
					instancesToReset.add(instance);
				}
			}
		}

		int destroyedCount = 0;
		for (WorldMapInstance instance : instancesToReset) {
			if (isInstanceExist(instance.getMapId(), instance.getInstanceId())) {
				destroyInstance(instance);
				destroyedCount++;
			}
		}
		return destroyedCount;
	}

	/**
	 * 重置指定玩家拥有的全部单人副本（兼容旧接口）。
	 * Resets all solo instances owned by the given player (compatibility alias).
	 * @param player 拥有者 / owner
	 * @return 已销毁的副本数量 / number of destroyed instances
	 */
	public synchronized static int resetPlayerSoloInstances(Player player) {
		return resetPlayerInstances(player);
	}

	/**
	 * 判断实例是否属于指定玩家个人或其当前所在的队伍/联盟。
	 * Whether the instance belongs to the player personally or to their current group/alliance/league.
	 * @param instance 待检查副本 / instance to inspect
	 * @param player 玩家 / player
	 * @return 属于该玩家或其当前队伍则为 true / true if belongs to the player or their current team
	 */
	static boolean isPlayerInstance(WorldMapInstance instance, Player player) {
		if (player == null) {
			return false;
		}
		int playerObjectId = player.getObjectId();
		if (playerObjectId == 0) {
			return false;
		}

		// 1. 个人单人副本或个人单独进入的副本（无队伍/联盟/军团登记）
		// Solo or solo-entered instances (no group/alliance/league registered)
		if (isPlayerSoloInstance(instance, playerObjectId)) {
			return true;
		}

		// 2. 玩家当前所在队伍登记的副本 / Instances registered to player's current group
		if (player.isInGroup2() && instance.getRegisteredGroup() != null) {
			PlayerGroup group = player.getPlayerGroup2();
			if (group.equals(instance.getRegisteredGroup())
					|| (group.getTeamId() != null && group.getTeamId().equals(instance.getRegisteredGroup().getTeamId()))) {
				return true;
			}
		}

		// 3. 玩家当前所在联盟登记的副本 / Instances registered to player's current alliance
		if (player.isInAlliance2() && instance.getRegistredAlliance() != null) {
			PlayerAlliance alliance = player.getPlayerAlliance2();
			if (alliance.equals(instance.getRegistredAlliance())
					|| (alliance.getObjectId() != null && alliance.getObjectId().equals(instance.getRegistredAlliance().getObjectId()))) {
				return true;
			}
		}

		// 4. 玩家当前所在军团联盟登记的副本 / Instances registered to player's current league
		if (player.isInLeague() && instance.getRegistredLeague() != null) {
			League league = player.getPlayerAlliance2().getLeague();
			if (league != null && (league.equals(instance.getRegistredLeague())
					|| (league.getObjectId() != null && league.getObjectId().equals(instance.getRegistredLeague().getObjectId())))) {
				return true;
			}
		}

		return false;
	}

	/**
	 * 判断实例是否为指定玩家拥有的单人副本（包括个人独立进入的组队副本）。
	 * Whether the instance is a solo instance owned by the given player (including group instances entered solo).
	 * @param instance 待检查副本 / instance to inspect
	 * @param playerObjectId 玩家对象 ID / player object id
	 * @return 属于该玩家的单人/独立副本则为 true / true when owned solo/personal instance
	 */
	static boolean isPlayerSoloInstance(WorldMapInstance instance, int playerObjectId) {
		if (instance.getRegisteredGroup() != null || instance.getRegistredAlliance() != null
				|| instance.getRegistredLeague() != null) {
			return false;
		}
		if (instance.isPersonal()) {
			return instance.getOwnerId() == playerObjectId;
		}
		Integer soloPlayerObjectId = instance.getSoloPlayerObj();
		return soloPlayerObjectId != null && soloPlayerObjectId == playerObjectId;
	}

	/**
	 * 销毁副本：踢出玩家、删除对象并通知处理器。
	 * Destroys an instance: ejects players, deletes objects, notifies the handler.
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
		var pathService = GameWorldServices.pathService();
		map.removeWorldMapInstance(instanceId);
		try {
			RetailConditionSpawnEngine.clear(instance);
			RetailAreaEngine.clear(instance);
			RetailWindstreamEngine.clear(instance);
			RetailDynamicAreaEngine.clear(instance);
			log.info(I18n.get("log.e1c9d831d4ea", worldId, instanceId));
			Iterator<VisibleObject> it = instance.objectIterator();
			while (it.hasNext()) {
				VisibleObject obj = it.next();
				if (obj instanceof Player player) {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(SystemMessageId.LEAVE_INSTANCE_NOT_PARTY));
					moveToExitPoint((Player) obj);
				} else {
					obj.getController().onDelete();
				}
			}
			instance.getInstanceHandler().onInstanceDestroy();
			if (instance instanceof WorldMap2DInstance w2d) {
				if (w2d.isPersonal()) {
					GameHousingServices.housingService().onInstanceDestroy(w2d.getOwnerId());
				}
			}
		} finally {
			QuestRuntimeResources.cleanupInstance(instanceId);
			pathService.instanceDestroyed(worldId, instanceId);
		}
	}

	/**
	 * 将玩家注册到副本。
	 * Registers a player with the instance.
	 * @param instance 副本实例 / instance
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
	 * @param instance 副本实例 / instance
	 * @param group 队伍 / group
	 */
	public static void registerGroupWithInstance(WorldMapInstance instance, PlayerGroup group) {
		instance.registerGroup(group);
	}

	/**
	 * 将联盟注册到副本。
	 * Registers a player alliance with the instance.
	 * @param instance 副本实例 / instance
	 * @param group 联盟 / alliance
	 */
	public static void registerAllianceWithInstance(WorldMapInstance instance, PlayerAlliance group) {
		instance.registerGroup(group);
	}

	/**
	 * 将军团联赛注册到副本。
	 * Registers a league with the instance.
	 * @param instance 副本实例 / instance
	 * @param group 战团 / league
	 */
	public static void registerLeagueWithInstance(WorldMapInstance instance, League group) {
		instance.registerGroup(group);
	}

	/**
	 * 按对象 ID 查找已注册的副本。
	 * Finds a registered instance by object id.
	 * @param worldId 世界地图 ID / world map id
	 * @param objectId 注册对象 ID / registered object id
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
	 * @param worldId 世界地图 ID / world map id
	 * @param ownerId 所有者 ID / owner id
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
	 * @param worldId 世界地图 ID / world map id
	 * @param registeredId 注册 ID / registered id
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
	 * 玩家 / player
	 * @param player 玩家（查找 ID） / player (lookup id)
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
	 * @param player 玩家 / player
	 */
	public static void moveToExitPoint(Player player) {
		TeleportService2.moveToInstanceExit(player, player.getWorldId(), player.getRace());
	}

	/**
	 * 判断指定世界与实例 ID 的副本是否仍存在。
	 * Whether the instance still exists for the given world and instance id.
	 * @param worldId 世界地图 ID / world map id
	 * @param instanceId 副本 ID / instance id
	 * @return 是否存在 / whether it exists
	 */
	public static boolean isInstanceExist(int worldId, int instanceId) {
		return com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(worldId).getWorldMapInstanceById(instanceId) != null;
	}

	/**
	 * 副本在玩家离开后是否为空，可安排重置。
	 * Whether the instance is empty and eligible for reset after leave.
	 * @param instance 副本实例 / instance
	 * @return 是否为空 / whether empty
	 */
	static boolean isEmptyForResetAfterLeave(WorldMapInstance instance) {
		return instance.playersCount() == 0;
	}

	/**
	 * 若副本为空则安排延迟销毁。
	 * Schedules delayed destroy when the instance is empty.
	 * @param instance 副本实例 / instance
	 */
	private static void scheduleResetIfEmpty(final WorldMapInstance instance) {
		if (instance.getEmptyInstanceTask() != null) {
			instance.getEmptyInstanceTask().cancel(false);
		}
		pendingResets.add(instance);
		instance.setEmptyInstanceTask(GameThreadPoolServices.threadPoolManager().schedule(() -> {
			pendingResets.remove(instance);
			if (isInstanceExist(instance.getMapId(), instance.getInstanceId()) && isEmptyForResetAfterLeave(instance)) {
				destroyInstance(instance);
			}
		}, getScheduledDestroyDelayMillis(instance)));
	}

	/**
	 * 启动副本空闲检查（创建后即调度重置任务）。
	 * Starts empty-instance checking by scheduling a reset task.
	 * @param worldMapInstance 副本实例 / instance
	 */
	private static void startInstanceChecker(WorldMapInstance worldMapInstance) {
		scheduleResetIfEmpty(worldMapInstance);
	}

	/**
	 * 玩家进入后取消空副本销毁任务并刷新缩放。
	 * Cancels empty-destroy task and refreshes scaler when a player is added.
	 * @param instance 副本实例 / instance
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
	 * @param instance 副本实例 / instance
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
	 * @param player 玩家 / player
	 */
	public static void onLogOut(Player player) {
		player.getPosition().getWorldMapInstance().getInstanceHandler().onPlayerLogOut(player);
	}

	/**
	 * 玩家进入副本：更新区域/任务、通知处理器并清理非本图归属物品。
	 * On enter: update zone/quests, notify handler, drop non-owned-world items.
	 * @param player 玩家 / player
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
	 * 玩家 / player
	 * @param zone 区域 / zone
	 */
	public static void onEnterZone(Player player, ZoneInstance zone) {
		player.getPosition().getWorldMapInstance().getInstanceHandler().onEnterZone(player, zone);
	}

	/**
	 * 玩家开门时转发至副本处理器。
	 * Forwards door open to the instance handler.
	 * 玩家 / player
	 * @param door 门 ID / door id
	 */
	public static void onOpenDoor(Player player, int door) {
		player.getPosition().getWorldMapInstance().getInstanceHandler().onOpenDoor(player, door);
	}

	/**
	 * 玩家离开区域时转发至副本处理器。
	 * Forwards zone leave to the instance handler.
	 * 玩家 / player
	 * @param zone 区域 / zone
	 */
	public static void onLeaveZone(Player player, ZoneInstance zone) {
		player.getPosition().getWorldMapInstance().getInstanceHandler().onLeaveZone(player, zone);
	}

	/**
	 * 判断地图是否启用副本仇恨。
	 * Whether the map uses instance-mob aggro.
	 * @param mapId 地图 ID / map id
	 * @return 是否启用仇恨 / whether aggro is enabled
	 */
	public static boolean isAggro(int mapId) {
		return instanceAggro.contains(mapId);
	}

	/**
	 * 计算玩家在指定地图的冷却倍率。
	 * Computes the instance cooldown rate for a player on a map.
	 * 玩家 / player
	 * @param mapId 地图 ID / map id
	 * @return 冷却倍率 / cooldown rate
	 */
	public static int getInstanceRate(Player player, int mapId) {
		return player.havePermission(MembershipConfig.INSTANCES_COOLDOWN) && !InstanceConfig.isCooldownExcluded(mapId)
				? InstanceConfig.COOLDOWN_RATE : 1;
	}

	/**
	 * 获取空副本销毁延迟（毫秒）。
	 * Returns empty-instance destroy delay in milliseconds.
	 * @param soloInstance 是否单人副本 / whether solo instance
	 * @return 延迟毫秒数 / delay in ms
	 */
	static long getDestroyDelayMillis(boolean soloInstance) {
		return (soloInstance ? InstanceConfig.SOLO_DESTROY_DELAY_SECONDS : InstanceConfig.DESTROY_DELAY_SECONDS) * 1000L;
	}

	/**
	 * 获取调度用销毁延迟，至少 1 秒。
	 * Returns scheduled destroy delay, at least 1 second.
	 * @param soloInstance 是否单人副本 / whether solo instance
	 * @return 延迟毫秒数 / delay in ms
	 */
	static long getScheduledDestroyDelayMillis(boolean soloInstance) {
		return Math.max(1000L, getDestroyDelayMillis(soloInstance));
	}

	/**
	 * 按副本类型返回调度销毁延迟。
	 * Returns scheduled destroy delay for the given instance.
	 * @param instance 副本实例 / instance
	 * @return 延迟毫秒数 / delay in ms
	 */
	static long getScheduledDestroyDelayMillis(WorldMapInstance instance) {
		return getScheduledDestroyDelayMillis(isSoloInstance(instance));
	}

	/**
	 * 判断是否为单人副本（最大人数为 1，或无组队注册的单人登记）。
	 * Whether the instance is solo (max 1, or solo-registered without groups).
	 * @param instance 副本实例 / instance
	 * @return 是否单人间 / whether solo
	 */
	private static boolean isSoloInstance(WorldMapInstance instance) {
		int maxPlayers = getMaxPlayers(instance.getMapId());
		return maxPlayers <= 1 || (instance.getSoloPlayerObj() != null
				&& instance.getRegisteredGroup() == null && instance.getRegistredAlliance() == null && instance.getRegistredLeague() == null);
	}

	/**
	 * 获取地图允许的最大玩家数（光/暗侧取较大值）。
	 * Max players allowed on the map (greater of light/dark caps).
	 * @param mapId 地图 ID / map id
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
	 * @param value 值 / value
	 * @return 非空整数 / non-null int
	 */
	private static int valueOrZero(Integer value) {
		return value == null ? 0 : value;
	}

	/**
	 * 创建下一可用战场（BG）副本实例。
	 * Creates the next available battleground instance.
	 * @param worldId 世界地图 ID / world map id
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
