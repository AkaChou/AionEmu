package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.GameServer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 世界激活生命周期：驱动世界激活并返回激活后的 GameServer。
 * World-activation lifecycle: drives world activation and returns the activated GameServer.
 */
@Component
@RequiredArgsConstructor
public class GameWorldActivationLifecycle {

    /**
     * 世界激活网关。
     * World-activation gateway.
     */
    private final GameWorldActivationGateway worldActivationGateway;

    /**
     * 当前激活的 GameServer。
     * Currently activated GameServer.
     */
    private GameServer activeServer;

    /**
     * 是否已激活。
     * Whether the world has been activated.
     */
    private boolean activated;

    /**
     * 激活耗时毫秒；未启动前为 -1。
     * Activation time in milliseconds; {@code -1} before start.
     */
    private long activationTimeMillis = -1;

    /**
     * 最近一次失败。
     * Last failure, if any.
     */
    private Throwable lastFailure;

    /**
     * 启动本阶段：激活世界并返回 GameServer。
     * Start this stage: activate the world and return the GameServer.
     *
     * @return 激活后的 GameServer；已激活则返回缓存实例 / Activated GameServer; cached instance if already activated
     */
    public synchronized GameServer start() {
        if (activated) {
            return activeServer;
        }

        long start = System.currentTimeMillis();
        try {
            activeServer = worldActivationGateway.activate();
            activated = true;
            lastFailure = null;
            return activeServer;
        } catch (RuntimeException | Error e) {
            activeServer = null;
            activated = false;
            lastFailure = e;
            throw e;
        } finally {
            activationTimeMillis = System.currentTimeMillis() - start;
        }
    }

    /**
     * 是否已激活。
     * Whether the world has been activated.
     *
     * @return {@code true} if activated。
     */
    public synchronized boolean isActivated() {
        return activated;
    }

    /**
     * 激活耗时毫秒。
     * Activation time in milliseconds.
     *
     * @return 耗时毫秒，未启动为 -1 / Elapsed millis, or {@code -1} if not started
     */
    public synchronized long getActivationTimeMillis() {
        return activationTimeMillis;
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
