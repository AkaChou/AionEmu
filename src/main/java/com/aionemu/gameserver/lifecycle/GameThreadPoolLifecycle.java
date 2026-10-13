package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.utils.ThreadPoolManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 线程池生命周期：启动/停止线程池并跟踪 started 状态（保留可重启语义，不采用基类三件套）。
 * Thread-pool lifecycle: starts/stops the pool and tracks started state (restartable semantics,
 * not the base-class loaded pattern).
 */
@Component
public class GameThreadPoolLifecycle {

    /**
     * ThreadPoolManager 的可选 Spring 提供者。
     * Optional Spring provider for {@link ThreadPoolManager}.
     */
    private ObjectProvider<ThreadPoolManager> threadPoolManagerProvider;

    /**
     * 是否已启动。
     * Whether the thread pool has been started.
     */
    private boolean started;

    /**
     * 注入 ThreadPoolManager 提供者。
     * Inject the ThreadPoolManager provider.
     * @param threadPoolManagerProvider ThreadPoolManager 提供者 / ThreadPoolManager provider
     */
    @Autowired(required = false)
    void setThreadPoolManagerProvider(ObjectProvider<ThreadPoolManager> threadPoolManagerProvider) {
        this.threadPoolManagerProvider = threadPoolManagerProvider;
    }

    /**
     * 启动线程池（幂等）。
     * Start the thread pool (idempotent).
     */
    public synchronized void start() {
        if (started) {
            return;
        }
        startThreadPool();
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
            stopThreadPool();
        } finally {
            started = false;
        }
    }

    /**
     * 是否已启动。
     * Whether the thread pool has been started.
     * @return 已启动为 {@code true} / {@code true} if started
     */
    public synchronized boolean isStarted() {
        return started;
    }

    /**
     * 启动线程池实体（解析 ThreadPoolManager 以触发初始化）。
     * Start the pool entity (resolve ThreadPoolManager to trigger initialization).
     */
    protected void startThreadPool() {
        threadPoolManager();
    }

    /**
     * 关闭线程池实体。
     * Shut down the pool entity.
     */
    protected void stopThreadPool() {
        threadPoolManager().shutdown();
    }

    /**
     * 解析 ThreadPoolManager 并缓存解析结果。
     * Resolve ThreadPoolManager and remember the resolved instance.
     * @return ThreadPoolManager 实例 / ThreadPoolManager instance
     */
    private ThreadPoolManager threadPoolManager() {
        return GameThreadPoolServices.rememberThreadPoolManager(threadPoolManagerProvider.getObject());
    }
}
