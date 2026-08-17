package com.aionemu.gameserver.questEngine.e2e.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * 不依赖真实线程的确定性虚拟时钟；只有显式 tick 才会触发已安排的 AI/任务回调。
 * Deterministic virtual clock independent of real threads; scheduled AI/quest callbacks run only on explicit ticks.
 */
public final class VirtualClock {
	private final List<Scheduled> scheduled = new ArrayList<>();
	private long nowMillis;

	/** 安排一个未来回调并返回可取消句柄。 / Schedules a future callback and returns a cancellable handle. */
	public Future<?> schedule(long delayMillis, Runnable action) {
		if (delayMillis < 0 || action == null) {
			throw new IllegalArgumentException("delayMillis must be non-negative and action must not be null");
		}
		Scheduled scheduledAction = new Scheduled(nowMillis + delayMillis, action);
		scheduled.add(scheduledAction);
		return scheduledAction.future;
	}

	/** 前进虚拟时间并执行到期回调。 / Advances virtual time and executes due callbacks. */
	public void tick(long deltaMillis) {
		if (deltaMillis < 0) {
			throw new IllegalArgumentException("deltaMillis must be non-negative");
		}
		nowMillis += deltaMillis;
		scheduled.sort(Comparator.comparingLong(entry -> entry.dueMillis));
		for (Scheduled entry : List.copyOf(scheduled)) {
			if (entry.dueMillis <= nowMillis && !entry.future.isCancelled()) {
				entry.future.complete(null);
				entry.action.run();
				scheduled.remove(entry);
			}
		}
	}

	/** 当前虚拟时间。 / Current virtual time. */
	public long nowMillis() {
		return nowMillis;
	}

	private static final class Scheduled {
		private final long dueMillis;
		private final Runnable action;
		private final CompletableFuture<Void> future = new CompletableFuture<>();

		private Scheduled(long dueMillis, Runnable action) {
			this.dueMillis = dueMillis;
			this.action = action;
		}
	}
}
