package com.aionemu.gameserver.services;

import lombok.AllArgsConstructor;


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
import com.aionemu.gameserver.configs.schedule.MoltenusSchedule;
import com.aionemu.gameserver.configs.schedule.MoltenusSchedule.Moltenus;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.moltenus.MoltenusLocation;
import com.aionemu.gameserver.model.moltenus.MoltenusStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.moltenusspawns.MoltenusSpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.moltenusservice.Boss;
import com.aionemu.gameserver.services.moltenusservice.MoltenusFight;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 熔岩领主（Moltenus）服务：按 MoltenusSchedule 调度战斗，管理刷怪与欧比斯堡垒公告。
 * Moltenus service: schedule-driven fights via MoltenusSchedule; manages spawns and fortress notices.
 * <p>
 * 参考 / See also: http://aion.power.plaync.com/wiki/%EB%B6%84%EB%85%B8%EC%9D%98+%ED%8C%8C%ED%8E%B8+%EB%A9%94%EB%85%B8%ED%8B%B0%EC%98%A4%EC%8A%A4
 *
 * @author Rinzler (Encom)
 */
@Slf4j

public class MoltenusService {
	private static volatile ObjectProvider<MoltenusService> instanceProvider;
	private MoltenusSchedule moltenusSchedule;
	private final List<Runnable> scheduledTasks = new ArrayList<>();
	private Map<Integer, MoltenusLocation> moltenus;
	private final ConcurrentMap<Integer, MoltenusFight<?>> activeMoltenus = new ConcurrentHashMap<>();

	/**
	 * 加载熔岩领主地点并刷和平态 NPC。
	 * Loads Moltenus locations and spawns peace-state NPCs.
	 */
	public void initMoltenusLocations() {
		if (CustomConfig.MOLTENUS_ENABLED) {
			moltenus = DataManager.MOLTENUS_DATA.getMoltenusLocations();
			for (MoltenusLocation loc : getMoltenusLocations().values()) {
				spawn(loc, MoltenusStateType.PEACE);
			}
			log.info(I18n.get("log.81dabfa11dc2", moltenus.size()));

		} else {
			log.info(I18n.get("log.681bddb050f1"));
			moltenus = Collections.emptyMap();
		}
	}

	/**
	 * 初始化并（重）加载战斗 cron 调度。
	 * Initializes and (re)loads fight cron schedules.
	 */
	public void initMoltenus() {
		if (CustomConfig.MOLTENUS_ENABLED) {
			log.info(I18n.get("log.67f190aa0332"));
		}
		reloadSchedule();
	}

	/**
	 * 重新加载 MoltenusSchedule：取消旧任务并注册新 cron。
	 * Reloads MoltenusSchedule: cancels old tasks and registers new crons.
	 */
	public synchronized void reloadSchedule() {
		MoltenusSchedule newSchedule = CustomConfig.MOLTENUS_ENABLED ? MoltenusSchedule.load() : null;
		scheduledTasks.forEach(GameCronServices.cronService()::cancel);
		scheduledTasks.clear();
		moltenusSchedule = newSchedule;
		if (moltenusSchedule != null) {
			for (Moltenus moltenus : moltenusSchedule.getMoltenussList()) {
				for (String fightTime : moltenus.getFightTimes()) {
					Runnable task = new MoltenusStartRunnable(moltenus.getId());
					scheduledTasks.add(task);
					GameCronServices.cronService().schedule(task, fightTime);
				}
			}
		}
	}

	/**
	 * 启动指定 ID 的熔岩领主战斗，广播消息并在持续时长后自动结束。
	 * Starts the Moltenus fight for the given id, broadcasts, and auto-stops after duration.
	 *
	 * @param id 地点 ID / location id
	 */
	public void startMoltenus(final int id) {
		MoltenusFight<?> boss = new Boss(moltenus.get(id));
		if (activeMoltenus.putIfAbsent(id, boss) != null) {
			return;
		}
		boss.start();
		moltenusMsg(id);
		GameThreadPoolServices.threadPoolManager().schedule(() -> stopMoltenus(id), (long) CustomConfig.MOLTENUS_DURATION * 3600 * 1000);
	}

	/**
	 * 停止指定 ID 的熔岩领主战斗。
	 * Stops the Moltenus fight for the given id.
	 *
	 * @param id 地点 ID / location id
	 */
	public void stopMoltenus(int id) {
		MoltenusFight<?> boss = activeMoltenus.remove(id);
		if (boss == null || boss.isFinished()) {
			return;
		}
		boss.stop();
	}

	/**
	 * 按状态类型在地点刷出对应模板 NPC。
	 * Spawns NPCs for the location matching the given state type.
	 *
	 * @param loc location
	 * @param mstate state type
	 */
	public void spawn(MoltenusLocation loc, MoltenusStateType mstate) {
		List<SpawnGroup2> locSpawns = DataManager.SPAWNS_DATA2.getMoltenusSpawnsByLocId(loc.getId());
		for (SpawnGroup2 group : locSpawns) {
			for (SpawnTemplate st : group.getSpawnTemplates()) {
				MoltenusSpawnTemplate moltenustemplate = (MoltenusSpawnTemplate) st;
				if (moltenustemplate.getMStateType().equals(mstate)) {
					loc.getSpawned().add(SpawnEngine.spawnObject(moltenustemplate, 1));
				}
			}
		}
	}

	/**
	 * 向全体玩家广播复活熔岩领主出现消息。
	 * Broadcasts resurrected-Moltenus appearance message to all players.
	 *
	 * @param id 地点 ID / location id
	 * @return 若 handled 则为 true / true if handled
	 */
	public boolean moltenusMsg(int id) {
		return switch (id) {
			case 1 -> {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(player -> PacketSendUtility.sendSys3Message(player, "\uE005",
					"<Resurrected Moltenus> appear in the abyss !!!"));
				yield true;
			}
			default -> false;
		};
	}

	/**
	 * 硫磺堡垒守护者即将出现的预告消息。
	 * Pre-spawn notice for Enraged Sulfur Guardian.
	 *
	 * @param id 地点 ID / location id
	 * @return 若 handled 则为 true / true if handled
	 */
	public boolean sulfurFortressMsg(int id) {
		return switch (id) {
			case 4, 7 -> {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(player -> {
					// 暴怒的硫磺守护者将在 10 分钟后出现。 / Enraged Sulfur Guardian will appear in 10 minutes.
					PacketSendUtility.playerSendPacketTime(player,
						SM_SYSTEM_MESSAGE.STR_Ab1_BossNamed_65_Al_Spawnmsg_01, 0);
				});
				yield true;
			}
			default -> false;
		};
	}

	/**
	 * 西部堡垒守护者即将出现的预告消息。
	 * Pre-spawn notice for Enraged Western Guardian.
	 *
	 * @param id 地点 ID / location id
	 * @return 若 handled 则为 true / true if handled
	 */
	public boolean westernFortressMsg(int id) {
		return switch (id) {
			case 5, 8 -> {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(player -> {
					// 暴怒的西部守护者将在 10 分钟后出现。 / Enraged Western Guardian will appear in 10 minutes.
					PacketSendUtility.playerSendPacketTime(player,
						SM_SYSTEM_MESSAGE.STR_Ab1_BossNamed_65_Al_Spawnmsg_02, 10000);
				});
				yield true;
			}
			default -> false;
		};
	}

	/**
	 * 东部堡垒守护者即将出现的预告消息。
	 * Pre-spawn notice for Enraged Eastern Guardian.
	 *
	 * @param id 地点 ID / location id
	 * @return 若 handled 则为 true / true if handled
	 */
	public boolean easternFortressMsg(int id) {
		return switch (id) {
			case 6, 9 -> {
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(player -> {
					// 暴怒的东部守护者将在 10 分钟后出现。 / Enraged Eastern Guardian will appear in 10 minutes.
					PacketSendUtility.playerSendPacketTime(player,
						SM_SYSTEM_MESSAGE.STR_Ab1_BossNamed_65_Al_Spawnmsg_03, 20000);
				});
				yield true;
			}
			default -> false;
		};
	}

	/**
	 * 清除地点上已刷出的对象（无仇恨时立即删除）。
	 * Clears spawned objects at the location (deletes immediately when no aggro).
	 *
	 * @param loc location
	 */
	public void despawn(MoltenusLocation loc) {
		if (loc.getSpawned() == null) {
			return;
		}
		for (VisibleObject obj : new ArrayList<>(loc.getSpawned())) {
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
	 * 指定熔岩领主战斗是否正在进行中。
	 * Whether the Moltenus fight is in progress for the given id.
	 *
	 * @param id 地点 ID / location id
	 * @return 若 active 则为 true / true if active
	 */
	public boolean isMoltenusInProgress(int id) {
		return activeMoltenus.containsKey(id);
	}

	/**
	 * 获取当前激活的熔岩领主战斗映射。
	 * Returns the map of active Moltenus fights.
	 *
	 * active fights
	 */
	public Map<Integer, MoltenusFight<?>> getActiveMoltenus() {
		return activeMoltenus;
	}

	/**
	 * 返回配置的持续时长（小时）。
	 * Returns configured duration in hours.
	 *
	 * @return 持续小时数 / duration hours
	 */
	public int getDuration() {
		return CustomConfig.MOLTENUS_DURATION;
	}

	/**
	 * 按 ID 获取地点。
	 * Returns the location by id.
	 *
	 * @param id 地点 ID / location id
	 * location
	 */
	public MoltenusLocation getMoltenusLocation(int id) {
		return moltenus.get(id);
	}

	/**
	 * 获取全部地点。
	 * Returns all locations.
	 *
	 * location map
	 */
	public Map<Integer, MoltenusLocation> getMoltenusLocations() {
		return moltenus;
	}

	/**
	 * 获取服务单例（优先 Spring ObjectProvider，否则 holder）。
	 * Returns the service singleton (Spring ObjectProvider if set, else holder).
	 *
	 * service instance
	 */
	public static MoltenusService getInstance() {
		ObjectProvider<MoltenusService> provider = instanceProvider;
		if (provider == null) {
			return MoltenusServiceHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> MoltenusServiceHolder.INSTANCE);
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<MoltenusService> instanceProvider) {
		MoltenusService.instanceProvider = instanceProvider;
	}

	private static class MoltenusServiceHolder {
		private static final MoltenusService INSTANCE = new MoltenusService();
	}

	/**
	 * 熔岩领主（Moltenus）活动启动定时任务。
	 * Start runnable for the Moltenus world event.
	 */
	@AllArgsConstructor
	private static class MoltenusStartRunnable implements Runnable {
		private final int id;

		@Override
		public void run() {
			// 暴怒的硫磺守护者将在 10 分钟后出现。 / Enraged Sulfur Guardian will appear in 10 minutes.
			MoltenusService.getInstance().sulfurFortressMsg(id);
			// 暴怒的西部守护者将在 10 分钟后出现。 / Enraged Western Guardian will appear in 10 minutes.
			MoltenusService.getInstance().westernFortressMsg(id);
			// 暴怒的东部守护者将在 10 分钟后出现。 / Enraged Eastern Guardian will appear in 10 minutes.
			MoltenusService.getInstance().easternFortressMsg(id);
			GameThreadPoolServices.threadPoolManager().schedule(() -> {
				Map<Integer, MoltenusLocation> locations = MoltenusService.getInstance().getMoltenusLocations();
				for (final MoltenusLocation loc : locations.values()) {
					if (loc.getId() == id) {
						MoltenusService.getInstance().startMoltenus(loc.getId());
					}
				}
			}, 600000);
		}
	}
}
