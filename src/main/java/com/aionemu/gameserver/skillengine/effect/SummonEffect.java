package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.Future;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.summons.SummonsService;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 召唤效果基类：为玩家创建召唤物，并在时限到达后自动解散。
 * Base summon effect: creates a player summon and auto-releases it after the configured time.
 *
 * @author Simple
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SummonEffect")
public class SummonEffect extends EffectTemplate {

	@XmlAttribute(name = "npc_id", required = true)
	protected int npcId;
	@XmlAttribute(name = "time", required = true)
	protected int time; // in seconds

	protected static Creature getPositionReference(Effect effect) {
		return effect.getEffected() != null ? effect.getEffected() : effect.getEffector();
	}

	/**
	 * 创建召唤物；若配置了存活时间则调度自动解散任务。
	 * Creates the summon and, when time > 0, schedules an auto-release task.
	 */
	@Override
	public void applyEffect(Effect effect) {
		Player effected = (Player) effect.getEffected();
		SummonsService.createSummon(effected, npcId, effect.getSkillId(), effect.getSkillLevel(), time);
		if (time > 0 && (effect.getEffected() instanceof Player)) {
			final Player effector = (Player) effect.getEffected();
			final Summon summon = effector.getSummon();
			Future<?> task = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

				@Override
				public void run() {
					SummonsService.expire(summon);
				}
			}, time * 1000);
			summon.getController().addTask(TaskId.DESPAWN, task);
		}
	}

	/**
	 * 直接标记本效果成功。
	 * Always marks this effect successful.
	 */
	@Override
	public void calculate(Effect effect) {
		effect.addSucessEffect(this);
	}
}
