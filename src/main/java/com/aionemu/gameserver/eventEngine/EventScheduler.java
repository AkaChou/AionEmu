package com.aionemu.gameserver.eventEngine;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.ObjectProvider;

/**
 * 事件调度器：从延迟队列取事件执行，并支持固定周期调度与暂停。
 * Event scheduler that drains a delay queue, supports fixed-rate schedule and pause.
 *
 * @author wanke
 */
@Slf4j
public class EventScheduler implements Runnable {

	/**
	 * Spring 可选实例提供者。
	 * Optional Spring instance provider.
	 */
	private static volatile ObjectProvider<EventScheduler> instanceProvider;

	/**
	 * 单次事件最长执行超时（分钟）。
	 * Max execution timeout per event in minutes.
	 */
	private static final int TIMEOUT = 15;

	/**
	 * 调度轮询间隔（毫秒）。
	 * Scheduler poll interval in millis.
	 */
	private static final int WORKING_DELAY = 1000;

	/**
	 * 延迟事件队列。
	 * Delayed event queue.
	 */
	private final EventQueue<DelayedEvent> queue = new EventQueue<DelayedEvent>();

	/**
	 * 当前执行开始时间戳；0 表示空闲。
	 * Current execution start millis; 0 means idle.
	 */
	private long startTime = 0;

	/**
	 * 是否暂停调度。
	 * Whether scheduling is paused.
	 */
	private AtomicBoolean paused = new AtomicBoolean(false);

	/**
	 * 构造并启动调度循环。
	 * Constructs and starts the schedule loop.
	 */
	public EventScheduler() {
		GameThreadPoolServices.threadPoolManager().schedule(this, 1);
		log.info(I18n.get("log.103bcf34081c"));
	}

	/**
	 * 延迟投递事件（先 reset）。
	 * Schedules an event after delay (resets first).
	 *
	 * @param event 事件 / event
	 * @param delay 延迟毫秒 / delay millis
	 */
	public void schedule(final Event event, int delay) {
		event.reset();
		queue.offer(new DelayedEvent(event, delay));
	}

	/**
	 * 固定周期调度事件。
	 * Schedules an event at fixed rate.
	 *
	 * @param event 事件 / event
	 * @param delay 首次延迟毫秒 / initial delay millis
	 * @param period 周期毫秒 / period millis
	 * @return 调度 future / scheduled future
	 */
	public ScheduledFuture<?> scheduleAtFixedRate(Event event, int delay, int period) {
		ScheduledFuture<?> future = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new EventScheduleWrapper(event),
				delay, period);
		return future;
	}

	/**
	 * 调度循环：暂停则延后；否则取到期事件并执行。
	 * Schedule loop: defer when paused; otherwise poll and execute due events.
	 */
	@Override
	public void run() {
		if (paused.get()) {
			GameThreadPoolServices.threadPoolManager().schedule(this, WORKING_DELAY);
			return;
		}

		Event event = queue.poll();
		if (event == null) {
			GameThreadPoolServices.threadPoolManager().schedule(this, WORKING_DELAY);
			return;
		}
		execute(event);
	}

	/**
	 * 异步执行事件并启动超时监控。
	 * Executes the event asynchronously and starts timeout watch.
	 *
	 * @param event 事件 / event
	 */
	private void execute(Event event) {
		GameThreadPoolServices.threadPoolManager().schedule(event, 0);
		startTime = System.currentTimeMillis();
		GameThreadPoolServices.threadPoolManager().schedule(new WaitForExecutionRunnable(event), WORKING_DELAY);
		return;
	}

	/**
	 * 等待事件完成或超时，然后按冷却重新进入调度循环。
	 * Waits for finish or timeout, then re-enters the loop after cooldown.
	 *
	 * @param event 事件 / event
	 */
	private void waitForExecution(Event event) {
		if (event.isFinished()) {
			startTime = 0;
			GameThreadPoolServices.threadPoolManager().schedule(this, event.getCooldown());
		} else if ((System.currentTimeMillis() - startTime) > TIMEOUT * 60 * 1000) {
			log.warn(I18n.get("log.645de9a4710b", event.getClass().getName(), TIMEOUT));
			startTime = 0;
			GameThreadPoolServices.threadPoolManager().schedule(this, event.getCooldown());
		} else {
			GameThreadPoolServices.threadPoolManager().schedule(new WaitForExecutionRunnable(event), WORKING_DELAY);
		}
	}

	/**
	 * 等待事件完成的定时任务。
	 * Periodic task that waits for event completion.
	 */
	private class WaitForExecutionRunnable implements Runnable {

		/**
		 * 被监控的事件。
		 * Watched event.
		 */
		private final Event event;

		/**
		 * 构造等待任务。
		 * Constructs the wait task.
		 *
		 * @param event 事件 / event
		 */
		public WaitForExecutionRunnable(Event event) {
			this.event = event;
		}

		@Override
		public void run() {
			waitForExecution(event);
		}
	}

	/**
	 * 切换暂停状态。
	 * Toggles pause state.
	 */
	public void pause() {
		paused.set(!paused.get());
	}

	/**
	 * 非 Spring 环境下的单例持有者。
	 * Singleton holder for non-Spring fallback.
	 */
	private static class SingletonHolder {

		public static EventScheduler singleton = new EventScheduler();
	}

	/**
	 * 获取调度器实例（优先 Spring provider）。
	 * Returns scheduler instance (prefers Spring provider).
	 *
	 * @return 调度器 / scheduler
	 */
	static public EventScheduler getInstance() {
		ObjectProvider<EventScheduler> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.singleton);
		}
		return SingletonHolder.singleton;
	}

	/**
	 * 设置 Spring 实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param provider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<EventScheduler> provider) {
		instanceProvider = provider;
	}
}
