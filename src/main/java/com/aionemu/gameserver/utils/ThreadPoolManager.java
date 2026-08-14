package com.aionemu.gameserver.utils;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.network.util.ThreadUncaughtExceptionHandler;
import com.aionemu.commons.utils.concurrent.AionRejectedExecutionHandler;
import com.aionemu.commons.utils.concurrent.PriorityThreadFactory;
import com.aionemu.commons.utils.concurrent.RunnableWrapper;
import java.util.concurrent.ForkJoinPool;
import com.aionemu.gameserver.configs.main.ThreadConfig;

/**
 * 游戏服线程池管理器：调度、即时、长任务与工作窃取池的统一入口。
 * Game-server thread-pool manager: unified entry for scheduled, instant, long-running, and work-stealing pools.
 */
@Slf4j
public final class ThreadPoolManager {

	/** 无警告的最大运行时长（毫秒，常量备份） / Max runtime without warning (ms, constant backup) */
	public static final long MAXIMUM_RUNTIME_IN_MILLISEC_WITHOUT_WARNING = 5000;

	/** 可调度延迟上限（毫秒） / Maximum schedulable delay in milliseconds */
	private static final long MAX_DELAY = TimeUnit.NANOSECONDS.toMillis(Long.MAX_VALUE - System.nanoTime()) / 2;

	/** 长任务队列容量 / Long-running queue capacity */
	private static final int LONG_RUNNING_QUEUE_CAPACITY = 100000;

	/** 可选 Spring 实例提供者 / Optional Spring instance provider */
	private static volatile ObjectProvider<ThreadPoolManager> instanceProvider;

	/** 定时任务池 / Scheduled task pool */
	private final ScheduledThreadPoolExecutor scheduledPool;

	/** 即时任务池 / Instant task pool */
	private final ThreadPoolExecutor instantPool;

	/** 长时任务池 / Long-running task pool */
	private final ThreadPoolExecutor longRunningPool;

	/** 工作窃取（ForkJoin）线程池 / Work-stealing (ForkJoin) pool */
	private final ForkJoinPool workStealingPool;

	/**
	 * 初始化各线程池并启动周期性 purge。
	 * Initialize all pools and start periodic purge.
	 */
	public ThreadPoolManager() {
		final int instantPoolSize = instantPoolSize(ThreadConfig.THREAD_POOL_SIZE);
		instantPool = new ThreadPoolExecutor(instantPoolSize, instantPoolSize, 0, TimeUnit.SECONDS,
				new ArrayBlockingQueue<Runnable>(100000),
				new PriorityThreadFactory("InstantPool", ThreadConfig.USE_PRIORITIES ? 7 : Thread.NORM_PRIORITY));
		instantPool.setRejectedExecutionHandler(new AionRejectedExecutionHandler());
		instantPool.prestartAllCoreThreads();
		scheduledPool = new ScheduledThreadPoolExecutor(
				Math.max(1, ThreadConfig.EXTRA_THREAD_PER_CORE) * Runtime.getRuntime().availableProcessors());
		scheduledPool.setRejectedExecutionHandler(new AionRejectedExecutionHandler());
		scheduledPool.prestartAllCoreThreads();
		int longRunningPoolSize = longRunningPoolSize();
		longRunningPool = new ThreadPoolExecutor(longRunningPoolSize, longRunningPoolSize, 0, TimeUnit.SECONDS,
				new ArrayBlockingQueue<Runnable>(LONG_RUNNING_QUEUE_CAPACITY),
				new PriorityThreadFactory("LongRunningPool", Thread.NORM_PRIORITY));
		longRunningPool.setRejectedExecutionHandler(new AionRejectedExecutionHandler());
		longRunningPool.prestartAllCoreThreads();
		WorkStealThreadFactory forkJoinThreadFactory = new WorkStealThreadFactory("ForkJoinPool");
		workStealingPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors(), forkJoinThreadFactory,
				new ThreadUncaughtExceptionHandler(), true);
		forkJoinThreadFactory.setDefaultPool(workStealingPool);
		scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				purge();
			}
		}, 1000000, 1000000);
	}

	static int instantPoolSize(int configuredPoolSize) {
		return Math.max(1, configuredPoolSize);
	}

	/**
	 * 将延迟钳制到 [0, MAX_DELAY]。
	 * Clamp delay into [0, MAX_DELAY].
	 *
	 * @param delay 原始延迟（毫秒） / Raw delay in milliseconds
	 * @return 校验后的延迟 / Validated delay
	 */
	private long validate(long delay) {
		return Math.max(0, Math.min(MAX_DELAY, delay));
	}

	/**
	 * 计算长时任务池大小（至少 2，默认 CPU 核数）。
	 * Compute long-running pool size (at least 2, default = CPU count).
	 *
	 * @return 池大小 / Pool size
	 */
	private int longRunningPoolSize() {
		return Math.max(2, Runtime.getRuntime().availableProcessors());
	}

	/**
	 * 使用线程配置超时阈值的 Runnable 包装器。
	 * Runnable wrapper using thread-config warning threshold.
	 */
	private static final class ThreadPoolRunnableWrapper extends RunnableWrapper {
		private ThreadPoolRunnableWrapper(Runnable runnable) {
			super(runnable, ThreadConfig.MAXIMUM_RUNTIME_IN_MILLISEC_WITHOUT_WARNING);
		}
	}

	/**
	 * 延迟执行一次任务。
	 * Schedule a one-shot delayed task.
	 *
	 * @param r 任务 / Task
	 * @param delay 延迟毫秒 / Delay in milliseconds
	 * @return 调度的 future / Scheduled future
	 */
	public final ScheduledFuture<?> schedule(Runnable r, long delay) {
		r = new ThreadPoolRunnableWrapper(r);
		delay = validate(delay);
		return scheduledPool.schedule(r, delay, TimeUnit.MILLISECONDS);
	}

	/**
	 * 以固定频率周期性执行任务。
	 * Schedule a fixed-rate periodic task.
	 *
	 * @param r 任务 / Task
	 * @param delay 首次延迟毫秒 / Initial delay in milliseconds
	 * 周期（毫秒） / Period in milliseconds
	 * @param period 调度的 future / Scheduled future
	 */
	public final ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long delay, long period) {
		r = new ThreadPoolRunnableWrapper(r);
		delay = validate(delay);
		period = validate(period);
		return scheduledPool.scheduleAtFixedRate(r, delay, period, TimeUnit.MILLISECONDS);
	}

	/**
	 * 获取工作窃取（ForkJoin）池。
	 * Get the work-stealing (ForkJoin) pool.
	 *
	 * @return ForkJoin 池 / ForkJoin pool
	 */
	public ForkJoinPool getForkingPool() {
		return workStealingPool;
	}

	/**
	 * 在即时池中执行任务。
	 * Execute a task on the instant pool.
	 *
	 * @param r 任务 / Task
	 */
	public final void execute(Runnable r) {
		r = new ThreadPoolRunnableWrapper(r);
		instantPool.execute(r);
	}

	/**
	 * 在长时任务池中执行任务。
	 * Execute a task on the long-running pool.
	 *
	 * @param r 任务 / Task
	 */
	public final void executeLongRunning(Runnable r) {
		r = new RunnableWrapper(r);
		longRunningPool.execute(r);
	}

	/**
	 * 向即时池提交任务并返回 Future。
	 * Submit a task to the instant pool and return a Future.
	 *
	 * @param r 任务 / Task
	 * @return Future 句柄 / Future handle
	 */
	public final Future<?> submit(Runnable r) {
		r = new ThreadPoolRunnableWrapper(r);
		return instantPool.submit(r);
	}

	/**
	 * 向长时任务池提交任务并返回 Future。
	 * Submit a task to the long-running pool and return a Future.
	 *
	 * @param r 任务 / Task
	 * @return Future 句柄 / Future handle
	 */
	public final Future<?> submitLongRunning(Runnable r) {
		r = new RunnableWrapper(r);
		return longRunningPool.submit(r);
	}

	/**
	 * 执行登录服相关数据包任务（委托即时池）。
	 * Execute a login-server packet task (delegates to the instant pool).
	 *
	 * @param pkt 数据包任务 / Packet task
	 */
	public void executeLsPacket(Runnable pkt) {
		execute(pkt);
	}

	/**
	 * 清理各线程池已取消任务。
	 * Purge cancelled tasks from all pools.
	 */
	public void purge() {
		scheduledPool.purge();
		instantPool.purge();
		longRunningPool.purge();
	}

	/**
	 * 优雅关闭全部线程池并记录队列状态。
	 * Gracefully shut down all pools and log queue state.
	 */
	public void shutdown() {
		final long begin = System.currentTimeMillis();
		log.info(I18n.get("log.8a50f53595f0"));
		log.info(I18n.get("log.e0894695d98b", getTaskCount(scheduledPool)));
		log.info(I18n.get("log.6aca868692a9", getTaskCount(instantPool)));
		log.info(I18n.get("log.f6e5ab713d99", getTaskCount(longRunningPool)));
		log.info(I18n.get("log.ab2806542e7c", (workStealingPool.getQueuedTaskCount() + workStealingPool.getQueuedSubmissionCount())));
		scheduledPool.shutdown();
		instantPool.shutdown();
		longRunningPool.shutdown();
		workStealingPool.shutdown();
		boolean success = false;
		try {
			success |= awaitTermination(5000);
			scheduledPool.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
			scheduledPool.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
			success |= awaitTermination(10000);
		} catch (InterruptedException e) {
			log.warn(I18n.get("log.8f2ed10ffefe", e));
			Thread.currentThread().interrupt();
		}
		log.info(I18n.get("log.e2793575e244", success, (System.currentTimeMillis() - begin)));
		log.info(I18n.get("log.a4e82809c60b", getTaskCount(scheduledPool)));
		log.info(I18n.get("log.5e21f4d731c1", getTaskCount(instantPool)));
		log.info(I18n.get("log.63d1ee00d183", getTaskCount(longRunningPool)));
		log.info(I18n.get("log.ab2806542e7c", (workStealingPool.getQueuedTaskCount() + workStealingPool.getQueuedSubmissionCount())));
		workStealingPool.shutdownNow();
	}

	/**
	 * 统计线程池队列长度与活跃线程数之和。
	 * Sum of queue size and active count for a pool.
	 *
	 * @param tp 线程池 / Thread pool
	 * @return 任务数 / Task count
	 */
	private int getTaskCount(ThreadPoolExecutor tp) {
		return tp.getQueue().size() + tp.getActiveCount();
	}

	/**
	 * 收集各线程池的运行时统计行。
	 * Collect runtime statistics lines for all pools.
	 *
	 * @return 统计文本行列表 / List of stats lines
	 */
	public List<String> getStats() {
		List<String> list = new ArrayList<String>();
		list.add("");
		list.add("Scheduled pool:");
		list.add("=================================================");
		list.add("\tgetActiveCount: ...... " + scheduledPool.getActiveCount());
		list.add("\tgetCorePoolSize: ..... " + scheduledPool.getCorePoolSize());
		list.add("\tgetPoolSize: ......... " + scheduledPool.getPoolSize());
		list.add("\tgetLargestPoolSize: .. " + scheduledPool.getLargestPoolSize());
		list.add("\tgetMaximumPoolSize: .. " + scheduledPool.getMaximumPoolSize());
		list.add("\tgetCompletedTaskCount: " + scheduledPool.getCompletedTaskCount());
		list.add("\tgetQueuedTaskCount: .. " + scheduledPool.getQueue().size());
		list.add("\tgetTaskCount: ........ " + scheduledPool.getTaskCount());
		list.add("");
		list.add("Instant pool:");
		list.add("=================================================");
		list.add("\tgetActiveCount: ...... " + instantPool.getActiveCount());
		list.add("\tgetCorePoolSize: ..... " + instantPool.getCorePoolSize());
		list.add("\tgetPoolSize: ......... " + instantPool.getPoolSize());
		list.add("\tgetLargestPoolSize: .. " + instantPool.getLargestPoolSize());
		list.add("\tgetMaximumPoolSize: .. " + instantPool.getMaximumPoolSize());
		list.add("\tgetCompletedTaskCount: " + instantPool.getCompletedTaskCount());
		list.add("\tgetQueuedTaskCount: .. " + instantPool.getQueue().size());
		list.add("\tgetTaskCount: ........ " + instantPool.getTaskCount());
		list.add("");
		list.add("Long running pool:");
		list.add("=================================================");
		list.add("\tgetActiveCount: ...... " + longRunningPool.getActiveCount());
		list.add("\tgetCorePoolSize: ..... " + longRunningPool.getCorePoolSize());
		list.add("\tgetPoolSize: ......... " + longRunningPool.getPoolSize());
		list.add("\tgetLargestPoolSize: .. " + longRunningPool.getLargestPoolSize());
		list.add("\tgetMaximumPoolSize: .. " + longRunningPool.getMaximumPoolSize());
		list.add("\tgetCompletedTaskCount: " + longRunningPool.getCompletedTaskCount());
		list.add("\tgetQueuedTaskCount: .. " + longRunningPool.getQueue().size());
		list.add("\tgetTaskCount: ........ " + longRunningPool.getTaskCount());
		list.add("");
		list.add("Work forking pool:");
		list.add("=================================================");
		list.add("\tgetActiveCount: ...... " + workStealingPool.getActiveThreadCount());
		list.add("\tgetPoolSize: ......... " + workStealingPool.getPoolSize());
		list.add("\tgetStealCount: ........" + workStealingPool.getStealCount());
		list.add("\tgetQueuedTaskCount: .. " + workStealingPool.getQueuedTaskCount());
		list.add("\tgetRunningThreadCount: " + workStealingPool.getRunningThreadCount());
		return list;
	}

	/**
	 * 在超时内轮询等待各池终止。
	 * Poll-wait for all pools to terminate within a timeout.
	 *
	 * @param timeoutInMillisec 毫秒超时 / Timeout in milliseconds
	 * @return 全部终止返回 true / True if all terminated
	 * @throws InterruptedException 等待被中断 / Wait interrupted
	 */
	private boolean awaitTermination(long timeoutInMillisec) throws InterruptedException {
		final long begin = System.currentTimeMillis();
		while (System.currentTimeMillis() - begin < timeoutInMillisec) {
			if (!scheduledPool.awaitTermination(10, TimeUnit.MILLISECONDS) && scheduledPool.getActiveCount() > 0) {
				continue;
			}
			if (!instantPool.awaitTermination(10, TimeUnit.MILLISECONDS) && instantPool.getActiveCount() > 0) {
				continue;
			}
			if (!workStealingPool.awaitTermination(10, TimeUnit.MILLISECONDS)
					&& workStealingPool.getActiveThreadCount() > 0) {
				continue;
			}
			if (!longRunningPool.awaitTermination(10, TimeUnit.MILLISECONDS) && longRunningPool.getActiveCount() > 0) {
				continue;
			}
			return true;
		}
		return false;
	}

	/**
	 * 单例持有者。
	 * Singleton holder.
	 */
	private static final class SingletonHolder {
		private static final ThreadPoolManager INSTANCE = new ThreadPoolManager();
	}

	/**
	 * 获取实例（优先 Spring 提供者，否则单例）。
	 * Get instance (prefer Spring provider, else singleton).
	 *
	 * @return ThreadPoolManager 实例 / ThreadPoolManager instance
	 */
	public static ThreadPoolManager getInstance() {
		ObjectProvider<ThreadPoolManager> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.INSTANCE;
		}
		return provider.getIfAvailable(() -> SingletonHolder.INSTANCE);
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Inject a Spring instance provider.
	 *
	 * @param instanceProvider Spring ObjectProvider / Spring ObjectProvider
	 */
	public static void setInstanceProvider(ObjectProvider<ThreadPoolManager> instanceProvider) {
		ThreadPoolManager.instanceProvider = instanceProvider;
	}
}
