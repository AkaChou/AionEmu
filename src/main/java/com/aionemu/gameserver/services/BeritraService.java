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
import com.aionemu.gameserver.configs.schedule.BeritraSchedule;
import com.aionemu.gameserver.configs.schedule.BeritraSchedule.Beritra;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.beritra.BeritraLocation;
import com.aionemu.gameserver.model.beritra.BeritraStateType;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.beritraspawns.BeritraSpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.beritraservice.BeritraInvasion;
import com.aionemu.gameserver.services.beritraservice.BeritraStartRunnable;
import com.aionemu.gameserver.services.beritraservice.Invade;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 贝里特拉/艾雷什基伽尔入侵（Beritra / Ereshkigal Invasion）世界活动服务。
 * Service for Beritra and Ereshkigal world-invasion events (schedule, spawn, SP effects, messages).
 *
 * @author Rinzler (Encom)
 */
@Slf4j

public class BeritraService {
	private static volatile ObjectProvider<BeritraService> instanceProvider;
	private BeritraSchedule beritraSchedule;
	private final List<Runnable> scheduledTasks = new ArrayList<>();
	private Map<Integer, BeritraLocation> beritra;
	// 贝里特拉入侵 4.7 / Beritra Invasion 4.7
	private Map<Integer, VisibleObject> adventPortal = new HashMap<>();
	private Map<Integer, VisibleObject> adventEffect = new HashMap<>();
	private Map<Integer, VisibleObject> adventControl = new HashMap<>();
	private Map<Integer, VisibleObject> adventDirecting = new HashMap<>();
	// 埃雷什基伽尔入侵 4.9 / Ereshkigal Invasion 4.9
	private Map<Integer, VisibleObject> adventEreshPortal = new HashMap<>();
	private Map<Integer, VisibleObject> adventEreshEffect = new HashMap<>();
	private Map<Integer, VisibleObject> adventEreshControl = new HashMap<>();
	private Map<Integer, VisibleObject> adventEreshDirecting = new HashMap<>();

	private final ConcurrentMap<Integer, BeritraInvasion<?>> activeInvasions = new ConcurrentHashMap<Integer, BeritraInvasion<?>>();

	/**
	 * 初始化入侵活动地点：按配置加载并在和平状态刷怪。
	 * Initialize invasion locations: load data and spawn peace-state NPCs when enabled.
	 */
	public void initBeritraLocations() {
		if (CustomConfig.BERITRA_ENABLED) {
			beritra = DataManager.BERITRA_DATA.getBeritraLocations();
			for (BeritraLocation loc : getBeritraLocations().values()) {
				spawn(loc, BeritraStateType.PEACE);
			}
			log.info(I18n.get("log.6162acf484de", beritra.size()));
		} else {
			beritra = Collections.emptyMap();
		}
	}

	/**
	 * 初始化贝里特拉入侵并装载 cron 调度。
	 * Initialize the Beritra invasion and load its cron schedule.
	 */
	public void initBeritra() {
		if (CustomConfig.BERITRA_ENABLED) {
			log.info(I18n.get("log.8317eae99b1f"));
		}
		reloadSchedule();
	}

	/**
	 * 重新加载入侵时间表：取消旧任务并按新 cron 注册。
	 * Reload the invasion schedule: cancel old tasks and re-register from cron.
	 */
	public synchronized void reloadSchedule() {
		BeritraSchedule newSchedule = CustomConfig.BERITRA_ENABLED ? BeritraSchedule.load() : null;
		scheduledTasks.forEach(GameCronServices.cronService()::cancel);
		scheduledTasks.clear();
		beritraSchedule = newSchedule;
		if (beritraSchedule != null) {
			for (Beritra beritra : beritraSchedule.getBeritrasList()) {
				for (String invasionTime : beritra.getInvasionTimes()) {
					Runnable task = new BeritraStartRunnable(beritra.getId());
					scheduledTasks.add(task);
					GameCronServices.cronService().schedule(task, invasionTime);
				}
			}
		}
	}

	/**
	 * 启动指定地点的入侵，并在持续时长结束后自动停止。
	 * Start the invasion at the given location and auto-stop after the configured duration.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 */
	public void startBeritraInvasion(final int id) {
		BeritraInvasion<?> invade = new Invade(beritra.get(id));
		if (activeInvasions.putIfAbsent(id, invade) != null) {
			return;
		}
		invade.start();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				stopBeritraInvasion(id);
			}
		}, CustomConfig.BERITRA_DURATION * 3600 * 1000);
	}

	/**
	 * 停止指定地点的入侵。
	 * Stop the invasion at the given location.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 */
	public void stopBeritraInvasion(int id) {
		BeritraInvasion<?> invade = activeInvasions.remove(id);
		if (invade == null || invade.isFinished()) {
			return;
		}
		invade.stop();
		devilUnitReturnMsg(id);
		beritraLegionReturnMsg(id);
	}

	/**
	 * 按状态刷出入侵相关 NPC。
	 * Spawn invasion NPCs for the given location and state.
	 *
	 * @param loc 入侵地点 / beritra location
	 * spawn state
	 */
	public void spawn(BeritraLocation loc, BeritraStateType bstate) {
		if (bstate.equals(BeritraStateType.INVASION)) {
		}
		List<SpawnGroup2> locSpawns = DataManager.SPAWNS_DATA2.getBeritraSpawnsByLocId(loc.getId());
		for (SpawnGroup2 group : locSpawns) {
			for (SpawnTemplate st : group.getSpawnTemplates()) {
				BeritraSpawnTemplate beritratemplate = (BeritraSpawnTemplate) st;
				if (beritratemplate.getBStateType().equals(bstate)) {
					loc.getSpawned().add(SpawnEngine.spawnObject(beritratemplate, 1));
				}
			}
		}
	}

		/**
	 * 广播贝里特拉入侵系统消息。
	 * Broadcast Beritra invasion system messages.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean beritraInvasionMsg(int id) {
		switch (id) {
		case 1:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_WORLDRAID_INVADE_VRITRA_SPECIAL);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播入侵走廊相关消息。
	 * Broadcast invasion-corridor messages.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean invasionCorridorMsg(int id) {
		switch (id) {
		case 1:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 贝里特拉军团入侵走廊已出现。 / The Beritra Legion's Invasion Corridor has appeared.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_WORLDRAID_MESSAGE_01);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播魔族部队通过消息。
	 * Broadcast devil-unit through messages.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean devilUnitThroughMsg(int id) {
		switch (id) {
		case 1:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 恶魔部队已通过入侵走廊渗透。 / The Devil Unit has infiltrated through the Invasion Corridor.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_WORLDRAID_MESSAGE_02);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播魔族部队撤退消息。
	 * Broadcast devil-unit return messages.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean devilUnitReturnMsg(int id) {
		switch (id) {
		case 1:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 恶魔部队正准备返回。 / The Devil Unit is preparing for its return.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_WORLDRAID_MESSAGE_03);
				}
			});
			return true;
		default:
			return false;
		}
	}

		/**
	 * 广播艾雷什基伽尔入侵消息。
	 * Broadcast Ereshkigal invasion messages.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean ereshkigalInvasionMsg(int id) {
		switch (id) {
		case 35:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_WORLDRAID_INVADE_VRITRA_SPECIAL);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播艾雷什基伽尔走廊消息。
	 * Broadcast Ereshkigal corridor messages.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean ereshkigalCorridorMsg(int id) {
		switch (id) {
		case 35:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 埃雷什基伽尔军团入侵走廊已创建。 / The Ereshkigal Legion's Invasion Corridor has been created.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_WORLDRAID_Ere_MESSAGE_01);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播艾雷什基伽尔军团通过消息。
	 * Broadcast Ereshkigal legion through messages.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean ereshkigalLegionThroughMsg(int id) {
		switch (id) {
		case 35:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 埃雷什基伽尔军团的魔法武器已通过入侵 / The Ereshkigal Legion's Magic weapon has infiltrated through the Invasion
					// 走廊。 / Corridor.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_WORLDRAID_Ere_MESSAGE_02);
				}
			});
			return true;
		default:
			return false;
		}
	}

	/**
	 * 广播贝里特拉军团撤退消息。
	 * Broadcast Beritra legion return messages.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean beritraLegionReturnMsg(int id) {
		switch (id) {
		case 35:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					// 贝里特拉军团恶魔部队正准备返回。 / The Beritra Legion Devil Unit is preparing for its return.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_WORLDRAID_MESSAGE_03);
				}
			});
			return true;
		default:
			return false;
		}
	}

		/**
	 * 广播无舰防御相关消息。
	 * Broadcast dredgion-defense messages.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean dredgionDefenseMsg(int id) {
		switch (id) {
		case 57:
			com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
				@Override
				public void visit(Player player) {
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Dreadgion_Start_L);
				}
			});
			return true;
		default:
			return false;
		}
	}

		/**
	 * 刷出贝里特拉降临控制特效 NPC。
	 * Spawn Beritra advent-control SP NPCs.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean adventControlSP(int id) {
		switch (id) {
		case 1:
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210020000, 702529, 858.5479f, 1151.3783f, 278.46576f, (byte) 71),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210020000, 702529, 1519.0f, 1911.0f, 289.5f, (byte) 10), 1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210020000, 702529, 260.20285f, 2134.1099f, 207.375f, (byte) 9),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210030000, 702529, 1586.9154f, 2078.2305f, 155.875f, (byte) 66),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210030000, 702529, 1702.6613f, 1662.9213f, 102.19242f, (byte) 64),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210030000, 702529, 2485.3333f, 824.3736f, 100.625f, (byte) 56),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210040000, 702529, 382.0f, 2929.0f, 100.25f, (byte) 42), 1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210040000, 702529, 1470.0549f, 1890.8654f, 106.22974f, (byte) 7),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210040000, 702529, 540.073f, 2107.656f, 103.375f, (byte) 107),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210050000, 702529, 2762.5203f, 830.4615f, 383.87866f, (byte) 58),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210050000, 702529, 1929.1307f, 1953.1182f, 289.32068f, (byte) 64),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210050000, 702529, 2729.864f, 1890.5359f, 189.625f, (byte) 39),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210060000, 702529, 2429.1567f, 2619.0974f, 40.25f, (byte) 40),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210060000, 702529, 555.4294f, 2231.6064f, 44.089336f, (byte) 71),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210060000, 702529, 1771.0f, 1356.0f, 18.125f, (byte) 34), 1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210070000, 702529, 593.9777f, 481.57568f, 416.42203f, (byte) 60),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210070000, 702529, 2943.966f, 2272.9531f, 231.43457f, (byte) 32),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210070000, 702529, 1429.0f, 1949.0f, 138.5625f, (byte) 27), 1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220020000, 702529, 400.68732f, 1715.6395f, 441.6271f, (byte) 20),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220020000, 702529, 580.48895f, 355.1831f, 485.2271f, (byte) 61),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220030000, 702529, 2895.708f, 1516.8243f, 250.65457f, (byte) 70),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220030000, 702529, 1761.3639f, 506.55444f, 247.3006f, (byte) 53),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220030000, 702529, 1870.0f, 1675.0f, 247.375f, (byte) 53), 1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220040000, 702529, 780.0f, 1240.0f, 224.0f, (byte) 76), 1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220040000, 702529, 2565.876f, 286.7999f, 287.49225f, (byte) 18),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220040000, 702529, 465.72607f, 1786.9429f, 206.01352f, (byte) 3),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220050000, 702529, 2473.5154f, 1896.5199f, 23.560577f, (byte) 19),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220050000, 702529, 335.0f, 370.0f, 5.25f, (byte) 83), 1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220050000, 702529, 417.68674f, 2836.982f, 245.81363f, (byte) 49),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220070000, 702529, 591.8911f, 1341.8286f, 276.875f, (byte) 119),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220070000, 702529, 1488.8247f, 1256.1757f, 298.05154f, (byte) 46),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220070000, 702529, 2186.1123f, 922.95953f, 186.69003f, (byte) 32),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220080000, 702529, 2651.8286f, 2716.1633f, 202.89534f, (byte) 80),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(SpawnEngine.addNewSingleTimeSpawn(220080000, 702529,
					192.61913f, 527.22363f, 196.70428f, (byte) 103), 1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220080000, 702529, 1583.1567f, 1106.1504f, 132.79529f, (byte) 87),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 702529, 2020.6847f, 2832.924f, 2830.972f, (byte) 51),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 702529, 820.0f, 865.0f, 1671.1095f, (byte) 83), 1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 702529, 2280.3706f, 870.72766f, 2831.548f, (byte) 98),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 702529, 3332.5474f, 1371.9772f, 2666.258f, (byte) 89),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 702529, 1123.5983f, 2096.5151f, 2886.9402f, (byte) 96),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600090000, 702529, 656.06f, 808.15f, 165.125f, (byte) 78), 1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600090000, 702529, 289.80325f, 506.02426f, 158.125f, (byte) 114),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600090000, 702529, 1182.1178f, 348.7145f, 128.5f, (byte) 76), 1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600100000, 702529, 1155.9978f, 1075.4766f, 303.375f, (byte) 104),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600100000, 702529, 675.446f, 1001.7693f, 274.59036f, (byte) 66),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600100000, 702529, 386.59848f, 1810.1382f, 226.42104f, (byte) 89),
					1));
			adventControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600100000, 702529, 1836.0f, 142.0f, 242.625f, (byte) 86), 1));
			return true;
		default:
			return false;
		}
	}

	/**
	 * 刷出贝里特拉降临效果特效 NPC。
	 * Spawn Beritra advent-effect SP NPCs.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean adventEffectSP(int id) {
		switch (id) {
		case 1:
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210020000, 702549, 858.5479f, 1151.3783f, 278.46576f, (byte) 71),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210020000, 702549, 1519.0f, 1911.0f, 289.5f, (byte) 10), 1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210020000, 702549, 260.20285f, 2134.1099f, 207.375f, (byte) 9),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210030000, 702549, 1586.9154f, 2078.2305f, 155.875f, (byte) 66),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210030000, 702549, 1702.6613f, 1662.9213f, 102.19242f, (byte) 64),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210030000, 702549, 2485.3333f, 824.3736f, 100.625f, (byte) 56),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210040000, 702549, 382.0f, 2929.0f, 100.25f, (byte) 42), 1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210040000, 702549, 1470.0549f, 1890.8654f, 106.22974f, (byte) 7),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210040000, 702549, 540.073f, 2107.656f, 103.375f, (byte) 107),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210050000, 702549, 2762.5203f, 830.4615f, 383.87866f, (byte) 58),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210050000, 702549, 1929.1307f, 1953.1182f, 289.32068f, (byte) 64),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210050000, 702549, 2729.864f, 1890.5359f, 189.625f, (byte) 39),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210060000, 702549, 2429.1567f, 2619.0974f, 40.25f, (byte) 40),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210060000, 702549, 555.4294f, 2231.6064f, 44.089336f, (byte) 71),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210060000, 702549, 1771.0f, 1356.0f, 18.125f, (byte) 34), 1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210070000, 702549, 593.9777f, 481.57568f, 416.42203f, (byte) 60),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210070000, 702549, 2943.966f, 2272.9531f, 231.43457f, (byte) 32),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210070000, 702549, 1429.0f, 1949.0f, 138.5625f, (byte) 27), 1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220020000, 702549, 400.68732f, 1715.6395f, 441.6271f, (byte) 20),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220020000, 702549, 580.48895f, 355.1831f, 485.2271f, (byte) 61),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220030000, 702549, 2895.708f, 1516.8243f, 250.65457f, (byte) 70),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220030000, 702549, 1761.3639f, 506.55444f, 247.3006f, (byte) 53),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220030000, 702549, 1870.0f, 1675.0f, 247.375f, (byte) 53), 1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220040000, 702549, 780.0f, 1240.0f, 224.0f, (byte) 76), 1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220040000, 702549, 2565.876f, 286.7999f, 287.49225f, (byte) 18),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220040000, 702549, 465.72607f, 1786.9429f, 206.01352f, (byte) 3),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220050000, 702549, 2473.5154f, 1896.5199f, 23.560577f, (byte) 19),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220050000, 702549, 335.0f, 370.0f, 5.25f, (byte) 83), 1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220050000, 702549, 417.68674f, 2836.982f, 245.81363f, (byte) 49),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220070000, 702549, 591.8911f, 1341.8286f, 276.875f, (byte) 119),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220070000, 702549, 1488.8247f, 1256.1757f, 298.05154f, (byte) 46),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220070000, 702549, 2186.1123f, 922.95953f, 186.69003f, (byte) 32),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220080000, 702549, 2651.8286f, 2716.1633f, 202.89534f, (byte) 80),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(SpawnEngine.addNewSingleTimeSpawn(220080000, 702549,
					192.61913f, 527.22363f, 196.70428f, (byte) 103), 1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220080000, 702549, 1583.1567f, 1106.1504f, 132.79529f, (byte) 87),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 702549, 2020.6847f, 2832.924f, 2830.972f, (byte) 51),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 702549, 820.0f, 865.0f, 1671.1095f, (byte) 83), 1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 702549, 2280.3706f, 870.72766f, 2831.548f, (byte) 98),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 702549, 3332.5474f, 1371.9772f, 2666.258f, (byte) 89),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 702549, 1123.5983f, 2096.5151f, 2886.9402f, (byte) 96),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600090000, 702549, 656.06f, 808.15f, 165.125f, (byte) 78), 1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600090000, 702549, 289.80325f, 506.02426f, 158.125f, (byte) 114),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600090000, 702549, 1182.1178f, 348.7145f, 128.5f, (byte) 76), 1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600100000, 702549, 1155.9978f, 1075.4766f, 303.375f, (byte) 104),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600100000, 702549, 675.446f, 1001.7693f, 274.59036f, (byte) 66),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600100000, 702549, 386.59848f, 1810.1382f, 226.42104f, (byte) 89),
					1));
			adventEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600100000, 702549, 1836.0f, 142.0f, 242.625f, (byte) 86), 1));
			return true;
		default:
			return false;
		}
	}

	/**
	 * 刷出贝里特拉降临传送门特效 NPC。
	 * Spawn Beritra advent-portal SP NPCs.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean adventPortalSP(int id) {
		switch (id) {
		case 1:
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210020000, 702550, 858.5479f, 1151.3783f, 278.46576f, (byte) 71),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210020000, 702550, 1519.0f, 1911.0f, 289.5f, (byte) 10), 1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210020000, 702550, 260.20285f, 2134.1099f, 207.375f, (byte) 9),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210030000, 702550, 1586.9154f, 2078.2305f, 155.875f, (byte) 66),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210030000, 702550, 1702.6613f, 1662.9213f, 102.19242f, (byte) 64),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210030000, 702550, 2485.3333f, 824.3736f, 100.625f, (byte) 56),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210040000, 702550, 382.0f, 2929.0f, 100.25f, (byte) 42), 1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210040000, 702550, 1470.0549f, 1890.8654f, 106.22974f, (byte) 7),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210040000, 702550, 540.073f, 2107.656f, 103.375f, (byte) 107),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210050000, 702550, 2762.5203f, 830.4615f, 383.87866f, (byte) 58),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210050000, 702550, 1929.1307f, 1953.1182f, 289.32068f, (byte) 64),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210050000, 702550, 2729.864f, 1890.5359f, 189.625f, (byte) 39),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210060000, 702550, 2429.1567f, 2619.0974f, 40.25f, (byte) 40),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210060000, 702550, 555.4294f, 2231.6064f, 44.089336f, (byte) 71),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210060000, 702550, 1771.0f, 1356.0f, 18.125f, (byte) 34), 1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210070000, 702550, 593.9777f, 481.57568f, 416.42203f, (byte) 60),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210070000, 702550, 2943.966f, 2272.9531f, 231.43457f, (byte) 32),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210070000, 702550, 1429.0f, 1949.0f, 138.5625f, (byte) 27), 1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220020000, 702550, 400.68732f, 1715.6395f, 441.6271f, (byte) 20),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220020000, 702550, 580.48895f, 355.1831f, 485.2271f, (byte) 61),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220030000, 702550, 2895.708f, 1516.8243f, 250.65457f, (byte) 70),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220030000, 702550, 1761.3639f, 506.55444f, 247.3006f, (byte) 53),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220030000, 702550, 1870.0f, 1675.0f, 247.375f, (byte) 53), 1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220040000, 702550, 780.0f, 1240.0f, 224.0f, (byte) 76), 1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220040000, 702550, 2565.876f, 286.7999f, 287.49225f, (byte) 18),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220040000, 702550, 465.72607f, 1786.9429f, 206.01352f, (byte) 3),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220050000, 702550, 2473.5154f, 1896.5199f, 23.560577f, (byte) 19),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220050000, 702550, 335.0f, 370.0f, 5.25f, (byte) 83), 1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220050000, 702550, 417.68674f, 2836.982f, 245.81363f, (byte) 49),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220070000, 702550, 591.8911f, 1341.8286f, 276.875f, (byte) 119),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220070000, 702550, 1488.8247f, 1256.1757f, 298.05154f, (byte) 46),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220070000, 702550, 2186.1123f, 922.95953f, 186.69003f, (byte) 32),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220080000, 702550, 2651.8286f, 2716.1633f, 202.89534f, (byte) 80),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(SpawnEngine.addNewSingleTimeSpawn(220080000, 702550,
					192.61913f, 527.22363f, 196.70428f, (byte) 103), 1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220080000, 702550, 1583.1567f, 1106.1504f, 132.79529f, (byte) 87),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 702550, 2020.6847f, 2832.924f, 2830.972f, (byte) 51),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 702550, 820.0f, 865.0f, 1671.1095f, (byte) 83), 1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 702550, 2280.3706f, 870.72766f, 2831.548f, (byte) 98),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 702550, 3332.5474f, 1371.9772f, 2666.258f, (byte) 89),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 702550, 1123.5983f, 2096.5151f, 2886.9402f, (byte) 96),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600090000, 702550, 656.06f, 808.15f, 165.125f, (byte) 78), 1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600090000, 702550, 289.80325f, 506.02426f, 158.125f, (byte) 114),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600090000, 702550, 1182.1178f, 348.7145f, 128.5f, (byte) 76), 1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600100000, 702550, 1155.9978f, 1075.4766f, 303.375f, (byte) 104),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600100000, 702550, 675.446f, 1001.7693f, 274.59036f, (byte) 66),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600100000, 702550, 386.59848f, 1810.1382f, 226.42104f, (byte) 89),
					1));
			adventPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600100000, 702550, 1836.0f, 142.0f, 242.625f, (byte) 86), 1));
			return true;
		default:
			return false;
		}
	}

	/**
	 * 刷出贝里特拉降临导向特效 NPC。
	 * Spawn Beritra advent-directing SP NPCs.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean adventDirectingSP(int id) {
		switch (id) {
		case 1:
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210020000, 855231, 858.5479f, 1151.3783f, 278.46576f, (byte) 71),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210020000, 855231, 1519.0f, 1911.0f, 289.5f, (byte) 10), 1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210020000, 855231, 260.20285f, 2134.1099f, 207.375f, (byte) 9),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210030000, 855231, 1586.9154f, 2078.2305f, 155.875f, (byte) 66),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210030000, 855231, 1702.6613f, 1662.9213f, 102.19242f, (byte) 64),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210030000, 855231, 2485.3333f, 824.3736f, 100.625f, (byte) 56),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210040000, 855231, 382.0f, 2929.0f, 100.25f, (byte) 42), 1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210040000, 855231, 1470.0549f, 1890.8654f, 106.22974f, (byte) 7),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210040000, 855231, 540.073f, 2107.656f, 103.375f, (byte) 107),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210050000, 855231, 2762.5203f, 830.4615f, 383.87866f, (byte) 58),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210050000, 855231, 1929.1307f, 1953.1182f, 289.32068f, (byte) 64),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210050000, 855231, 2729.864f, 1890.5359f, 189.625f, (byte) 39),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210060000, 855231, 2429.1567f, 2619.0974f, 40.25f, (byte) 40),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210060000, 855231, 555.4294f, 2231.6064f, 44.089336f, (byte) 71),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210060000, 855231, 1771.0f, 1356.0f, 18.125f, (byte) 34), 1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210070000, 855231, 593.9777f, 481.57568f, 416.42203f, (byte) 60),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210070000, 855231, 2943.966f, 2272.9531f, 231.43457f, (byte) 32),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(210070000, 855231, 1429.0f, 1949.0f, 138.5625f, (byte) 27), 1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220020000, 855231, 400.68732f, 1715.6395f, 441.6271f, (byte) 20),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220020000, 855231, 580.48895f, 355.1831f, 485.2271f, (byte) 61),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220030000, 855231, 2895.708f, 1516.8243f, 250.65457f, (byte) 70),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220030000, 855231, 1761.3639f, 506.55444f, 247.3006f, (byte) 53),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220030000, 855231, 1870.0f, 1675.0f, 247.375f, (byte) 53), 1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220040000, 855231, 780.0f, 1240.0f, 224.0f, (byte) 76), 1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220040000, 855231, 2565.876f, 286.7999f, 287.49225f, (byte) 18),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220040000, 855231, 465.72607f, 1786.9429f, 206.01352f, (byte) 3),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220050000, 855231, 2473.5154f, 1896.5199f, 23.560577f, (byte) 19),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220050000, 855231, 335.0f, 370.0f, 5.25f, (byte) 83), 1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220050000, 855231, 417.68674f, 2836.982f, 245.81363f, (byte) 49),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220070000, 855231, 591.8911f, 1341.8286f, 276.875f, (byte) 119),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220070000, 855231, 1488.8247f, 1256.1757f, 298.05154f, (byte) 46),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220070000, 855231, 2186.1123f, 922.95953f, 186.69003f, (byte) 32),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220080000, 855231, 2651.8286f, 2716.1633f, 202.89534f, (byte) 80),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(SpawnEngine.addNewSingleTimeSpawn(220080000, 855231,
					192.61913f, 527.22363f, 196.70428f, (byte) 103), 1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(220080000, 855231, 1583.1567f, 1106.1504f, 132.79529f, (byte) 87),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 855231, 2020.6847f, 2832.924f, 2830.972f, (byte) 51),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 855231, 820.0f, 865.0f, 1671.1095f, (byte) 83), 1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 855231, 2280.3706f, 870.72766f, 2831.548f, (byte) 98),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 855231, 3332.5474f, 1371.9772f, 2666.258f, (byte) 89),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 855231, 1123.5983f, 2096.5151f, 2886.9402f, (byte) 96),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600090000, 855231, 656.06f, 808.15f, 165.125f, (byte) 78), 1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600090000, 855231, 289.80325f, 506.02426f, 158.125f, (byte) 114),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600090000, 855231, 1182.1178f, 348.7145f, 128.5f, (byte) 76), 1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600100000, 855231, 1155.9978f, 1075.4766f, 303.375f, (byte) 104),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600100000, 855231, 675.446f, 1001.7693f, 274.59036f, (byte) 66),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600100000, 855231, 386.59848f, 1810.1382f, 226.42104f, (byte) 89),
					1));
			adventDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(600100000, 855231, 1836.0f, 142.0f, 242.625f, (byte) 86), 1));
			return true;
		default:
			return false;
		}
	}

		/**
	 * 刷出艾雷什基伽尔降临控制特效 NPC。
	 * Spawn Ereshkigal advent-control SP NPCs.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean adventControlEreshSP(int id) {
		switch (id) {
		case 35:
			adventEreshControl.put(702529, SpawnEngine.spawnObject(SpawnEngine.addNewSingleTimeSpawn(400010000, 702529,
					2065.3005f, 2473.1807f, 2900.1775f, (byte) 115), 1));
			adventEreshControl.put(702529, SpawnEngine.spawnObject(SpawnEngine.addNewSingleTimeSpawn(400010000, 702529,
					1722.8392f, 1903.1249f, 2892.1248f, (byte) 107), 1));
			adventEreshControl.put(702529, SpawnEngine.spawnObject(SpawnEngine.addNewSingleTimeSpawn(400010000, 702529,
					670.00000f, 2700.0000f, 2897.5470f, (byte) 107), 1));
			adventEreshControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 702529, 1813.9344f, 1827.3582f, 2885.6187f, (byte) 33),
					1));
			adventEreshControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 702529, 2606.2485f, 1892.8187f, 2908.7598f, (byte) 47),
					1));
			adventEreshControl.put(702529, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 702529, 2773.0369f, 1152.2582f, 2801.5713f, (byte) 37),
					1));
			return true;
		default:
			return false;
		}
	}

	/**
	 * 刷出艾雷什基伽尔降临效果特效 NPC。
	 * Spawn Ereshkigal advent-effect SP NPCs.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean adventEffectEreshSP(int id) {
		switch (id) {
		case 35:
			adventEreshEffect.put(702549, SpawnEngine.spawnObject(SpawnEngine.addNewSingleTimeSpawn(400010000, 702549,
					2065.3005f, 2473.1807f, 2900.1775f, (byte) 115), 1));
			adventEreshEffect.put(702549, SpawnEngine.spawnObject(SpawnEngine.addNewSingleTimeSpawn(400010000, 702549,
					1722.8392f, 1903.1249f, 2892.1248f, (byte) 107), 1));
			adventEreshEffect.put(702549, SpawnEngine.spawnObject(SpawnEngine.addNewSingleTimeSpawn(400010000, 702549,
					670.00000f, 2700.0000f, 2897.5470f, (byte) 107), 1));
			adventEreshEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 702549, 1813.9344f, 1827.3582f, 2885.6187f, (byte) 33),
					1));
			adventEreshEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 702549, 2606.2485f, 1892.8187f, 2908.7598f, (byte) 47),
					1));
			adventEreshEffect.put(702549, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 702549, 2773.0369f, 1152.2582f, 2801.5713f, (byte) 37),
					1));
			return true;
		default:
			return false;
		}
	}

	/**
	 * 刷出艾雷什基伽尔降临传送门特效 NPC。
	 * Spawn Ereshkigal advent-portal SP NPCs.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean adventPortalEreshSP(int id) {
		switch (id) {
		case 35:
			adventEreshPortal.put(702550, SpawnEngine.spawnObject(SpawnEngine.addNewSingleTimeSpawn(400010000, 702550,
					2065.3005f, 2473.1807f, 2900.1775f, (byte) 115), 1));
			adventEreshPortal.put(702550, SpawnEngine.spawnObject(SpawnEngine.addNewSingleTimeSpawn(400010000, 702550,
					1722.8392f, 1903.1249f, 2892.1248f, (byte) 107), 1));
			adventEreshPortal.put(702550, SpawnEngine.spawnObject(SpawnEngine.addNewSingleTimeSpawn(400010000, 702550,
					670.00000f, 2700.0000f, 2897.5470f, (byte) 107), 1));
			adventEreshPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 702550, 1813.9344f, 1827.3582f, 2885.6187f, (byte) 33),
					1));
			adventEreshPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 702550, 2606.2485f, 1892.8187f, 2908.7598f, (byte) 47),
					1));
			adventEreshPortal.put(702550, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 702550, 2773.0369f, 1152.2582f, 2801.5713f, (byte) 37),
					1));
			return true;
		default:
			return false;
		}
	}

	/**
	 * 刷出艾雷什基伽尔降临导向特效 NPC。
	 * Spawn Ereshkigal advent-directing SP NPCs.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean adventDirectingEreshSP(int id) {
		switch (id) {
		case 35:
			adventEreshDirecting.put(855231, SpawnEngine.spawnObject(SpawnEngine.addNewSingleTimeSpawn(400010000,
					855231, 2065.3005f, 2473.1807f, 2900.1775f, (byte) 115), 1));
			adventEreshDirecting.put(855231, SpawnEngine.spawnObject(SpawnEngine.addNewSingleTimeSpawn(400010000,
					855231, 1722.8392f, 1903.1249f, 2892.1248f, (byte) 107), 1));
			adventEreshDirecting.put(855231, SpawnEngine.spawnObject(SpawnEngine.addNewSingleTimeSpawn(400010000,
					855231, 670.00000f, 2700.0000f, 2897.5470f, (byte) 107), 1));
			adventEreshDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 855231, 1813.9344f, 1827.3582f, 2885.6187f, (byte) 33),
					1));
			adventEreshDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 855231, 2606.2485f, 1892.8187f, 2908.7598f, (byte) 47),
					1));
			adventEreshDirecting.put(855231, SpawnEngine.spawnObject(
					SpawnEngine.addNewSingleTimeSpawn(400010000, 855231, 2773.0369f, 1152.2582f, 2801.5713f, (byte) 37),
					1));
			return true;
		default:
			return false;
		}
	}

	/**
	 * 清除指定地点已刷出的入侵 NPC。
	 * Despawn invasion NPCs at the given location.
	 *
	 * @param loc 入侵地点 / beritra location
	 */
	public void despawn(BeritraLocation loc) {
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
	 * 判断指定地点是否正在入侵。
	 * Whether an invasion is in progress at the given location.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 若 in progress 则为 true / true if in progress
	 */
	public boolean isInvasionInProgress(int id) {
		return activeInvasions.containsKey(id);
	}

	/**
	 * 返回当前活跃的入侵映射。
	 * Return the map of currently active invasions.
	 *
	 * @return 地点 ID → 入侵实例 / location id to invasion instance
	 */
	public Map<Integer, BeritraInvasion<?>> getActiveInvasions() {
		return activeInvasions;
	}

	/**
	 * 返回入侵持续时长（小时，来自配置）。
	 * Return invasion duration in hours (from config).
	 *
	 * @return 持续小时数 / duration hours
	 */
	public int getDuration() {
		return CustomConfig.BERITRA_DURATION;
	}

	/**
	 * 按 ID 获取入侵地点。
	 * Get a beritra location by id.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * beritra location
	 */
	public BeritraLocation getBeritraLocation(int id) {
		return beritra.get(id);
	}

	/**
	 * 返回全部入侵地点。
	 * Return all beritra locations.
	 *
	 * location map
	 */
	public Map<Integer, BeritraLocation> getBeritraLocations() {
		return beritra;
	}

	/**
	 * 获取 BeritraService 单例（Spring 提供者优先，否则 holder）。
	 * Return the BeritraService singleton (Spring provider first, else holder).
	 *
	 * service instance
	 */
	public static BeritraService getInstance() {
		ObjectProvider<BeritraService> provider = instanceProvider;
		if (provider == null) {
			return BeritraServiceHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> BeritraServiceHolder.INSTANCE);
	}

	/**
	 * 注入 Spring ObjectProvider，供 getInstance 使用。
	 * Inject the Spring ObjectProvider used by getInstance().
	 *
	 * Spring provider
	 */
	public static void setInstanceProvider(ObjectProvider<BeritraService> instanceProvider) {
		BeritraService.instanceProvider = instanceProvider;
	}

	private static class BeritraServiceHolder {
		private static final BeritraService INSTANCE = new BeritraService();
	}
}
