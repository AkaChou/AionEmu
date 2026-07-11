package com.aionemu.gameserver.services;

import com.aionemu.boot.i18n.I18n;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.AbyssSpecialLandingDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.landing_special.LandingSpecialLocation;
import com.aionemu.gameserver.model.landing_special.LandingSpecialStateType;
import com.aionemu.gameserver.model.templates.spawns.SpawnGroup2;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.landingspecialspawns.LandingSpecialSpawnTemplate;
import com.aionemu.gameserver.services.abysslandingservice.landingspecialservice.SPLanding;
import com.aionemu.gameserver.services.abysslandingservice.landingspecialservice.SpecialLanding;
import com.aionemu.gameserver.spawnengine.SpawnEngine;

/**
 * 欧比斯特殊登陆点服务，管理特殊登陆点的初始化、启停与刷怪。
 * Abyss special landing service managing location init, start/stop and spawns.
 */
@Slf4j(topic = "com.aionemu.gameserver.services.AbyssLandingService")
public class AbyssLandingSpecialService {
	private static volatile ObjectProvider<AbyssLandingSpecialService> instanceProvider;
	/** 特殊登陆点 ID → 位置。 / Special landing id → location. */
	private static Map<Integer, LandingSpecialLocation> abyssSpecialLanding;
	/** 当前活跃的特殊登陆实例。 / Currently active special landing instances. */
	private final ConcurrentMap<Integer, SpecialLanding<?>> activeSpecialLanding = new ConcurrentHashMap<Integer, SpecialLanding<?>>();

	/**
	 * 初始化特殊登陆点：加载模板与数据库状态，并刷新 ACTIVE 位置。
	 * Initializes special landings: loads templates and DB state, then spawns ACTIVE locations.
	 */
	public void initLandingSpecialLocations() {
		abyssSpecialLanding = DataManager.LANDING_SPECIAL_LOCATION_DATA.getLandingSpecialLocations();
		DAOManager.getDAO(AbyssSpecialLandingDAO.class).loadLandingSpecialLocations(abyssSpecialLanding);
		for (LandingSpecialLocation loc : getLandingSpecialLocations().values()) {
			if (loc.getType().equals(LandingSpecialStateType.ACTIVE)) {
				spawn(loc, LandingSpecialStateType.ACTIVE);
			}
			log.info(I18n.get("log.f1b25999792d", loc.getId(), loc.getType()));
		}
		log.info(I18n.get("log.73861d844d0c", abyssSpecialLanding.size()));
	}

	/**
	 * 启动指定特殊登陆点。
	 * Starts the special landing for the given id.
	 *
	 * @param id 登陆点 ID / landing id
	 */
	public void startLanding(final int id) {
		SpecialLanding<?> land = new SPLanding(abyssSpecialLanding.get(id));
		if (activeSpecialLanding.putIfAbsent(id, land) != null) {
			return;
		}
		land.start();
	}

	/**
	 * 停止指定特殊登陆点。
	 * Stops the special landing for the given id.
	 *
	 * @param id 登陆点 ID / landing id
	 */
	public void stopLanding(int id) {
		SpecialLanding<?> landing = activeSpecialLanding.remove(id);
		if (landing == null) {
			return;
		}
		landing.stop();
	}

	/**
	 * 按状态刷新特殊登陆点怪物。
	 * Spawns NPCs for a special landing location by state.
	 *
	 * @param loc 登陆点位置 / landing location
	 * target state
	 */
	public static void spawn(LandingSpecialLocation loc, LandingSpecialStateType fstate) {
		if (fstate.equals(LandingSpecialStateType.ACTIVE)) {
			List<SpawnGroup2> locSpawns = DataManager.SPAWNS_DATA2.getLandingSpecialSpawnsByLocId(loc.getId());
			for (SpawnGroup2 group : locSpawns) {
				for (SpawnTemplate st : group.getSpawnTemplates()) {
					LandingSpecialSpawnTemplate landingtTemplate = (LandingSpecialSpawnTemplate) st;
					if (landingtTemplate.getFStateType().equals(fstate)) {
						loc.getSpawned().add(SpawnEngine.spawnObject(landingtTemplate, 1));
					}
				}
			}
		}
	}

	/**
	 * 将登陆点状态持久化到数据库。
	 * Persists the landing location state to the database.
	 *
	 * @param loc 登陆点位置 / landing location
	 */
	public static void onSave(LandingSpecialLocation loc) {
		getDAO().updateLocation(loc);
	}

	/**
	 * 清理并删除特殊登陆点已刷出的 NPC。
	 * Despawns and clears NPCs for the special landing location.
	 *
	 * @param loc 登陆点位置 / landing location
	 */
	public static void despawn(LandingSpecialLocation loc) {
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
	 * 获取服务单例，优先走 Spring ObjectProvider。
	 * Returns the service singleton, preferring Spring ObjectProvider when available.
	 *
	 * service instance
	 */
	public static AbyssLandingSpecialService getInstance() {
		ObjectProvider<AbyssLandingSpecialService> provider = instanceProvider;
		if (provider == null) {
			return AbyssLandingSpecialService.SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> AbyssLandingSpecialService.SingletonHolder.instance);
	}

	/**
	 * 注入 Spring ObjectProvider 以覆盖默认单例。
	 * Injects a Spring ObjectProvider to override the default singleton.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<AbyssLandingSpecialService> instanceProvider) {
		AbyssLandingSpecialService.instanceProvider = instanceProvider;
	}

	private static class SingletonHolder {
		protected static final AbyssLandingSpecialService instance = new AbyssLandingSpecialService();
	}

	/**
	 * 按 ID 获取特殊登陆点位置。
	 * Returns the special landing location by id.
	 *
	 * @param id 登陆点 ID / landing id
	 * location
	 */
	public LandingSpecialLocation getLandingSpecialLocation(int id) {
		return abyssSpecialLanding.get(id);
	}

	/**
	 * 获取全部特殊登陆点位置映射。
	 * Returns the full special landing location map.
	 *
	 * location map
	 */
	public static Map<Integer, LandingSpecialLocation> getLandingSpecialLocations() {
		return abyssSpecialLanding;
	}

	/**
	 * 获取特殊登陆点 DAO。
	 * Returns the special landing DAO.
	 *
	 * DAO instance
	 */
	public static AbyssSpecialLandingDAO getDAO() {
		return DAOManager.getDAO(AbyssSpecialLandingDAO.class);
	}
}
