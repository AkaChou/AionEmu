package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCronServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.iu.IuLocation;
import com.aionemu.gameserver.model.iu.IuStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.iuspawns.IuSpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.iuservice.CircusBound;
import com.aionemu.gameserver.services.iuservice.Iu;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 现场演唱会（IU）服务：按计划开启 Live Party Concert Hall，管理传送门刷怪与倒计时消息。
 * IU (Live Party Concert) service: schedule-opens the concert hall and manages portal spawns/countdown.
 *
 * @author Rinzler (Encom)
 */
@Slf4j

public class IuService {
	private static volatile ObjectProvider<IuService> instanceProvider;
	private Map<Integer, IuLocation> iu;
	private final ConcurrentMap<Integer, Iu<?>> activeConcert = new ConcurrentHashMap<Integer, Iu<?>>();

	/**
	 * 加载演唱会地点、刷关闭态 NPC，并注册开启 cron。
	 * Loads concert locations, spawns closed-state NPCs, and registers open cron.
	 */
	public void initConcertLocations() {
		if (CustomConfig.IU_ENABLED) {
			iu = DataManager.IU_DATA.getIuLocations();
			for (IuLocation loc : getIuLocations().values()) {
				spawn(loc, IuStateType.CLOSED);
			}
			log.info(I18n.get("log.521fbac2260d", iu.size()));
			GameCronServices.cronService().schedule(new Runnable() {
				@Override
				public void run() {
					for (IuLocation loc : getIuLocations().values()) {
						startConcert(loc.getId());
					}
					com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player player) {
							PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_EVENT_DIRECT_PORTAL_OPEN);
						}
					});
				}
			}, () -> CustomConfig.IU_SCHEDULE);
		} else {
			//log.info(I18n.get("log.14574ab2d3ba"));
			iu = Collections.emptyMap();
		}
	}

	/**
	 * 记录演唱会功能启用/禁用日志。
	 * Logs whether the concert feature is enabled or disabled.
	 */
	public void initConcert() {
		if (CustomConfig.IU_ENABLED) {
			log.info(I18n.get("log.3f20fa884766"));
		} else {
			log.info(I18n.get("log.14574ab2d3ba"));
		}
	}

	/**
	 * 启动指定 ID 的演唱会，发送倒计时消息，并在持续时长后自动关闭。
	 * Starts the concert for the given id, sends countdown messages, and auto-stops after duration.
	 *
	 * @param id 地点 ID / location id
	 */
	public void startConcert(final int id) {
		Iu<?> circusBound = new CircusBound(iu.get(id));
		if (activeConcert.putIfAbsent(id, circusBound) != null) {
			return;
		}
		circusBound.start();
		lPCHCountdownMsg(id);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				stopConcert(id);
			}
		}, CustomConfig.IU_DURATION * 3600 * 1000);
	}

	/**
	 * 停止指定 ID 的演唱会。
	 * Stops the concert for the given id.
	 *
	 * @param id 地点 ID / location id
	 */
	public void stopConcert(int id) {
		Iu<?> circusBound = activeConcert.remove(id);
		if (circusBound == null || circusBound.isFinished()) {
			return;
		}
		circusBound.stop();
	}

	/**
	 * 按状态类型在地点刷出对应模板 NPC。
	 * Spawns NPCs for the location matching the given state type.
	 *
	 * location
	 * state type
	 */
	public void spawn(IuLocation loc, IuStateType iustate) {
		if (iustate.equals(IuStateType.OPEN)) {
		}
		List<SpawnGroup2> locSpawns = DataManager.SPAWNS_DATA2.getIuSpawnsByLocId(loc.getId());
		for (SpawnGroup2 group : locSpawns) {
			for (SpawnTemplate st : group.getSpawnTemplates()) {
				IuSpawnTemplate iutemplate = (IuSpawnTemplate) st;
				if (iutemplate.getIUStateType().equals(iustate)) {
					loc.getSpawned().add(SpawnEngine.spawnObject(iutemplate, 1));
				}
			}
		}
	}

	/**
	 * 现场演唱会入口关闭倒计时消息（90/60/30/15/10/5/3/2/1 分钟）。
	 * Live Party Concert Hall entrance-close countdown messages (90/60/30/15/10/5/3/2/1 minutes).
	 *
	 * @param id 地点 ID / location id
	 * 若 handled 则为 true / true if handled
	 */
	public boolean lPCHCountdownMsg(int id) {
		switch (id) {
		case 1:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 现场派对音乐厅入口已出现。 / The entrance to the Live Party Concert Hall appeared.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_EVENT_DIRECT_PORTAL_OPEN, 0);
					// 现场派对音乐厅入口将在 90 分钟后关闭，将启动逃离。 / The entrance to the Live Party Concert Hall closes in 90 minutes. Escape will engage.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_EVENT_DIRECT_PORTAL_CLOSE_TIMER_90M, 1800000);
					// 现场派对音乐厅入口将在 60 分钟后关闭，将启动逃离。 / The entrance to the Live Party Concert Hall closes in 60 minutes. Escape will engage.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_EVENT_DIRECT_PORTAL_CLOSE_TIMER_60M, 3600000);
					// 现场派对音乐厅入口将在 30 分钟后关闭，将启动逃离。 / The entrance to the Live Party Concert Hall closes in 30 minutes. Escape will engage.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_EVENT_DIRECT_PORTAL_CLOSE_TIMER_30M, 5400000);
					// 现场派对音乐厅入口将在 15 分钟后关闭，将启动逃离。 / The entrance to the Live Party Concert Hall closes in 15 minutes. Escape will engage.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_EVENT_DIRECT_PORTAL_CLOSE_TIMER_15M, 6300000);
					// 现场派对音乐厅入口将在 10 分钟后关闭，将启动逃离。 / The entrance to the Live Party Concert Hall closes in 10 minutes. Escape will engage.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_EVENT_DIRECT_PORTAL_CLOSE_TIMER_10M, 6600000);
					// 现场派对音乐厅入口将在 5 分钟后关闭，将启动逃离。 / The entrance to the Live Party Concert Hall closes in 5 minutes. Escape will engage.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_EVENT_DIRECT_PORTAL_CLOSE_TIMER_5M, 6900000);
					// 现场派对音乐厅入口将在 3 分钟后关闭，将启动逃离。 / The entrance to the Live Party Concert Hall closes in 3 minutes. Escape will engage.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_EVENT_DIRECT_PORTAL_CLOSE_TIMER_3M, 7020000);
					// 现场派对音乐厅入口将在 2 分钟后关闭，将启动逃离。 / The entrance to the Live Party Concert Hall closes in 2 minutes. Escape will engage.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_EVENT_DIRECT_PORTAL_CLOSE_TIMER_2M, 7080000);
					// 现场派对音乐厅入口将在 1 分钟后关闭，将启动逃离。 / The entrance to the Live Party Concert Hall closes in 1 minutes. Escape will engage.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_EVENT_DIRECT_PORTAL_CLOSE_TIMER_1M, 7140000);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 清除地点上已刷出的对象（无仇恨时立即删除）。
	 * Clears spawned objects at the location (deletes immediately when no aggro).
	 *
	 * location
	 */
	public void despawn(IuLocation loc) {
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
	 * 指定演唱会是否正在进行中。
	 * Whether the concert is in progress for the given id.
	 *
	 * @param id 地点 ID / location id
	 * @return 若 active 则为 true / true if active
	 */
	public boolean isConcertInProgress(int id) {
		return activeConcert.containsKey(id);
	}

	/**
	 * 获取当前激活的演唱会映射。
	 * Returns the map of active concerts.
	 *
	 * @return 激活演唱会 / active concerts
	 */
	public Map<Integer, Iu<?>> getActiveIu() {
		return activeConcert;
	}

	/**
	 * 返回配置的持续时长（小时）。
	 * Returns configured duration in hours.
	 *
	 * @return 持续小时数 / duration hours
	 */
	public int getDuration() {
		return CustomConfig.IU_DURATION;
	}

	/**
	 * 按 ID 获取地点。
	 * Returns the location by id.
	 *
	 * @param id 地点 ID / location id
	 * location
	 */
	public IuLocation getIuLocation(int id) {
		return iu.get(id);
	}

	/**
	 * 获取全部地点。
	 * Returns all locations.
	 *
	 * location map
	 */
	public Map<Integer, IuLocation> getIuLocations() {
		return iu;
	}

	/**
	 * 获取服务单例（优先 Spring ObjectProvider，否则 holder）。
	 * Returns the service singleton (Spring ObjectProvider if set, else holder).
	 *
	 * service instance
	 */
	public static IuService getInstance() {
		ObjectProvider<IuService> provider = instanceProvider;
		if (provider == null) {
			return IuServiceHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> IuServiceHolder.INSTANCE);
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<IuService> instanceProvider) {
		IuService.instanceProvider = instanceProvider;
	}

	private static class IuServiceHolder {
		private static final IuService INSTANCE = new IuService();
	}
}
