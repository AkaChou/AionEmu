package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 真实 {@link QuestBroadcastPort}：提交后通过生产分发器向每个任务广播区域任务结束。
 * Real {@link QuestBroadcastPort}: after commit, dispatches zone-mission-end to
 * each listed quest via the production dispatcher. The dispatcher callback is
 * injected after the dispatcher is constructed (break the composition cycle).
 */
public final class PlayerQuestBroadcastPort implements QuestBroadcastPort {
	@FunctionalInterface
	public interface BroadcastCall {
		boolean broadcast(Player player, int[] questIds);
	}

	@FunctionalInterface
	public interface DelayedScheduler {
		void schedule(Runnable task, long delayMillis);
	}

	private final QuestPlayerPort players;
	private final BroadcastCall broadcast;
	private final BroadcastCall refresh;
	private final DelayedScheduler scheduler;
	private final Set<ScheduleKey> pendingRefreshes = ConcurrentHashMap.newKeySet();

	public PlayerQuestBroadcastPort(QuestPlayerPort players, BroadcastCall broadcast) {
		this(players, broadcast, null,
			(task, delayMillis) -> GameThreadPoolServices.threadPoolManager().schedule(task, delayMillis));
	}

	public PlayerQuestBroadcastPort(QuestPlayerPort players, BroadcastCall broadcast, BroadcastCall refresh) {
		this(players, broadcast, refresh,
			(task, delayMillis) -> GameThreadPoolServices.threadPoolManager().schedule(task, delayMillis));
	}

	PlayerQuestBroadcastPort(QuestPlayerPort players, BroadcastCall broadcast, BroadcastCall refresh,
			DelayedScheduler scheduler) {
		this.players = Objects.requireNonNull(players, "players");
		this.broadcast = Objects.requireNonNull(broadcast, "broadcast");
		this.refresh = refresh;
		this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
	}

	@Override
	public boolean broadcastZoneMissionEnd(QuestSnapshot snapshot, QuestMutationPlan plan, int[] questIds) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		Objects.requireNonNull(questIds, "questIds");
		Player player = players.find(snapshot.playerId());
		if (player == null) {
			// 提交已成功但玩家已登出：无可分发对象，best-effort 跳过。 / Commit succeeded but player logged out: nothing to dispatch to, best-effort skip.
			return false;
		}
		return broadcast.broadcast(player, questIds);
	}

	@Override
	public boolean scheduleEventQuestRefresh(QuestSnapshot snapshot, QuestMutationPlan plan, int seconds,
			int[] questIds) {
		Objects.requireNonNull(snapshot, "snapshot");
		Objects.requireNonNull(plan, "plan");
		Objects.requireNonNull(questIds, "questIds");
		if (refresh == null || seconds <= 0 || questIds.length == 0) {
			return false;
		}
		int[] targets = questIds.clone();
		ScheduleKey key = new ScheduleKey(snapshot.playerId(), plan.questId(),
			Arrays.stream(targets).boxed().toList());
		if (!pendingRefreshes.add(key)) {
			return true;
		}
		try {
			scheduler.schedule(() -> {
				try {
					Player player = players.find(snapshot.playerId());
					if (player != null) {
						refresh.broadcast(player, targets);
					}
				} finally {
					pendingRefreshes.remove(key);
				}
			}, Math.multiplyExact(seconds, 1000L));
			return true;
		} catch (RuntimeException schedulingFailure) {
			pendingRefreshes.remove(key);
			return false;
		}
	}

	private record ScheduleKey(int playerId, int sourceQuestId, List<Integer> targets) {
		private ScheduleKey {
			targets = List.copyOf(targets);
		}
	}
}
