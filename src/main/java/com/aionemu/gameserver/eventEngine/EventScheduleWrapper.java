package com.aionemu.gameserver.eventEngine;

import com.aionemu.gameserver.lifecycle.GameEventServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.ScheduledFuture;

/**
 * 周期调度包装：在固定周期内重新投递事件，并在上一次未完成时尝试取消。
 * Fixed-rate schedule wrapper that requeues the event and cancels if still running.
 *
 * @author wanke
 */
class EventScheduleWrapper implements Runnable {

	/**
	 * 未完成时的重检延迟（分钟）。
	 * Recheck delay in minutes when previous run is unfinished.
	 */
	private static final int RECHECK_DELAY = 2;

	/**
	 * 被周期调度的事件。
	 * Event under fixed-rate schedule.
	 */
	private final Event event;

	/**
	 * 是否首次调度。
	 * Whether this is the first schedule pass.
	 */
	private boolean first = true;

	/**
	 * 最近一次重检 future。
	 * Last recheck future.
	 */
	private ScheduledFuture<?> last_future;

	/**
	 * 包装指定事件。
	 * Wraps the given event.
	 *
	 * @param event 目标事件 / target event
	 */
	public EventScheduleWrapper(Event event) {
		this.event = event;
	}

	/**
	 * 周期触发：若上一次重检未完成则跳过；否则检查并重新投递。
	 * Periodic tick: skip if last recheck is pending; otherwise check and requeue.
	 */
	@Override
	public void run() {
		if (last_future != null) {
			if (!last_future.isDone()) {
				return;
			}
		}
		if (!check()) {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					check();
				}
			};
			last_future = GameThreadPoolServices.threadPoolManager().schedule(runnable, RECHECK_DELAY * 60 * 1000);
		}
	}

	/**
	 * 首次或已完成则重新调度；否则取消并返回 false。
	 * Requeues when first or finished; otherwise cancels and returns false.
	 *
	 * @return 是否成功投递 / whether requeue succeeded
	 */
	private boolean check() {
		if (event.isFinished() || first) {
			first = false;
			GameEventServices.eventScheduler().schedule(event, 10);
			return true;
		} else {
			event.cancel(true);
			return false;
		}
	}
}
