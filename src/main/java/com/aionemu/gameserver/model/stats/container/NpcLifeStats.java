package com.aionemu.gameserver.model.stats.container;

import com.aionemu.gameserver.lifecycle.GameGameplayServices;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.services.LifeStatsRestoreService;

/**
 * NPCLife 属性，用于属性相关逻辑。
 * Npc Life Stats for stats logic.
 *
 * @author ATracer
 */
public class NpcLifeStats extends CreatureLifeStats<Npc> {

	/**
	 * @param owner
	 */
	public NpcLifeStats(Npc owner) {
		super(owner, owner.getGameStats().getMaxHp().getCurrent(), owner.getGameStats().getMaxMp().getCurrent());
	}

	@Override
	protected void onIncreaseHp(TYPE type, int value, int skillId, LOG log) {
		sendAttackStatusPacketUpdate(type, value, skillId, log);
	}

	@Override
	protected void onIncreaseMp(TYPE type, int value, int skillId, LOG log) {
	}

	@Override
	protected void onReduceHp() {
	}

	@Override
	protected void onReduceMp() {
	}

	/** 触发恢复任务 / Trigger restore task */
	@Override
	public void triggerRestoreTask() {
		restoreLock.lock();
		try {
			if (lifeRestoreTask == null && !alreadyDead) {
				this.lifeRestoreTask = GameGameplayServices.lifeStatsRestoreService().scheduleHpRestoreTask(this);
			}
		} finally {
			restoreLock.unlock();
		}
	}

	/** Start resting / Start resting */
	public void startResting() {
		triggerRestoreTask();
	}
}
