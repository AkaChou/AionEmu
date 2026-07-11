package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCronServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.schedule.SvsSchedule;
import com.aionemu.gameserver.configs.schedule.SvsSchedule.Svs;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.svs.SvsLocation;
import com.aionemu.gameserver.model.svs.SvsStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.svsspawns.SvsSpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.svsservice.Gate;
import com.aionemu.gameserver.services.svsservice.Panesterra;
import com.aionemu.gameserver.services.svsservice.SvsStartRunnable;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 战场对决（SvS）服务，管理潘斯特拉等战场开关与刷怪。
 * SvS battlefield service managing Panesterra battlefield open/close and spawns.
 *
 * @author Rinzler (Encom)
 */
@Slf4j

public class SvsService {
	private static volatile ObjectProvider<SvsService> instanceProvider;
	private SvsSchedule svsSchedule;
	private final List<Runnable> scheduledTasks = new ArrayList<>();
	private Map<Integer, SvsLocation> svs;
	// 特兰西迪姆附楼 4.7 / Transidium Annex 4.7
	private Map<Integer, VisibleObject> advanceCorridor = new HashMap<>();
	private final ConcurrentMap<Integer, Panesterra<?>> activeSvs = new ConcurrentHashMap<Integer, Panesterra<?>>();

	/**
	 * 初始化 SvS 地点并按和平状态刷怪。
	 * Initializes SvS locations and spawns them in the peace state.
	 */
	public void initSvsLocations() {
		if (CustomConfig.SVS_ENABLED) {
			svs = DataManager.SVS_DATA.getSvsLocations();
			for (SvsLocation loc : getSvsLocations().values()) {
				spawn(loc, SvsStateType.PEACE);
			}
			log.info(I18n.get("log.52d31014b3d7", svs.size()));
		} else {
			log.info(I18n.get("log.4719a93aa4c6"));
			svs = Collections.emptyMap();
		}
	}

	/**
	 * 初始化 SvS 并加载定时计划。
	 * Initializes SvS and loads the schedule.
	 */
	public void initSvs() {
		if (CustomConfig.SVS_ENABLED) {
			log.info(I18n.get("log.9f1c04e917fb"));
		}
		reloadSchedule();
	}

	/**
	 * 重载 SvS Cron 计划（先取消旧任务）。
	 * Reloads the SvS cron schedule (cancels previous tasks first).
	 */
	public synchronized void reloadSchedule() {
		SvsSchedule newSchedule = CustomConfig.SVS_ENABLED ? SvsSchedule.load() : null;
		scheduledTasks.forEach(GameCronServices.cronService()::cancel);
		scheduledTasks.clear();
		svsSchedule = newSchedule;
		if (svsSchedule != null) {
			for (Svs svs : svsSchedule.getSvssList()) {
				for (String svsTime : svs.getSvsTimes()) {
					Runnable task = new SvsStartRunnable(svs.getId());
					scheduledTasks.add(task);
					GameCronServices.cronService().schedule(task, svsTime);
				}
			}
		}
	}

	/**
	 * 启动指定 ID 的 SvS 战场。
	 * Starts the SvS battlefield for the given id.
	 *
	 * @param id 地点 ID / location id
	 */
	public void startSvs(final int id) {
		Panesterra<?> gate = new Gate(svs.get(id));
		if (activeSvs.putIfAbsent(id, gate) != null) {
			return;
		}
		gate.start();
		advanceCorridorCountdownMsg(id);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				stopSvs(id);
			}
		}, CustomConfig.SVS_DURATION * 3600 * 1000);
	}

	/**
	 * 停止指定 ID 的 SvS 战场。
	 * Stops the SvS battlefield for the given id.
	 *
	 * @param id 地点 ID / location id
	 */
	public void stopSvs(int id) {
		Panesterra<?> gate = activeSvs.remove(id);
		if (gate == null || gate.isFinished()) {
			return;
		}
		distinguishedServiceMsg(id);
		gate.stop();
	}

	/**
	 * 按状态在地点刷出对应 NPC。
	 * Spawns NPCs for the location according to the given state.
	 *
	 * location
	 * state type
	 */
	public void spawn(SvsLocation loc, SvsStateType pstate) {
		if (pstate.equals(SvsStateType.SVS)) {
		}
		List<SpawnGroup2> locSpawns = DataManager.SPAWNS_DATA2.getSvsSpawnsByLocId(loc.getId());
		for (SpawnGroup2 group : locSpawns) {
			for (SpawnTemplate st : group.getSpawnTemplates()) {
				SvsSpawnTemplate svstemplate = (SvsSpawnTemplate) st;
				if (svstemplate.getPStateType().equals(pstate)) {
					loc.getSpawned().add(SpawnEngine.spawnObject(svstemplate, 1));
				}
			}
		}
	}

		/**
	 * 广播前进走廊倒计时系统消息。
	 * Broadcasts Advance Corridor countdown system messages.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean advanceCorridorCountdownMsg(int id) {
		switch (id) {
		case 1:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 通往裂隙传送门战的进阶走廊已出现。 / An Advance Corridor to a Rift Portal battle has appeared.
					PacketSendUtility.playerSendPacketTime(player,
							SM_SYSTEM_MESSAGE.STR_MSG_SVS_INVADE_DIRECT_PORTAL_OPEN, 0);
					// 通往帕内斯特拉要塞战的进阶走廊即将关闭。 / The Advance Corridor leading to the Panesterra Fortress Battle will be closed
					// 10 分钟后。 / in 10 minutes.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Gab1_End01, 3000000);
					// 通往帕内斯特拉要塞战的进阶走廊即将关闭。 / The Advance Corridor leading to the Panesterra Fortress Battle will be closed
					// 5 分钟后。 / in 5 minutes.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Gab1_End02, 3300000);
					// 通往帕内斯特拉要塞战的进阶走廊即将关闭。 / The Advance Corridor leading to the Panesterra Fortress Battle will be closed
					// 1 分钟后。 / in 1 minute.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Gab1_End03, 3540000);
					// 通往帕内斯特拉要塞战的进阶走廊已关闭。 / The Advance Corridor leading to the Panesterra Fortress Battle has closed.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Gab1_End05, 3600000);
				}
			});
			return true;
		default:
			return false;
		}
	}

		/**
	 * 广播殊勋攻城传送门相关系统消息。
	 * Broadcasts Distinguished Service siege portal system messages.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean distinguishedServiceMsg(int id) {
		switch (id) {
		case 1:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 通往帕内斯特拉的功勋攻城传送门已开启。 / The Distinguished Service Siege Portal leading to Panesterra opened.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Gab1_End11, 0);
					// 通往帕内斯特拉攻城的功勋攻城传送门将关闭于 / The Distinguished Service Siege Portal to the Panesterra Siege will close in
					// 5 分钟。 / 5 minutes.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Gab1_End06, 10000);
					// 通往帕内斯特拉攻城的功勋攻城传送门将关闭于 / The Distinguished Service Siege Portal to the Panesterra Siege will close in
					// 3 分钟。 / 3 minutes.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Gab1_End07, 120000);
					// 通往帕内斯特拉攻城的功勋攻城传送门将关闭于 / The Distinguished Service Siege Portal to the Panesterra Siege will close in
					// 1 分钟。 / 1 minute.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Gab1_End08, 240000);
					// 通往帕内斯特拉攻城的功勋攻城传送门已关闭。 / The Distinguished Service Siege Portal to the Panesterra Siege has closed.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_LDF5_Gab1_End10, 300000);
				}
			});
			return true;
		default:
			return false;
		}
	}

		/**
	 * 广播 Transidium Annex 入口相关系统消息。
	 * Broadcasts Transidium Annex entrance system messages.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean transidiumAnnexMsg(int id) {
		switch (id) {
		case 5:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 正在加载进阶走廊护盾……请稍候。 / Loading the Advance Corridor Shield... Please wait.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_GAB1_SUB_ALARM_01, 0);
					// 特兰西迪姆附楼入口将在 8 分钟后开启。 / The entrance to the Transidium Annex will open in 8 minutes.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_GAB1_SUB_ALARM_02, 10000);
					// 特兰西迪姆附楼入口将在 6 分钟后开启。 / The entrance to the Transidium Annex will open in 6 minutes.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_GAB1_SUB_ALARM_03, 120000);
					// 特兰西迪姆附楼入口将在 4 分钟后开启。 / The entrance to the Transidium Annex will open in 4 minutes.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_GAB1_SUB_ALARM_04, 240000);
					// 特兰西迪姆附楼入口将在 2 分钟后开启。 / The entrance to the Transidium Annex will open in 2 minutes.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_GAB1_SUB_ALARM_05, 360000);
					// 特兰西迪姆附楼入口将在 1 分钟后开启。 / The entrance to the Transidium Annex will open in 1 minute.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_GAB1_SUB_ALARM_06, 420000);
					// 特兰西迪姆附楼入口已开启。 / The entrance to the Transidium Annex has opened.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_GAB1_SUB_ALARM_08, 480000);
				}
			});
			return true;
		default:
			return false;
		}
	}

		/**
	 * 刷出前进走廊（Transidium Annex）相关 NPC。
	 * Spawns Advance Corridor (Transidium Annex) related NPCs.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已刷出 / whether spawned
	 */
	public boolean advanceCorridorSP(int id) {
		switch (id) {
		case 5:
			advanceCorridor.put(802219, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400020000, 802219, 1024.12f, 1078.747f, 1530.2688f, (byte) 90),
					1));
			advanceCorridor.put(802221, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400040000, 802221, 1024.12f, 1078.747f, 1530.2688f, (byte) 90),
					1));
			advanceCorridor.put(802223, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400050000, 802223, 1024.12f, 1078.747f, 1530.2688f, (byte) 90),
					1));
			advanceCorridor.put(802225, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400060000, 802225, 1024.12f, 1078.747f, 1530.2688f, (byte) 90),
					1));
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
	public void despawn(SvsLocation loc) {
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
	 * 判断指定 SvS 是否进行中。
	 * Checks whether the SvS with the given id is in progress.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否进行中 / whether in progress
	 */
	public boolean isSvsInProgress(int id) {
		return activeSvs.containsKey(id);
	}

	/**
	 * 获取进行中的 SvS 实例映射。
	 * Returns the map of active SvS instances.
	 *
	 * @return 活动实例映射 / active instances map
	 */
	public Map<Integer, Panesterra<?>> getActiveSvs() {
		return activeSvs;
	}

	/**
	 * 获取活动持续时长（小时）。
	 * Returns the event duration in hours.
	 *
	 * @return 持续小时数 / duration hours
	 */
	public int getDuration() {
		return CustomConfig.SVS_DURATION;
	}

	/**
	 * 按 ID 获取 SvS 地点。
	 * Returns the SvS location by id.
	 *
	 * @param id 地点 ID / location id
	 * location
	 */
	public SvsLocation getSvsLocation(int id) {
		return svs.get(id);
	}

	/**
	 * 获取全部 SvS 地点。
	 * Returns all SvS locations.
	 *
	 * locations map
	 */
	public Map<Integer, SvsLocation> getSvsLocations() {
		return svs;
	}

	/**
	 * 获取服务单例（优先 Spring Provider）。
	 * Returns the service singleton (prefers Spring provider).
	 *
	 * service instance
	 */
	public static SvsService getInstance() {
		ObjectProvider<SvsService> provider = instanceProvider;
		if (provider == null) {
			return SvsServiceHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> SvsServiceHolder.INSTANCE);
	}

	/**
	 * 注入 Spring 的实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<SvsService> instanceProvider) {
		SvsService.instanceProvider = instanceProvider;
	}

	private static class SvsServiceHolder {
		private static final SvsService INSTANCE = new SvsService();
	}
}
