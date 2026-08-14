package com.aionemu.gameserver.lifecycle;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 启动完成阶段的生命周期封装：幂等输出完成日志并记录耗时。
 * Lifecycle wrapper for startup completion: idempotent completion logging with timing.
 */
@Component
@RequiredArgsConstructor
public class GameStartupCompletionLifecycle {

    /**
     * 启动完成网关。
     * Startup completion gateway.
     */
    private final GameStartupCompletionGateway startupCompletionGateway;

    /**
     * 是否已成功加载。
     * Whether loading completed successfully.
     */
    private boolean loaded;

    /**
     * 最近一次启动耗时（毫秒）；未启动为 -1。
     * Last start duration in milliseconds; -1 if never started.
     */
    private long loadTimeMillis = -1;

    /**
     * 最近一次失败异常；成功时为 null。
     * Last failure throwable; null after success.
     */
    private Throwable lastFailure;

    /**
     * 幂等记录启动完成信息。
     * Idempotently log startup completion.
     *
     * @param startupTime 从启动日志计时起的总耗时（毫秒） / Total startup duration from log start (ms)
     */
    public synchronized void start(long startupTime) {
        if (loaded) {
            return;
        }

        long start = startupCompletionGateway.currentTimeMillis();
        try {
            startupCompletionGateway.logStartupComplete(startupTime);
            loaded = true;
            lastFailure = null;
        } catch (RuntimeException | Error e) {
            loaded = false;
            lastFailure = e;
            throw e;
        } finally {
            loadTimeMillis = startupCompletionGateway.currentTimeMillis() - start;
        }
    }

    /**
     * 是否已成功加载。
     * Whether loading completed successfully.
     *
     * @return 已加载为 {@code true} / {@code true} when loaded
     */
    public synchronized boolean isLoaded() {
        return loaded;
    }

    /**
     * 最近一次启动耗时（毫秒）。
     * Last start duration in milliseconds.
     *
     * @return 耗时毫秒；未启动为 -1 / duration ms, or -1 if never started
     */
    public synchronized long getLoadTimeMillis() {
        return loadTimeMillis;
    }

    /**
     * 最近一次失败异常。
     * Last failure throwable.
     *
     * @return 失败异常，无则为 null / throwable or null
     */
    public synchronized Throwable getLastFailure() {
        return lastFailure;
    }
}
