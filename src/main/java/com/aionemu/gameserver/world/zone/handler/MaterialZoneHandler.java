package com.aionemu.gameserver.world.zone.handler;

import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.AbstractCollisionObserver.CheckType;
import com.aionemu.gameserver.controllers.observer.CollisionMaterialActor;
import com.aionemu.gameserver.controllers.observer.IActor;
import com.aionemu.gameserver.geoEngine.scene.Spatial;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.templates.materials.MaterialSkill;
import com.aionemu.gameserver.model.templates.materials.MaterialTemplate;
import com.aionemu.gameserver.world.zone.ZoneInstance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 材质碰撞区域处理器：进入时挂载材质技能观察者，离开时卸载。
 * Material-collision zone handler: attaches a material-skill observer on enter and removes it on leave.
 *
 * @author Rolandas
 */
public class MaterialZoneHandler implements ZoneHandler {

	/** 当前区域内已观察的生物 / creatures currently observed inside the zone */
	Map<Integer, IActor> observed = new ConcurrentHashMap<Integer, IActor>();

	/** 材质几何体 / material geometry */
	private Spatial geometry;
	/** 材质模板 / material template */
	private MaterialTemplate template;
	/** 是否在进入时立即触发材质效果 / whether to act immediately on zone enter */
	private boolean actOnEnter = false;
	/** 所属阵营（同阵营不受影响）/ owner race (same race is immune) */
	private Race ownerRace = Race.NONE;

	/**
	 * 创建材质区域处理器。
	 * Create a material zone handler.
	 *
	 * geometry
	 * material template
	 */
	public MaterialZoneHandler(Spatial geometry, MaterialTemplate template) {
		this.geometry = geometry;
		this.template = template;
		String name = geometry.getName();
		actOnEnter = CollisionMaterialActor.actsOnZoneEnter(geometry);
		if (name.startsWith("BU_AB_DARKSP")) {
			ownerRace = Race.ASMODIANS;
		} else if (name.startsWith("BU_AB_LIGHTSP")) {
			ownerRace = Race.ELYOS;
		}
	}

	/**
	 * 进入材质区：为匹配目标挂载碰撞材质观察者。
	 * Enter material zone: attach a collision-material observer for matching targets.
	 *
	 * creature
	 * @param zone     区域实例 / zone instance
	 */
	@Override
	public void onEnterZone(Creature creature, ZoneInstance zone) {
		if (ownerRace == creature.getRace()) {
			return;
		}
		MaterialSkill foundSkill = null;
		for (MaterialSkill skill : template.getSkills()) {
			if (skill.getTarget().isTarget(creature)) {
				foundSkill = skill;
				break;
			}
		}
		if (foundSkill == null) {
			return;
		}
		CheckType checkType = geometry.getMaterialId() >= 14 && geometry.getMaterialId() <= 16 ? CheckType.PASS : CheckType.TOUCH;
		CollisionMaterialActor actor = new CollisionMaterialActor(creature, geometry, template, checkType);
		creature.getObserveController().addObserver(actor);
		observed.put(creature.getObjectId(), actor);
		if (actOnEnter) {
			actor.act();
		}
		actor.moved();
	}

	/**
	 * 离开材质区：移除并中止材质观察者。
	 * Leave material zone: remove and abort the material observer.
	 *
	 * creature
	 * @param zone     区域实例 / zone instance
	 */
	@Override
	public void onLeaveZone(Creature creature, ZoneInstance zone) {
		IActor actor = observed.get(creature.getObjectId());
		if (actor != null) {
			creature.getObserveController().removeObserver((ActionObserver) actor);
			observed.remove(creature.getObjectId());
			actor.abort();
		}
	}
}
