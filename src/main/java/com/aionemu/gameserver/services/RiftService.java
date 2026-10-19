package com.aionemu.gameserver.services;


import com.aionemu.commons.services.CronService;
import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.schedule.RiftSchedule;
import com.aionemu.gameserver.configs.schedule.RiftSchedule.Rift;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.rift.RiftLocation;
import com.aionemu.gameserver.services.rift.RiftInformer;
import com.aionemu.gameserver.services.rift.RiftManager;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 裂隙服务，管理世界裂隙开关、刷怪与定时计划，批量操作按地图记录实际新增摘要。
 * Rift service managing spawns and schedules, reporting actual additions per map for batch opens.
 * @author Rinzler (Encom)
 */
@Slf4j
public class RiftService {
	private static volatile ObjectProvider<RiftService> instanceProvider;
	private static volatile RiftService resolvedInstance;
	private RiftSchedule riftSchedule;
	private final List<Runnable> scheduledTasks = new ArrayList<>();
	private Map<Integer, RiftLocation> locations;
	private final Lock closing = new ReentrantLock();
	private final Map<Integer, RiftLocation> activeRifts = new HashMap<>();
	private final Map<Integer, Object> pendingCloses = new HashMap<>();

	/**
	 * 初始化裂隙地点数据。
	 * Initializes rift location data.
	 */
	public void initRiftLocations() {
		if (CustomConfig.RIFT_ENABLED) {
			locations = DataManager.RIFT_DATA.getRiftLocations();
		} else {
			locations = Collections.emptyMap();
		}
	}

	/**
	 * 初始化裂隙并加载定时计划。
	 * Initializes rifts and loads the schedule.
	 */
	public void initRifts() {
		reloadSchedule();
	}

	/**
	 * 重载裂隙 Cron 计划（先取消旧任务）。
	 * Reloads the rift cron schedule (cancels previous tasks first).
	 */
	public synchronized void reloadSchedule() {
		RiftSchedule newSchedule = CustomConfig.RIFT_ENABLED ? RiftSchedule.load() : null;
		scheduledTasks.forEach(CronService.requireCurrent()::cancel);
		scheduledTasks.clear();
		riftSchedule = newSchedule;
		if (riftSchedule != null) {
			for (Rift rift : riftSchedule.getRiftsList()) {
				for (String openTimes : rift.getOpenTime()) {
					Runnable task = new RiftOpenRunnable(this, rift.getWorldId());
					scheduledTasks.add(task);
					CronService.requireCurrent().schedule(task, openTimes);
				}
			}
		}
	}

	/**
	 * 校验裂隙 ID 或世界 ID 是否有效。
	 * Validates whether the rift id or world id is valid.
	 * @param id 裂隙/世界 ID / rift or world id
	 * whether valid
	 */
	public boolean isValidId(int id) {
		if (isRift(id)) {
			return getRiftLocations().containsKey(id);
		} else {
			for (RiftLocation loc : getRiftLocations().values()) {
				if (loc.getWorldId() == id) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isRift(int id) {
		return id < 10000;
	}

	/**
	 * 按裂隙 ID 或世界 ID 开启裂隙。
	 * Opens rifts by rift id or world id.
	 * @param id 裂隙 / 世界 ID / rift or world id
	 * @return 是否成功开启 / whether opened
	 */
	public boolean openRifts(int id) {
		if (isValidId(id)) {
			if (isRift(id)) {
				RiftLocation rift = getRiftLocation(id);
				if (rift.getSpawned().isEmpty()) {
					openRifts(rift);
					RiftInformer.sendRiftsInfo(rift.getWorldId());
					return true;
				}
				log.info(I18n.get("rift.open.result", rift.getId(), 0, 0));
			} else {
				boolean opened = openRiftsOnMap(id);
				RiftInformer.sendRiftsInfo(id);
				return opened;
			}
		}
		return false;
	}

	/**
	 * 按地图执行入口开启并汇总实际新增数量；返回值保留原有尝试开启语义。
	 * Opens entrances on a map and summarizes actual additions, preserving the attempted-open return semantics.
	 */
	private boolean openRiftsOnMap(int worldId) {
		boolean opened = false;
		int entrances = 0;
		int npcs = 0;
		for (RiftLocation rift : getRiftLocations().values()) {
			if (rift.getWorldId() == worldId && rift.getSpawned().isEmpty()) {
				int spawned = openRift(rift);
				if (spawned > 0) {
					entrances++;
					npcs += spawned;
				}
				opened = true;
			}
		}
		log.info(I18n.get("rift.open.map_summary", worldId, entrances, npcs));
		return opened;
	}

	/**
	 * 按裂隙 ID 或世界 ID 关闭裂隙。
	 * Closes rifts by rift id or world id.
	 * @param id 裂隙 / 世界 ID / rift or world id
	 * @return 是否成功关闭 / whether closed
	 */
	public boolean closeRifts(int id) {
		if (isValidId(id)) {
			if (isRift(id)) {
				RiftLocation rift = getRiftLocation(id);
				if (!rift.getSpawned().isEmpty()) {
					closeRift(rift);
					return true;
				}
			} else {
				boolean opened = false;
				for (RiftLocation rift : getRiftLocations().values()) {
					if (rift.getWorldId() == id && !rift.getSpawned().isEmpty()) {
						closeRift(rift);
						opened = true;
					}
				}
				return opened;
			}
		}
		return false;
	}

	/**
	 * 开启指定裂隙地点并在结束后自动关闭。
	 * Opens the given rift location and auto-closes after duration.
	 * @param location rift location
	 */
	public void openRifts(RiftLocation location) {
		if (!location.getSpawned().isEmpty()) {
			log.info(I18n.get("rift.open.result", location.getId(), 0, 0));
			return;
		}
		cancelPendingClose(location);
		int spawned = openRift(location);
		log.info(I18n.get("rift.open.result", location.getId(), spawned > 0 ? 1 : 0, spawned));
	}

	/**
	 * 重新开启时退出旧计时器阶段，避免旧关闭任务作用于本次开启。
	 * Leaves the previous timer phase on reopen so its close task cannot affect this opening.
	 * @param location 裂隙地点 / rift location
	 */
	private void cancelPendingClose(RiftLocation location) {
		closing.lock();
		try {
			pendingCloses.remove(location.getId());
			activeRifts.remove(location.getId());
		} finally {
			closing.unlock();
		}
	}

	/**
	 * 开启尚未生成的入口，返回实际新增 NPC 数；批量调用不输出逐入口 INFO。
	 * Opens an unspawned entrance and returns actual new NPCs, without per-entrance INFO in batches.
	 */
	private int openRift(RiftLocation location) {
		if (!location.getSpawned().isEmpty()) {
			return 0;
		}
		location.setOpened(true);
		RiftManager.getInstance().spawnRift(location);
		Object phase = new Object();
		closing.lock();
		try {
			activeRifts.put(location.getId(), location);
			pendingCloses.put(location.getId(), phase);
		} finally {
			closing.unlock();
		}
		ThreadPoolManager.getInstance().schedule(() -> closeOpenedRift(location, phase),
			(long) CustomConfig.RIFT_DURATION * 3600 * 1000);
		return location.getSpawned().size();
	}

	/**
	 * 仅在入口仍处于待关闭状态时关闭该入口，避免迟到旧计时任务关闭重新开启的裂隙。
	 * Closes only while the entrance is still pending, preventing a late old timer from closing a reopened rift.
	 * @param location 裂隙地点 / rift location
	 * @param phase 本次开启的计时阶段 / timer phase of this opening
	 */
	private void closeOpenedRift(RiftLocation location, Object phase) {
		closing.lock();
		try {
			if (pendingCloses.get(location.getId()) != phase) {
				return;
			}
			pendingCloses.remove(location.getId());
			activeRifts.remove(location.getId());
		} finally {
			closing.unlock();
		}
		closeRift(location);
	}

	/**
	 * 关闭单个裂隙地点并删除刷怪。
	 * Closes a single rift location and deletes its spawns.
	 * @param location rift location
	 */
	public void closeRift(RiftLocation location) {
		location.setOpened(false);
		for (VisibleObject npc : new ArrayList<>(location.getSpawned())) {
			((Npc) npc).getController().cancelTask(TaskId.RESPAWN);
			npc.getController().onDelete();
		}
		location.getSpawned().clear();
	}

	/**
	 * 关闭全部活动裂隙。
	 * Closes all active rifts.
	 */
	public void closeRifts() {
		List<RiftLocation> rifts;
		closing.lock();
		try {
			rifts = new ArrayList<>(activeRifts.values());
			activeRifts.clear();
			pendingCloses.clear();
		} finally {
			closing.unlock();
		}
		for (RiftLocation rift : rifts) {
			closeRift(rift);
		}
	}

	/**
	 * 获取裂隙持续时长（小时）。
	 * Returns the rift duration in hours.
	 * @return 持续小时数 / duration hours
	 */
	public int getDuration() {
		return CustomConfig.RIFT_DURATION;
	}

	/**
	 * 按 ID 获取裂隙地点。
	 * Returns the rift location by id.
	 * @param id 地点 ID / location id
	 * location
	 */
	public RiftLocation getRiftLocation(int id) {
		return locations.get(id);
	}

	/**
	 * 获取全部裂隙地点。
	 * Returns all rift locations.
	 * locations map
	 */
	public Map<Integer, RiftLocation> getRiftLocations() {
		return locations;
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
	public static RiftService getInstance() {
		RiftService resolved = resolvedInstance;
		if (resolved != null) {
			return resolved;
		}
		ObjectProvider<RiftService> provider = instanceProvider;
		resolved = provider == null ? null : provider.getIfAvailable();
		if (resolved == null) {
			throw new IllegalStateException("RiftService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		resolvedInstance = resolved;
		return resolved;
	}

	/**
	 * 注入 Spring 的实例提供者。
	 * Sets the Spring instance provider.
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<RiftService> instanceProvider) {
		RiftService.instanceProvider = instanceProvider;
		resolvedInstance = null;
	}

	/**
	 * 裂隙开启定时任务，按世界 ID 打开对应裂隙并向玩家广播状态。
	 * Runnable that opens rifts for a world id and broadcasts rift state to players.
	 */
	private record RiftOpenRunnable(RiftService service, int worldId) implements Runnable {
		/**
		 * 打开该世界全部裂隙位置并发送裂隙信息。
		 * Opens all rift locations in the world and sends rift info.
		 */
		@Override
		public void run() {
			service.openRifts(worldId);
		}
	}
}
