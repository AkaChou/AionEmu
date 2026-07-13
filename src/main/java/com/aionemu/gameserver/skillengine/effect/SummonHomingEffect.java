package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.Future;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Homing;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.spawnengine.VisibleObjectSpawner;

/**
 * 召唤追踪弹/自导弹效果：生成若干次攻击次数受限的追踪单位。
 * Summon homing effect: spawns homing units limited by attack count.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SummonHomingEffect")
public class SummonHomingEffect extends SummonEffect {

	@XmlAttribute(name = "npc_count", required = true)
	protected int npcCount;
	@XmlAttribute(name = "attack_count", required = true)
	protected int attackCount;
	@XmlAttribute(name = "attack_count_delta")
	protected int attackCountDelta;
	@XmlAttribute(name = "homing_id")
	protected int homingId;
	@XmlAttribute(name = "skill_id", required = false)
	protected int skillId;

	/**
	 * 按 npc_count 生成 Homing，注册攻击次数观察者与超时删除，并切入攻击。
	 * Spawns Homing units, attaches attack-count observers and despawn tasks, then starts attack AI.
	 */
	@Override
	public void applyEffect(Effect effect) {
		Creature effector = effect.getEffector();
		float x = effector.getX();
		float y = effector.getY();
		float z = effector.getZ();
		byte heading = effector.getHeading();
		int worldId = effector.getWorldId();
		int instanceId = effector.getInstanceId();

		int existingCount = 0;
		if (homingId > 0) {
			existingCount = (int) effector.getPosition().getWorldMapInstance().getNpcs().stream()
					.filter(Homing.class::isInstance)
					.map(Homing.class::cast)
					.filter(homing -> homing.getCreator() == effector && homing.getHomingId() == homingId)
					.count();
		}
		int spawnCount = Math.max(0, npcCount - existingCount);
		int calculatedAttackCount = Math.min(254, attackCountDelta * effect.getSkillLevel() + attackCount);
		for (int i = 0; i < spawnCount; i++) {
			SpawnTemplate spawn = SpawnEngine.addNewSingleTimeSpawn(worldId, npcId, x, y, z, heading);
			final Homing homing = VisibleObjectSpawner.spawnHoming(spawn, instanceId, effector, homingId,
					calculatedAttackCount, effect.getSkillId(), effect.getSkillLevel());

			if (calculatedAttackCount > 0) {
				ActionObserver observer = new ActionObserver(ObserverType.ATTACK) {

					@Override
					public void attack(Creature creature) {
						homing.setAttackCount(homing.getAttackCount() - 1);
						if (homing.getAttackCount() <= 0) {
							homing.getController().onDelete();
						}
					}
				};
				homing.getObserveController().addObserver(observer);
				effect.setActionObserver(observer, position);
			}
			// 以防万一调度取消生成 / Schedule a despawn just in case
			Future<?> task = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

				@Override
				public void run() {
					if ((homing != null) && (homing.isSpawned())) {
						homing.getController().onDelete();
					}
				}
			}, 15 * 1000L);
			homing.getController().addTask(TaskId.DESPAWN, task);
			homing.getAi2().onCreatureEvent(AIEventType.ATTACK, effect.getEffected());
		}
	}
}
