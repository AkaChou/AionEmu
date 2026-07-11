package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameCronServices;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.OutpostDAO;
import com.aionemu.gameserver.dao.SiegeDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.outpost.OutpostLocation;
import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.model.siege.SiegeModType;
import com.aionemu.gameserver.model.siege.SiegeRace;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FLAG_INFO;
import com.aionemu.gameserver.services.outpost.Outpost;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 前哨（Outpost）服务，管理前哨点位初始化、占领、重置与旗帜广播。
 * Outpost service managing location init, capture, weekly reset, and flag broadcast.
 *
 * @author Wnkrz
 */
@Slf4j
public class OutpostService {
	private static volatile ObjectProvider<OutpostService> instanceProvider;

	/** 当前活跃的前哨实例。 / Currently active outpost instances. */
	private final ConcurrentMap<Integer, Outpost<?>> active = new ConcurrentHashMap<Integer, Outpost<?>>();
	/** 前哨点位数据。 / Outpost location data. */
	private Map<Integer, OutpostLocation> outposts;

	/**
	 * 初始化前哨点位数据并加载数据库状态。
	 * Initializes outpost location data and loads DB state.
	 */
	public void initOutpostLocations() {
		log.info(I18n.get("log.e8bd199248a9"));
		outposts = DataManager.OUTPOST_DATA.getOutpostLocations();
		DAOManager.getDAO(OutpostDAO.class).loadOutposLocations(outposts);
	}

	/**
	 * 启动所有已配置的前哨。
	 * Starts all configured outposts.
	 */
	public void initOutposts() {
		for (OutpostLocation outpost : getOutpostLocations().values()) {
			start(outpost.getId());
		}
	}

	/**
	 * 注册每周三 9:00 的前哨与神器重置任务（因吉森 / 格克马洛斯）。
	 * Schedules the weekly Wed 09:00 outpost/artifact reset for Inggison and Gelkmaros.
	 */
	public void initOupostReset() {
		Race race = null;
		log.info(I18n.get("log.9028be61d10e"));
		String weekly = "0 0 9 ? * WED *";
		GameCronServices.cronService().schedule(new Runnable() {
			public void run() {
				// 因格森。 / Inggison.
				capture(101, Race.NPC);
				capture(102, Race.NPC);
				capture(103, Race.NPC);
				capture(104, Race.NPC);
				capture(105, Race.NPC);
				capture(106, Race.NPC);
				capture(107, Race.NPC);
				captureArtifact(101, Race.NPC);
				captureArtifact(102, Race.NPC);
				captureArtifact(103, Race.NPC);
				captureArtifact(104, Race.NPC);
				captureArtifact(105, Race.NPC);
				captureArtifact(106, Race.NPC);
				captureArtifact(107, Race.NPC);
				// 吉尔克马罗斯。 / Gelkmaros.
				capture(201, Race.NPC);
				capture(202, Race.NPC);
				capture(203, Race.NPC);
				capture(204, Race.NPC);
				capture(205, Race.NPC);
				capture(206, Race.NPC);
				capture(207, Race.NPC);
				captureArtifact(201, Race.NPC);
				captureArtifact(202, Race.NPC);
				captureArtifact(203, Race.NPC);
				captureArtifact(204, Race.NPC);
				captureArtifact(205, Race.NPC);
				captureArtifact(206, Race.NPC);
				captureArtifact(207, Race.NPC);
			}
		}, weekly);
	}

	/**
	 * 获取全部前哨点位。
	 * Returns all outpost locations.
	 *
	 * location map
	 */
	public Map<Integer, OutpostLocation> getOutpostLocations() {
		return outposts;
	}

	/**
	 * 按 ID 获取前哨点位。
	 * Returns an outpost location by id.
	 *
	 * @param id 前哨 ID / outpost id
	 * location
	 */
	public OutpostLocation getOutpostLocation(int id) {
		return outposts.get(id);
	}

	/**
	 * 启动指定前哨；若已活跃则忽略。
	 * Starts the given outpost; no-ops if already active.
	 *
	 * @param id 前哨 ID / outpost id
	 */
	public void start(final int id) {
		Outpost<?> outpost = new Outpost<>(getOutpostLocation(id));
		if (active.putIfAbsent(id, outpost) != null) {
			return;
		}
		outpost.start();
	}

	/**
	 * 停止指定前哨并立即重新启动。
	 * Stops the given outpost and restarts it immediately.
	 *
	 * @param id 前哨 ID / outpost id
	 */
	public void stop(int id) {
		Outpost<?> outpost = active.remove(id);
		if (outpost == null || outpost.isFinished()) {
			log.info(I18n.get("log.7283db504b30", id));
			return;
		}
		outpost.stop();
		start(id);
	}

	/**
	 * 将前哨占领权设为指定种族并广播更新。
	 * Sets outpost ownership to the given race and broadcasts the update.
	 *
	 * @param id 前哨 ID / outpost id
	 * @param race 占领种族 / capturing race
	 */
	public void capture(int id, Race race) {
		Outpost<?> outpost = getActiveOutpost(id);
		if (outpost == null) {
			log.info(I18n.get("log.0094a6179979"));
			return;
		}
		OutpostLocation outpostLocation = getOutpostLocation(id);
		outpost.setRace(race);
		stop(id);
		broadcastUpdate(outpostLocation);
		getDAO().updateLocation(outpostLocation);
	}

	/**
	 * 同步占领与前哨绑定的神器据点。
	 * Captures the artifact siege location bound to the outpost.
	 *
	 * @param id 前哨 ID / outpost id
	 * @param race 占领种族 / capturing race
	 */
	public void captureArtifact(int id, Race race) {
		// 占领神器。 / Capture Artifact.
		SiegeRace sr = null;
		if (race == Race.ASMODIANS) {
			sr = SiegeRace.ASMODIANS;
		} else if (race == Race.ELYOS) {
			sr = SiegeRace.ELYOS;
		} else {
			sr = SiegeRace.BALAUR;
		}
		SiegeLocation loc = GameFeatureServices.siegeService().getSiegeLocation(getOutpostLocation(id).getArtifactId());
		GameFeatureServices.siegeService().deSpawnNpcs(getOutpostLocation(id).getArtifactId());
		loc.setVulnerable(false);
		loc.setUnderShield(false);
		loc.setRace(sr);
		loc.setLegionId(0);
		GameFeatureServices.siegeService().spawnNpcs(getOutpostLocation(id).getArtifactId(), sr, SiegeModType.SIEGE);
		DAOManager.getDAO(SiegeDAO.class).updateSiegeLocation(loc);
	}

	/**
	 * 判断前哨是否处于活跃状态。
	 * Returns whether the outpost is currently active.
	 *
	 * @param id 前哨 ID / outpost id
	 * active flag
	 */
	public boolean isActive(int id) {
		return active.containsKey(id);
	}

	/**
	 * 获取活跃前哨实例。
	 * Returns the active outpost instance.
	 *
	 * @param id 前哨 ID / outpost id
	 * outpost instance
	 */
	public Outpost<?> getActiveOutpost(int id) {
		return active.get(id);
	}

	/**
	 * 玩家进入前哨所在地图时下发旗帜信息并刷新区域/任务。
	 * On map enter, sends flag info and refreshes zone/quests for matching outposts.
	 *
	 * @param player 进入地图的玩家 / entering player
	 */
	public void onEnterOutpostWorld(Player player) {
		for (OutpostLocation outpostLocation : getOutpostLocations().values()) {
			if (outpostLocation.getWorldId() == player.getWorldId()) {
				Outpost<?> outpost = getActiveOutpost(outpostLocation.getId());
				if (outpost == null) {
					continue;
				}
				PacketSendUtility.sendPacket(player, new SM_FLAG_INFO(1, outpost.getFlag()));
				player.getController().updateZone();
				player.getController().updateNearbyQuests();
			}
		}
	}

	/**
	 * 向前哨地图内所有玩家广播旗帜与区域更新。
	 * Broadcasts flag and zone updates to all players on the outpost map.
	 *
	 * outpost location
	 */
	public void broadcastUpdate(final OutpostLocation outpostLocation) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(outpostLocation.getWorldId()).getMainWorldMapInstance()
				.doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						Outpost<?> outpost = getActiveOutpost(outpostLocation.getId());
						if (outpost == null) {
							return;
						}
						PacketSendUtility.sendPacket(player, new SM_FLAG_INFO(1, outpost.getFlag()));
						player.getController().updateZone();
						player.getController().updateNearbyQuests();
					}
				});
	}

	/**
	 * 获取服务单例，优先走 Spring ObjectProvider。
	 * Returns the service singleton, preferring Spring ObjectProvider when available.
	 *
	 * service instance
	 */
	public static OutpostService getInstance() {
		ObjectProvider<OutpostService> provider = instanceProvider;
		if (provider == null) {
			return OutpostServiceHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> OutpostServiceHolder.INSTANCE);
	}

	/**
	 * 注入 Spring ObjectProvider。
	 * Injects the Spring ObjectProvider.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<OutpostService> instanceProvider) {
		OutpostService.instanceProvider = instanceProvider;
	}

	private static class OutpostServiceHolder {
		private static final OutpostService INSTANCE = new OutpostService();
	}

	/**
	 * 获取前哨 DAO。
	 * Returns the outpost DAO.
	 *
	 * DAO instance
	 */
	private OutpostDAO getDAO() {
		return DAOManager.getDAO(OutpostDAO.class);
	}
}
