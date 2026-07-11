package com.aionemu.gameserver.movement.processors;


import com.aionemu.boot.i18n.I18n;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.commons.utils.concurrent.AionRejectedExecutionHandler;
import com.aionemu.commons.utils.concurrent.RunnableWrapper;
import com.aionemu.gameserver.configs.main.ThreadConfig;

/**
 * 游戏侧调度处理器基类：封装固定线程池上的立即执行与延时/周期调度。
 * Base game-side scheduler wrapping immediate, delayed and fixed-rate tasks on a fixed thread pool.
 */
@Slf4j
public class AGameProcessor {

	/**
	 * 底层定时线程池。
	 * Underlying scheduled thread pool.
	 */
	private ScheduledThreadPoolExecutor _processorPool;

	/**
	 * 以指定核心线程数创建处理器并预启动全部核心线程。
	 * Create a processor with the given core-thread count and prestart all core threads.
	 *
	 * @param threadsCount 核心线程数 / Core thread count
	 */
	protected AGameProcessor(int threadsCount) {
		this._processorPool = new ScheduledThreadPoolExecutor(threadsCount);
		this._processorPool.setRejectedExecutionHandler((RejectedExecutionHandler) new AionRejectedExecutionHandler());
		this._processorPool.prestartAllCoreThreads();
	}

	/**
	 * 立即提交任务到线程池。
	 * Submit a task for immediate execution.
	 *
	 * @param r 待执行任务 / Task to run
	 */
	public void execute(Runnable r) {
		this._processorPool.execute(r);
	}

	/**
	 * 延时调度任务；延时会被钳制到合法区间。
	 * Schedule a delayed task; the delay is clamped to a valid range.
	 *
	 * @param r 待执行任务 / Task to run
	 * @param delay 延时毫秒数 / Delay in milliseconds
	 * Scheduled future
	 */
	public ScheduledFuture<?> schedule(Runnable r, long delay) {
		r = new RunnableTaskWrapper((Runnable) r);
		long validated = Math.max(0L, Math.min(Integer.MAX_VALUE, delay));
		if (validated < delay) {
			log.warn(I18n.get("log.fb1d6c13ae41", (Object) delay, (Object) validated));
		}
		delay = validated;
		return this._processorPool.schedule((Runnable) r, delay, TimeUnit.MILLISECONDS);
	}

	/**
	 * 固定周期调度任务；初始延时会被钳制到合法区间。
	 * Schedule a fixed-rate task; the initial delay is clamped to a valid range.
	 *
	 * @param r 待执行任务 / Task to run
	 * @param delay 初始延时毫秒数 / Initial delay in milliseconds
	 * @param period 周期毫秒数 / Period in milliseconds
	 * Scheduled future
	 */
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long delay, long period) {
		r = new RunnableTaskWrapper((Runnable) r);
		long validated = Math.max(0L, Math.min(Integer.MAX_VALUE, delay));
		if (validated < delay) {
			log.warn(I18n.get("log.fb1d6c13ae41", (Object) delay, (Object) validated));
		}
		delay = validated;
		return this._processorPool.scheduleAtFixedRate((Runnable) r, delay, period, TimeUnit.MILLISECONDS);
	}

	/**
	 * 延时调度并将句柄写入 {@link Task}；延时非法时返回 false。
	 * Schedule a delayed task into a {@link Task} holder; returns false when delay is invalid.
	 *
	 * @param r 待执行任务 / Task to run
	 * @param delay 延时毫秒数 / Delay in milliseconds
	 * @param out 输出任务句柄容器 / Output task holder
	 * @return 调度成功为 true / {@code true} if scheduled
	 */
	public boolean schedule(Runnable r, long delay, Task out) {
		r = new RunnableTaskWrapper((Runnable) r);
		long validated = Math.max(0L, Math.min(Integer.MAX_VALUE, delay));
		if (validated < delay) {
			log.warn(I18n.get("log.832d28ea08bf", (Object) delay, (Object) validated));
			return false;
		}
		delay = validated;
		out.setTask(this._processorPool.schedule((Runnable) r, delay, TimeUnit.MILLISECONDS));
		return true;
	}

	/**
	 * 可取消调度任务的句柄容器。
	 * Holder for a cancellable scheduled-task handle.
	 */
	public static class Task {

		/**
		 * 底层调度句柄。
		 * Underlying scheduled future.
		 */
		private ScheduledFuture<?> _task;

		/**
		 * 创建空任务容器。
		 * Create an empty task holder.
		 *
		 * New holder
		 */
		public static Task create() {
			return new Task();
		}

		/**
		 * 返回底层调度句柄。
		 * Return the underlying scheduled future.
		 *
		 * @return 调度句柄，可能为 null / Scheduled future, may be null
		 */
		public ScheduledFuture<?> getTask() {
			return this._task;
		}

		/**
		 * 设置底层调度句柄。
		 * Set the underlying scheduled future.
		 *
		 * @param task 调度句柄 / Scheduled future
		 */
		private void setTask(ScheduledFuture<?> task) {
			this._task = task;
		}
	}

	/**
	 * 带运行时告警阈值的任务包装器。
	 * Runnable wrapper with a runtime warning threshold.
	 */
	private static final class RunnableTaskWrapper extends RunnableWrapper {

		/**
		 * 使用全局线程配置中的最大无告警运行时间包装任务。
		 * Wrap a runnable with the global max runtime without warning.
		 *
		 * Original runnable
		 */
		private RunnableTaskWrapper(Runnable runnable) {
			super(runnable, ThreadConfig.MAXIMUM_RUNTIME_IN_MILLISEC_WITHOUT_WARNING);
		}
	}
}
