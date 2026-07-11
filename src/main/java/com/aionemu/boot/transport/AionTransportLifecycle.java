package com.aionemu.boot.transport;

import com.aionemu.boot.config.AionServicesProperties.TransportMode;

/**
 * 传输层生命周期契约：按模式启动与停止共享网络资源。
 * Transport lifecycle contract: start and stop shared network resources by mode.
 */
public interface AionTransportLifecycle {

    /**
     * 该实现对应的传输模式。
     * Transport mode this implementation owns.
     *
     * Transport mode
     */
    TransportMode mode();

    /**
     * 启动传输资源（幂等，已启动则直接返回）。
     * Start transport resources (idempotent; no-op if already started).
     */
    void start();

    /**
     * 停止传输资源并释放事件循环等共享状态。
     * Stop transport resources and release shared state such as event loops.
     */
    void stop();
}
