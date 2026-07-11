package com.aionemu.gameserver.skillengine.effect;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.Future;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.NpcObjectType;
import com.aionemu.gameserver.model.gameobjects.Servant;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.skillengine.properties.FirstTargetAttribute;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.spawnengine.VisibleObjectSpawner;

/**
 * 召唤侍从效果：生成会主动攻击的侍从型 NPC。
 * Summon servant effect: spawns a servant NPC that engages the target.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SummonServantEffect")
@Slf4j
public class SummonServantEffect extends SummonEffect {


	@XmlAttribute(name = "skill_id", required = true)
	protected int skillId;

	/**
	 * 在施法者位置生成 SERVANT 类型侍从。
	 * Spawns a SERVANT-type servant at the effector position.
	 */
	@Override
	public void applyEffect(Effect effect) {
		Creature effector = effect.getEffector();
		float x = effector.getX();
		float y = effector.getY();
		float z = effector.getZ();
		spawnServant(effect, time, NpcObjectType.SERVANT, x, y, z);
	}

	/**
	 * 按坐标生成侍从，调度超时删除，并向目标发起攻击。
	 * Spawns the servant at the given coords, schedules despawn, and issues an attack AI event.
	 */
	protected Servant spawnServant(Effect effect, int time, NpcObjectType npcObjectType, float x, float y, float z) {
		Creature effector = effect.getEffector();
		byte heading = effector.getHeading();
		int worldId = effector.getWorldId();
		int instanceId = effector.getInstanceId();

		final Creature target = (Creature) effector.getTarget();
		Creature effected = effect.getEffected();

		SkillTemplate template = effect.getSkillTemplate();

		if (template.getProperties().getFirstTarget() != FirstTargetAttribute.ME && target == null) {
			log.warn(I18n.get("log.6806029742a7"));
			return null;
		}

		SpawnTemplate spawn = SpawnEngine.addNewSingleTimeSpawn(worldId, npcId, x, y, z, heading);
		final Servant servant = VisibleObjectSpawner.spawnServant(spawn, instanceId, effector, skillId,
				effect.getSkillLevel(), npcObjectType);

		Future<?> task = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

			@Override
			public void run() {
				servant.getController().onDelete();
			}
		}, time * 1000);
		servant.getController().addTask(TaskId.DESPAWN, task);
		servant.getAi2().onCreatureEvent(AIEventType.ATTACK, target != null ? target : effected);
		return servant;
	}
}
