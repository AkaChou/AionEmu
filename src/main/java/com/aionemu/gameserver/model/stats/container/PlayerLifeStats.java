package com.aionemu.gameserver.model.stats.container;

import com.aionemu.gameserver.lifecycle.GameGameplayServices;
import com.aionemu.gameserver.lifecycle.GameTaskManagerServices;

import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_FLY_TIME;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STATUPDATE_HP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STATUPDATE_MP;
import com.aionemu.gameserver.services.LifeStatsRestoreService;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster.BroadcastMode;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 玩家 Life 属性，用于属性相关逻辑。
 * Player Life Stats for stats logic.
 *
 * @author ATracer, sphinx
 */
public class PlayerLifeStats extends CreatureLifeStats<Player> {

	protected int currentFp;
	private final ReentrantLock fpLock = new ReentrantLock();

	private Future<?> flyRestoreTask;
	private Future<?> flyReduceTask;

	public PlayerLifeStats(Player owner) {
		super(owner, owner.getGameStats().getMaxHp().getCurrent(), owner.getGameStats().getMaxMp().getCurrent());
		this.currentFp = owner.getGameStats().getFlyTime().getCurrent();
	}

	@Override
	protected void onReduceHp() {
		sendHpPacketUpdate();
		triggerRestoreTask();
		sendGroupPacketUpdate();
	}

	@Override
	protected void onReduceMp() {
		sendMpPacketUpdate();
		triggerRestoreTask();
		sendGroupPacketUpdate();
	}

	@Override
	protected void onIncreaseMp(TYPE type, int value, int skillId, LOG log) {
		if (value > 0) {
			sendMpPacketUpdate();
			sendAttackStatusPacketUpdate(type, value, skillId, log);
			sendGroupPacketUpdate();
		}
	}

	@Override
	protected void onIncreaseHp(TYPE type, int value, int skillId, LOG log) {
		if (value > 0) {
			sendHpPacketUpdate();
			sendAttackStatusPacketUpdate(type, value, skillId, log);
			sendGroupPacketUpdate();
		}
	}

	private void sendGroupPacketUpdate() {
		if (owner.isInTeam()) {
			GameTaskManagerServices.teamEffectUpdater().startTask(owner);
		}
	}

	/** SynchronizeWith 最大 Stats / Synchronize With Max Stats */
	@Override
	public void synchronizeWithMaxStats() {
		if (isAlreadyDead()) {
			return;
		}
		super.synchronizeWithMaxStats();
		int maxFp = getMaxFp();
		if (currentFp != maxFp) {
			currentFp = maxFp;
		}
	}

	/** 更新 current stats / Update current stats */
	@Override
	public void updateCurrentStats() {
		super.updateCurrentStats();

		if (getMaxFp() < currentFp) {
			currentFp = getMaxFp();
		}
		if (!owner.isFlying() && !owner.isInSprintMode()) {
			triggerFpRestore();
		}
	}

	/** Sendhppacket 更新 / Send hp packet update */
	public void sendHpPacketUpdate() {
		owner.addPacketBroadcastMask(BroadcastMode.UPDATE_PLAYER_HP_STAT);
	}

	/** Sendhppacket 更新 impl / Send hp packet update impl */
	public void sendHpPacketUpdateImpl() {
		PacketSendUtility.sendPacket(owner, new SM_STATUPDATE_HP(currentHp, getMaxHp()));
	}

	/** Sendmppacket 更新 / Send mp packet update */
	public void sendMpPacketUpdate() {
		owner.addPacketBroadcastMask(BroadcastMode.UPDATE_PLAYER_MP_STAT);
	}

	/** Sendmppacket 更新 impl / Send mp packet update impl */
	public void sendMpPacketUpdateImpl() {
		PacketSendUtility.sendPacket(owner, new SM_STATUPDATE_MP(currentMp, getMaxMp()));
	}

	/**
	 * @return the currentFp
	 */
	@Override
	public int getCurrentFp() {
		return this.currentFp;
	}

	/** 返回最大飞行点 / Returns the max fp*/
	@Override
	public int getMaxFp() {
		return owner.getGameStats().getFlyTime().getCurrent();
	}

	/**
	 * @return FP percentage 0 - 100
	 */
	public int getFpPercentage() {
		return 100 * currentFp / getMaxFp();
	}

	/**
	 * 调用方欲恢复生物 FP 时调用。
	 * Called whenever the caller wants to restore the creature's FP.
	 */
	@Override
	public int increaseFp(TYPE type, int value) {
		return this.increaseFp(type, value, 0, LOG.REGULAR);
	}

	/** 增加飞行值。 / Increase fp. */
	public int increaseFp(TYPE type, int value, int skillId, LOG log) {
		fpLock.lock();

		try {
			if (isAlreadyDead()) {
				return 0;
			}
			int newFp = this.currentFp + value;
			if (newFp > getMaxFp()) {
				newFp = getMaxFp();
			}
			if (currentFp != newFp) {
				onIncreaseFp(type, newFp - currentFp, skillId, log);
				this.currentFp = newFp;
			}
		} finally {
			fpLock.unlock();
		}
		return currentFp;

	}

	/**
	 * 减少当前 FP，最低降至 0。
	 * Reduces current FP, clamped at zero.
	 *
	 * @param value 要减少的 FP / FP to remove
	 * @return 剩余 FP / remaining FP
	 */
	public int reduceFp(int value) {
		fpLock.lock();
		try {
			int newFp = this.currentFp - value;

			if (newFp < 0) {
				newFp = 0;
			}
			this.currentFp = newFp;
		} finally {
			fpLock.unlock();
		}

		onReduceFp();

		return currentFp;
	}

	/** 设置 current fp / Sets the current fp */
	public int setCurrentFp(int value) {
		fpLock.lock();
		try {
			int newFp = value;

			if (newFp < 0) {
				newFp = 0;
			}
			this.currentFp = newFp;
		} finally {
			fpLock.unlock();
		}

		onReduceFp();

		return currentFp;
	}

	protected void onIncreaseFp(TYPE type, int value, int skillId, LOG log) {
		if (value > 0) {
			sendAttackStatusPacketUpdate(type, value, skillId, log);
			owner.addPacketBroadcastMask(BroadcastMode.UPDATE_PLAYER_FLY_TIME);
		}
	}

	protected void onReduceFp() {
		owner.addPacketBroadcastMask(BroadcastMode.UPDATE_PLAYER_FLY_TIME);
	}

	/** Sendfppacket 更新 impl / Send fp packet update impl */
	public void sendFpPacketUpdateImpl() {
		if (owner == null) {
			return;
		}
		PacketSendUtility.sendPacket(owner, new SM_FLY_TIME(currentFp, getMaxFp()));
	}

	/**
	 * this method should be used only on FlyTimeRestoreService
	 */
	public void restoreFp() {
		// 每 2 秒恢复多少飞行时间。 / how much fly time restoring per 2 second.
		increaseFp(TYPE.AUTO_HEAL_FP, 1);
	}

	/** Specialrestore Fp / Specialrestore Fp */
	public void specialrestoreFp() {
		if (owner.getGameStats().getStat(StatEnum.REGEN_FP, 0).getCurrent() != 0) {
			increaseFp(TYPE.AUTO_HEAL_FP, owner.getGameStats().getStat(StatEnum.REGEN_FP, 0).getCurrent() / 3);
		}
	}

	/** Trigger fp restore / Trigger fp restore */
	public void triggerFpRestore() {
		cancelFpReduce();

		restoreLock.lock();
		try {
			if (flyRestoreTask == null && !alreadyDead && !isFlyTimeFullyRestored()) {
				this.flyRestoreTask = GameGameplayServices.lifeStatsRestoreService().scheduleFpRestoreTask(this);
			}
		} finally {
			restoreLock.unlock();
		}
	}

	/** 取消 fp restore / Cancel fp restore */
	public void cancelFpRestore() {
		restoreLock.lock();
		try {
			if (flyRestoreTask != null && !flyRestoreTask.isCancelled()) {
				flyRestoreTask.cancel(false);
				this.flyRestoreTask = null;
			}
		} finally {
			restoreLock.unlock();
		}
	}

	/** Triggerfpreduce 按 cost / Trigger fp reduce by cost */
	public void triggerFpReduceByCost(Integer costFp) {
		triggerFpReduce(costFp);
	}

	/** Trigger fp reduce / Trigger fp reduce */
	public void triggerFpReduce() {
		triggerFpReduce(null);
	}

	private void triggerFpReduce(Integer costFp) {
		cancelFpRestore();
		restoreLock.lock();
		try {
			if (flyReduceTask == null && !alreadyDead && owner.getAccessLevel() < AdminConfig.GM_FLIGHT_UNLIMITED
					&& !owner.isUnderNoFPConsum()) {
				this.flyReduceTask = GameGameplayServices.lifeStatsRestoreService().scheduleFpReduceTask(this, costFp);
			}
		} finally {
			restoreLock.unlock();
		}
	}

	/** 取消 fp reduce / Cancel fp reduce */
	public void cancelFpReduce() {
		restoreLock.lock();
		try {
			if (flyReduceTask != null && !flyReduceTask.isCancelled()) {
				flyReduceTask.cancel(false);
				this.flyReduceTask = null;
			}
		} finally {
			restoreLock.unlock();
		}
	}

	/**
	 * @return Whether fly time fully restored
	 */
	public boolean isFlyTimeFullyRestored() {
		return getMaxFp() == currentFp;
	}

	/** 取消 all tasks / Cancel all tasks */
	@Override
	public void cancelAllTasks() {
		super.cancelAllTasks();
		cancelFpReduce();
		cancelFpRestore();
	}

	/** Triggerrestore 在 revive / Trigger restore on revive */
	public void triggerRestoreOnRevive() {
		this.triggerRestoreTask();
		triggerFpRestore();
	}
}
