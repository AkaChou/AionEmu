package com.aionemu.gameserver.lifecycle;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 线程池生命周期：启动/停止线程池并跟踪 started 状态（无 loadTimeMillis 模式）。
 * Thread-pool lifecycle: starts/stops the pool and tracks started state (no loadTimeMillis pattern).
 */
@Component
@RequiredArgsConstructor
public class GameThreadPoolLifecycle {

    /**
     * 线程池网关。
     * Thread-pool gateway.
     */
    private final GameThreadPoolGateway threadPoolGateway;

    /**
     * 是否已启动。
     * Whether the thread pool has been started.
     */
    private boolean started;

    /**
     * 启动线程池（幂等）。
     * Start the thread pool (idempotent).
     */
    public synchronized void start() {
        if (started) {
            return;
        }
        threadPoolGateway.start();
        started = true;
    }

    /**
     * 停止线程池（幂等）。
     * Stop the thread pool (idempotent).
     */
    public synchronized void stop() {
        if (!started) {
            return;
        }
        try {
            threadPoolGateway.stop();
        } finally {
            started = false;
        }
    }

    /**
     * 是否已启动。
     * Whether the thread pool has been started.
     *
     * @return {@code true} if started。
     */
    public synchronized boolean isStarted() {
        return started;
    }
}
