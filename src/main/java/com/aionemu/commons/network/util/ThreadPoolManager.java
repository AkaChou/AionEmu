/**
 * This file is part of Aion-Lightning <aion-lightning.org>.
 *
 * Aion-Lightning is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Aion-Lightning is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. *
 *
 * You should have received a copy of the GNU General Public License along with Aion-Lightning. If not, see <http://www.gnu.org/licenses/>.
 *
 * Credits goes to all Open Source Core Developer Groups listed below Please do not change here something, ragarding the developer credits, except the
 * "developed by XXXX". Even if you edit a lot of files in this source, you still have no rights to call it as "your Core". Everybody knows that this
 * Emulator Core was developed by Aion Lightning
 * 
 * @-Aion-Unique-
 * @-Aion-Lightning
 * @Aion-Engine
 * @Aion-Extreme
 * @Aion-NextGen
 * @Aion-Core Dev.
 */
package com.aionemu.commons.network.util;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.utils.concurrent.PriorityThreadFactory;
import com.aionemu.commons.utils.concurrent.RunnableWrapper;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * 线程池管理器，负责管理和调度系统中的线程池
 * Thread Pool Manager responsible for managing and scheduling thread pools in the system
 *
 * 该类实现了单例模式，提供以下功能:
 * 1. 管理定时任务线程池
 * 2. 管理游戏服务器数据包处理线程池
 * 3. 提供任务调度和执行接口
 * 4. 集成死锁检测
 *
 * This class implements Singleton pattern and provides:
 * 1. Scheduled task thread pool management
 * 2. Game server packet processing thread pool management 
 * 3. Task scheduling and execution interface
 * 4. Integrated deadlock detection
 *
 * @author -Nemesiss-, Rolandas
 */
public class ThreadPoolManager implements Executor {
    
    /**
     * 单例持有者
     * Singleton holder
     */
    private static class SingletonHolder {
        protected static final ThreadPoolManager instance = new ThreadPoolManager();
    }
    
    private static final Logger log = LoggerFactory.getLogger(ThreadPoolManager.class);
    
    /**
     * 获取ThreadPoolManager实例
     * Get ThreadPoolManager instance
     *
     * @return ThreadPoolManager实例 / ThreadPoolManager instance
     */
    public static final ThreadPoolManager getInstance() {
        return SingletonHolder.instance;
    }
    
    /**
     * 定时任务线程池执行器
     * Scheduled task thread pool executor
     */
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private ListeningScheduledExecutorService scheduledThreadPool;
    
    /**
     * 游戏服务器数据包处理线程池执行器
     * Game server packet processing thread pool executor
     */
    private final ThreadPoolExecutor generalPacketsThreadPoolExecutor;
    private final ListeningExecutorService generalPacketsThreadPool;
    
    /**
     * 构造函数，初始化线程池和死锁检测器
     * Constructor, initialize thread pools and deadlock detector
     */
    private ThreadPoolManager() {
        new DeadLockDetector(60, DeadLockDetector.RESTART).start();
        
        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(4, new PriorityThreadFactory("ScheduledThreadPool", Thread.NORM_PRIORITY));
        scheduledThreadPool = MoreExecutors.listeningDecorator(scheduledThreadPoolExecutor);
        
        generalPacketsThreadPoolExecutor = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
        generalPacketsThreadPool = MoreExecutors.listeningDecorator(generalPacketsThreadPoolExecutor);
    }
    
    /**
     * 执行游戏服务器客户端数据包任务
     * Execute game server client packet task
     *
     * @param pkt 要执行的任务 / Task to execute
     */
    @Override
    public void execute(final Runnable pkt) {
        generalPacketsThreadPool.execute(new RunnableWrapper(pkt));
    }
    
    /**
     * 获取数据包处理线程池
     * Get packet processing thread pool
     *
     * @return 数据包处理线程池 / Packet processing thread pool
     */
    public ListeningExecutorService getPacketsThreadPool() {
        return generalPacketsThreadPool;
    }
    
    /**
     * 调度任务在指定延迟后执行
     * Schedule task to execute after specified delay
     *
     * @param <T> 任务类型 / Task type
     * @param r 要执行的任务 / Task to execute
     * @param delay 延迟时间(毫秒) / Delay time in milliseconds
     * @return 可监听的Future / Listenable future
     */
    @SuppressWarnings("unchecked")
    public <T extends Runnable> ListenableFuture<T> schedule(final T r, long delay) {
        try {
            if (delay < 0) {
                delay = 0;
            }
            return (ListenableFuture<T>) JdkFutureAdapters.listenInPoolThread(scheduledThreadPool.schedule(r, delay, TimeUnit.MILLISECONDS));
        } catch (RejectedExecutionException e) {
            return null; /* shutdown, ignore */
        }
    }
    
    /**
     * 调度任务以固定速率执行
     * Schedule task to execute at fixed rate
     *
     * @param <T> 任务类型 / Task type
     * @param r 要执行的任务 / Task to execute
     * @param initial 初始延迟(毫秒) / Initial delay in milliseconds
     * @param delay 执行间隔(毫秒) / Execution interval in milliseconds
     * @return 可监听的Future / Listenable future
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
            return (ListenableFuture<T>) JdkFutureAdapters.listenInPoolThread(scheduledThreadPool.scheduleAtFixedRate(r, initial, delay, TimeUnit.MILLISECONDS));
        } catch (RejectedExecutionException e) {
            return null;
        }
    }
    
    /**
     * 关闭所有线程池
     * Shutdown all thread pools
     */
    public void shutdown() {
        try {
            scheduledThreadPool.shutdown();
            generalPacketsThreadPool.shutdown();
            scheduledThreadPool.awaitTermination(2, TimeUnit.SECONDS);
            generalPacketsThreadPool.awaitTermination(2, TimeUnit.SECONDS);
            log.info("All ThreadPools are now stopped.");
        } catch (InterruptedException e) {
            log.error("Can't shutdown ThreadPoolManager", e);
        }
    }
}
