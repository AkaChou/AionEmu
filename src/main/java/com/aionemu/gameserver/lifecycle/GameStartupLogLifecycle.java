package com.aionemu.gameserver.lifecycle;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 启动日志计时生命周期：幂等记录启动起点时间戳并统计本阶段耗时。
 * Lifecycle for startup-log timing: idempotently capture the startup epoch and phase duration.
 */
@Component
@RequiredArgsConstructor
public class GameStartupLogLifecycle {

    /**
     * 启动日志网关。
     * Startup log gateway.
     */
    private final GameStartupLogGateway startupLogGateway;

    /**
     * 是否已成功加载。
     * Whether loading completed successfully.
     */
    private boolean loaded;

    /**
     * 启动起点时间戳（毫秒）；未启动为 -1。
     * Startup epoch millis; -1 if never started.
     */
    private long startupTimeMillis = -1;

    /**
     * 本阶段执行耗时（毫秒）；未启动为 -1。
     * Phase duration in milliseconds; -1 if never started.
     */
    private long loadTimeMillis = -1;

    /**
     * 最近一次失败异常；成功时为 null。
     * Last failure throwable; null after success.
     */
    private Throwable lastFailure;

    /**
     * 幂等开始启动日志计时；已加载则返回缓存的起点时间。
     * Idempotently start startup-log timing; return cached epoch when already loaded.
     *
     * @return 启动起点毫秒时间戳 / Startup epoch millis
     */
    public synchronized long start() {
        if (loaded) {
            return startupTimeMillis;
        }

        long start = System.currentTimeMillis();
        try {
            startupTimeMillis = startupLogGateway.start();
            loaded = true;
            lastFailure = null;
            return startupTimeMillis;
        } catch (RuntimeException | Error e) {
            loaded = false;
            startupTimeMillis = -1;
            lastFailure = e;
            throw e;
        } finally {
            loadTimeMillis = System.currentTimeMillis() - start;
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
     * 本阶段执行耗时（毫秒）。
     * Phase duration in milliseconds.
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
