package com.aionemu.commons.utils.concurrent;

import com.aionemu.commons.network.util.ThreadUncaughtExceptionHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 优先级线程工厂类，用于创建具有指定优先级的线程
 * Priority thread factory for creating threads with specified priority level
 */
public class PriorityThreadFactory implements ThreadFactory {
    // 线程优先级 Thread priority level
    private int prio;
    // 线程名称前缀 Thread name prefix
    private String name;
    // 线程池引用 Reference to thread pool
    private ExecutorService threadPool;
    // 线程计数器 Thread counter
    private AtomicInteger threadNumber;
    // 线程组 Thread group
    private ThreadGroup group;

    /**
     * 创建具有指定名称和优先级的线程工厂
     * Create a thread factory with specified name and priority
     *
     * @param name 线程名称前缀 Thread name prefix
     * @param prio 线程优先级 Thread priority level
     */
    public PriorityThreadFactory(String name, int prio) {
        this.threadNumber = new AtomicInteger(1);
        this.prio = prio;
        this.name = name;
        this.group = new ThreadGroup(this.name);
    }

    /**
     * 创建具有指定名称和默认优先级(5)的线程工厂
     * Create a thread factory with specified name and default priority(5)
     *
     * @param name 线程名称前缀 Thread name prefix
     * @param defaultPool 默认线程池 Default thread pool
     */
    public PriorityThreadFactory(String name, ExecutorService defaultPool) {
        this(name, 5);
        this.setDefaultPool(defaultPool);
    }

    /**
     * 设置默认线程池
     * Set the default thread pool
     */
    protected void setDefaultPool(ExecutorService pool) {
        this.threadPool = pool;
    }

    /**
     * 获取默认线程池
     * Get the default thread pool
     */
    protected ExecutorService getDefaultPool() {
        return this.threadPool;
    }

    /**
     * 创建新线程
     * Create a new thread
     *
     * @param r Runnable任务 The runnable task
     * @return 新创建的线程 Newly created thread
     */
    public Thread newThread(Runnable r) {
        Thread t = new Thread(this.group, r);
        t.setName(this.name + "-" + this.threadNumber.getAndIncrement());
        t.setPriority(this.prio);
        t.setUncaughtExceptionHandler(new ThreadUncaughtExceptionHandler());
        return t;
    }
}
