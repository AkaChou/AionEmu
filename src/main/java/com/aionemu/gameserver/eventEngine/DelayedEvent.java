package com.aionemu.gameserver.eventEngine;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import lombok.Getter;

/**
 * 带延迟触发时间的事件包装，支持按剩余延迟与优先级比较排序。
 * Delayed event wrapper ordered by remaining delay and priority.
 *
 * @author wanke
 */
class DelayedEvent extends Event implements Comparable<DelayedEvent> {

	/**
	 * 预计触发时间。
	 * Forecast fire time.
	 */
	private final Date forecast;

	/**
	 * 被包装的实际事件。
	 * Wrapped real event.
	 */
	@Getter
	private final Event event;

	/**
	 * 以当前时间加延迟构造延迟事件。
	 * Builds a delayed event from now plus the given delay.
	 *
	 * @param event 实际事件 / real event
	 * @param delay 延迟毫秒 / delay millis
	 */
	public DelayedEvent(Event event, int delay) {
		this.forecast = new Date(System.currentTimeMillis() + delay);
		this.event = event;
	}

	/**
	 * 按剩余延迟比较；延迟接近时叠加优先级偏移。
	 * Compares by remaining delay; near delays also factor priority.
	 *
	 * @param o 另一延迟事件 / other delayed event
	 * comparison result
	 */
	@Override
	public int compareTo(DelayedEvent o) {
		int delay = (int) (this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
		if (delay > (0 - Event.MAX_PRIORITY) * 60 * 1000 && delay < MAX_PRIORITY * 60 * 1000) {
			delay = (int) ((getDelay(TimeUnit.MILLISECONDS) - getEvent().getPriority() * 60 * 1000)
					- (o.getDelay(TimeUnit.MILLISECONDS) - o.getEvent().getPriority() * 60 * 1000));
		}
		return delay;
	}

	/**
	 * 距离触发时间的剩余延迟。
	 * Remaining delay until forecast fire time.
	 *
	 * @param unit 时间单位 / time unit
	 * remaining delay
	 */
	public long getDelay(TimeUnit unit) {
		return unit.convert(forecast.compareTo(new Date()), TimeUnit.MILLISECONDS);
	}

	@Override
	public void execute() {
		getEvent().execute();
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return event.cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isFinished() {
		return event.isFinished();
	}

	@Override
	public int getCooldown() {
		return event.getCooldown();
	}

	@Override
	public int getPriority() {
		return event.getPriority();
	}

	@Override
	public void setPriority(int priority) {
		event.setPriority(priority);
	}

	@Override
	protected void onReset() {
		event.onReset();
	}
}
