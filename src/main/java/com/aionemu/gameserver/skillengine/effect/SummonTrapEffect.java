package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Trap;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.spawnengine.VisibleObjectSpawner;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * 召唤陷阱效果：在落点生成陷阱，并限制同一主人的陷阱数量。
 * Summon trap effect: spawns a trap at the landing point and enforces a per-owner trap cap.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SummonTrapEffect")
public class SummonTrapEffect extends SummonEffect {
	/**
	 * 计算落点（部分 NPC 用施法者坐标），限制数量后生成陷阱并调度删除。
	 * Computes spawn point, enforces max traps, spawns the trap, and schedules despawn.
	 */
	@Override
	public void applyEffect(Effect effect) {
		Creature effector = effect.getEffector();
		if (effect.getEffector().getTarget() == null) {
			effect.getEffector().setTarget(effect.getEffector());
		}
		double radian = Math.toRadians(MathUtil.convertHeadingToDegree((byte) effect.getEffector().getHeading()));
		float x = effect.getX();
		float y = effect.getY();
		float z = effect.getZ();
		if (x == 0 && y == 0) {
			Creature effected = getPositionReference(effect);
			x = effected.getX() + (float) (Math.cos(radian) * 2);
			y = effected.getY() + (float) (Math.sin(radian) * 2);
			z = effected.getZ();
		}
		byte heading = effector.getHeading();
		int worldId = effector.getWorldId();
		int instanceId = effector.getInstanceId();
		if (npcId == 749300 || npcId == 749301 || // Scrapped Mechanisms.
				npcId == 833699 || npcId == 833700 || // Highdeva_Fire_NPC.
				npcId == 246363) { // IDEvent_Solo_Paralyze_NPC.
			x = effector.getX();
			y = effector.getY();
			z = effector.getZ();
		}
		maxTraps(effector);
		SpawnTemplate spawn = SpawnEngine.addNewSingleTimeSpawn(worldId, npcId, x, y, z, heading);
		final Trap trap = VisibleObjectSpawner.spawnTrap(spawn, instanceId, effector);
		Future<?> task = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				trap.getController().onDelete();
			}
		}, time * 1000);
		trap.getController().addTask(TaskId.DESPAWN, task);
	}

	/**
	 * 同一主人陷阱达到 2 个时删除最早的一个。
	 * Deletes the oldest trap when the owner already has two.
	 */
	private void maxTraps(Creature effector) {
		List<Trap> traps = effector.getPosition().getWorldMapInstance().getTraps(effector);
		if (traps.size() >= 2) {
			Iterator<Trap> trapIter = traps.iterator();
			Trap t = trapIter.next();
			t.getController().cancelTask(TaskId.DESPAWN);
			t.getController().onDelete();
		}
	}
}
