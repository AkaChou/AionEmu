package com.aionemu.gameserver.controllers.observer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import com.aionemu.gameserver.configs.main.GeoDataConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.materials.MaterialSkill;
import com.aionemu.gameserver.model.templates.materials.MaterialTemplate;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.geo.GeoService;

public class TerrainZoneCollisionMaterialActor extends ActionObserver implements IActor {

	private final Creature creature;
	private final AtomicReference<List<MaterialSkill>> currentSkills = new AtomicReference<List<MaterialSkill>>(Collections.emptyList());
	private int lastMaterialId;

	public TerrainZoneCollisionMaterialActor(Creature creature) {
		super(ObserverType.MOVE_OR_DIE);
		this.creature = creature;
	}

	@Override
	public void moved() {
		int materialId = GeoService.getInstance().getTerrainMaterialAt(creature.getWorldId(), creature.getX(), creature.getY(), creature.getZ(),
				creature.getInstanceId());
		if (materialId == lastMaterialId && !currentSkills.get().isEmpty()) {
			return;
		}
		lastMaterialId = materialId;
		List<MaterialSkill> skills = findSkills(materialId);
		if (!currentSkills.getAndSet(skills).equals(skills)) {
			cancelTask();
			if (!skills.isEmpty()) {
				start(skills);
			}
		}
	}

	private List<MaterialSkill> findSkills(int materialId) {
		if (materialId == 0 || DataManager.MATERIAL_DATA == null) {
			return Collections.emptyList();
		}
		MaterialTemplate template = DataManager.MATERIAL_DATA.getTemplate(materialId);
		if (template == null) {
			return Collections.emptyList();
		}
		List<MaterialSkill> matches = new ArrayList<MaterialSkill>();
		for (MaterialSkill skill : template.getSkills()) {
			if (skill.getTarget().isTarget(creature)) {
				matches.add(skill);
			}
		}
		return matches;
	}

	private void start(final List<MaterialSkill> materialSkills) {
		if (materialSkills.isEmpty()) {
			return;
		}
		final int[] secondsElapsed = new int[1];
		Future<?> task = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				for (MaterialSkill materialSkill : materialSkills) {
					if (secondsElapsed[0] % materialSkill.getFrequency() != 0 || creature.getEffectController().hasAbnormalEffect(materialSkill.getId())) {
						continue;
					}
					if (GeoDataConfig.GEO_MATERIALS_SHOWDETAILS && creature instanceof Player && ((Player) creature).isGM()) {
						PacketSendUtility.sendMessage((Player) creature, "Use terrain material skill=" + materialSkill.getId());
					}
					Skill skill = GameEngineServices.skillEngine().getSkill(creature, materialSkill.getId(), materialSkill.getSkillLevel(), creature);
					skill.getEffectedList().add(creature);
					skill.useWithoutPropSkill();
				}
				secondsElapsed[0]++;
			}
		}, 0, 1000);
		creature.getController().addTask(TaskId.TERRAIN_MATERIAL_ACTION, task);
	}

	@Override
	public void act() {
		List<MaterialSkill> skills = currentSkills.get();
		if (!skills.isEmpty()) {
			cancelTask();
			start(skills);
		}
	}

	@Override
	public void abort() {
		cancelTask();
		currentSkills.set(Collections.emptyList());
	}

	private void cancelTask() {
		Future<?> existingTask = creature.getController().getTask(TaskId.TERRAIN_MATERIAL_ACTION);
		if (existingTask != null) {
			creature.getController().cancelTask(TaskId.TERRAIN_MATERIAL_ACTION);
		}
	}

	@Override
	public void died(Creature creature) {
		abort();
	}

	@Override
	public void setEnabled(boolean enable) {
	}
}
