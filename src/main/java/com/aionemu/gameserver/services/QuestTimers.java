package com.aionemu.gameserver.services;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.questEngine.definition.QuestTimerPolicy;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 任务计时器注册表：可见/不可见计时器的启动、取消，以及按玩家、任务、副本维度的清理。
 * <p>状态与并发语义刻意集中在这里：单把 {@link #questTimerLock} 保护注册表，替换计时器时先取消旧任务再注册
 * 新任务，超时回调只在"确实从注册表移除"时触发，避免重复投递。{@link QuestService} 保留同签名的
 * 静态门面，既有调用方（任务脚本、任务引擎端口、GM 命令）无需改动。</p>
 * Quest timer registry: start, cancel and player/quest/instance-scoped cleanup of visible and
 * invisible quest timers.
 * <p>State and concurrency semantics deliberately live here: a single {@link #questTimerLock} guards the
 * registry, a replacement cancels the old task before registering the new one, and the expiry
 * callback only runs when the entry was really removed, so a timer cannot fire twice.
 * {@link QuestService} keeps the same static facade so existing callers (quest scripts, the quest
 * engine timer port, GM commands) stay untouched.</p>
 */
final class QuestTimers {

	/**
	 * 在跑的计时器，键为（玩家、任务、策略身份）。
	 * Running timers keyed by (player, quest, policy identity).
	 */
	private static final ConcurrentMap<QuestTimerKey, ManagedQuestTimer> questTimers = new ConcurrentHashMap<>();

	/**
	 * 注册表锁：注册、取消与超时移除共用。
	 * Registry lock shared by registration, cancellation and expiry removal.
	 */
	private static final Object questTimerLock = new Object();

	/**
	 * 工具类禁止实例化。
	 * Utility class; not instantiable.
	 */
	private QuestTimers() {
	}

	/**
	 * 启动可见的任务计时器。
	 * Starts a visible quest timer.
	 * @param env 任务环境 / quest environment
	 * @param timeInSeconds 计时秒数 / timer length in seconds
	 * @return 是否已启动 / whether started
	 */
	static boolean questTimerStart(QuestEnv env, int timeInSeconds) {
		return questTimerStart(env, timeInSeconds, QuestTimerPolicy.visible());
	}

	/**
	 * 以指定策略启动可见的任务计时器。
	 * Starts a visible quest timer with the given policy.
	 * @param env 任务环境 / quest environment
	 * @param timeInSeconds 计时秒数 / timer length in seconds
	 * @param policy 计时器策略 / timer policy
	 * @return 是否已启动 / whether started
	 */
	static boolean questTimerStart(QuestEnv env, int timeInSeconds, QuestTimerPolicy policy) {
		Player player = requireTimerArguments(env, timeInSeconds, policy);
		TimerStartOutcome outcome = startManagedTimer(player, env.getQuestId(), timeInSeconds, policy, true,
			expiredQuestId -> GameEngineServices.questEngine().onQuestTimerEnd(
				new QuestEnv(null, player, expiredQuestId, 0)));
		if (outcome == TimerStartOutcome.STARTED) {
			PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(env.getQuestId(), timeInSeconds));
		}
		return true;
	}

	/**
	 * 启动不可见任务计时器（超时回调引擎）。
	 * Starts an invisible quest timer (engine callback on timeout).
	 * @param env 任务环境 / quest environment
	 * @param timeInSeconds 计时秒数 / timer length in seconds
	 * @return 是否已启动 / whether started
	 */
	static boolean invisibleTimerStart(QuestEnv env, int timeInSeconds) {
		return invisibleTimerStart(env, timeInSeconds, QuestTimerPolicy.invisible());
	}

	/**
	 * 以指定策略启动不可见任务计时器。
	 * Starts an invisible quest timer with the given policy.
	 * @param env 任务环境 / quest environment
	 * @param timeInSeconds 计时秒数 / timer length in seconds
	 * @param policy 计时器策略 / timer policy
	 * @return 是否已启动 / whether started
	 */
	static boolean invisibleTimerStart(QuestEnv env, int timeInSeconds, QuestTimerPolicy policy) {
		Player player = requireTimerArguments(env, timeInSeconds, policy);
		startManagedTimer(player, env.getQuestId(), timeInSeconds, policy, false,
			expiredQuestId -> GameEngineServices.questEngine().onInvisibleTimerEnd(
				new QuestEnv(null, player, expiredQuestId, 0)));
		return true;
	}

	/**
	 * 结束并取消任务计时器。
	 * Ends and cancels the quest timer.
	 * @param env 任务环境 / quest environment
	 * @return 是否已处理 / whether handled
	 */
	static boolean questTimerEnd(QuestEnv env) {
		return questTimerEnd(env, QuestTimerPolicy.visible().identity());
	}

	/**
	 * 以指定身份结束并取消任务计时器。
	 * Ends and cancels the quest timer with the given identity.
	 * @param env 任务环境 / quest environment
	 * @param identity 计时器身份 / timer identity
	 * @return 是否已处理 / whether handled
	 */
	static boolean questTimerEnd(QuestEnv env, QuestTimerPolicy.Identity identity) {
		if (env == null || env.getPlayer() == null || env.getQuestId() <= 0 || identity == null) {
			return false;
		}
		Player player = env.getPlayer();
		ManagedQuestTimer removed = cancelManagedTimer(player.getObjectId(), env.getQuestId(), identity);
		if ((removed != null && removed.visible) || QuestTimerPolicy.VISIBLE_TIMER_ID.equals(identity.timerId())) {
			PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(env.getQuestId(), 0));
		}
		return true;
	}

	/**
	 * 清理指定玩家的指定任务计时器。
	 * Cleans the timers of one quest for one player.
	 * @param playerId 玩家对象 ID / player object id
	 * @param questId 任务 ID / quest id
	 */
	static void cleanupQuestTimers(int playerId, int questId) {
		cleanupTimers(key -> key.playerId == playerId && key.questId == questId);
	}

	/**
	 * 清理指定玩家的全部任务计时器。
	 * Cleans every quest timer of one player.
	 * @param playerId 玩家对象 ID / player object id
	 */
	static void cleanupPlayerQuestTimers(int playerId) {
		cleanupTimers(key -> key.playerId == playerId);
	}

	/**
	 * 清理指定副本的全部任务计时器。
	 * Cleans every quest timer of one instance.
	 * @param instanceId 副本实例 ID / instance id
	 */
	static void cleanupInstanceQuestTimers(int instanceId) {
		cleanupTimers(key -> {
			ManagedQuestTimer timer = questTimers.get(key);
			return timer != null && timer.instanceId == instanceId;
		});
	}

	/**
	 * 清空全部任务计时器。
	 * Clears every quest timer.
	 */
	static void cleanupAllQuestTimers() {
		cleanupTimers(key -> true);
	}

	/**
	 * 判断某玩家的某任务是否已有计时器在跑。
	 * Reports whether a player has a running timer for a quest.
	 * @param playerId 玩家对象 ID / player object id
	 * @param questId 任务 ID / quest id
	 * @return 是否有计时器 / whether any timer is registered
	 */
	static boolean hasQuestTimers(int playerId, int questId) {
		return questTimers.keySet().stream().anyMatch(key -> key.playerId == playerId && key.questId == questId);
	}

	/**
	 * 校验计时器参数并返回玩家。
	 * Validates the timer arguments and returns the player.
	 * @param env 任务环境 / quest environment
	 * @param seconds 计时秒数 / timer length in seconds
	 * @param policy 计时器策略 / timer policy
	 * @return 玩家 / player
	 */
	private static Player requireTimerArguments(QuestEnv env, int seconds, QuestTimerPolicy policy) {
		if (env == null || env.getPlayer() == null || env.getQuestId() <= 0) {
			throw new IllegalArgumentException("timer requires a player and positive questId");
		}
		if (seconds <= 0) {
			throw new IllegalArgumentException("timer seconds must be positive");
		}
		if (policy == null) {
			throw new NullPointerException("policy");
		}
		return env.getPlayer();
	}

	/**
	 * 用默认调度器启动受管计时器。
	 * Starts a managed timer with the default scheduler.
	 * @param player 玩家 / player
	 * @param questId 任务 ID / quest id
	 * @param seconds 计时秒数 / timer length in seconds
	 * @param policy 计时器策略 / timer policy
	 * @param visible 是否可见 / whether visible
	 * @param callback 到期回调 / expiry callback
	 * @return 启动结果 / start outcome
	 */
	private static TimerStartOutcome startManagedTimer(Player player, int questId, int seconds, QuestTimerPolicy policy,
			boolean visible, IntConsumer callback) {
		return startManagedTimer(player, questId, seconds, policy, visible, callback,
			(task, delayMillis) -> GameThreadPoolServices.threadPoolManager().schedule(task, delayMillis));
	}

	/**
	 * 用指定调度器启动受管计时器。
	 * Starts a managed timer with the given scheduler.
	 * @param player 玩家 / player
	 * @param questId 任务 ID / quest id
	 * @param seconds 计时秒数 / timer length in seconds
	 * @param policy 计时器策略 / timer policy
	 * @param visible 是否可见 / whether visible
	 * @param callback 到期回调 / expiry callback
	 * @param scheduler 调度器 / scheduler
	 * @return 启动结果 / start outcome
	 */
	static TimerStartOutcome startManagedTimer(Player player, int questId, int seconds, QuestTimerPolicy policy,
			boolean visible, IntConsumer callback, QuestTimerScheduler scheduler) {
		if (player == null || questId <= 0 || seconds <= 0) {
			throw new IllegalArgumentException("managed timer requires a player, positive questId, and positive seconds");
		}
		if (policy == null || callback == null || scheduler == null) {
			throw new NullPointerException("managed timer policy, callback, and scheduler are required");
		}
		QuestTimerKey key = new QuestTimerKey(player.getObjectId(), questId, policy.identity());
		synchronized (questTimerLock) {
			ManagedQuestTimer existing = questTimers.get(key);
			if (existing != null) {
                switch (policy.overwritePolicy()) {
                    case KEEP_EXISTING:
                        return TimerStartOutcome.KEPT_EXISTING;
                    case FAIL_IF_RUNNING:
                        throw new IllegalStateException(
                                "quest timer is already running: " + policy.identity().timerId());
                    case REPLACE:
                        existing.cancel();
                        break;
                }
			}
			ManagedQuestTimer timer = new ManagedQuestTimer(policy, player.getInstanceId(), visible);
			Future<?> future = scheduler.schedule(() -> {
				boolean deliver;
				synchronized (questTimerLock) {
					deliver = questTimers.remove(key, timer);
				}
				if (deliver) {
					callback.accept(questId);
				}
			}, seconds * 1000L);
			timer.future = future;
			questTimers.put(key, timer);
			return TimerStartOutcome.STARTED;
		}
	}

	/**
	 * 计时器启动结果。
	 * Quest timer start outcome.
	 */
	enum TimerStartOutcome {
		/** 已启动 / started */
		STARTED,
		/** 已有同名计时器，保持原样 / an existing timer was kept */
		KEPT_EXISTING
	}

	/**
	 * 计时器调度端口，便于测试注入确定性调度。
	 * Timer scheduling port so tests can inject a deterministic scheduler.
	 */
	@FunctionalInterface
	interface QuestTimerScheduler {
		/**
		 * 在指定延迟后执行任务。
		 * Schedules the task after the given delay.
		 * @param task 任务 / task
		 * @param delayMillis 延迟毫秒 / delay in milliseconds
		 * @return 任务句柄 / task handle
		 */
		Future<?> schedule(Runnable task, long delayMillis);
	}

	/**
	 * 取消并移除指定计时器。
	 * Cancels and removes the given timer.
	 * @param playerId 玩家对象 ID / player object id
	 * @param questId 任务 ID / quest id
	 * @param identity 计时器身份 / timer identity
	 * @return 被移除的计时器；不存在时为 {@code null} / removed timer, or {@code null}
	 */
	private static ManagedQuestTimer cancelManagedTimer(int playerId, int questId,
			QuestTimerPolicy.Identity identity) {
		QuestTimerKey key = new QuestTimerKey(playerId, questId, identity);
		synchronized (questTimerLock) {
			ManagedQuestTimer timer = questTimers.remove(key);
			if (timer != null) {
				timer.cancel();
			}
			return timer;
		}
	}

	/**
	 * 按谓词清理计时器并取消其任务。
	 * Removes the timers matching the predicate and cancels their tasks.
	 * @param predicate 匹配条件 / match predicate
	 */
	private static void cleanupTimers(Predicate<QuestTimerKey> predicate) {
		synchronized (questTimerLock) {
			questTimers.entrySet().removeIf(entry -> {
				boolean match = predicate.test(entry.getKey());
				if (match) {
					entry.getValue().cancel();
				}
				return match;
			});
		}
	}

	/**
	 * 计时器键：（玩家、任务、策略身份）。
	 * Timer key: (player, quest, policy identity).
	 * 玩家对象 ID / player object id
	 * 任务 ID / quest id
	 */
	private record QuestTimerKey(int playerId, int questId, QuestTimerPolicy.Identity identity) {
	}

	/**
	 * 受管计时器：记录策略、副本与可见性，持有可取消的任务句柄。
	 * Managed timer holding the policy, instance, visibility and the cancellable task handle.
	 */
	private static final class ManagedQuestTimer {
		private final QuestTimerPolicy policy;
		private final int instanceId;
		private final boolean visible;
		private volatile Future<?> future;

		/**
		 * 创建受管计时器。
		 * Creates a managed timer.
		 * @param policy 计时器策略 / timer policy
		 * @param instanceId 副本实例 ID / instance id
		 * @param visible 是否可见 / whether visible
		 */
		private ManagedQuestTimer(QuestTimerPolicy policy, int instanceId, boolean visible) {
			this.policy = policy;
			this.instanceId = instanceId;
			this.visible = visible;
		}

		/**
		 * 取消底层任务。
		 * Cancels the underlying task.
		 */
		private void cancel() {
			Future<?> task = future;
			if (task != null) {
				task.cancel(false);
			}
		}
	}
}
