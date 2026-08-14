package com.aionemu.gameserver.taskmanager;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.locks.ReentrantLock;

/**
 * FIFO 可执行队列基类：保证同一时刻至多一个执行线程按顺序处理队列。
 * Base FIFO executable queue: at most one worker drains the queue in order at a time.
 *
 * <p>计划移除（Nemesiss）。/ Going to be removed (Nemesiss).</p>
 *
 * @author NB4L1
 */
public abstract class FIFOExecutableQueue implements Runnable {

	/**
	 * 空闲状态。
	 * Idle state.
	 */
	private static final byte NONE = 0;

	/**
	 * 已入线程池等待执行。
	 * Queued in the thread pool awaiting run.
	 */
	private static final byte QUEUED = 1;

	/**
	 * 正在执行中。
	 * Currently running.
	 */
	private static final byte RUNNING = 2;

	/**
	 * 状态与临界区锁。
	 * Lock guarding state and critical sections.
	 */
	private final ReentrantLock lock = new ReentrantLock();

	/**
	 * 当前队列状态：{@link #NONE} / {@link #QUEUED} / {@link #RUNNING}。
	 * Current queue state: {@link #NONE} / {@link #QUEUED} / {@link #RUNNING}.
	 */
	private volatile byte state = NONE;

	/**
	 * 若当前为空闲则标记为已排队并提交到线程池。
	 * If idle, mark as queued and submit to the thread pool.
	 */
	protected final void execute() {
		lock();
		try {
			if (state != NONE) {
				return;
			}
			state = QUEUED;
		} finally {
			unlock();
		}
		GameThreadPoolServices.threadPoolManager().execute(this);
	}

	/**
	 * 获取队列锁。
	 * Acquire the queue lock.
	 */
	public final void lock() {
		lock.lock();
	}

	/**
	 * 释放队列锁。
	 * Release the queue lock.
	 */
	public final void unlock() {
		lock.unlock();
	}

	/**
	 * 循环排空队列：在 RUNNING/QUEUED 间切换直至为空，最后回到 NONE。
	 * Drain the queue in a loop, toggling RUNNING/QUEUED until empty, then back to NONE.
	 */
	public final void run() {
		try {
			while (!isEmpty()) {
				setState(QUEUED, RUNNING);

				try {
					while (!isEmpty()) {
						removeAndExecuteFirst();
					}
				} finally {
					setState(RUNNING, QUEUED);
				}
			}
		} finally {
			setState(QUEUED, NONE);
		}
	}

	/**
	 * 在持锁下校验期望状态并切换到新状态。
	 * Under lock, assert expected state and transition to the new value.
	 *
	 * @param expected 期望当前状态 / Expected current state
	 * @param value 目标状态 / Target state
	 */
	private void setState(byte expected, byte value) {
		lock();
		try {
			if (state != expected) {
				throw new IllegalStateException("state: " + state + ", expected: " + expected);
			}
		} finally {
			state = value;

			unlock();
		}
	}

	/**
	 * 队列是否为空。
	 * Whether the queue is empty.
	 *
	 * @return 若 empty 则为 true / true if empty
	 */
	protected abstract boolean isEmpty();

	/**
	 * 取出并执行队首任务。
	 * Remove and execute the first queued item.
	 */
	protected abstract void removeAndExecuteFirst();
}
