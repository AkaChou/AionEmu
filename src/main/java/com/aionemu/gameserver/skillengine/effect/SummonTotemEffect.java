package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.Future;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.NpcObjectType;
import com.aionemu.gameserver.model.gameobjects.Servant;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.spawnengine.VisibleObjectSpawner;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * 召唤图腾效果：生成图腾或特殊技能区域型侍从。
 * Summon totem effect: spawns a totem or special skill-area servant.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SummonTotemEffect")
public class SummonTotemEffect extends SummonServantEffect {
	/**
	 * 特定技能在身前生成 SKILLAREA；其它在施法者位置生成 TOTEM。
	 * For specific skills spawns SKILLAREA ahead; otherwise spawns TOTEM at the effector.
	 */
	@Override
	public void applyEffect(Effect effect) {
		Creature effector = effect.getEffector();
		switch (effect.getSkillId()) {
		case 657:
		case 658:
		case 659:
		case 660:
		case 661:
		case 662:
			if (effect.getEffector().getTarget() == null) {
				effect.getEffector().setTarget(effect.getEffector());
			}
			double radian = Math.toRadians(MathUtil.convertHeadingToDegree((byte) effect.getEffector().getHeading()));
			float x = effect.getX();
			float y = effect.getY();
			float z = effect.getZ();
			if (x == 0 && y == 0) {
				Creature effected = effect.getEffected();
				x = effected.getX() + (float) (Math.cos(radian) * 2);
				y = effected.getY() + (float) (Math.sin(radian) * 2);
				z = effected.getZ();
			}
			byte heading = effector.getHeading();
			int worldId = effector.getWorldId();
			int instanceId = effector.getInstanceId();
			SpawnTemplate spawn = SpawnEngine.addNewSingleTimeSpawn(worldId, npcId, x, y, z, heading);
			final Servant servant = VisibleObjectSpawner.spawnServant(spawn, instanceId, effector, effect.getSkillId(),
					effect.getSkillLevel(), NpcObjectType.SKILLAREA);
			Future<?> task = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					servant.getController().delete();
				}
			}, time * 1000);
			servant.getController().addTask(TaskId.DESPAWN, task);
			return;
		default:
			float x1 = effector.getX();
			float y1 = effector.getY();
			float z1 = effector.getZ();
			spawnServant(effect, time, NpcObjectType.TOTEM, x1, y1, z1);
		}
	}
}
