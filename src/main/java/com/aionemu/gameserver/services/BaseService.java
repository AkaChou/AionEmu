package com.aionemu.gameserver.services;

import com.aionemu.boot.i18n.I18n;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCronServices;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.base.BaseLocation;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FLAG_INFO;
import com.aionemu.gameserver.persistence.dao.BaseLocationDao;
import com.aionemu.gameserver.services.base.Base;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 据点（Base）服务，管理据点位置初始化、启停、占领与旗帜同步。
 * Base service managing base location init, start/stop, capture and flag sync.
 * @author Rinzler
 */
@Slf4j
public class BaseService {
	@Setter
	private static volatile ObjectProvider<BaseService> instanceProvider;
	private final BaseLocationDao baseDAO;
	/** 当前活跃据点实例。 / Currently active base instances. */
	private final ConcurrentMap<Integer, Base<?>> active = new ConcurrentHashMap<>();
	/** 据点位置映射。 / Base location map. */
	private Map<Integer, BaseLocation> bases;

	/**
	 * 使用指定的 BaseDAO 构造据点服务。
	 * Constructs the base service with the given BaseDAO.
	 *
	 * @param baseDAO 据点数据访问对象 / Base DAO
	 */
	public BaseService(@NonNull BaseLocationDao baseDAO) {
		this.baseDAO = baseDAO;
	}

	/**
	 * 初始化据点位置数据并加载数据库状态。
	 * Initializes base locations and loads their database state.
	 */
	public void initBaseLocations() {
		bases = DataManager.BASE_DATA.getBaseLocations();
		this.baseDAO.loadBaseLocations(bases);
		log.info(I18n.get("log.e322faae29f4", bases.size()));
	}

	/**
	 * 启动全部据点。
	 * Starts all bases.
	 */
	public void initBases() {
		log.info(I18n.get("log.b00d281537f8"));
		for (BaseLocation base : getBaseLocations().values()) {
			start(base.getId());
		}
	}

	/**
	 * 注册每周三的据点重置 Cron，将多处据点重置为 NPC 占领。
	 * Registers the Wednesday cron that resets multiple bases to NPC ownership.
	 */
	public void initBaseReset() {
		Race race = null;
		log.info(I18n.get("log.0b82f7fdbd34"));
		String weekly = "0 0 9 ? * WED *";
		GameCronServices.cronService().schedule(() -> {
			// 埃尔特内。 / Elten.
			capture(45, Race.NPC);
			capture(46, Race.NPC);
			// 因特尔蒂卡。 / Heiron.
			capture(47, Race.NPC);
			capture(48, Race.NPC);
			// 莫尔海姆。 / Morheim.
			capture(49, Race.NPC);
			capture(50, Race.NPC);
			// 贝鲁斯兰。 / Beluslan.
			capture(51, Race.NPC);
			capture(52, Race.NPC);
			// 雷珊塔。 / Reshanta.
			capture(53, Race.NPC);
			capture(54, Race.NPC);
			capture(55, Race.NPC);
			capture(56, Race.NPC);
			capture(57, Race.NPC);
			capture(58, Race.NPC);
			capture(59, Race.NPC);
			capture(60, Race.NPC);
			capture(61, Race.NPC);
			capture(62, Race.NPC);
			capture(63, Race.NPC);
			capture(64, Race.NPC);
			// 卡塔拉姆。 / Katalam.
			capture(71, Race.NPC);
			capture(72, Race.NPC);
			capture(73, Race.NPC);
			capture(74, Race.NPC);
			capture(75, Race.NPC);
			capture(76, Race.NPC);
			capture(77, Race.NPC);
			capture(78, Race.NPC);
			capture(79, Race.NPC);
			// 莱文肖尔。 / Levinshor.
			capture(90, Race.NPC);
			capture(91, Race.NPC);
			capture(92, Race.NPC);
			capture(93, Race.NPC);
			capture(94, Race.NPC);
			capture(95, Race.NPC);
			capture(96, Race.NPC);
			capture(97, Race.NPC);
			capture(98, Race.NPC);
			capture(99, Race.NPC);
			capture(100, Race.NPC);
			capture(101, Race.NPC);
			capture(102, Race.NPC);
			// 卡尔多。 / Kaldor.
			capture(103, Race.NPC);
			capture(104, Race.NPC);
		}, weekly);
	}

	/**
	 * 获取全部据点位置。
	 * Returns all base locations.
	 * location map
	 */
	public Map<Integer, BaseLocation> getBaseLocations() {
		return bases;
	}

	/**
	 * 按 ID 获取据点位置。
	 * Returns a base location by id.
	 * @param id 据点 ID / base id
	 * location
	 */
	public BaseLocation getBaseLocation(int id) {
		return bases.get(id);
	}

	/**
	 * 启动指定据点。
	 * Starts the base for the given id.
	 * @param id 据点 ID / base id
	 */
	public void start(final int id) {
		Base<?> base = new Base<>(getBaseLocation(id));
		if (active.putIfAbsent(id, base) != null) {
			return;
		}
		base.start();
	}

	/**
	 * 停止指定据点并立即重新启动。
	 * Stops the base for the given id and restarts it immediately.
	 * @param id 据点 ID / base id
	 */
	public void stop(int id) {
		Base<?> base = active.remove(id);
		if (base == null || base.isFinished()) {
			log.info(I18n.get("log.847b0668d197", id));
			return;
		}
		base.stop();
		start(id);
	}

	/**
	 * 将据点占领权设为指定阵营，并广播旗帜更新。
	 * Captures the base for the given race and broadcasts the flag update.
	 * @param id 据点 ID / base id
	 * @param race 占领阵营 / capturing race
	 */
	public void capture(int id, Race race) {
		if (!isActive(id)) {
			//log.info(I18n.get("log.ecbf9933ee2c"));
			return;
		}
		getActiveBase(id).setRace(race);
		stop(id);
		broadcastUpdate(getBaseLocation(id));
		getDAO().updateLocation(getBaseLocation(getBaseLocation(id).getId()));
	}

	/**
	 * 判断据点是否处于活跃状态。
	 * Returns whether the base is active.
	 * @param id 据点 ID / base id
	 * @return 若 active 则为 true / true if active
	 */
	public boolean isActive(int id) {
		return active.containsKey(id);
	}

	/**
	 * 获取活跃据点实例。
	 * Returns the active base instance.
	 * @param id 据点 ID / base id
	 * base instance
	 */
	public Base<?> getActiveBase(int id) {
		return active.get(id);
	}

	/**
	 * 玩家进入据点地图时同步旗帜与区域/任务状态。
	 * Syncs flag, zone and nearby quests when a player enters a base world.
	 * @param player 玩家 / player
	 */
	public void onEnterBaseWorld(Player player) {
		for (BaseLocation baseLocation : getBaseLocations().values()) {
			if (baseLocation.getWorldId() == player.getWorldId() && isActive(baseLocation.getId())) {
				Base<?> base = getActiveBase(baseLocation.getId());
				PacketSendUtility.sendPacket(player, new SM_FLAG_INFO(1, base.getFlag()));
				player.getController().updateZone();
				player.getController().updateNearbyQuests();
			}
		}
	}

	/**
	 * 向据点地图内玩家广播旗帜与区域更新。
	 * Broadcasts flag and zone updates to players on the base map.
	 * base location
	 */
	public void broadcastUpdate(final BaseLocation baseLocation) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(baseLocation.getWorldId()).getMainWorldMapInstance()
				.doOnAllPlayers(player -> {
					if (isActive(baseLocation.getId())) {
						Base<?> base = getActiveBase(baseLocation.getId());
						PacketSendUtility.sendPacket(player, new SM_FLAG_INFO(1, base.getFlag()));
						player.getController().updateZone();
						player.getController().updateNearbyQuests();
					}
				});
	}

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean / when no provider or bean is available
	 */
	public static BaseService getInstance() {
		ObjectProvider<BaseService> provider = instanceProvider;
		BaseService provided = provider == null ? null : provider.getIfAvailable();
		if (provided == null) {
			throw new IllegalStateException("BaseService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		return provided;
	}

	/**
	 * 获取据点 DAO。
	 * Returns the base DAO.
	 * DAO instance
	 */
	private BaseLocationDao getDAO() {
		return this.baseDAO;
	}
}
