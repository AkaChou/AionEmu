package com.aionemu.gameserver.lifecycle;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 挖掘场生命周期：在启动序列中触发 gateway 并记录加载状态。
 * Lifecycle for dredgion: drives the gateway during startup and tracks load state.
 */
@Component
@RequiredArgsConstructor
public class GameDredgionLifecycle {

    /**
     * 挖掘场网关。
     * Dredgion gateway.
     */
    private final GameDredgionGateway dredgionGateway;

    /**
     * 是否已加载。
     * Whether this stage is loaded.
     */
    private boolean loaded;

    /**
     * 加载耗时毫秒；未启动前为 -1。
     * Load time in milliseconds; {@code -1} before start.
     */
    private long loadTimeMillis = -1;

    /**
     * 最近一次失败。
     * Last failure, if any.
     */
    private Throwable lastFailure;

    /**
     * 启动本阶段：初始化挖掘场相关实例服务。
     * Start this stage: initialize dredgion-related instance services.
     */
    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            dredgionGateway.start();
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
     * 是否已加载。
     * Whether this stage is loaded.
     *
     * @return {@code true} if loaded。
     */
    public synchronized boolean isLoaded() {
        return loaded;
    }

    /**
     * 加载耗时毫秒。
     * Load time in milliseconds.
     *
     * @return 耗时毫秒，未启动为 -1 / Elapsed millis, or {@code -1} if not started
     */
    public synchronized long getLoadTimeMillis() {
        return loadTimeMillis;
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
