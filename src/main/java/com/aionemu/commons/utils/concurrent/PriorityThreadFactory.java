package com.aionemu.commons.utils.concurrent;

import com.aionemu.commons.network.util.ThreadUncaughtExceptionHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 按指定优先级创建线程的工厂。
 * Thread factory that creates threads with a given priority.
 */
public class PriorityThreadFactory implements ThreadFactory {

    /**
     * 线程优先级。
     * Thread priority.
     */
    private int prio;

    /**
     * 线程名前缀。
     * Thread name prefix.
     */
    private String name;

    /**
     * 关联线程池。
     * Associated thread pool.
     */
    private ExecutorService threadPool;

    /**
     * 线程序号。
     * Thread sequence counter.
     */
    private AtomicInteger threadNumber;

    /**
     * 线程组。
     * Thread group.
     */
    private ThreadGroup group;

    /**
     * 使用名称与优先级创建工厂。
     * Create a factory with name and priority.
     *
     * @param name 线程名前缀 / Thread name prefix
     * @param prio 线程优先级 / Thread priority
     */
    public PriorityThreadFactory(String name, int prio) {
        this.threadNumber = new AtomicInteger(1);
        this.prio = prio;
        this.name = name;
        this.group = new ThreadGroup(this.name);
    }

    /**
     * 使用名称与默认优先级 5，并绑定默认线程池。
     * Create with name, default priority 5, and bind a default pool.
     *
     * @param name        线程名前缀 / Thread name prefix
     * @param defaultPool 默认线程池 / Default thread pool
     */
    public PriorityThreadFactory(String name, ExecutorService defaultPool) {
        this(name, 5);
        this.setDefaultPool(defaultPool);
    }

    /**
     * 设置默认线程池。
     * Set the default thread pool.
     *
     * Thread pool
     */
    protected void setDefaultPool(ExecutorService pool) {
        this.threadPool = pool;
    }

    /**
     * 获取默认线程池。
     * Get the default thread pool.
     *
     * Thread pool
     */
    protected ExecutorService getDefaultPool() {
        return this.threadPool;
    }

    /**
     * 创建新线程。
     * Create a new thread.
     *
     * @param r 任务 / Runnable task
     * New thread
     */
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(this.group, r);
        t.setName(this.name + "-" + this.threadNumber.getAndIncrement());
        t.setPriority(this.prio);
        t.setUncaughtExceptionHandler(new ThreadUncaughtExceptionHandler());
        return t;
    }
}
