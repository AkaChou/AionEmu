package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.CollisionDieActor;
import com.aionemu.gameserver.controllers.observer.ShieldObserver;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.shield.Shield;
import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.model.siege.SiegeShield;
import com.aionemu.gameserver.model.templates.shield.ShieldTemplate;
import com.aionemu.gameserver.world.zone.ZoneInstance;

/**
 * 护盾服务，管理球形护盾加载、生成以及攻城护盾绑定。
 * Shield service managing sphere shield loading/spawning and siege shield binding.
 */
@Slf4j
public class ShieldService {
	private static volatile ObjectProvider<ShieldService> instanceProvider;

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		protected static final ShieldService instance = new ShieldService();
	}

	private final Map<Integer, Shield> sphereShields = new HashMap<>();
	private final Map<Integer, List<SiegeShield>> registeredShields = new HashMap<>();

	/**
	 * 获取护盾服务单例（优先 Spring ObjectProvider）。
	 * Returns the shield service singleton (preferring Spring ObjectProvider).
	 *
	 * service instance
	 */
	public static final ShieldService getInstance() {
		ObjectProvider<ShieldService> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Injects the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<ShieldService> instanceProvider) {
		ShieldService.instanceProvider = instanceProvider;
	}

	/**
	 * 默认构造。
	 * Default constructor.
	 */
	public ShieldService() {
	}

	/**
	 * 加载指定地图的球形护盾模板。
	 * Loads sphere shield templates for the given map.
	 *
	 * map id
	 */
	public void load(int mapId) {
		for (ShieldTemplate template : DataManager.SHIELD_DATA.getShieldTemplates()) {
			if (template.getMap() != mapId) {
				continue;
			}
			Shield f = new Shield(template);
			sphereShields.put(f.getId(), f);
		}
	}

	/**
	 * 生成所有已加载的球形护盾，并记录未绑定攻城护盾。
	 * Spawns all loaded sphere shields and logs unbound siege shields.
	 */
	public void spawnAll() {
		for (Shield shield : sphereShields.values()) {
			shield.spawn();
			log.debug("Added " + shield.getName() + " at m=" + shield.getWorldId() + ",x=" + shield.getX() + ",y="
					+ shield.getY() + ",z=" + shield.getZ());
		}
		for (List<SiegeShield> otherShields : registeredShields.values()) {
			for (SiegeShield shield : otherShields)
				log.debug("Not bound shield " + shield.getGeometry().getName());
		}
	}

	/**
	 * 按据点 ID 创建球形护盾观察者。
	 * Creates a sphere shield observer for the given location id.
	 *
	 * location id
	 *
	 * @param observed 被观察生物 / observed creature
	 * @param observed @return 观察者，不存在时为 null / observer, or null if none
	 */
	public ActionObserver createShieldObserver(int locationId, Creature observed) {
		if (sphereShields.containsKey(locationId))
			return new ShieldObserver(sphereShields.get(locationId), observed);
		return null;
	}

	/**
	 * 按攻城几何护盾创建碰撞死亡观察者（受 GEO 开关控制）。
	 * Creates a collision-die observer for a siege geo shield (gated by GEO config).
	 *
	 * @param geoShield 攻城几何护盾 / siege geo shield
	 * @param observed 被观察生物 / observed creature
	 * @return 观察者，未启用时为 null / observer, or null if disabled
	 */
	public ActionObserver createShieldObserver(SiegeShield geoShield, Creature observed) {
		ActionObserver observer = null;
		if (GeoDataConfig.GEO_SHIELDS_ENABLE) {
			observer = new CollisionDieActor(observed, geoShield.getGeometry());
			((CollisionDieActor) observer).setEnabled(true);
		}
		return observer;
	}

	/**
	 * 向指定世界注册攻城护盾。
	 * Registers a siege shield for the given world.
	 *
	 * 世界 ID / world id
	 * siege shield
	 */
	public void registerShield(int worldId, SiegeShield shield) {
		List<SiegeShield> mapShields = registeredShields.get(worldId);
		if (mapShields == null) {
			mapShields = new ArrayList<SiegeShield>();
			registeredShields.put(worldId, mapShields);
		}
		mapShields.add(shield);
	}

	/**
	 * 将位于据点区域内的攻城护盾绑定到该据点。
	 * Attaches siege shields that lie inside the location zone to that siege location.
	 *
	 * siege location
	 */
	public void attachShield(SiegeLocation location) {
		List<SiegeShield> mapShields = registeredShields.get(location.getTemplate().getWorldId());
		if (mapShields == null)
			return;
		ZoneInstance zone = location.getZone().get(0);
		List<SiegeShield> shields = new ArrayList<SiegeShield>();
		for (int index = mapShields.size() - 1; index >= 0; index--) {
			SiegeShield shield = mapShields.get(index);
			Vector3f center = shield.getGeometry().getWorldBound().getCenter();
			if (zone.getAreaTemplate().isInside3D(center.x, center.y, center.z)) {
				shields.add(shield);
				mapShields.remove(index);
				Shield sphereShield = sphereShields.get(location.getLocationId());
				if (sphereShield != null) {
					sphereShields.remove(location.getLocationId());
				}
				shield.setSiegeLocationId(location.getLocationId());
			}
		}
		if (shields.size() == 0) {
			log.warn(I18n.get("log.da634bb6f328", location.getLocationId()));
		} else {
			location.setShields(shields);
		}
	}
}
