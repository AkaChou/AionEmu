package com.aionemu.gameserver.taskmanager;

import com.aionemu.boot.i18n.I18n;
import java.util.LinkedHashSet;
import java.util.Set;

import com.aionemu.commons.utils.concurrent.RunnableStatsManager;

/**
 * 迭代式周期性任务管理器：维护可启动/停止的活跃任务集，每周期遍历执行。
 * Iterative periodic task manager: maintains a start/stop active-task set and runs over it each tick.
 *
 * @param <T> 任务元素类型 / Task element type
 * @author NB4L1
 */
public abstract class AbstractIterativePeriodicTaskManager<T> extends AbstractPeriodicTaskManager {

	/**
	 * 待加入活跃集的任务。
	 * Tasks pending addition to the active set.
	 */
	private final Set<T> startList = new LinkedHashSet<T>();

	/**
	 * 待从活跃集移除的任务。
	 * Tasks pending removal from the active set.
	 */
	private final Set<T> stopList = new LinkedHashSet<T>();

	/**
	 * 当前活跃任务集合。
	 * Currently active tasks.
	 */
	private final Set<T> activeTasks = new LinkedHashSet<T>();

	/**
	 * 以给定周期构造迭代式任务管理器。
	 * Construct an iterative task manager with the given period.
	 *
	 * @param period 周期毫秒数 / Period in milliseconds
	 */
	protected AbstractIterativePeriodicTaskManager(int period) {
		super(period);
	}

	/**
	 * 判断任务是否处于活跃或即将启动（且不在停止列表中）。
	 * Whether the task is active or pending start (and not on the stop list).
	 *
	 * Task
	 *
	 * @param task 若 tracked as active 则为 true / True if tracked as active
	 */
	public boolean hasTask(T task) {
		readLock();
		try {
			if (stopList.contains(task)) {
				return false;
			}
			return activeTasks.contains(task) || startList.contains(task);
		} finally {
			readUnlock();
		}
	}

	/**
	 * 请求启动任务：加入 start 列表并取消 stop。
	 * Request starting a task: add to start list and clear stop.
	 *
	 * Task
	 */
	public void startTask(T task) {
		writeLock();
		try {
			startList.add(task);

			stopList.remove(task);
		} finally {
			writeUnlock();
		}
	}

	/**
	 * 请求停止任务：加入 stop 列表并取消 start。
	 * Request stopping a task: add to stop list and clear start.
	 *
	 * Task
	 */
	public void stopTask(T task) {
		writeLock();
		try {
			stopList.add(task);

			startList.remove(task);
		} finally {
			writeUnlock();
		}
	}

	/**
	 * 合并 start/stop 到活跃集，再遍历调用 {@link #callTask(Object)} 并记录统计。
	 * Merge start/stop into the active set, then iterate {@link #callTask(Object)} with stats.
	 */
	@Override
	public final void run() {
		writeLock();
		try {
			activeTasks.addAll(startList);
			activeTasks.removeAll(stopList);

			startList.clear();
			stopList.clear();
		} finally {
			writeUnlock();
		}

		for (T task : activeTasks) {
			final long begin = System.nanoTime();

			try {
				callTask(task);
			} catch (RuntimeException e) {
				log.warn(I18n.get("log.4c80525d5d73", task, getClass().getSimpleName(), e));
			} finally {
				RunnableStatsManager.handleStats(task.getClass(), getCalledMethodName(), System.nanoTime() - begin);
			}
		}
	}

	/**
	 * 处理单个活跃任务。
	 * Process a single active task.
	 *
	 * Task
	 */
	protected abstract void callTask(T task);

	/**
	 * 供耗时统计使用的被调方法名。
	 * Method name used for runtime statistics.
	 *
	 * Method name
	 */
	protected abstract String getCalledMethodName();
}
