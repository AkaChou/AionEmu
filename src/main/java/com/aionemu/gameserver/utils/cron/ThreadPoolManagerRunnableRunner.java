package com.aionemu.gameserver.utils.cron;

import com.aionemu.commons.services.cron.RunnableRunner;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

/**
 * 将 cron 任务委托到游戏线程池执行的 RunnableRunner。
 * RunnableRunner that dispatches cron tasks to the game thread pool.
 */
public class ThreadPoolManagerRunnableRunner extends RunnableRunner {

	/**
	 * 在普通线程池中执行任务。
	 * Execute a task on the standard thread pool.
	 *
	 * @param r 任务 / Runnable
	 */
	@Override
	public void executeRunnable(Runnable r) {
		GameThreadPoolServices.threadPoolManager().execute(r);
	}

	/**
	 * 在长任务线程池中执行任务。
	 * Execute a long-running task on the long-running pool.
	 *
	 * @param r 任务 / Runnable
	 */
	@Override
	public void executeLongRunningRunnable(Runnable r) {
		GameThreadPoolServices.threadPoolManager().executeLongRunning(r);
	}
}
