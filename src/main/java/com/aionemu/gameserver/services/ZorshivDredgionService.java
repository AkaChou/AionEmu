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
import com.aionemu.gameserver.configs.schedule.DredgionSchedule;
import com.aionemu.gameserver.configs.schedule.DredgionSchedule.Dredgion;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.zorshivdredgionspawns.ZorshivDredgionSpawnTemplate;
import com.aionemu.gameserver.model.zorshivdredgion.ZorshivDredgionLocation;
import com.aionemu.gameserver.model.zorshivdredgion.ZorshivDredgionStateType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.zorshivdredgionservice.DredgionStartRunnable;
import com.aionemu.gameserver.services.zorshivdredgionservice.Zorshiv;
import com.aionemu.gameserver.services.zorshivdredgionservice.ZorshivDredgion;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 佐西夫无畏舰服务，管理无畏舰降落地点、刷怪与入侵特效。
 * Zorshiv Dredgion service managing dredgion landing locations, spawns, and invasion effects.
 *
 * @author Rinzler (Encom)
 */
@Slf4j
public class ZorshivDredgionService {
	private static volatile ObjectProvider<ZorshivDredgionService> instanceProvider;
	private DredgionSchedule dredgionSchedule;
	private final List<Runnable> scheduledTasks = new ArrayList<>();
	private Map<Integer, ZorshivDredgionLocation> zorshivDredgion;
	private final ConcurrentMap<Integer, ZorshivDredgion<?>> activeZorshivDredgion = new ConcurrentHashMap<Integer, ZorshivDredgion<?>>();

	// 英吉斯温入侵 / Inggison Invasion
	private Map<Integer, VisibleObject> adventPortal = new HashMap<>();
	private Map<Integer, VisibleObject> adventEffect = new HashMap<>();
	private Map<Integer, VisibleObject> adventControl = new HashMap<>();
	private Map<Integer, VisibleObject> adventDirecting = new HashMap<>();

	/**
	 * 初始化无畏舰地点并按和平状态刷怪。
	 * Initializes dredgion locations and spawns them in the peace state.
	 */
	public void initZorshivDredgionLocations() {
		if (CustomConfig.ZORSHIV_DREDGION_ENABLED) {
			zorshivDredgion = DataManager.ZORSHIV_DREDGION_DATA.getZorshivDredgionLocations();
			for (ZorshivDredgionLocation loc : getZorshivDredgionLocations().values()) {
				spawn(loc, ZorshivDredgionStateType.PEACE);
			}
			log.info(I18n.get("log.10861add441d", zorshivDredgion.size()));
		} else {
			log.info(I18n.get("log.14c1b1085d6f"));
			zorshivDredgion = Collections.emptyMap();
		}
	}

	/**
	 * 初始化无畏舰并加载定时计划。
	 * Initializes dredgion and loads the schedule.
	 */
	public void initZorshivDredgion() {
		if (CustomConfig.ZORSHIV_DREDGION_ENABLED) {
			log.info(I18n.get("log.7aab3940411b"));
		}
		reloadSchedule();
	}

	/**
	 * 重载无畏舰 Cron 计划（先取消旧任务）。
	 * Reloads the dredgion cron schedule (cancels previous tasks first).
	 */
	public synchronized void reloadSchedule() {
		DredgionSchedule newSchedule = CustomConfig.ZORSHIV_DREDGION_ENABLED ? DredgionSchedule.load() : null;
		scheduledTasks.forEach(GameCronServices.cronService()::cancel);
		scheduledTasks.clear();
		dredgionSchedule = newSchedule;
		if (dredgionSchedule != null) {
			for (Dredgion dredgion : dredgionSchedule.getDredgionsList()) {
				for (String zorshivTime : dredgion.getZorshivTimes()) {
					Runnable task = new DredgionStartRunnable(dredgion.getId());
					scheduledTasks.add(task);
					GameCronServices.cronService().schedule(task, zorshivTime);
				}
			}
		}
	}

	/**
	 * 启动指定 ID 的佐西夫无畏舰活动。
	 * Starts the Zorshiv Dredgion event for the given id.
	 *
	 * @param id 地点 ID / location id
	 */
	public void startZorshivDredgion(final int id) {
		ZorshivDredgion<?> zorshiv = new Zorshiv(zorshivDredgion.get(id));
		if (activeZorshivDredgion.putIfAbsent(id, zorshiv) != null) {
			return;
		}
		zorshiv.start();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				stopZorshivDredgion(id);
			}
		}, CustomConfig.ZORSHIV_DREDGION_DURATION * 3600 * 1000);
	}

	/**
	 * 停止指定 ID 的佐西夫无畏舰活动。
	 * Stops the Zorshiv Dredgion event for the given id.
	 *
	 * @param id 地点 ID / location id
	 */
	public void stopZorshivDredgion(int id) {
		ZorshivDredgion<?> zorshiv = activeZorshivDredgion.remove(id);
		if (zorshiv == null || zorshiv.isPeace()) {
			return;
		}
		zorshiv.stop();
	}

	/**
	 * 按状态在地点刷出对应 NPC。
	 * Spawns NPCs for the location according to the given state.
	 *
	 * location
	 * state type
	 */
	public void spawn(ZorshivDredgionLocation loc, ZorshivDredgionStateType zstate) {
		if (zstate.equals(ZorshivDredgionStateType.LANDING)) {
		}
		List<SpawnGroup2> locSpawns = DataManager.SPAWNS_DATA2.getZorshivDredgionSpawnsByLocId(loc.getId());
		for (SpawnGroup2 group : locSpawns) {
			for (SpawnTemplate st : group.getSpawnTemplates()) {
				ZorshivDredgionSpawnTemplate zorshivDredgiontemplate = (ZorshivDredgionSpawnTemplate) st;
				if (zorshivDredgiontemplate.getZStateType().equals(zstate)) {
					loc.getSpawned().add(SpawnEngine.spawnObject(zorshivDredgiontemplate, 1));
				}
			}
		}
	}

		/**
	 * 广播 Levinshor 无畏舰入侵系统消息。
	 * Broadcasts Levinshor dredgion invasion system messages.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean levinshorMsg(int id) {
		switch (id) {
		case 1:
		case 2:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.sendSys3Message(player, "\uE050",
							"The <Zorshiv Dredgion> to lands at levinshor !!!");
					// 龙族战舰已出现。 / The Balaur Dredgion has appeared.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_FIELDABYSS_CARRIER_SPAWN,
							120000);
					// 战舰投下了龙族士兵。 / The Dredgion has dropped Balaur Troopers.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_FIELDABYSS_CARRIER_DROP_DRAGON,
							300000);
					// 龙族战舰已消失。 / The Balaur Dredgion has disappeared.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_FIELDABYSS_CARRIER_DESPAWN,
							3600000);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播 Inggison 无畏舰入侵系统消息。
	 * Broadcasts Inggison dredgion invasion system messages.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已广播 / whether message was sent
	 */
	public boolean inggisonMsg(int id) {
		switch (id) {
		case 3:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.sendSys3Message(player, "\uE050",
							"The <Zorshiv Dredgion> to lands at inggison !!!");
					// 龙族战舰已出现。 / The Balaur Dredgion has appeared.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_FIELDABYSS_CARRIER_SPAWN,
							120000);
					// 战舰投下了龙族士兵。 / The Dredgion has dropped Balaur Troopers.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_FIELDABYSS_CARRIER_DROP_DRAGON,
							300000);
					// 龙族战舰已消失。 / The Balaur Dredgion has disappeared.
					PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_FIELDABYSS_CARRIER_DESPAWN,
							3600000);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 刷出入侵控制类特效/NPC。
	 * Spawns advent control effect/NPC.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已刷出 / whether spawned
	 */
	public boolean adventControlSP(int id) {
		switch (id) {
		case 3:
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210130000, 702529, 1439.8473f, 407.9271f, 552.26624f, (byte) 78),
					1));
			return true;
		default:
			return false;
		}
	}

	/**
	 * 刷出入侵视觉特效。
	 * Spawns advent visual effect.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已刷出 / whether spawned
	 */
	public boolean adventEffectSP(int id) {
		switch (id) {
		case 3:
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210130000, 702549, 1439.8473f, 407.9271f, 552.26624f, (byte) 78),
					1));
			return true;
		default:
			return false;
		}
	}

	/**
	 * 刷出入侵传送门。
	 * Spawns advent portal.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已刷出 / whether spawned
	 */
	public boolean adventPortalSP(int id) {
		switch (id) {
		case 3:
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210130000, 702550, 1439.8473f, 407.9271f, 552.26624f, (byte) 78),
					1));
			return true;
		default:
			return false;
		}
	}

	/**
	 * 刷出入侵引导/指向特效。
	 * Spawns advent directing effect.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否已刷出 / whether spawned
	 */
	public boolean adventDirectingSP(int id) {
		switch (id) {
		case 3:
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210130000, 855231, 1439.8473f, 407.9271f, 552.26624f, (byte) 78),
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
	public void despawn(ZorshivDredgionLocation loc) {
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
	 * 判断指定无畏舰是否进行中。
	 * Checks whether the dredgion with the given id is in progress.
	 *
	 * @param id 地点 ID / location id
	 * @return 是否进行中 / whether in progress
	 */
	public boolean isZorshivDredgionInProgress(int id) {
		return activeZorshivDredgion.containsKey(id);
	}

	/**
	 * 获取进行中的无畏舰实例映射。
	 * Returns the map of active dredgion instances.
	 *
	 * @return 活动实例映射 / active instances map
	 */
	public Map<Integer, ZorshivDredgion<?>> getActiveZorshivDredgion() {
		return activeZorshivDredgion;
	}

	/**
	 * 获取活动持续时长（小时）。
	 * Returns the event duration in hours.
	 *
	 * @return 持续小时数 / duration hours
	 */
	public int getDuration() {
		return CustomConfig.ZORSHIV_DREDGION_DURATION;
	}

	/**
	 * 按 ID 获取无畏舰地点。
	 * Returns the dredgion location by id.
	 *
	 * @param id 地点 ID / location id
	 * location
	 */
	public ZorshivDredgionLocation getZorshivDredgionLocation(int id) {
		return zorshivDredgion.get(id);
	}

	/**
	 * 获取全部无畏舰地点。
	 * Returns all dredgion locations.
	 *
	 * locations map
	 */
	public Map<Integer, ZorshivDredgionLocation> getZorshivDredgionLocations() {
		return zorshivDredgion;
	}

	/**
	 * 获取服务单例（优先 Spring Provider）。
	 * Returns the service singleton (prefers Spring provider).
	 *
	 * service instance
	 */
	public static ZorshivDredgionService getInstance() {
		ObjectProvider<ZorshivDredgionService> provider = instanceProvider;
		if (provider == null) {
			return ZorshivDredgionServiceHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> ZorshivDredgionServiceHolder.INSTANCE);
	}

	/**
	 * 注入 Spring 的实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<ZorshivDredgionService> instanceProvider) {
		ZorshivDredgionService.instanceProvider = instanceProvider;
	}

	private static class ZorshivDredgionServiceHolder {
		private static final ZorshivDredgionService INSTANCE = new ZorshivDredgionService();
	}
}
