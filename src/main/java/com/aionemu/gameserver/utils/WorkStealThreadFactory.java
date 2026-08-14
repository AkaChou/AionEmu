package com.aionemu.gameserver.utils;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

import com.aionemu.commons.utils.concurrent.PriorityThreadFactory;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;

/**
 * 支持工作窃取（ForkJoin）的优先级线程工厂。
 * Priority thread factory that produces work-stealing (ForkJoin) worker threads.
 */
public class WorkStealThreadFactory extends PriorityThreadFactory implements ForkJoinWorkerThreadFactory {

	/**
	 * 使用给定名称前缀与普通优先级创建工厂。
	 * Creates a factory with the given name prefix and normal priority.
	 *
	 * @param namePrefix 线程名前缀 / Thread name prefix
	 */
	public WorkStealThreadFactory(String namePrefix) {
		super(namePrefix, Thread.NORM_PRIORITY);
	}

	/**
	 * 设置默认 ForkJoin 池；若为 null 则使用公共池。
	 * Sets the default ForkJoin pool; uses the common pool when null.
	 *
	 * @param pool ForkJoin 池 / ForkJoin pool
	 */
	public void setDefaultPool(ForkJoinPool pool) {
		if (pool == null) {
			pool = ForkJoinPool.commonPool();
		}
		super.setDefaultPool(pool);
	}

	/**
	 * 获取默认 ForkJoin 池。
	 * Returns the default ForkJoin pool.
	 *
	 * @return 默认池 / Default pool
	 */
	@Override
	public ForkJoinPool getDefaultPool() {
		return (ForkJoinPool) super.getDefaultPool();
	}

	/**
	 * 为指定池创建工作窃取线程。
	 * Creates a work-stealing worker thread for the given pool.
	 *
	 * @param pool ForkJoin 池 / ForkJoin pool
	 * @return 工作线程 / Worker thread
	 */
	@Override
	public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
		return new WorkStealThread(pool);
	}

	/**
	 * 工作窃取线程，终止时记录异常。
	 * Work-stealing thread that logs exceptions on termination.
	 */
	@Slf4j
	private static class WorkStealThread extends ForkJoinWorkerThread {

		/**
		 * 绑定到指定池创建工作线程。
		 * Creates a worker thread bound to the given pool.
		 *
		 * @param pool ForkJoin 池 / ForkJoin pool
		 */
		public WorkStealThread(ForkJoinPool pool) {
			super(pool);
		}

		/**
		 * 线程启动钩子。
		 * Thread start hook.
		 */
		@Override
		protected void onStart() {
			super.onStart();
		}

		/**
		 * 线程终止钩子；若有异常则记录。
		 * Thread termination hook; logs any terminating exception.
		 *
		 * @param exception 终止异常，可为 null / Termination exception, may be null
		 */
		@Override
		protected void onTermination(Throwable exception) {
			if (exception != null) {
				log.error(I18n.get("log.07f358910e11", this.getName(), exception));
			}
			super.onTermination(exception);
		}
	}
}
