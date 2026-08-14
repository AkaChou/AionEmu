package com.aionemu.gameserver.services;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.Future;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.stats.container.CreatureLifeStats;
import com.aionemu.gameserver.model.stats.container.PlayerLifeStats;
import com.aionemu.gameserver.model.templates.zone.ZoneType;
import com.aionemu.gameserver.world.World;

/**
 * 生命值恢复服务，调度 HP/MP/飞行值的恢复与消耗任务。
 * Life stats restore service scheduling HP/MP/FP restore and reduce tasks.
 *
 * @author ATracer
 */
public class LifeStatsRestoreService {

	private static volatile ObjectProvider<LifeStatsRestoreService> instanceProvider;

	/** 默认生命/魔法恢复间隔（毫秒） / Default HP/MP restore interval in ms*/
	private static final int DEFAULT_DELAY = 6000;
	/** 飞行值消耗默认间隔（毫秒）。 / Default FP reduce interval in ms. */
	private static final int DEFAULT_FPREDUCE_DELAY = 2000;
	/** 飞行值恢复默认间隔（毫秒）。 / Default FP restore interval in ms. */
	private static final int DEFAULT_FPRESTORE_DELAY = 2000;

	private static LifeStatsRestoreService instance = new LifeStatsRestoreService();

	/**
	 * 调度 HP 与 MP 恢复任务。
	 * Schedules an HP and MP restore task.
	 *
	 * life stats
	 * scheduled future
	 */
	public Future<?> scheduleRestoreTask(CreatureLifeStats<? extends Creature> lifeStats) {
		return GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new HpMpRestoreTask(lifeStats), 1700, DEFAULT_DELAY);
	}

	/**
	 * 调度仅恢复 HP 的任务。
	 * Schedules an HP-only restore task.
	 *
	 * life stats
	 * scheduled future
	 */
	public Future<?> scheduleHpRestoreTask(CreatureLifeStats<? extends Creature> lifeStats) {
		return GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new HpRestoreTask(lifeStats), 1700, DEFAULT_DELAY);
	}

	/**
	 * 调度飞行值消耗任务。
	 * Schedules a flight points reduce task.
	 *
	 * @param lifeStats 玩家生命状态 / player life stats
	 * @param costFp 每次消耗量，可为 null 使用默认 / cost per tick, null for default
	 * scheduled future
	 */
	public Future<?> scheduleFpReduceTask(final PlayerLifeStats lifeStats, Integer costFp) {
		return GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new FpReduceTask(lifeStats, costFp), 2000,
				DEFAULT_FPREDUCE_DELAY);
	}

	/**
	 * 调度飞行值恢复任务。
	 * Schedules a flight points restore task.
	 *
	 * @param lifeStats 玩家生命状态 / player life stats
	 * scheduled future
	 */
	public Future<?> scheduleFpRestoreTask(PlayerLifeStats lifeStats) {
		return GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new FpRestoreTask(lifeStats), 2000,
				DEFAULT_FPRESTORE_DELAY);
	}

	/**
	 * 获取服务单例，优先走 Spring ObjectProvider。
	 * Returns the service singleton, preferring Spring ObjectProvider when available.
	 *
	 * service instance
	 */
	public static LifeStatsRestoreService getInstance() {
		ObjectProvider<LifeStatsRestoreService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> instance);
		}
		return instance;
	}

	/**
	 * 注入 Spring ObjectProvider 以覆盖默认单例。
	 * Injects a Spring ObjectProvider to override the default singleton.
	 *
	 * provider
	 */
	public static void setInstanceProvider(ObjectProvider<LifeStatsRestoreService> provider) {
		instanceProvider = provider;
	}

	/**
	 * 仅恢复 HP 的定时任务；战斗中或已满则取消。
	 * HP-only restore runnable; cancels when fighting, dead or fully restored.
	 */
	private static class HpRestoreTask implements Runnable {

		private CreatureLifeStats<?> lifeStats;

		private HpRestoreTask(CreatureLifeStats<?> lifeStats) {
			this.lifeStats = lifeStats;
		}

		@Override
		public void run() {
			boolean inWorld = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().isInWorld(lifeStats.getOwner());
			AIState state = lifeStats.getOwner().getAi2().getState();
			// FIGHT/FEAR 都算战斗中：逃跑中被追击不应回血
			if (!inWorld || lifeStats.isAlreadyDead() || lifeStats.isFullyRestoredHp()
					|| state == AIState.FIGHT || state == AIState.FEAR) {
				lifeStats.cancelRestoreTask();
				lifeStats = null;
			} else {
				lifeStats.restoreHp();
			}
		}
	}

	/**
	 * 同时恢复 HP 与 MP 的定时任务。
	 * Combined HP and MP restore runnable.
	 */
	private static class HpMpRestoreTask implements Runnable {

		private CreatureLifeStats<?> lifeStats;

		private HpMpRestoreTask(CreatureLifeStats<?> lifeStats) {
			this.lifeStats = lifeStats;
		}

		@Override
		public void run() {
			boolean inWorld = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().isInWorld(lifeStats.getOwner());
			if (!inWorld || lifeStats.isAlreadyDead() || lifeStats.isFullyRestoredHpMp()) {
				lifeStats.cancelRestoreTask();
				lifeStats = null;
			} else {
				lifeStats.restoreHp();
				lifeStats.restoreMp();
			}
		}
	}

	/**
	 * 飞行值消耗定时任务，耗尽时结束飞行或触发恢复。
	 * FP reduce runnable; ends flight or triggers restore when empty.
	 */
	private static class FpReduceTask implements Runnable {

		private PlayerLifeStats lifeStats;
		private Integer costFp;

		private FpReduceTask(PlayerLifeStats lifeStats, final Integer costFp) {
			this.lifeStats = lifeStats;
			this.costFp = costFp;
		}

		@Override
		public void run() {
			boolean inWorld = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().isInWorld(lifeStats.getOwner());
			if (!inWorld || lifeStats.isAlreadyDead()) {
				lifeStats.cancelFpReduce();
				lifeStats = null;
				return;
			}

			if (lifeStats.getCurrentFp() == 0) {
				if (lifeStats.getOwner().getFlyState() > 0) {
					lifeStats.getOwner().getFlyController().endFly(true);
				} else {
					lifeStats.triggerFpRestore();
				}
			} else {
				int reduceFp = lifeStats.getOwner().getFlyState() == 2
						&& lifeStats.getOwner().isInsideZoneType(ZoneType.FLY) ? 1 : 2;
				if (costFp != null) {
					reduceFp = costFp.intValue();
				}

				lifeStats.reduceFp(reduceFp);
				lifeStats.specialrestoreFp();
			}
		}
	}

	/**
	 * 飞行值恢复定时任务，满值后取消。
	 * FP restore runnable; cancels when dead or fully restored.
	 */
	private static class FpRestoreTask implements Runnable {

		private PlayerLifeStats lifeStats;

		private FpRestoreTask(PlayerLifeStats lifeStats) {
			this.lifeStats = lifeStats;
		}

		@Override
		public void run() {
			if (lifeStats.isAlreadyDead() || lifeStats.isFlyTimeFullyRestored()) {
				lifeStats.cancelFpRestore();
				lifeStats = null;
			} else {
				lifeStats.restoreFp();
			}
		}
	}
}
