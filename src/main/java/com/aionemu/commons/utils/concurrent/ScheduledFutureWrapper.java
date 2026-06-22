package com.aionemu.commons.utils.concurrent;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 调度任务包装类，用于包装和管理调度任务的执行
 * Scheduled future wrapper class for wrapping and managing scheduled task execution
 */
public final class ScheduledFutureWrapper implements ScheduledFuture<Object> {
    // 被包装的调度任务 The wrapped scheduled future
    private final ScheduledFuture<?> future;

    /**
     * 创建调度任务包装器
     * Create a scheduled future wrapper
     *
     * @param future 需要包装的调度任务 The scheduled future to be wrapped
     */
    public ScheduledFutureWrapper(ScheduledFuture<?> future) {
        this.future = future;
    }

    /**
     * 获取任务延迟时间
     * Get the remaining delay
     *
     * @param unit 时间单位 Time unit
     * @return 延迟时间 Remaining delay
     */
    public long getDelay(TimeUnit unit) {
        return this.future.getDelay(unit);
    }

    /**
     * 比较延迟时间
     * Compare the remaining delay
     *
     * @param o 需要比较的延迟对象 The delayed object to compare with
     * @return 比较结果 Comparison result
     */
    public int compareTo(Delayed o) {
        return this.future.compareTo(o);
    }

    /**
     * 取消任务执行
     * Cancel the task execution
     *
     * @param mayInterruptIfRunning 是否中断正在运行的任务 Whether to interrupt if running
     * @return 是否取消成功 Whether cancelled successfully
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        return this.future.cancel(false);
    }

    /**
     * 获取任务执行结果
     * Get the task execution result
     *
     * @return 执行结果 Execution result
     * @throws InterruptedException 中断异常 If interrupted while waiting
     * @throws ExecutionException 执行异常 If the computation threw an exception
     */
    public Object get() throws InterruptedException, ExecutionException {
        return this.future.get();
    }

    /**
     * 在指定时间内获取任务执行结果
     * Get the task execution result within specified timeout
     *
     * @param timeout 超时时间 Timeout duration
     * @param unit 时间单位 Time unit
     * @return 执行结果 Execution result
     * @throws InterruptedException 中断异常 If interrupted while waiting
     * @throws ExecutionException 执行异常 If the computation threw an exception
     * @throws TimeoutException 超时异常 If the wait timed out
     */
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.future.get(timeout, unit);
    }

    /**
     * 检查任务是否已取消
     * Check if the task is cancelled
     *
     * @return 是否已取消 Whether cancelled
     */
    public boolean isCancelled() {
        return this.future.isCancelled();
    }

    /**
     * 检查任务是否已完成
     * Check if the task is done
     *
     * @return 是否已完成 Whether done
     */
    public boolean isDone() {
        return this.future.isDone();
    }
}
