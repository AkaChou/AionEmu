package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.services.CronService;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.schedule.BeritraSchedule;
import com.aionemu.gameserver.configs.schedule.BeritraSchedule.Beritra;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.beritra.BeritraLocation;
import com.aionemu.gameserver.model.beritra.BeritraStateType;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.beritraspawns.BeritraSpawnTemplate;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.beritraservice.BeritraInvasion;
import com.aionemu.gameserver.services.beritraservice.BeritraStartRunnable;
import com.aionemu.gameserver.services.beritraservice.Invade;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.World;

/**
 * 贝里特拉/艾雷什基伽尔入侵（Beritra / Ereshkigal Invasion）世界活动服务。
 * Service for Beritra and Ereshkigal world-invasion events (schedule, spawn, SP effects, messages).
 *
 * @author Rinzler (Encom)
 */
@Slf4j

public class BeritraService {
	private static volatile ObjectProvider<BeritraService> instanceProvider;
	private static volatile BeritraService resolvedInstance;
	private final List<Runnable> scheduledTasks = new ArrayList<>();
	private Map<Integer, BeritraLocation> beritra;
	// 贝里特拉入侵 4.7 / Beritra Invasion 4.7
	private final List<VisibleObject> adventPortal = Collections.synchronizedList(new ArrayList<>());
	private final List<VisibleObject> adventEffect = Collections.synchronizedList(new ArrayList<>());
	private final List<VisibleObject> adventControl = Collections.synchronizedList(new ArrayList<>());
	private final List<VisibleObject> adventDirecting = Collections.synchronizedList(new ArrayList<>());
	// 埃雷什基伽尔入侵 4.9 / Ereshkigal Invasion 4.9
	private final List<VisibleObject> adventEreshPortal = Collections.synchronizedList(new ArrayList<>());
	private final List<VisibleObject> adventEreshEffect = Collections.synchronizedList(new ArrayList<>());
	private final List<VisibleObject> adventEreshControl = Collections.synchronizedList(new ArrayList<>());
	private final List<VisibleObject> adventEreshDirecting = Collections.synchronizedList(new ArrayList<>());

	private final ConcurrentMap<Integer, BeritraInvasion<?>> activeInvasions = new ConcurrentHashMap<>();

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
			log.debug(I18n.get("log.8317eae99b1f"));
		}
		reloadSchedule();
	}

	/**
	 * 重新加载入侵时间表：取消旧任务并按新 cron 注册。
	 * Reload the invasion schedule: cancel old tasks and re-register from cron.
	 */
	public synchronized void reloadSchedule() {
		BeritraSchedule newSchedule = CustomConfig.BERITRA_ENABLED ? BeritraSchedule.load() : null;
		scheduledTasks.forEach(CronService.requireCurrent()::cancel);
		scheduledTasks.clear();
		if (newSchedule != null) {
			for (Beritra beritra : newSchedule.getBeritrasList()) {
				for (String invasionTime : beritra.getInvasionTimes()) {
					Runnable task = new BeritraStartRunnable(beritra.getId());
					scheduledTasks.add(task);
					CronService.requireCurrent().schedule(task, invasionTime);
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
		ThreadPoolManager.getInstance().schedule(() -> stopBeritraInvasion(id), (long) CustomConfig.BERITRA_DURATION * 3600 * 1000);
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
		clearAdventObjects(id);
		devilUnitReturnMsg(id);
		beritraLegionReturnMsg(id);
	}

	/**
	 * 清理指定活动阶段创建的临时特效对象。
	 * Despawns temporary effect objects created by the given event stage.
	 *
	 * @param id 入侵地点 ID / invasion location id
	 */
	public void clearAdventObjects(int id) {
		if (id == 1) {
			despawnAdventObjects(adventPortal);
			despawnAdventObjects(adventEffect);
			despawnAdventObjects(adventControl);
			despawnAdventObjects(adventDirecting);
		} else if (id == 35) {
			despawnAdventObjects(adventEreshPortal);
			despawnAdventObjects(adventEreshEffect);
			despawnAdventObjects(adventEreshControl);
			despawnAdventObjects(adventEreshDirecting);
		}
	}

	/**
	 * 清理一组临时特效对象并释放其引用。
	 * Despawns one group of temporary effect objects and releases its references.
	 *
	 * @param objects 临时对象集合 / temporary objects
	 */
	private void despawnAdventObjects(List<VisibleObject> objects) {
		synchronized (objects) {
			for (VisibleObject object : new ArrayList<>(objects)) {
				if (object != null && object.isSpawned()) {
					object.getController().onDelete();
				}
			}
			objects.clear();
		}
	}

	/**
	 * 按状态刷出入侵相关 NPC。
	 * Spawn invasion NPCs for the given location and state.
	 *
	 * @param loc 入侵地点 / beritra location
	 *            spawn state
	 */
	public void spawn(BeritraLocation loc, BeritraStateType bstate) {
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

	private static boolean broadcastMessage(int actualId, int targetId, AionServerPacket packet) {
		if (actualId == targetId) {
			World.getInstance().doOnAllPlayers(player -> PacketSendUtility.sendPacket(player, packet));
			return true;
		}
		return false;
	}

	/**
	 * 广播贝里特拉入侵系统消息。
	 * Broadcast Beritra invasion system messages.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public void beritraInvasionMsg(int id) {
		broadcastMessage(id, 1, SM_SYSTEM_MESSAGE.STR_MSG_WORLDRAID_INVADE_VRITRA_SPECIAL);
	}

	/**
	 * 广播入侵走廊相关消息。
	 * Broadcast invasion-corridor messages.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public void invasionCorridorMsg(int id) {
		broadcastMessage(id, 1, SM_SYSTEM_MESSAGE.STR_MSG_WORLDRAID_MESSAGE_01);
	}

	/**
	 * 广播魔族部队通过消息。
	 * Broadcast devil-unit through messages.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public void devilUnitThroughMsg(int id) {
		broadcastMessage(id, 1, SM_SYSTEM_MESSAGE.STR_MSG_WORLDRAID_MESSAGE_02);
	}

	/**
	 * 广播魔族部队撤退消息。
	 * Broadcast devil-unit return messages.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean devilUnitReturnMsg(int id) {
		return broadcastMessage(id, 1, SM_SYSTEM_MESSAGE.STR_MSG_WORLDRAID_MESSAGE_03);
	}

	/**
	 * 广播艾雷什基伽尔入侵消息。
	 * Broadcast Ereshkigal invasion messages.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public void ereshkigalInvasionMsg(int id) {
		broadcastMessage(id, 35, SM_SYSTEM_MESSAGE.STR_MSG_WORLDRAID_INVADE_VRITRA_SPECIAL);
	}

	/**
	 * 广播艾雷什基伽尔走廊消息。
	 * Broadcast Ereshkigal corridor messages.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public void ereshkigalCorridorMsg(int id) {
		broadcastMessage(id, 35, SM_SYSTEM_MESSAGE.STR_MSG_WORLDRAID_Ere_MESSAGE_01);
	}

	/**
	 * 广播艾雷什基伽尔军团通过消息。
	 * Broadcast Ereshkigal legion through messages.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public void ereshkigalLegionThroughMsg(int id) {
		broadcastMessage(id, 35, SM_SYSTEM_MESSAGE.STR_MSG_WORLDRAID_Ere_MESSAGE_02);
	}

	/**
	 * 广播贝里特拉军团撤退消息。
	 * Broadcast Beritra legion return messages.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public boolean beritraLegionReturnMsg(int id) {
		return broadcastMessage(id, 35, SM_SYSTEM_MESSAGE.STR_MSG_WORLDRAID_MESSAGE_03);
	}

	/**
	 * 广播无舰防御相关消息。
	 * Broadcast dredgion-defense messages.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public void dredgionDefenseMsg(int id) {
		broadcastMessage(id, 57, SM_SYSTEM_MESSAGE.STR_MSG_Dreadgion_Start_L);
	}

	private record AdventPoint(int worldId, float x, float y, float z, byte heading) {
		VisibleObject spawn(int npcId) {
			return SpawnEngine.spawnObject(
				SpawnEngine.addNewSingleTimeSpawn(worldId, npcId, x, y, z, heading), 1);
		}
	}

	private static final List<AdventPoint> BERITRA_ADVENT_POINTS = List.of(
		new AdventPoint(210020000, 858.5479f, 1151.3783f, 278.46576f, (byte) 71),
		new AdventPoint(210020000, 1519.0f, 1911.0f, 289.5f, (byte) 10),
		new AdventPoint(210020000, 260.20285f, 2134.1099f, 207.375f, (byte) 9),
		new AdventPoint(210030000, 1586.9154f, 2078.2305f, 155.875f, (byte) 66),
		new AdventPoint(210030000, 1702.6613f, 1662.9213f, 102.19242f, (byte) 64),
		new AdventPoint(210030000, 2485.3333f, 824.3736f, 100.625f, (byte) 56),
		new AdventPoint(210040000, 382.0f, 2929.0f, 100.25f, (byte) 42),
		new AdventPoint(210040000, 1470.0549f, 1890.8654f, 106.22974f, (byte) 7),
		new AdventPoint(210040000, 540.073f, 2107.656f, 103.375f, (byte) 107),
		new AdventPoint(210050000, 2762.5203f, 830.4615f, 383.87866f, (byte) 58),
		new AdventPoint(210050000, 1929.1307f, 1953.1182f, 289.32068f, (byte) 64),
		new AdventPoint(210050000, 2729.864f, 1890.5359f, 189.625f, (byte) 39),
		new AdventPoint(210060000, 2429.1567f, 2619.0974f, 40.25f, (byte) 40),
		new AdventPoint(210060000, 555.4294f, 2231.6064f, 44.089336f, (byte) 71),
		new AdventPoint(210060000, 1771.0f, 1356.0f, 18.125f, (byte) 34),
		new AdventPoint(210070000, 593.9777f, 481.57568f, 416.42203f, (byte) 60),
		new AdventPoint(210070000, 2943.966f, 2272.9531f, 231.43457f, (byte) 32),
		new AdventPoint(210070000, 1429.0f, 1949.0f, 138.5625f, (byte) 27),
		new AdventPoint(220020000, 400.68732f, 1715.6395f, 441.6271f, (byte) 20),
		new AdventPoint(220020000, 580.48895f, 355.1831f, 485.2271f, (byte) 61),
		new AdventPoint(220030000, 2895.708f, 1516.8243f, 250.65457f, (byte) 70),
		new AdventPoint(220030000, 1761.3639f, 506.55444f, 247.3006f, (byte) 53),
		new AdventPoint(220030000, 1870.0f, 1675.0f, 247.375f, (byte) 53),
		new AdventPoint(220040000, 780.0f, 1240.0f, 224.0f, (byte) 76),
		new AdventPoint(220040000, 2565.876f, 286.7999f, 287.49225f, (byte) 18),
		new AdventPoint(220040000, 465.72607f, 1786.9429f, 206.01352f, (byte) 3),
		new AdventPoint(220050000, 2473.5154f, 1896.5199f, 23.560577f, (byte) 19),
		new AdventPoint(220050000, 335.0f, 370.0f, 5.25f, (byte) 83),
		new AdventPoint(220050000, 417.68674f, 2836.982f, 245.81363f, (byte) 49),
		new AdventPoint(220070000, 591.8911f, 1341.8286f, 276.875f, (byte) 119),
		new AdventPoint(220070000, 1488.8247f, 1256.1757f, 298.05154f, (byte) 46),
		new AdventPoint(220070000, 2186.1123f, 922.95953f, 186.69003f, (byte) 32),
		new AdventPoint(220080000, 2651.8286f, 2716.1633f, 202.89534f, (byte) 80),
		new AdventPoint(220080000, 192.61913f, 527.22363f, 196.70428f, (byte) 103),
		new AdventPoint(220080000, 1583.1567f, 1106.1504f, 132.79529f, (byte) 87),
		new AdventPoint(400010000, 2020.6847f, 2832.924f, 2830.972f, (byte) 51),
		new AdventPoint(400010000, 820.0f, 865.0f, 1671.1095f, (byte) 83),
		new AdventPoint(400010000, 2280.3706f, 870.72766f, 2831.548f, (byte) 98),
		new AdventPoint(400010000, 3332.5474f, 1371.9772f, 2666.258f, (byte) 89),
		new AdventPoint(400010000, 1123.5983f, 2096.5151f, 2886.9402f, (byte) 96),
		new AdventPoint(600090000, 656.06f, 808.15f, 165.125f, (byte) 78),
		new AdventPoint(600090000, 289.80325f, 506.02426f, 158.125f, (byte) 114),
		new AdventPoint(600090000, 1182.1178f, 348.7145f, 128.5f, (byte) 76),
		new AdventPoint(600100000, 1155.9978f, 1075.4766f, 303.375f, (byte) 104),
		new AdventPoint(600100000, 675.446f, 1001.7693f, 274.59036f, (byte) 66),
		new AdventPoint(600100000, 386.59848f, 1810.1382f, 226.42104f, (byte) 89),
		new AdventPoint(600100000, 1836.0f, 142.0f, 242.625f, (byte) 86)
	);

	private static final List<AdventPoint> ERESHKIGAL_ADVENT_POINTS = List.of(
		new AdventPoint(400010000, 2065.3005f, 2473.1807f, 2900.1775f, (byte) 115),
		new AdventPoint(400010000, 1722.8392f, 1903.1249f, 2892.1248f, (byte) 107),
		new AdventPoint(400010000, 670.00000f, 2700.0000f, 2897.5470f, (byte) 107),
		new AdventPoint(400010000, 1813.9344f, 1827.3582f, 2885.6187f, (byte) 33),
		new AdventPoint(400010000, 2606.2485f, 1892.8187f, 2908.7598f, (byte) 47),
		new AdventPoint(400010000, 2773.0369f, 1152.2582f, 2801.5713f, (byte) 37)
	);

	private static boolean spawnAdventSp(int actualId, int targetId, int npcId,
										 List<AdventPoint> points, List<VisibleObject> targetList) {
		if (actualId == targetId) {
			for (AdventPoint point : points) {
				targetList.add(point.spawn(npcId));
			}
			return true;
		}
		return false;
	}

	/**
	 * 刷出贝里特拉降临控制特效 NPC。
	 * Spawn Beritra advent-control SP NPCs.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public void adventControlSP(int id) {
		spawnAdventSp(id, 1, 702529, BERITRA_ADVENT_POINTS, adventControl);
	}

	/**
	 * 刷出贝里特拉降临效果特效 NPC。
	 * Spawn Beritra advent-effect SP NPCs.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public void adventEffectSP(int id) {
		spawnAdventSp(id, 1, 702549, BERITRA_ADVENT_POINTS, adventEffect);
	}

	/**
	 * 刷出贝里特拉降临传送门特效 NPC。
	 * Spawn Beritra advent-portal SP NPCs.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public void adventPortalSP(int id) {
		spawnAdventSp(id, 1, 702550, BERITRA_ADVENT_POINTS, adventPortal);
	}

	/**
	 * 刷出贝里特拉降临导向特效 NPC。
	 * Spawn Beritra advent-directing SP NPCs.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public void adventDirectingSP(int id) {
		spawnAdventSp(id, 1, 855231, BERITRA_ADVENT_POINTS, adventDirecting);
	}

	/**
	 * 刷出艾雷什基伽尔降临控制特效 NPC。
	 * Spawn Ereshkigal advent-control SP NPCs.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public void adventControlEreshSP(int id) {
		spawnAdventSp(id, 35, 702529, ERESHKIGAL_ADVENT_POINTS, adventEreshControl);
	}

	/**
	 * 刷出艾雷什基伽尔降临效果特效 NPC。
	 * Spawn Ereshkigal advent-effect SP NPCs.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public void adventEffectEreshSP(int id) {
		spawnAdventSp(id, 35, 702549, ERESHKIGAL_ADVENT_POINTS, adventEreshEffect);
	}

	/**
	 * 刷出艾雷什基伽尔降临传送门特效 NPC。
	 * Spawn Ereshkigal advent-portal SP NPCs.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public void adventPortalEreshSP(int id) {
		spawnAdventSp(id, 35, 702550, ERESHKIGAL_ADVENT_POINTS, adventEreshPortal);
	}

	/**
	 * 刷出艾雷什基伽尔降临导向特效 NPC。
	 * Spawn Ereshkigal advent-directing SP NPCs.
	 *
	 * @param id 入侵地点 ID / beritra location id
	 * @return 是否已处理该 ID / whether the id was handled
	 */
	public void adventDirectingEreshSP(int id) {
		spawnAdventSp(id, 35, 855231, ERESHKIGAL_ADVENT_POINTS, adventEreshDirecting);
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
	 *           beritra location
	 */
	public BeritraLocation getBeritraLocation(int id) {
		return beritra.get(id);
	}

	/**
	 * 返回全部入侵地点。
	 * Return all beritra locations.
	 * location map
	 */
	public Map<Integer, BeritraLocation> getBeritraLocations() {
		return beritra;
	}

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 *
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *                               when no provider or bean is available
	 */
	public static BeritraService getInstance() {
		BeritraService resolved = resolvedInstance;
		if (resolved != null) {
			return resolved;
		}
		ObjectProvider<BeritraService> provider = instanceProvider;
		resolved = provider == null ? null : provider.getIfAvailable();
		if (resolved == null) {
			throw new IllegalStateException("BeritraService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		resolvedInstance = resolved;
		return resolved;
	}

	/**
	 * 注入 Spring ObjectProvider，供 getInstance 使用。
	 * Inject the Spring ObjectProvider used by getInstance().
	 *
	 * @param instanceProvider Spring provider
	 */
	public static void setInstanceProvider(ObjectProvider<BeritraService> instanceProvider) {
		BeritraService.instanceProvider = instanceProvider;
		resolvedInstance = null;
	}
}
