package com.aionemu.gameserver.lifecycle;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 静态数据生命周期：驱动静态数据加载与移动循环初始化，并记录状态。
 * Static-data lifecycle: drives static-data load and movement-loop init, and tracks state.
 */
@Component
@RequiredArgsConstructor
public class GameStaticDataLifecycle {

    /**
     * 静态数据网关。
     * Static-data gateway.
     */
    private final GameStaticDataGateway staticDataGateway;

    /**
     * 移动循环网关的可选提供者。
     * Optional provider for the movement-loop gateway.
     */
    private ObjectProvider<GameMovementLoopGateway> movementLoopGatewayProvider;

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
     * 启动本阶段：加载静态数据并初始化移动循环。
     * Start this stage: load static data and initialize the movement loop.
     */
    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            staticDataGateway.load();
            movementLoopGateway().initialize();
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
     * @return {@code true} if loaded。 / {@code true} if loaded
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

    /**
     * 注入移动循环网关提供者。
     * Inject the movement-loop gateway provider.
     *
     * @param movementLoopGatewayProvider 移动循环网关提供者 / Movement-loop gateway provider
     */
    @Autowired(required = false)
    void setMovementLoopGatewayProvider(ObjectProvider<GameMovementLoopGateway> movementLoopGatewayProvider) {
        this.movementLoopGatewayProvider = movementLoopGatewayProvider;
    }

    /**
     * 解析移动循环网关：优先 Spring，否则新建。
     * Resolve the movement-loop gateway: prefer Spring, otherwise create new.
     *
     * @return 移动循环网关 / Movement-loop gateway
     */
    private GameMovementLoopGateway movementLoopGateway() {
        if (movementLoopGatewayProvider == null) {
            return new GameMovementLoopGateway();
        }
        return movementLoopGatewayProvider.getIfAvailable(GameMovementLoopGateway::new);
    }
}
