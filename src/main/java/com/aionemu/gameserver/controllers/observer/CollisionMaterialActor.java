package com.aionemu.gameserver.controllers.observer;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import com.aionemu.gameserver.controllers.observer.AbstractCollisionObserver.CheckType;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.geoEngine.scene.Spatial;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.materials.MaterialActTime;
import com.aionemu.gameserver.model.templates.materials.MaterialSkill;
import com.aionemu.gameserver.model.templates.materials.MaterialTemplate;
import com.aionemu.gameserver.model.templates.zone.ZoneClassName;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.utils.gametime.DayTime;
import com.aionemu.gameserver.utils.gametime.GameTime;
import com.aionemu.gameserver.utils.gametime.GameTimeManager;
import com.aionemu.gameserver.world.zone.ZoneInstance;

/**
 * 碰撞材质行为者：与材质几何碰撞时按天气/昼夜周期施放技能。
 * Collision material actor: applies skills on material-geometry collision, filtered by weather/day-night.
 */
public class CollisionMaterialActor extends AbstractCollisionObserver implements IActor {
	/** 材质行为模板 / Material action template */
	private MaterialTemplate actionTemplate;
	/** 当前生效技能列表 / Currently active skills */
	private AtomicReference<List<MaterialSkill>> currentSkills = new AtomicReference<List<MaterialSkill>>(Collections.emptyList());
	/** 不再接触时是否停止 / Whether to stop when no longer touching */
	private final boolean stopWhenNotTouching;
	/** 周期任务 / Periodic task */
	private Future<?> task;

	/**
	 * 默认 PASS 检测类型构造。
	 * Constructor with default PASS check type.
	 *
	 * @param creature 被观察生物 / observed creature
	 * @param geometry 碰撞几何体 / collision geometry
	 * @param actionTemplate 材质行为模板 / material action template
	 */
	public CollisionMaterialActor(Creature creature, Spatial geometry, MaterialTemplate actionTemplate) {
		this(creature, geometry, actionTemplate, CheckType.PASS);
	}

	/**
	 * @param creature 被观察生物 / observed creature
	 * @param geometry 碰撞几何体 / collision geometry
	 * @param actionTemplate 材质行为模板 / material action template
	 * @param checkType 检测类型 / check type
	 */
	public CollisionMaterialActor(Creature creature, Spatial geometry, MaterialTemplate actionTemplate, CheckType checkType) {
		super(creature, geometry, CollisionIntention.MATERIAL.getId(), checkType);
		this.actionTemplate = actionTemplate;
		this.stopWhenNotTouching = checkType == CheckType.TOUCH && !actsOnZoneEnter(geometry);
	}

	/**
	 * 几何体是否在进入区域时即生效（火焰等特殊命名）。
	 * Whether the geometry acts on zone enter (special fire-named meshes).
	 *
	 * @param geometry 几何体 / geometry
	 * @return 是否进入即生效 / whether acts on enter
	 */
	public static boolean actsOnZoneEnter(Spatial geometry) {
		String name = geometry.getName();
		return name.indexOf("FIRE_BOX") != -1 || name.indexOf("FIRE_SEMISPHERE") != -1 || name.indexOf("FIREPOT") != -1
				|| name.indexOf("FIRE_CYLINDER") != -1 || name.indexOf("FIRE_CONE") != -1
				|| name.startsWith("BU_H_CENTERHALL");
	}

	/**
	 * 根据目标、天气与昼夜筛选应激活的材质技能。
	 * Resolve material skills active for the target, weather and day-night.
	 *
	 * @param creature 目标生物 / target creature
	 * @return 激活技能列表 / active skill list
	 */
	private List<MaterialSkill> getSkillsForTarget(Creature creature) {
		if (creature instanceof Player) {
			Player player = (Player) creature;
			if (player.isProtectionActive()) {
				return Collections.emptyList();
			}
		}
		List<MaterialSkill> foundSkills = new ArrayList<MaterialSkill>();
		for (MaterialSkill skill : actionTemplate.getSkills()) {
			if (skill.getTarget().isTarget(creature)) {
				foundSkills.add(skill);
			}
		}
		if (foundSkills.isEmpty()) {
			return Collections.emptyList();
		}
		int weatherCode = -1;
		if (creature.getActiveRegion() == null) {
			return Collections.emptyList();
		}
		List<ZoneInstance> zones = creature.getActiveRegion().getZones(creature);
		for (ZoneInstance regionZone : zones) {
			if (regionZone.getZoneTemplate().getZoneType() == ZoneClassName.WEATHER) {
				Vector3f center = geometry.getWorldBound().getCenter();
				if (!regionZone.getAreaTemplate().isInside3D(center.x, center.y, center.z)) {
					continue;
				}
				int weatherZoneId = DataManager.ZONE_DATA.getWeatherZoneId(regionZone.getZoneTemplate());
				weatherCode = GameRuntimeServices.weatherService().getWeatherCode(creature.getWorldId(), weatherZoneId);
				break;
			}
		}

		boolean dependsOnWeather = geometry.getName().indexOf("WEATHER") != -1;
		if (dependsOnWeather && weatherCode > 0) {
			return Collections.emptyList();
		}
		GameTime gameTime = (GameTime) GameTimeManager.getGameTime().clone();
		List<MaterialSkill> activeSkills = new ArrayList<MaterialSkill>();
		for (MaterialSkill foundSkill : foundSkills) {
			if (foundSkill.getTime() == null || foundSkill.getTime() == MaterialActTime.DAY && weatherCode == 0
					|| gameTime.getDayTime() == DayTime.NIGHT && foundSkill.getTime() == MaterialActTime.NIGHT
					|| gameTime.getDayTime() != DayTime.NIGHT) {
				activeSkills.add(foundSkill);
			}
		}
		return activeSkills;
	}

	@Override
	public void onMoved(CollisionResults collisionResults) {
		if (collisionResults.size() == 0) {
			if (stopWhenNotTouching) {
				abort();
			}
			return;
		}
		act();
	}

	/**
	 * 启动或刷新材质技能周期任务。
	 * Start or refresh the material skill periodic task.
	 */
	@Override
	public synchronized void act() {
		final List<MaterialSkill> actSkills = getSkillsForTarget(creature);
		boolean skillsChanged = !currentSkills.getAndSet(actSkills).equals(actSkills);
		synchronized (creature.getController()) {
			Future<?> existingTask = creature.getController().getTask(TaskId.ZONE_MATERIAL_ACTION);
			if (!skillsChanged && existingTask != null && !existingTask.isDone()) {
				return;
			}
			cancelOwnedTask();
			if (actSkills.isEmpty()) {
				return;
			}
			final int[] secondsElapsed = new int[1];
			task = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					for (MaterialSkill actSkill : actSkills) {
						if (secondsElapsed[0] % actSkill.getFrequency() != 0) {
							continue;
						}
						if (creature.getEffectController().hasAbnormalEffect(actSkill.getId())) {
							continue;
						}
						Skill skill = GameEngineServices.skillEngine().getSkill(creature, actSkill.getId(),
								actSkill.getSkillLevel(), creature);
						skill.getEffectedList().add(creature);
						skill.useWithoutPropSkill();
					}
					secondsElapsed[0]++;
				}
			}, 0, 1000);
			creature.getController().addTask(TaskId.ZONE_MATERIAL_ACTION, task);
		}
	}

	/**
	 * 中止并清空当前材质技能任务。
	 * Abort and clear the current material skill task.
	 */
	@Override
	public synchronized void abort() {
		synchronized (creature.getController()) {
			cancelOwnedTask();
		}
		currentSkills.set(Collections.emptyList());
	}

	/**
	 * 取消本行为者持有的区域材质任务。
	 * Cancel the zone-material task owned by this actor.
	 */
	private void cancelOwnedTask() {
		if (task != null && creature.getController().getTask(TaskId.ZONE_MATERIAL_ACTION) == task) {
			creature.getController().cancelTask(TaskId.ZONE_MATERIAL_ACTION);
		}
		task = null;
	}

	@Override
	public void died(Creature creature) {
		abort();
	}

	@Override
	public void setEnabled(boolean enable) {
	};
}
