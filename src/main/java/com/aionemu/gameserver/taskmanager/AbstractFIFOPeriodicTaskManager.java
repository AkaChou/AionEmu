package com.aionemu.gameserver.taskmanager;

import com.aionemu.boot.i18n.I18n;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.commons.utils.concurrent.RunnableStatsManager;

/**
 * FIFO 周期性任务管理器：每周期把入队任务批量取出并依次处理。
 * FIFO periodic task manager: each tick drains queued tasks and processes them in order.
 *
 * <p>基于 l2j-free 引擎思路。/ Based on l2j-free engines.</p>
 *
 * @param <T> 任务元素类型 / Task element type
 * @author lord_rex, MrPoke
 */
@Slf4j
public abstract class AbstractFIFOPeriodicTaskManager<T> extends AbstractPeriodicTaskManager {

	/**
	 * 待处理任务队列（保持插入顺序）。
	 * Pending task queue (insertion order preserved).
	 */
	private final Set<T> queue = new LinkedHashSet<T>();

	/**
	 * 当前周期内正在处理的任务集合。
	 * Active tasks being processed in the current tick.
	 */
	private final Set<T> activeTasks = new LinkedHashSet<T>();

	/**
	 * 以给定周期构造 FIFO 任务管理器。
	 * Construct a FIFO task manager with the given period.
	 *
	 * @param period 周期毫秒数 / Period in milliseconds
	 */
	public AbstractFIFOPeriodicTaskManager(int period) {
		super(period);
	}

	/**
	 * 将任务加入待处理队列（写锁保护）。
	 * Enqueue a task (write-locked).
	 *
	 * @param t 任务 / Task
	 */
	public final void add(T t) {
		writeLock();
		try {
			queue.add(t);
		} finally {
			writeUnlock();
		}
	}

	/**
	 * 将队列转入活动集，再逐个调用 {@link #callTask(Object)} 并记录耗时统计。
	 * Move the queue into the active set, then invoke {@link #callTask(Object)} for each and record stats.
	 */
	@Override
	public final void run() {
		writeLock();
		try {
			activeTasks.addAll(queue);

			queue.clear();
		} finally {
			writeUnlock();
		}

		for (T task; (task = removeFirst(activeTasks)) != null;) {
			final long begin = System.nanoTime();

			try {
				callTask(task);
			} catch (RuntimeException e) {
				log.warn(I18n.get("log.4c80525d5d73", task, getClass().getSimpleName(), e), e);
			} finally {
				RunnableStatsManager.handleStats(task.getClass(), getCalledMethodName(), System.nanoTime() - begin);
			}
		}
	}

	/**
	 * 处理单个任务。
	 * Process a single task.
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

	/**
	 * 从集合头部取出并移除第一个元素。
	 * Remove and return the first element of the set.
	 *
	 * @param tasks 任务集合 / Task set
	 * @return 首个任务；空则 null / First task, or null if empty
	 */
	private T removeFirst(Set<T> tasks) {
		Iterator<T> iterator = tasks.iterator();
		if (!iterator.hasNext()) {
			return null;
		}
		T task = iterator.next();
		iterator.remove();
		return task;
	}
}
