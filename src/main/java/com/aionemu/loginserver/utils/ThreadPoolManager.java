package com.aionemu.loginserver.utils;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.concurrent.AionRejectedExecutionHandler;
import com.aionemu.commons.utils.concurrent.PriorityThreadFactory;
import com.aionemu.commons.utils.concurrent.RunnableWrapper;
import com.aionemu.commons.utils.concurrent.ScheduledFutureWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 登录服线程池管理器：提供调度、瞬时与长任务三类执行池。
 * Login-server thread-pool manager: provides scheduled, instant and long-running executor pools.
 * @author -Nemesiss-, NB4L1, MrPoke, lord_rex
 */
@Slf4j
@Component("loginThreadPoolManager")
@Lazy
public class ThreadPoolManager {

    /**
     * 任务运行超过该毫秒数时发出警告。
     * Warn when a runnable runs longer than this many milliseconds.
     */
    public static final long MAXIMUM_RUNTIME_IN_MILLISEC_WITHOUT_WARNING = 5000;
    private static final long MAX_DELAY = TimeUnit.NANOSECONDS.toMillis(Long.MAX_VALUE - System.nanoTime()) / 2;
    private static final int LONG_RUNNING_QUEUE_CAPACITY = 100000;
    private final ScheduledThreadPoolExecutor scheduledPool;
    private final ThreadPoolExecutor instantPool;
    private final ThreadPoolExecutor longRunningPool;
    private final AtomicBoolean shutdownStarted = new AtomicBoolean();

    /**
     * 按 CPU 核数初始化三类线程池，并启动定期 purge。
     * Initializes the three pools based on CPU count and starts periodic purge.
     */
    public ThreadPoolManager() {

        int threadpoolsize = 2 + Runtime.getRuntime().availableProcessors() * 4;
        final int instantPoolSize = Math.max(1, threadpoolsize / 3);

        scheduledPool = new ScheduledThreadPoolExecutor(threadpoolsize - instantPoolSize);
        scheduledPool.setRejectedExecutionHandler(new AionRejectedExecutionHandler());
        scheduledPool.prestartAllCoreThreads();

        instantPool = new ThreadPoolExecutor(instantPoolSize, instantPoolSize, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100000));
        instantPool.setRejectedExecutionHandler(new AionRejectedExecutionHandler());
        instantPool.prestartAllCoreThreads();

        int longRunningPoolSize = longRunningPoolSize();
        longRunningPool = new ThreadPoolExecutor(longRunningPoolSize, longRunningPoolSize, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(LONG_RUNNING_QUEUE_CAPACITY),
                new PriorityThreadFactory("LongRunningPool", Thread.NORM_PRIORITY));
        longRunningPool.setRejectedExecutionHandler(new AionRejectedExecutionHandler());
        longRunningPool.prestartAllCoreThreads();

        scheduleAtFixedRate(this::purge, 150000, 150000);

        log.info(I18n.get("log.17977a3c1da1", scheduledPool.getPoolSize(), instantPool.getPoolSize(), longRunningPool.getPoolSize()));
    }

    private long validate(long delay) {
        return Math.clamp(delay, 0, MAX_DELAY);
    }

    private int longRunningPoolSize() {
        return Math.max(2, Runtime.getRuntime().availableProcessors());
    }

    private static final class ThreadPoolRunnableWrapper extends RunnableWrapper {

        private ThreadPoolRunnableWrapper(Runnable runnable) {
            super(runnable, MAXIMUM_RUNTIME_IN_MILLISEC_WITHOUT_WARNING);
        }
    }

    /**
     * 延迟调度任务。
     * Schedules a runnable after the given delay.
     * @param r 任务 / runnable
     * @param delay 延迟毫秒 / delay in milliseconds
     * @return 可取消的 Future / cancellable future
     */
    public ScheduledFuture<?> schedule(Runnable r, long delay) {
        r = new ThreadPoolRunnableWrapper(r);
        delay = validate(delay);

        return new ScheduledFutureWrapper(scheduledPool.schedule(r, delay, TimeUnit.MILLISECONDS));
    }

    /**
     * 延迟调度效果类任务（等同 schedule）。
     * Schedules an effect-style task (delegates to schedule).
     * @param r 任务 / runnable
     * @param delay 延迟毫秒 / delay in milliseconds
     * @return 可取消的 Future / cancellable future
     */
    public ScheduledFuture<?> scheduleEffect(Runnable r, long delay) {
        return schedule(r, delay);
    }


    /**
     * 固定频率周期调度。
     * Schedules a runnable at a fixed rate.
     * @param r 任务 / runnable
     * @param delay 初始延迟毫秒 / initial delay in milliseconds
     * @param period 周期（毫秒） / period in milliseconds
     * @return 可取消的 Future / cancellable future
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long delay, long period) {
        r = new ThreadPoolRunnableWrapper(r);
        delay = validate(delay);
        period = validate(period);

        return new ScheduledFutureWrapper(scheduledPool.scheduleAtFixedRate(r, delay, period, TimeUnit.MILLISECONDS));
    }

    /**
     * 固定频率调度效果类任务（等同 scheduleAtFixedRate）。
     * Schedules an effect-style task at a fixed rate (delegates to scheduleAtFixedRate).
     * @param r 任务 / runnable
     * @param delay 初始延迟毫秒 / initial delay in milliseconds
     * @param period 周期（毫秒） / period in milliseconds
     * @return 可取消的 Future / cancellable future
     */
    public ScheduledFuture<?> scheduleEffectAtFixedRate(Runnable r, long delay, long period) {
        return scheduleAtFixedRate(r, delay, period);
    }

    /**
     * 在瞬时线程池中执行任务。
     * Executes a runnable on the instant pool.
     * @param r 任务 / runnable
     */
    public void execute(Runnable r) {
        r = new ThreadPoolRunnableWrapper(r);

        instantPool.execute(r);
    }

    /**
     * 执行普通任务（等同 execute）。
     * Executes a general task (delegates to execute).
     * @param r 任务 / runnable
     */
    public void executeTask(Runnable r) {
        execute(r);
    }

    /**
     * 在长任务线程池中执行。
     * Executes a runnable on the long-running pool.
     * @param r 任务 / runnable
     */
    public void executeLongRunning(Runnable r) {
        r = new RunnableWrapper(r);

        longRunningPool.execute(r);
    }

    /**
     * 在长任务线程池中启动已有心跳循环（与游戏服同名入口同一语义：不改变慢任务告警阈值）。
     * Starts an existing heartbeat loop on the long-running pool (same semantics as the game-server entry:
     * the slow-task warning threshold is left alone).
     * @param heartbeat 已有心跳循环 / existing heartbeat loop
     */
    public void executeHeartbeat(Runnable heartbeat) {
        if (shutdownStarted.get()) {
            return;
        }
        longRunningPool.execute(heartbeat);
    }

    /**
     * 提交任务到瞬时线程池。
     * Submits a runnable to the instant pool.
     * @param r 任务 / runnable
     * @return 任务 Future / task future
     */
    public Future<?> submit(Runnable r) {
        r = new ThreadPoolRunnableWrapper(r);

        return instantPool.submit(r);
    }

    /**
     * 提交任务到长任务线程池。
     * Submits a runnable to the long-running pool.
     * @param r 任务 / runnable
     * @return 任务 Future / task future
     */
    public Future<?> submitLongRunning(Runnable r) {
        r = new RunnableWrapper(r);
        return longRunningPool.submit(r);
    }

    /**
     * 执行登录服数据包任务。
     * Executes a login-server packet task.
     * @param pkt 可运行的数据包任务 / runnable packet for Login Server
     */
    public void executeLsPacket(Runnable pkt) {
        execute(pkt);
    }

    /**
     * TaskManager 使用的延迟调度入口。
     * TaskManager delay scheduler entry point.
     * @param r 可运行任务 / runnable task
     * @param delay 执行前等待毫秒 / wait before task execution
     * @return 已调度任务 / scheduled task
     */
    public ScheduledFuture<?> scheduleTaskManager(Runnable r, long delay) {
        return schedule(r, delay);
    }

    /**
     * 清理各线程池中已取消的任务。
     * Purges cancelled tasks from all pools.
     */
    public void purge() {
        scheduledPool.purge();
        instantPool.purge();
        longRunningPool.purge();
    }

    /**
     * 关闭全部线程池并等待终止。
     * Shutdown all thread pools and wait for termination.
     */
    public void shutdown() {
        if (!shutdownStarted.compareAndSet(false, true)) {
            return;
        }
        long begin = System.nanoTime();
        log.debug(I18n.get("threadpool.shutdown.before", "login", getTaskCount(scheduledPool),
                getTaskCount(instantPool), getTaskCount(longRunningPool), 0));
        boolean success = shutdownPool(scheduledPool, "scheduled");
        success &= shutdownPool(instantPool, "instant");
        success &= shutdownPool(longRunningPool, "long-running");
        boolean terminated = false;
        try {
            awaitTermination(5000);
            scheduledPool.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
            scheduledPool.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
            terminated = awaitTermination(10000);
        } catch (InterruptedException e) {
            log.warn(I18n.get("log.8f2ed10ffefe"), e);
            Thread.currentThread().interrupt();
        } finally {
            scheduledPool.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
            scheduledPool.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        }
        int scheduled = getTaskCount(scheduledPool);
        int instant = getTaskCount(instantPool);
        int longRunning = getTaskCount(longRunningPool);
        long fork = 0;
        success &= terminated && scheduled == 0 && instant == 0 && longRunning == 0;
        success &= scheduledPool.isTerminated() && instantPool.isTerminated() && longRunningPool.isTerminated();
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin);
        if (success) {
            log.info(I18n.get("threadpool.shutdown.result", "login", true, elapsed, scheduled, instant,
                    longRunning, fork));
        } else {
            log.warn(I18n.get("threadpool.shutdown.result", "login", false, elapsed, scheduled, instant,
                    longRunning, fork));
        }
    }

    /**
     * 关闭单个池；保留失败原因并继续关闭其他池。
     * Shuts down one pool, retaining the failure cause while allowing other pools to close.
     */
    private boolean shutdownPool(ExecutorService pool, String name) {
        try {
            pool.shutdown();
            return true;
        } catch (RuntimeException e) {
            log.error(I18n.get("threadpool.shutdown.pool_failed", "login", name), e);
            return false;
        }
    }

    private int getTaskCount(ThreadPoolExecutor tp) {
        return tp.getQueue().size() + tp.getActiveCount();
    }

    /**
     * 收集三类线程池的运行时统计信息。
     * Collects runtime statistics for all three pools.
     * @return 可读统计行列表 / human-readable stats lines
     */
    public List<String> getStats() {
        List<String> list = new ArrayList<>();

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

        return list;
    }

    private boolean awaitTermination(long timeoutInMillisec) throws InterruptedException {
        final long begin = System.currentTimeMillis();

        while (System.currentTimeMillis() - begin < timeoutInMillisec) {
            if (!scheduledPool.awaitTermination(10, TimeUnit.MILLISECONDS) && scheduledPool.getActiveCount() > 0) {
                continue;
            }

            if (!instantPool.awaitTermination(10, TimeUnit.MILLISECONDS) && instantPool.getActiveCount() > 0) {
                continue;
            }

            if (!longRunningPool.awaitTermination(10, TimeUnit.MILLISECONDS) && longRunningPool.getActiveCount() > 0) {
                continue;
            }

            return true;
        }

        return false;
    }

}
