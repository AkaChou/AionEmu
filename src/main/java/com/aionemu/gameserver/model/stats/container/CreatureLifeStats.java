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
import lombok.Getter;

/**
 * 生物（NPC/玩家/召唤物）的生命值与魔法值统计及恢复逻辑。
 * Creature (NPC/player/summon) HP and MP stats and restore logic.
 */
@Slf4j

@Getter
public abstract class CreatureLifeStats<T extends Creature> {
	/** 返回当前生命 / Returns the current hp */
	protected int currentHp;
	/** 返回当前魔法 / Returns the current mp */
	protected int currentMp;
	/**
	 * @return 是否已死亡。 / Whether already dead
	 */
	protected boolean alreadyDead = false;
	/** 返回所有者 / Returns the owner*/
	protected T owner;
	/**
	 * 单一生命状态互斥量。
	 * Single life-state mutex.
	 *
	 * <p>HP/MP/恢复任务三段临界区都只做内存状态改写与一次任务调度（回调与观察者通知都在锁外执行），
	 * 既不阻塞也不互相嵌套，因此没有理由让每个生物持有三把锁：一把 {@link ReentrantLock} 实际是
	 * {@code ReentrantLock} + {@code NonfairSync} 两个对象，全服 ≈38 万把锁里这里占 ≈18 MB。
	 * 合并成一把后 {@code hpLock}/{@code mpLock}/{@code restoreLock} 指向同一实例：调用点与子类用法不变，
	 * 可重入保证嵌套安全，而且只剩一把锁后不再存在锁序问题。
	 * The HP, MP and restore-task critical sections only mutate in-memory state and schedule one task
	 * (callbacks and observer notifications already run outside the lock). They neither block nor nest, so
	 * three locks per creature buy nothing: one {@link ReentrantLock} is really a lock plus its sync object,
	 * and this class held ≈18 MB of the server's ≈380k locks. The three fields now alias one instance,
	 * which keeps every call site and subclass usage unchanged, stays reentrancy-safe, and removes any
	 * lock-ordering question by having a single lock.</p>
	 */
	private final ReentrantLock lifeLock = new ReentrantLock();
	private final Lock hpLock = lifeLock;
	private final Lock mpLock = lifeLock;
	protected final Lock restoreLock = lifeLock;
	protected volatile Future<?> lifeRestoreTask;

	public CreatureLifeStats(T owner, int currentHp, int currentMp) {
		this.owner = owner;
		this.currentHp = currentHp;
		this.currentMp = currentMp;
	}

	/** 返回最大生命 / Returns the max hp*/
	public int getMaxHp() {
		return this.getOwner().getGameStats().getMaxHp().getCurrent();
	}

	/** 返回最大魔法 / Returns the max mp*/
	public int getMaxMp() {
		return this.getOwner().getGameStats().getMaxMp().getCurrent();
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
	 * @return Whether fully restored hp mp
	 */
	public boolean isFullyRestoredHpMp() {
		return getMaxHp() == currentHp && getMaxMp() == currentMp;
	}

	/**
	 * @return Whether fully restored hp
	 */
	public boolean isFullyRestoredHp() {
		return getMaxHp() == currentHp;
	}

	/**
	 * @return Whether fully restored mp
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
