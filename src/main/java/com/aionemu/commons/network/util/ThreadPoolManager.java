package com.aionemu.commons.network.util;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.commons.utils.concurrent.AionRejectedExecutionHandler;
import com.aionemu.commons.utils.concurrent.PriorityThreadFactory;
import com.aionemu.commons.utils.concurrent.RunnableWrapper;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * 网络相关线程池管理器：定时任务、数据包池与死锁检测。
 * Network thread-pool manager for scheduled tasks, packet pool, and deadlock detection.
 *
 * @author -Nemesiss-, Rolandas
 */
@Slf4j
public class ThreadPoolManager implements Executor {
    private static final int PACKET_QUEUE_CAPACITY = 100000;

    /**
     * 单例持有者。
     * Singleton holder.
     */
    private static class SingletonHolder {
        protected static final ThreadPoolManager instance = new ThreadPoolManager();
    }

    /**
     * 获取单例实例。
     * Get singleton instance.
     *
     * @return 线程池管理器 / Thread pool manager
     */
    public static final ThreadPoolManager getInstance() {
        return SingletonHolder.instance;
    }

    /**
     * 定时任务线程池执行器。
     * Scheduled task thread-pool executor.
     */
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private ListeningScheduledExecutorService scheduledThreadPool;

    /**
     * 数据包处理线程池执行器。
     * Packet processing thread-pool executor.
     */
    private final ThreadPoolExecutor generalPacketsThreadPoolExecutor;
    private final ListeningExecutorService generalPacketsThreadPool;

    /**
     * 初始化线程池并启动死锁检测。
     * Initialize thread pools and start deadlock detection.
     */
    public ThreadPoolManager() {
        DeadLockDetector deadLockDetector = new DeadLockDetector(
            60,
            AionRuntimeMode.isBootEmbedded() ? DeadLockDetector.NOTHING : DeadLockDetector.RESTART
        );
        deadLockDetector.setDaemon(AionRuntimeMode.isBootEmbedded());
        deadLockDetector.start();

        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(4, new PriorityThreadFactory("ScheduledThreadPool", Thread.NORM_PRIORITY));
        scheduledThreadPool = MoreExecutors.listeningDecorator(scheduledThreadPoolExecutor);

        int packetPoolSize = packetPoolSize();
        generalPacketsThreadPoolExecutor = new ThreadPoolExecutor(packetPoolSize, packetPoolSize, 0L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(PACKET_QUEUE_CAPACITY),
            new PriorityThreadFactory("PacketPool", Thread.NORM_PRIORITY));
        generalPacketsThreadPoolExecutor.setRejectedExecutionHandler(new AionRejectedExecutionHandler());
        generalPacketsThreadPoolExecutor.prestartAllCoreThreads();
        generalPacketsThreadPool = MoreExecutors.listeningDecorator(generalPacketsThreadPoolExecutor);
    }

    /**
     * 计算数据包池大小。
     * Compute packet pool size.
     *
     * Pool size
     */
    private int packetPoolSize() {
        return Math.max(2, Runtime.getRuntime().availableProcessors() * 2);
    }

    /**
     * 执行数据包任务。
     * Execute packet task.
     *
     * Task
     */
    @Override
    public void execute(final Runnable pkt) {
        generalPacketsThreadPool.execute(new RunnableWrapper(pkt));
    }

    /**
     * 获取数据包线程池。
     * Get packet thread pool.
     *
     * @return 可监听执行器 / Listening executor
     */
    public ListeningExecutorService getPacketsThreadPool() {
        return generalPacketsThreadPool;
    }

    /**
     * 延迟调度任务。
     * Schedule task after delay.
     *
     * @param <T> 任务类型 / Task type
     * @param r 任务 / Task
     * @param delay 延迟毫秒 / Delay in milliseconds
     * @return 可监听 Future，关闭时可能为 null / Listenable future, may be null when shutting down
     */
    @SuppressWarnings("unchecked")
    public <T extends Runnable> ListenableFuture<T> schedule(final T r, long delay) {
        try {
            if (delay < 0) {
                delay = 0;
            }
            Runnable wrapped = new RunnableWrapper(r);
            return (ListenableFuture<T>) JdkFutureAdapters.listenInPoolThread(scheduledThreadPool.schedule(wrapped, delay, TimeUnit.MILLISECONDS));
        } catch (RejectedExecutionException e) {
            return null; /* shutdown, ignore */
        }
    }

    /**
     * 固定频率调度任务。
     * Schedule task at fixed rate.
     *
     * @param <T> 任务类型 / Task type
     * @param r 任务 / Task
     * @param initial 初始延迟毫秒 / Initial delay in milliseconds
     * @param delay 周期毫秒 / Period in milliseconds
     * @return 可监听 Future，关闭时可能为 null / Listenable future, may be null when shutting down
     */
    @SuppressWarnings("unchecked")
    public <T extends Runnable> ListenableFuture<T> scheduleAtFixedRate(final T r, long initial, long delay) {
        try {
            if (delay < 0) {
                delay = 0;
            }
            if (initial < 0) {
                initial = 0;
            }
            Runnable wrapped = new RunnableWrapper(r);
            return (ListenableFuture<T>) JdkFutureAdapters.listenInPoolThread(scheduledThreadPool.scheduleAtFixedRate(wrapped, initial, delay, TimeUnit.MILLISECONDS));
        } catch (RejectedExecutionException e) {
            return null;
        }
    }

    /**
     * 关闭全部线程池。
     * Shutdown all thread pools.
     */
    public void shutdown() {
        try {
            scheduledThreadPool.shutdown();
            generalPacketsThreadPool.shutdown();
            scheduledThreadPool.awaitTermination(2, TimeUnit.SECONDS);
            generalPacketsThreadPool.awaitTermination(2, TimeUnit.SECONDS);
            log.info(I18n.get("log.4ed0b19373c7"));
        } catch (InterruptedException e) {
            log.error(I18n.get("log.6fb05b263d35", e));
            Thread.currentThread().interrupt();
        }
    }
}
