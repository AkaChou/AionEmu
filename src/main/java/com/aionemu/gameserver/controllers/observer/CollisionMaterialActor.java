/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.controllers.observer;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.controllers.observer.AbstractCollisionObserver.CheckType;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.collision.CollisionResult;
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
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.gametime.DayTime;
import com.aionemu.gameserver.utils.gametime.GameTime;
import com.aionemu.gameserver.utils.gametime.GameTimeManager;
import com.aionemu.gameserver.world.zone.ZoneInstance;

public class CollisionMaterialActor extends AbstractCollisionObserver implements IActor {
	private MaterialTemplate actionTemplate;
	private AtomicReference<List<MaterialSkill>> currentSkills = new AtomicReference<List<MaterialSkill>>(Collections.emptyList());

	public CollisionMaterialActor(Creature creature, Spatial geometry, MaterialTemplate actionTemplate) {
		this(creature, geometry, actionTemplate, CheckType.PASS);
	}

	public CollisionMaterialActor(Creature creature, Spatial geometry, MaterialTemplate actionTemplate, CheckType checkType) {
		super(creature, geometry, CollisionIntention.MATERIAL.getId(), checkType);
		this.actionTemplate = actionTemplate;
	}

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
			return;
		} else {
			if (GeoDataConfig.GEO_MATERIALS_SHOWDETAILS && creature instanceof Player) {
				Player player = (Player) creature;
				if (player.isGM()) {
					CollisionResult result = collisionResults.getClosestCollision();
					PacketSendUtility.sendMessage(player, "Entered " + result.getGeometry().getName());
				}
			}
			act();
		}
	}

	@Override
	public void act() {
		final List<MaterialSkill> actSkills = getSkillsForTarget(creature);
		if (!currentSkills.getAndSet(actSkills).equals(actSkills)) {
			Future<?> existingTask = creature.getController().getTask(TaskId.MATERIAL_ACTION);
			if (existingTask != null) {
				creature.getController().cancelTask(TaskId.MATERIAL_ACTION);
			}
			if (actSkills.isEmpty()) {
				return;
			}
			final int[] secondsElapsed = new int[1];
			Future<?> task = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					for (MaterialSkill actSkill : actSkills) {
						if (secondsElapsed[0] % actSkill.getFrequency() != 0) {
							continue;
						}
						if (creature.getEffectController().hasAbnormalEffect(actSkill.getId())) {
							continue;
						}
						if (GeoDataConfig.GEO_MATERIALS_SHOWDETAILS && creature instanceof Player) {
							Player player = (Player) creature;
							if (player.isGM()) {
								PacketSendUtility.sendMessage(player, "Use skill=" + actSkill.getId());
							}
						}
						Skill skill = GameEngineServices.skillEngine().getSkill(creature, actSkill.getId(),
								actSkill.getSkillLevel(), creature);
						skill.getEffectedList().add(creature);
						skill.useWithoutPropSkill();
					}
					secondsElapsed[0]++;
				}
			}, 0, 1000);
			creature.getController().addTask(TaskId.MATERIAL_ACTION, task);
		}
	}

	@Override
	public void abort() {
		Future<?> existingTask = creature.getController().getTask(TaskId.MATERIAL_ACTION);
		if (existingTask != null) {
			creature.getController().cancelTask(TaskId.MATERIAL_ACTION);
		}
		currentSkills.set(Collections.emptyList());
	}

	@Override
	public void died(Creature creature) {
		abort();
	}

	@Override
	public void setEnabled(boolean enable) {
	};
}
