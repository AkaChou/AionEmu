package com.aionemu.commons.utils.concurrent;

/**
 * 可运行任务包装类，用于包装和监控任务的执行时间
 * Runnable wrapper class for wrapping and monitoring task execution time
 */
public class RunnableWrapper implements Runnable {
    // 被包装的原始任务 The original wrapped runnable task
    private final Runnable runnable;
    // 无警告运行的最大时间(毫秒) Maximum runtime in milliseconds without warning
    private final long maxRuntimeMsWithoutWarning;

    /**
     * 使用默认的最大运行时间创建包装器
     * Create wrapper with default maximum runtime
     *
     * @param runnable 需要包装的任务 The task to be wrapped
     */
    public RunnableWrapper(Runnable runnable) {
        this(runnable, Long.MAX_VALUE);
    }

    /**
     * 使用指定的最大运行时间创建包装器
     * Create wrapper with specified maximum runtime
     *
     * @param runnable 需要包装的任务 The task to be wrapped
     * @param maxRuntimeMsWithoutWarning 无警告运行的最大时间 Maximum runtime without warning
     */
    public RunnableWrapper(Runnable runnable, long maxRuntimeMsWithoutWarning) {
        this.runnable = runnable;
        this.maxRuntimeMsWithoutWarning = maxRuntimeMsWithoutWarning;
    }

    /**
     * 执行被包装的任务，并监控其运行时间
     * Execute the wrapped task and monitor its runtime
     */
    public final void run() {
        ExecuteWrapper.execute(this.runnable, this.maxRuntimeMsWithoutWarning);
    }
}
