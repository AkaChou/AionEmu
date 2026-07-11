package com.aionemu.gameserver.model.stats.container;

import com.aionemu.gameserver.lifecycle.GameGameplayServices;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SUMMON_UPDATE;
import com.aionemu.gameserver.services.LifeStatsRestoreService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 召唤物 Life 属性，用于属性相关逻辑。
 * Summon Life Stats for stats logic.
 *
 * @author ATracer
 */
public class SummonLifeStats extends CreatureLifeStats<Summon> {

	public SummonLifeStats(Summon owner) {
		super(owner, owner.getGameStats().getMaxHp().getCurrent(), owner.getGameStats().getMaxMp().getCurrent());
	}

	@Override
	protected void onIncreaseHp(TYPE type, int value, int skillId, LOG log) {
		Creature master = getOwner().getMaster();
		sendAttackStatusPacketUpdate(type, value, skillId, log);

		if (master instanceof Player) {
			PacketSendUtility.sendPacket((Player) master, new SM_SUMMON_UPDATE(getOwner()));
		}
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

	/** 返回所有者 / Returns the owner*/
	@Override
	public Summon getOwner() {
		return (Summon) super.getOwner();
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
}
