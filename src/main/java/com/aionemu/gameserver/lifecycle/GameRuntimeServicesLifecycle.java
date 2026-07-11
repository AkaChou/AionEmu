package com.aionemu.gameserver.lifecycle;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 运行时服务集合的生命周期封装：幂等启动并记录加载耗时与失败原因。
 * Lifecycle wrapper for the runtime services set: idempotent start with load timing and failure capture.
 */
@Component
@RequiredArgsConstructor
public class GameRuntimeServicesLifecycle {

    /**
     * 运行时服务网关。
     * Runtime services gateway.
     */
    private final GameRuntimeServicesGateway runtimeServicesGateway;

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
     * 幂等启动运行时服务集合；已加载则直接返回。
     * Idempotently start the runtime services set; no-op when already loaded.
     */
    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            runtimeServicesGateway.start();
            loaded = true;
            lastFailure = null;
        } catch (RuntimeException | Error e) {
            loaded = false;
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
     * true when loaded
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
     * throwable or null
     */
    public synchronized Throwable getLastFailure() {
        return lastFailure;
    }
}
