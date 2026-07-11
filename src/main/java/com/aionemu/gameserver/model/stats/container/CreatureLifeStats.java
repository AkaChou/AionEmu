package com.aionemu.gameserver.model.stats.container;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameGameplayServices;

import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.services.LifeStatsRestoreService;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.skillengine.model.HealType;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 生物 Life 属性，用于属性相关逻辑。
 * Creature Life Stats for stats logic.
 */
@Slf4j

public abstract class CreatureLifeStats<T extends Creature> {
	protected int currentHp;
	protected int currentMp;
	protected boolean alreadyDead = false;
	protected T owner;
	private final Lock hpLock = new ReentrantLock();
	private final Lock mpLock = new ReentrantLock();
	protected final Lock restoreLock = new ReentrantLock();
	protected volatile Future<?> lifeRestoreTask;

	public CreatureLifeStats(T owner, int currentHp, int currentMp) {
		this.owner = owner;
		this.currentHp = currentHp;
		this.currentMp = currentMp;
	}

	/** 返回所有者 / Returns the owner*/
	public T getOwner() {
		return owner;
	}

	/** 返回当前生命 / Returns the current hp */
	public int getCurrentHp() {
		return currentHp;
	}

	/** 返回当前魔法 / Returns the current mp */
	public int getCurrentMp() {
		return currentMp;
	}

	/** 返回最大生命 / Returns the max hp*/
	public int getMaxHp() {
		return this.getOwner().getGameStats().getMaxHp().getCurrent();
	}

	/** 返回最大魔法 / Returns the max mp*/
	public int getMaxMp() {
		return this.getOwner().getGameStats().getMaxMp().getCurrent();
	}

	/**
	 * @return 是否已经 dead / 是否已经 dead。 / Whether already dead / Whether already dead
	 */
	public boolean isAlreadyDead() {
		return alreadyDead;
	}

	/** Reduce Hp / Reduce Hp */
	public int reduceHp(int value, Creature attacker) {
		if (attacker == null) {
			throw new IllegalArgumentException("attacker");
		}
		boolean hpChanged = false;
		boolean isDied = false;
		hpLock.lock();
		try {
			if (!alreadyDead) {
				int newHp = this.currentHp - value;
				if (newHp <= 0) {
					newHp = 0;
					alreadyDead = true;
					isDied = true;
				}
				hpChanged = this.currentHp != newHp;
				this.currentHp = newHp;
			}
		} finally {
			hpLock.unlock();
		}
		if (value != 0) {
			onReduceHp();
		}
		if (hpChanged) {
			owner.getObserveController().notifyLifeChangedObservers(HealType.HP, currentHp);
		}
		if (isDied) {
			getOwner().getController().onDie(attacker);
		}
		return currentHp;
	}

	/** Reduce Mp / Reduce Mp */
	public int reduceMp(int value) {
		boolean mpChanged;
		mpLock.lock();
		try {
			int newMp = this.currentMp - value;
			if (newMp < 0) {
				newMp = 0;
			}
			mpChanged = this.currentMp != newMp;
			this.currentMp = newMp;
		} finally {
			mpLock.unlock();
		}
		if (value != 0) {
			onReduceMp();
		}
		if (mpChanged) {
			owner.getObserveController().notifyLifeChangedObservers(HealType.MP, currentMp);
		}
		return currentMp;
	}

	protected void sendAttackStatusPacketUpdate(TYPE type, int value, int skillId, LOG log) {
		if (owner == null)// possible?
		{
			return;
		}
		PacketSendUtility.broadcastPacketAndReceive(owner, new SM_ATTACK_STATUS(owner, owner, type, skillId, value, log));
	}

	/** 增加生命值。 / Increase hp. */
	public int increaseHp(TYPE type, int value) {
		return this.increaseHp(type, value, 0, LOG.REGULAR);
	}

	/** 增加生命值。 / Increase hp. */
	public int increaseHp(TYPE type, int value, int skillId, LOG log) {
		boolean hpIncreased = false;
		if (this.getOwner().getEffectController().isAbnormalSet(AbnormalState.DISEASE)) {
			return currentHp;
		}
		hpLock.lock();
		try {
			if (isAlreadyDead()) {
				return 0;
			}
			int newHp = this.currentHp + value;
			if (newHp > getMaxHp()) {
				newHp = getMaxHp();
			}
			if (currentHp != newHp) {
				this.currentHp = newHp;
				hpIncreased = true;
			}
		} finally {
			hpLock.unlock();
		}
		if (hpIncreased) {
			onIncreaseHp(type, value, skillId, log);
			owner.getObserveController().notifyLifeChangedObservers(HealType.HP, currentHp);
		}
		return currentHp;
	}

	/** 增加魔法值。 / Increase mp. */
	public int increaseMp(TYPE type, int value) {
		return this.increaseMp(type, value, 0, LOG.REGULAR);
	}

	/** 增加魔法值。 / Increase mp. */
	public int increaseMp(TYPE type, int value, int skillId, LOG log) {
		boolean mpIncreased = false;
		mpLock.lock();
		try {
			if (isAlreadyDead()) {
				return 0;
			}
			int newMp = this.currentMp + value;
			if (newMp > getMaxMp()) {
				newMp = getMaxMp();
			}
			if (currentMp != newMp) {
				this.currentMp = newMp;
				mpIncreased = true;
			}
		} finally {
			mpLock.unlock();
		}
		if (mpIncreased) {
			onIncreaseMp(type, value, skillId, log);
			owner.getObserveController().notifyLifeChangedObservers(HealType.MP, currentMp);
		}
		return currentMp;
	}

	/** Restore Hp / Restore Hp */
	public final void restoreHp() {
		increaseHp(TYPE.NATURAL_HP, getOwner().getGameStats().getHpRegenRate().getCurrent());
	}

	/** Restore Mp / Restore Mp */
	public final void restoreMp() {
		increaseMp(TYPE.NATURAL_MP, getOwner().getGameStats().getMpRegenRate().getCurrent());
	}

	/** 触发恢复任务 / Trigger restore task */
	public void triggerRestoreTask() {
		restoreLock.lock();
		try {
			if (lifeRestoreTask == null && !alreadyDead) {
				lifeRestoreTask = GameGameplayServices.lifeStatsRestoreService().scheduleRestoreTask(this);
			}
		} finally {
			restoreLock.unlock();
		}
	}

	/** 取消 restore task / Cancel restore task */
	public void cancelRestoreTask() {
		restoreLock.lock();
		try {
			if (lifeRestoreTask != null) {
				lifeRestoreTask.cancel(false);
				lifeRestoreTask = null;
			}
		} finally {
			restoreLock.unlock();
		}
	}

	/**
	 * @return Whether fully restored hp mp / Whether fully restored hp mp
	 */
	public boolean isFullyRestoredHpMp() {
		return getMaxHp() == currentHp && getMaxMp() == currentMp;
	}

	/**
	 * @return Whether fully restored hp / Whether fully restored hp
	 */
	public boolean isFullyRestoredHp() {
		return getMaxHp() == currentHp;
	}

	/**
	 * @return Whether fully restored mp / Whether fully restored mp
	 */
	public boolean isFullyRestoredMp() {
		return getMaxMp() == currentMp;
	}

	/** SynchronizeWith 最大 Stats / Synchronize With Max Stats */
	public void synchronizeWithMaxStats() {
		int maxHp = getMaxHp();
		if (currentHp != maxHp) {
			currentHp = maxHp;
		}
		int maxMp = getMaxMp();
		if (currentMp != maxMp) {
			currentMp = maxMp;
		}
	}

	/** 更新 current stats / Update current stats */
	public void updateCurrentStats() {
		int maxHp = getMaxHp();
		if (maxHp < currentHp) {
			currentHp = maxHp;
		}
		int maxMp = getMaxMp();
		if (maxMp < currentMp) {
			currentMp = maxMp;
		}
		if (!isFullyRestoredHpMp()) {
			triggerRestoreTask();
		}
	}

	/**
	 * @return HP percentage 0 - 100
	 */
	public int getHpPercentage() {
		if ((int) (100f * currentHp / getMaxHp()) == 0 && currentHp > 0) {
			return 1;
		} else {
			return (int) (100f * currentHp / getMaxHp());
		}
	}

	/** 返回 mp percentage / Returns the mp percentage */
	public int getMpPercentage() {
		return (int) (100f * currentMp / getMaxMp());
	}

	protected abstract void onIncreaseMp(TYPE type, int value, int skillId, LOG log);

	protected abstract void onReduceMp();

	protected abstract void onIncreaseHp(TYPE type, int value, int skillId, LOG log);

	protected abstract void onReduceHp();

	/** 增加飞行值。 / Increase fp. */
	public int increaseFp(TYPE type, int value) {
		return 0;
	}

	/** 返回最大飞行点 / Returns the max fp*/
	public int getMaxFp() {
		return 0;
	}

	/** 返回当前飞行点 / Returns the current fp */
	public int getCurrentFp() {
		return 0;
	}

	/** 取消 all tasks / Cancel all tasks */
	public void cancelAllTasks() {
		cancelRestoreTask();
	}

	/** 设置 current hp percent / Sets the current hp percent */
	public void setCurrentHpPercent(int hpPercent) {
		hpLock.lock();
		try {
			long maxHp = getMaxHp();
			this.currentHp = (int) (maxHp * hpPercent / 100f);
			if (this.currentHp > 0) {
				this.alreadyDead = false;
			}
		} finally {
			hpLock.unlock();
		}
	}

	/** 设置 current hp / Sets the current hp */
	public void setCurrentHp(int hp) {
		boolean hpNotAtMaxValue = false;
		hpLock.lock();
		try {
			this.currentHp = hp;
			if (this.currentHp > 0) {
				this.alreadyDead = false;
			}
			if (this.currentHp < getMaxHp()) {
				hpNotAtMaxValue = true;
			}
		} finally {
			hpLock.unlock();
		}
		if (hpNotAtMaxValue) {
			onReduceHp();
		}
	}

	/** 设置 current mp / Sets the current mp */
	public int setCurrentMp(int value) {
		mpLock.lock();
		try {
			int newMp = value;
			if (newMp < 0) {
				newMp = 0;
			}
			this.currentMp = newMp;
		} finally {
			mpLock.unlock();
		}
		onReduceMp();
		return currentMp;
	}

	/** 设置 current mp percent / Sets the current mp percent */
	public void setCurrentMpPercent(int mpPercent) {
		mpLock.lock();
		try {
			long maxMp = getMaxMp();
			this.currentMp = (int) (maxMp * mpPercent / 100f);
		} finally {
			mpLock.unlock();
		}
	}
}
