package com.aionemu.commons.utils.concurrent;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;

/**
 * {@link ScheduledFuture} 包装器，统一对外暴露类型。
 * Wrapper around {@link ScheduledFuture} for a uniform public type.
 */
@RequiredArgsConstructor
public final class ScheduledFutureWrapper implements ScheduledFuture<Object> {

    /**
     * 被包装的调度任务。
     * Wrapped scheduled future.
     */
    private final ScheduledFuture<?> future;

    /**
     * 获取剩余延迟。
     * Get remaining delay.
     *
     * @param unit 时间单位 / Time unit
     * Remaining delay
     */
    @Override
    public long getDelay(TimeUnit unit) {
        return this.future.getDelay(unit);
    }

    /**
     * 按延迟比较。
     * Compare by remaining delay.
     *
     * @param o 另一延迟对象 / Other delayed object
     * Comparison result
     */
    @Override
    public int compareTo(Delayed o) {
        return this.future.compareTo(o);
    }

    /**
     * 取消任务。
     * Cancel the task.
     *
     * @param mayInterruptIfRunning 是否中断运行中任务 / Whether to interrupt if running
     * @return 是否取消成功 / Whether cancelled
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return this.future.cancel(mayInterruptIfRunning);
    }

    /**
     * 阻塞获取结果。
     * Block until result is available.
     *
     * Result
     * If interrupted
     * If computation failed
     */
    @Override
    public Object get() throws InterruptedException, ExecutionException {
        return this.future.get();
    }

    /**
     * 限时获取结果。
     * Get result with timeout.
     *
     * Timeout
     * @param unit    时间单位 / Time unit
     * Result
     * If interrupted
     * If computation failed
     * If timed out
     */
    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.future.get(timeout, unit);
    }

    /**
     * 是否已取消。
     * Whether cancelled.
     *
     * @return 已取消则为 true / True if cancelled
     */
    @Override
    public boolean isCancelled() {
        return this.future.isCancelled();
    }

    /**
     * 是否已完成。
     * Whether done.
     *
     * @return 已完成则为 true / True if done
     */
    @Override
    public boolean isDone() {
        return this.future.isDone();
    }
}
