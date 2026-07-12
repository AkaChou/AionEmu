package com.aionemu.gameserver.lifecycle;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 房屋系统生命周期：委托网关启动房屋相关服务。
 * Housing lifecycle: starts housing-related services via the gateway.
 */
@Component
@RequiredArgsConstructor
public class GameHousingLifecycle {

    /**
     * 房屋系统网关。
     * Housing gateway.
     */
    private final GameHousingGateway housingGateway;
    /**
     * 是否已成功加载。
     * Whether loading has completed successfully.
     */
    private boolean loaded;
    /**
     * 最近一次加载耗时（毫秒）；未启动为 -1。
     * Last load duration in milliseconds; -1 if never started.
     */
    private long loadTimeMillis = -1;
    /**
     * 最近一次失败原因；成功时为 {@code null}。
     * Last failure cause; {@code null} on success.
     */
    private Throwable lastFailure;

    /**
     * 启动房屋系统（幂等）。
     * Start the housing system (idempotent).
     */
    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            housingGateway.start();
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
     * Whether loading has completed successfully.
     *
     * @return {@code true} if loaded。
     */
    public synchronized boolean isLoaded() {
        return loaded;
    }

    /**
     * 最近一次加载耗时（毫秒）。
     * Last load duration in milliseconds.
     *
     * @return 耗时毫秒数；未启动为 -1 / Duration ms; -1 if never started
     */
    public synchronized long getLoadTimeMillis() {
        return loadTimeMillis;
    }

    /**
     * 最近一次失败原因。
     * Last failure cause.
     *
     * @return 失败异常；成功为 {@code null} / Failure throwable; {@code null} on success
     */
    public synchronized Throwable getLastFailure() {
        return lastFailure;
    }
}
