package com.aionemu.gameserver.lifecycle;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 系统级收尾生命周期：驱动 gateway 打印启动完成信息并记录启动耗时。
 * System-finalization lifecycle: drives the gateway to print startup completion and records startup time.
 */
@Component
@RequiredArgsConstructor
public class GameSystemLifecycle {

    /**
     * 系统级收尾网关。
     * System-finalization gateway.
     */
    private final GameSystemGateway systemGateway;

    /**
     * 是否已加载。
     * Whether this stage is loaded.
     */
    private boolean loaded;

    /**
     * 本阶段执行耗时毫秒；未启动前为 -1。
     * Stage execution time in milliseconds; {@code -1} before start.
     */
    private long loadTimeMillis = -1;

    /**
     * 服务器整体启动耗时秒数；未成功前为 -1。
     * Overall server startup time in seconds; {@code -1} until success.
     */
    private long startupTimeSeconds = -1;

    /**
     * 最近一次失败。
     * Last failure, if any.
     */
    private Throwable lastFailure;

    /**
     * 启动本阶段：执行系统收尾并返回启动耗时秒数。
     * Start this stage: run system finalization and return startup time in seconds.
     *
     * @param serverStartTimeMillis 服务器启动时间戳（毫秒） / Server start time millis
     * @return 启动耗时秒数；已加载则返回缓存值 / Startup seconds; cached value if already loaded
     */
    public synchronized long start(long serverStartTimeMillis) {
        if (loaded) {
            return startupTimeSeconds;
        }

        long start = System.currentTimeMillis();
        try {
            startupTimeSeconds = systemGateway.start(serverStartTimeMillis);
            loaded = true;
            lastFailure = null;
            return startupTimeSeconds;
        } catch (RuntimeException | Error e) {
            loaded = false;
            startupTimeSeconds = -1;
            lastFailure = e;
            throw e;
        } finally {
            loadTimeMillis = System.currentTimeMillis() - start;
        }
    }

    /**
     * 是否已加载。
     * Whether this stage is loaded.
     *
     * @return {@code true} if loaded。 / {@code true} if loaded
     */
    public synchronized boolean isLoaded() {
        return loaded;
    }

    /**
     * 本阶段执行耗时毫秒。
     * Stage execution time in milliseconds.
     *
     * @return 耗时毫秒，未启动为 -1 / Elapsed millis, or {@code -1} if not started
     */
    public synchronized long getLoadTimeMillis() {
        return loadTimeMillis;
    }

    /**
     * 服务器整体启动耗时秒数。
     * Overall server startup time in seconds.
     *
     * @return 启动秒数，未成功为 -1 / Startup seconds, or {@code -1} if not successful
     */
    public synchronized long getStartupTimeSeconds() {
        return startupTimeSeconds;
    }

    /**
     * 最近失败。
     * Last failure.
     *
     * @return 最近异常，无则为 null / Last throwable, or {@code null}
     */
    public synchronized Throwable getLastFailure() {
        return lastFailure;
    }
}
