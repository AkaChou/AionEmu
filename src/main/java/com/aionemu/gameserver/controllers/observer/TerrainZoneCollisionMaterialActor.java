package com.aionemu.gameserver.controllers.observer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.templates.materials.MaterialSkill;
import com.aionemu.gameserver.model.templates.materials.MaterialTemplate;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.world.geo.GeoService;

/**
 * 地形区域材质行为者：根据脚下地形材质周期施加技能。
 * Terrain-zone material actor: periodically applies skills based on terrain material underfoot.
 */
public class TerrainZoneCollisionMaterialActor extends ActionObserver implements IActor {

	/** 被观察生物 / Observed creature */
	private final Creature creature;
	/** 当前生效的材质技能列表 / Currently active material skills */
	private final AtomicReference<List<MaterialSkill>> currentSkills = new AtomicReference<List<MaterialSkill>>(Collections.emptyList());
	/** 上次材质 ID / Last material id */
	private int lastMaterialId;
	/** 周期任务 / Periodic task */
	private Future<?> task;

	/**
	 * creature
	 */
	public TerrainZoneCollisionMaterialActor(Creature creature) {
		super(ObserverType.MOVE_OR_DIE);
		this.creature = creature;
	}

	@Override
	public synchronized void moved() {
		int materialId = GeoService.getInstance().getTerrainMaterialAt(creature.getWorldId(), creature.getX(), creature.getY(), creature.getZ(),
				creature.getInstanceId());
		if (materialId == lastMaterialId && !currentSkills.get().isEmpty() && hasActiveTask()) {
			return;
		}
		lastMaterialId = materialId;
		List<MaterialSkill> skills = findSkills(materialId);
		boolean skillsChanged = !currentSkills.getAndSet(skills).equals(skills);
		if (skillsChanged || !skills.isEmpty() && !hasActiveTask()) {
			synchronized (creature.getController()) {
				cancelOwnedTask();
				if (!skills.isEmpty()) {
					start(skills);
				}
			}
		}
	}

	/**
	 * 按材质 ID 查找适用于当前生物的技能。
	 * Find skills applicable to the creature for the given material id.
	 *
	 * material id
	 *
	 * @param materialId @return 匹配技能列表 / matching skill list
	 */
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

	/**
	 * 启动材质技能周期任务。
	 * Start the material skill periodic task.
	 *
	 * material skills
	 */
	private void start(final List<MaterialSkill> materialSkills) {
		if (materialSkills.isEmpty()) {
			return;
		}
		final int[] secondsElapsed = new int[1];
		task = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				for (MaterialSkill materialSkill : materialSkills) {
					if (secondsElapsed[0] % materialSkill.getFrequency() != 0 || creature.getEffectController().hasAbnormalEffect(materialSkill.getId())) {
						continue;
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
	public synchronized void act() {
		List<MaterialSkill> skills = currentSkills.get();
		if (!skills.isEmpty()) {
			synchronized (creature.getController()) {
				cancelOwnedTask();
				start(skills);
			}
		}
	}

	@Override
	public synchronized void abort() {
		synchronized (creature.getController()) {
			cancelOwnedTask();
		}
		currentSkills.set(Collections.emptyList());
		lastMaterialId = 0;
	}

	/**
	 * 是否存在由本行为者持有的活跃任务。
	 * Whether an active task owned by this actor exists.
	 *
	 * whether active
	 */
	private boolean hasActiveTask() {
		synchronized (creature.getController()) {
			return task != null && creature.getController().getTask(TaskId.TERRAIN_MATERIAL_ACTION) == task && !task.isDone();
		}
	}

	/**
	 * 取消本行为者持有的控制器任务。
	 * Cancel the controller task owned by this actor.
	 */
	private void cancelOwnedTask() {
		if (task != null && creature.getController().getTask(TaskId.TERRAIN_MATERIAL_ACTION) == task) {
			creature.getController().cancelTask(TaskId.TERRAIN_MATERIAL_ACTION);
		}
		task = null;
	}

	@Override
	public void died(Creature creature) {
		abort();
	}

	@Override
	public void setEnabled(boolean enable) {
	}
}
