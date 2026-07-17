package com.aionemu.gameserver.world.zone.scripts;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.controllers.observer.CollisionDieActor;
import com.aionemu.gameserver.geoEngine.scene.Spatial;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.handler.ZoneHandler;
import com.aionemu.gameserver.world.zone.handler.ZoneNameAnnotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 欧比斯核心碰撞区：非 GM 玩家进入时挂载碰撞致死观察者。
 * Abyss core collision zone: attaches a collision-die observer for non-GM players on enter.
 */
@ZoneNameAnnotation("CORE_400010000")
@Slf4j
public class AbyssCore implements ZoneHandler {

	/** 核心几何体资源路径 / core geometry resource path */
	private static final String CORE_GEOMETRY = "levels/common/abyss/abground/landmark/ground_a/na_ab_lmark_col_01a.cgf";

	/** 已挂载的碰撞观察者 / attached collision observers */
	Map<Integer, CollisionDieActor> observed = new ConcurrentHashMap<Integer, CollisionDieActor>();

	/** 核心几何体 / core geometry */
	private final Spatial geometry;

	/**
	 * 加载欧比斯核心碰撞几何体。
	 * Load the abyss-core collision geometry.
	 */
	public AbyssCore() {
		geometry = GameWorldServices.geoService().getGeometry(400010000, CORE_GEOMETRY);
		if (geometry == null) {
			log.error(I18n.get("log.a1345eb67e87", CORE_GEOMETRY));
		}
	}

	/**
	 * 进入核心区：为非 GM 玩家挂载碰撞致死观察者。
	 * Enter core zone: attach a collision-die observer for non-GM players.
	 *
	 * creature
	 * @param zone     区域实例 / zone instance
	 */
	@Override
	public void onEnterZone(Creature creature, ZoneInstance zone) {
		Creature acting = creature.getActingCreature();
		if (geometry != null && acting instanceof Player && !((Player) acting).isGM()) {
			CollisionDieActor observer = new CollisionDieActor(creature, geometry);
			creature.getObserveController().addObserver(observer);
			observed.put(creature.getObjectId(), observer);
		}
	}

	/**
	 * 离开核心区：移除碰撞致死观察者。
	 * Leave core zone: remove the collision-die observer.
	 *
	 * creature
	 * @param zone     区域实例 / zone instance
	 */
	@Override
	public void onLeaveZone(Creature creature, ZoneInstance zone) {
		Creature acting = creature.getActingCreature();
		if (acting instanceof Player && !((Player) acting).isGM()) {
			CollisionDieActor observer = observed.get(creature.getObjectId());
			if (observer != null) {
				creature.getObserveController().removeObserver(observer);
				observed.remove(creature.getObjectId());
			}
		}
	}
}
