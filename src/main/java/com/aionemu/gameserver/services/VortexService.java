package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.lifecycle.GameGameplayServices;

import com.aionemu.gameserver.lifecycle.GameCronServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.schedule.VortexSchedule;
import com.aionemu.gameserver.configs.schedule.VortexSchedule.Vortex;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.vortexspawns.VortexSpawnTemplate;
import com.aionemu.gameserver.model.vortex.VortexLocation;
import com.aionemu.gameserver.model.vortex.VortexStateType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.rift.RiftInformer;
import com.aionemu.gameserver.services.rift.RiftManager;
import com.aionemu.gameserver.services.vortexservice.DimensionalVortex;
import com.aionemu.gameserver.services.vortexservice.Invasion;
import com.aionemu.gameserver.services.vortexservice.VortexStartRunnable;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 漩涡入侵活动服务，管理次元漩涡开启、刷怪与入侵玩家。
 * Vortex invasion event service managing dimensional vortex open, spawns, and invaders.
 */
@Slf4j
public class VortexService {
	private static volatile ObjectProvider<VortexService> instanceProvider;
	private VortexSchedule vortexSchedule;
	private final List<Runnable> scheduledTasks = new ArrayList<>();
	private Map<Integer, VortexLocation> vortex;
	private final ConcurrentMap<Integer, DimensionalVortex<?>> activeInvasions = new ConcurrentHashMap<Integer, DimensionalVortex<?>>();

	/**
	 * 初始化漩涡地点并按和平状态刷怪。
	 * Initializes vortex locations and spawns them in the peace state.
	 */
	public void initVortexLocations() {
		if (CustomConfig.VORTEX_ENABLED) {
			vortex = DataManager.VORTEX_DATA.getVortexLocations();
			for (VortexLocation loc : getVortexLocations().values()) {
				spawn(loc, VortexStateType.PEACE);
			}
			log.info(I18n.get("log.4812ad0f0cf1", vortex.size()));
		} else {
			vortex = Collections.emptyMap();
		}
	}

	/**
	 * 初始化漩涡并加载定时计划。
	 * Initializes vortex and loads the schedule.
	 */
	public void initVortex() {
		if (CustomConfig.VORTEX_ENABLED) {
			log.info(I18n.get("log.2422075c2e20"));
		}
		reloadSchedule();
	}

	/**
	 * 重载漩涡 Cron 计划（先取消旧任务）。
	 * Reloads the vortex cron schedule (cancels previous tasks first).
	 */
	public synchronized void reloadSchedule() {
		VortexSchedule newSchedule = CustomConfig.VORTEX_ENABLED ? VortexSchedule.load() : null;
		scheduledTasks.forEach(GameCronServices.cronService()::cancel);
		scheduledTasks.clear();
		vortexSchedule = newSchedule;
		if (vortexSchedule != null) {
			for (Vortex vortex : vortexSchedule.getVortexsList()) {
				for (String invasionTime : vortex.getInvasionTimes()) {
					Runnable task = new VortexStartRunnable(vortex.getId());
					scheduledTasks.add(task);
					GameCronServices.cronService().schedule(task, invasionTime);
				}
			}
		}
	}

	/**
	 * 启动指定 ID 的次元入侵。
	 * Starts the dimensional invasion for the given id.
	 *
	 * @param id 地点 ID / location id
	 */
	public void startInvasion(final int id) {
		DimensionalVortex<?> invasion = new Invasion(vortex.get(id));
		if (activeInvasions.putIfAbsent(id, invasion) != null) {
			return;
		}
		invasion.start();
		theobomosVortexMsg(id);
		brusthoninVortexMsg(id);
		dimensionalVortexCountdownMsg(id);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				if (!invasion.isGeneratorDestroyed()) {
					stopInvasion(id);
				}
			}
		}, CustomConfig.VORTEX_DURATION * 3600 * 1000);
	}

	/**
	 * 停止指定 ID 的次元入侵。
	 * Stops the dimensional invasion for the given id.
	 *
	 * @param id 地点 ID / location id
	 */
	public void stopInvasion(int id) {
		DimensionalVortex<?> invasion = activeInvasions.remove(id);
		if (invasion == null || invasion.isFinished()) {
			return;
		}
		invasion.stop();
	}

	/**
	 * 按状态刷出漩涡相关 NPC/裂隙。
	 * Spawns vortex-related NPCs/rifts according to the given state.
	 *
	 * location
	 * @param state 状态类型 / state type
	 */
	public void spawn(VortexLocation loc, VortexStateType state) {
		if (state.equals(VortexStateType.INVASION)) {
			GameGameplayServices.riftManager().spawnVortex(loc);
			RiftInformer.sendRiftsInfo(loc.getHomeWorldId());
		}
		List<SpawnGroup2> locSpawns = DataManager.SPAWNS_DATA2.getVortexSpawnsByLocId(loc.getId());
		for (SpawnGroup2 group : locSpawns) {
			for (SpawnTemplate st : group.getSpawnTemplates()) {
				VortexSpawnTemplate vortextemplate = (VortexSpawnTemplate) st;
				if (vortextemplate.getStateType().equals(state)) {
					loc.getSpawned().add(SpawnEngine.spawnObject(vortextemplate, 1));
				}
			}
		}
	}

		/**
	 * 向魔族广播通往 Theobomos 的漩涡开启消息。
	 * Broadcasts Theobomos vortex-open message to Asmodians.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean theobomosVortexMsg(int id) {
		switch (id) {
		case 1:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					if (player.getCommonData().getRace() == Race.ASMODIANS) {
						// 通往泰奥勃莫斯的次元漩涡已出现。 / A Dimensional Vortex leading to Theobomos has appeared.
						PacketSendUtility.sendPacket(player,
								SM_SYSTEM_MESSAGE.STR_MSG_LIGHT_SIDE_INVADE_DIRECT_PORTAL_OPEN);
					}
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 向天族广播通往 Brusthonin 的漩涡开启消息。
	 * Broadcasts Brusthonin vortex-open message to Elyos.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean brusthoninVortexMsg(int id) {
		switch (id) {
		case 1:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					if (player.getCommonData().getRace() == Race.ELYOS) {
						// 通往布鲁斯特豪宁的次元漩涡已出现。 / A Dimensional Vortex leading to Brusthonin has appeared.
						PacketSendUtility.sendPacket(player,
								SM_SYSTEM_MESSAGE.STR_MSG_DARK_SIDE_INVADE_DIRECT_PORTAL_OPEN);
					}
				}
			});
			return true;
		default:
			return false;
		}
	}

		/**
	 * 广播次元漩涡关闭倒计时消息。
	 * Broadcasts dimensional vortex close-countdown messages.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean dimensionalVortexCountdownMsg(int id) {
		switch (id) {
		case 1:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 次元漩涡将在 90 分钟后关闭。关闭后联盟 / The Dimensional Vortex will close in 90 minutes. When it closes, the alliance
					// 将被解散，所有渗透者将被送回。 / will be disbanded and all infiltrators will be returned home.
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_TIMER_90M, 1800000);
					// 次元漩涡将在 60 分钟后关闭。关闭后联盟 / The Dimensional Vortex will close in 60 minutes. When it closes, the alliance
					// 将被解散，所有渗透者将被送回。 / will be disbanded and all infiltrators will be returned home.
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_TIMER_60M, 3600000);
					// 次元漩涡将在 30 分钟后关闭。关闭后联盟 / The Dimensional Vortex will close in 30 minutes. When it closes, the alliance
					// 将被解散，所有渗透者将被送回。 / will be disbanded and all infiltrators will be returned home.
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_TIMER_30M, 5400000);
					// 次元漩涡将在 15 分钟后关闭。关闭后联盟 / The Dimensional Vortex will close in 15 minutes. When it closes, the alliance
					// 将被解散，所有渗透者将被送回。 / will be disbanded and all infiltrators will be returned home.
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_TIMER_15M, 6300000);
					// 次元漩涡将在 10 分钟后关闭。关闭后联盟 / The Dimensional Vortex will close in 10 minutes. When it closes, the alliance
					// 将被解散，所有渗透者将被送回。 / will be disbanded and all infiltrators will be returned home.
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_TIMER_10M, 6600000);
					// 次元漩涡将在 5 分钟后关闭。关闭后联盟 / The Dimensional Vortex will close in 5 minutes. When it closes, the alliance
					// 将被解散，所有渗透者将被送回。 / will be disbanded and all infiltrators will be returned home.
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_TIMER_5M, 6900000);
					// 次元漩涡将在 3 分钟后关闭。关闭后联盟 / The Dimensional Vortex will close in 3 minutes. When it closes, the alliance
					// 将被解散，所有渗透者将被送回。 / will be disbanded and all infiltrators will be returned home.
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_TIMER_3M, 7020000);
					// 次元漩涡将在 2 分钟后关闭。关闭后联盟 / The Dimensional Vortex will close in 2 minutes. When it closes, the alliance
					// 将被解散，所有渗透者将被送回。 / will be disbanded and all infiltrators will be returned home.
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_TIMER_2M, 7080000);
					// 次元漩涡将在 1 分钟后关闭。关闭后联盟 / The Dimensional Vortex will close in 1 minutes. When it closes, the alliance
					// 将被解散，所有渗透者将被送回。 / will be disbanded and all infiltrators will be returned home.
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_TIMER_1M, 7140000);
					// 次元漩涡已关闭，你将被送回 / The Dimensional Vortex has closed, and you will be returned to where you
					// 已进入。 / entered.
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_MSG_INVADE_DIRECT_PORTAL_CLOSE_COMPULSION_TELEPORT, 7200000);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 清除地点已刷出的 NPC。
	 * Despawns NPCs previously spawned at the location.
	 *
	 * location
	 */
	public void despawn(VortexLocation loc) {
		loc.setVortexController(null);
		if (loc.getSpawned() == null) {
			return;
		}
		for (VisibleObject obj : new ArrayList<VisibleObject>(loc.getSpawned())) {
			Npc spawned = (Npc) obj;
			spawned.setDespawnDelayed(true);
			if (spawned.getAggroList().getList().isEmpty()) {
				spawned.getController().cancelTask(TaskId.RESPAWN);
				obj.getController().onDelete();
			}
		}
		loc.getSpawned().clear();
	}

	/**
	 * 判断指定入侵是否进行中。
	 * Checks whether the invasion with the given id is in progress.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否进行中 / whether in progress
	 */
	public boolean isInvasionInProgress(int id) {
		return activeInvasions.containsKey(id);
	}

	/**
	 * 获取进行中的入侵实例映射。
	 * Returns the map of active invasion instances.
	 *
	 * @return 活动实例映射 / active instances map
	 */
	public Map<Integer, DimensionalVortex<?>> getActiveInvasions() {
		return activeInvasions;
	}

	/**
	 * 获取活动持续时长（小时）。
	 * Returns the event duration in hours.
	 *
	 * @return 持续小时数 / duration hours
	 */
	public int getDuration() {
		return CustomConfig.VORTEX_DURATION;
	}

	/**
	 * 从活动入侵中移除防守方玩家。
	 * Removes a defender player from active invasions.
	 *
	 * @param player 玩家 / player
	 */
	public void removeDefenderPlayer(Player player) {
		for (DimensionalVortex<?> invasion : activeInvasions.values()) {
			if (invasion.getDefenders().containsKey(player.getObjectId())) {
				invasion.kickPlayer(player, false);
				return;
			}
		}
	}

	/**
	 * 从活动入侵中移除入侵方玩家。
	 * Removes an invader player from active invasions.
	 *
	 * @param player 玩家 / player
	 */
	public void removeInvaderPlayer(Player player) {
		for (DimensionalVortex<?> invasion : activeInvasions.values()) {
			if (invasion.getInvaders().containsKey(player.getObjectId())) {
				invasion.kickPlayer(player, true);
				return;
			}
		}
	}

	/**
	 * 判断玩家是否为当前入侵方成员。
	 * Checks whether the player is an invader in an active invasion.
	 *
	 * @param player 玩家 / player
	 * @return 是否入侵方 / whether invader
	 */
	public boolean isInvaderPlayer(Player player) {
		for (DimensionalVortex<?> invasion : activeInvasions.values()) {
			if (invasion.getInvaders().containsKey(player.getObjectId())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 判断玩家是否位于漩涡区域内。
	 * Checks whether the player is inside a vortex zone.
	 *
	 * @param player 玩家 / player
	 * @return 是否在区域内 / whether inside zone
	 */
	public boolean isInsideVortexZone(Player player) {
		int playerWorldId = player.getWorldId();
		if (playerWorldId == 210060000 || playerWorldId == 220050000) {
			VortexLocation loc = getLocationByWorld(playerWorldId);
			if (loc != null) {
				return loc.getPlayers().containsKey(player.getObjectId());
			}
		}
		return false;
	}

	/**
	 * 按裂隙 NPC ID 解析对应漩涡地点。
	 * Resolves the vortex location from a rift NPC id.
	 *
	 * rift npc id
	 * location
	 */
	public VortexLocation getLocationByRift(int npcId) {
		return getVortexLocation(npcId == 831073 ? 2 : 1); // Dimensional Vortex.
	}

	/**
	 * 按世界 ID 解析对应漩涡地点。
	 * Resolves the vortex location from a world id.
	 *
	 * 世界 ID / world id
	 * location
	 */
	public VortexLocation getLocationByWorld(int worldId) {
		if (worldId == 210060000) { // Theobomos.
			return getVortexLocation(1);
		} else if (worldId == 220050000) { // Brusthonin.
			return getVortexLocation(2);
		} else {
			return null;
		}
	}

	/**
	 * 按 ID 获取漩涡地点。
	 * Returns the vortex location by id.
	 *
	 * @param id 地点 ID / location id
	 * location
	 */
	public VortexLocation getVortexLocation(int id) {
		return vortex.get(id);
	}

	/**
	 * 获取全部漩涡地点。
	 * Returns all vortex locations.
	 *
	 * locations map
	 */
	public Map<Integer, VortexLocation> getVortexLocations() {
		return vortex;
	}

	/**
	 * 校验登录位置；非法入侵方坐标会被送回家园点。
	 * Validates login position; illegal invader coords are moved to home point.
	 *
	 * @param player 玩家 / player
	 */
	public void validateLoginZone(Player player) {
		VortexLocation loc = getLocationByWorld(player.getWorldId());
		if (loc != null && player.getRace().equals(loc.getInvadersRace())) {
			if (loc.isInsideLocation(player) && loc.isActive()
					&& loc.getVortexController().getPassedPlayers().containsKey(player.getObjectId())) {
				return;
			}
			int mapId = loc.getHomeWorldId();
			float x = loc.getHomePoint().getX();
			float y = loc.getHomePoint().getY();
			float z = loc.getHomePoint().getZ();
			byte h = loc.getHomePoint().getHeading();
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().setPosition(player, mapId, x, y, z, h);
		}
	}

	/**
	 * 获取服务单例（优先 Spring Provider）。
	 * Returns the service singleton (prefers Spring provider).
	 *
	 * service instance
	 */
	public static VortexService getInstance() {
		ObjectProvider<VortexService> provider = instanceProvider;
		if (provider == null) {
			return VortexServiceHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> VortexServiceHolder.INSTANCE);
	}

	/**
	 * 注入 Spring 的实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<VortexService> instanceProvider) {
		VortexService.instanceProvider = instanceProvider;
	}

	private static class VortexServiceHolder {
		private static final VortexService INSTANCE = new VortexService();
	}
}
