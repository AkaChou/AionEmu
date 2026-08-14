package com.aionemu.gameserver.model.stats.container;

import com.aionemu.gameserver.lifecycle.GameGameplayServices;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.services.LifeStatsRestoreService;

/**
 * NPC 的生命值/魔法值属性与恢复逻辑。
 * NPC HP/MP stats and restore logic.
 *
 * @author ATracer
 */
public class NpcLifeStats extends CreatureLifeStats<Npc> {

	/**
	 * 创建 NPC 生命属性。
	 * Creates NPC life stats.
	 *
	 * @param owner 所属 NPC / owner NPC
	 */
	public NpcLifeStats(Npc owner) {
		super(owner, owner.getGameStats().getMaxHp().getCurrent(), owner.getGameStats().getMaxMp().getCurrent());
	}

	@Override
	protected void onIncreaseHp(TYPE type, int value, int skillId, LOG log) {
		sendAttackStatusPacketUpdate(packetType(type), value, skillId, log);
	}

	static TYPE packetType(TYPE type) {
		return type == TYPE.NATURAL_HP ? TYPE.HP : type;
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

	/** 开始休息（触发恢复任务） / Start resting (triggers restore task) */
	public void startResting() {
		triggerRestoreTask();
	}
}
