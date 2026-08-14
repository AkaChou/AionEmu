package com.aionemu.gameserver.services;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameCronServices;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.BaseDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.base.BaseLocation;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FLAG_INFO;
import com.aionemu.gameserver.services.base.Base;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 据点（Base）服务，管理据点位置初始化、启停、占领与旗帜同步。
 * Base service managing base location init, start/stop, capture and flag sync.
 *
 * @author Rinzler
 */
@Slf4j
public class BaseService {
	private static volatile ObjectProvider<BaseService> instanceProvider;
	/** 当前活跃据点实例。 / Currently active base instances. */
	private final ConcurrentMap<Integer, Base<?>> active = new ConcurrentHashMap<Integer, Base<?>>();
	/** 据点位置映射。 / Base location map. */
	private Map<Integer, BaseLocation> bases;

	/**
	 * 初始化据点位置数据并加载数据库状态。
	 * Initializes base locations and loads their database state.
	 */
	public void initBaseLocations() {
		bases = DataManager.BASE_DATA.getBaseLocations();
		DAOManager.getDAO(BaseDAO.class).loadBaseLocations(bases);
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
		GameCronServices.cronService().schedule(new Runnable() {
			public void run() {
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
			}
		}, weekly);
	}

	/**
	 * 获取全部据点位置。
	 * Returns all base locations.
	 *
	 * location map
	 */
	public Map<Integer, BaseLocation> getBaseLocations() {
		return bases;
	}

	/**
	 * 按 ID 获取据点位置。
	 * Returns a base location by id.
	 *
	 * @param id 据点 ID / base id
	 * location
	 */
	public BaseLocation getBaseLocation(int id) {
		return bases.get(id);
	}

	/**
	 * 启动指定据点。
	 * Starts the base for the given id.
	 *
	 * @param id 据点 ID / base id
	 */
	public void start(final int id) {
		Base<?> base = new Base<BaseLocation>(getBaseLocation(id));
		if (active.putIfAbsent(id, base) != null) {
			return;
		}
		base.start();
	}

	/**
	 * 停止指定据点并立即重新启动。
	 * Stops the base for the given id and restarts it immediately.
	 *
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
	 *
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
	 *
	 * @param id 据点 ID / base id
	 * @return 若 active 则为 true / true if active
	 */
	public boolean isActive(int id) {
		return active.containsKey(id);
	}

	/**
	 * 获取活跃据点实例。
	 * Returns the active base instance.
	 *
	 * @param id 据点 ID / base id
	 * base instance
	 */
	public Base<?> getActiveBase(int id) {
		return active.get(id);
	}

	/**
	 * 玩家进入据点地图时同步旗帜与区域/任务状态。
	 * Syncs flag, zone and nearby quests when a player enters a base world.
	 *
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
	 *
	 * base location
	 */
	public void broadcastUpdate(final BaseLocation baseLocation) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(baseLocation.getWorldId()).getMainWorldMapInstance()
				.doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						if (isActive(baseLocation.getId())) {
							Base<?> base = getActiveBase(baseLocation.getId());
							PacketSendUtility.sendPacket(player, new SM_FLAG_INFO(1, base.getFlag()));
							player.getController().updateZone();
							player.getController().updateNearbyQuests();
						}
					}
				});
	}

	/**
	 * 获取服务单例，优先走 Spring ObjectProvider。
	 * Returns the service singleton, preferring Spring ObjectProvider when available.
	 *
	 * service instance
	 */
	public static BaseService getInstance() {
		ObjectProvider<BaseService> provider = instanceProvider;
		if (provider == null) {
			return BaseServiceHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> BaseServiceHolder.INSTANCE);
	}

	/**
	 * 注入 Spring ObjectProvider 以覆盖默认单例。
	 * Injects a Spring ObjectProvider to override the default singleton.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<BaseService> instanceProvider) {
		BaseService.instanceProvider = instanceProvider;
	}

	private static class BaseServiceHolder {
		private static final BaseService INSTANCE = new BaseService();
	}

	/**
	 * 获取据点 DAO。
	 * Returns the base DAO.
	 *
	 * DAO instance
	 */
	private BaseDAO getDAO() {
		return DAOManager.getDAO(BaseDAO.class);
	}
}
