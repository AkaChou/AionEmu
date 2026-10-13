package com.aionemu.gameserver.lifecycle;

/**
 * 启动阶段基类：承载幂等启动包装（加载状态、耗时与失败记录），子类只需实现 {@link #runStage()} 业务体。
 * Startup-stage base class: hosts the idempotent start wrapper (load state, duration and failure
 * tracking); subclasses only implement {@link #runStage()}.
 */
public abstract class GameStartupStage {

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
     * 幂等启动本阶段：已加载则直接返回；失败记录后重抛，允许重试。
     * Idempotently start this stage: return when already loaded; record the failure and rethrow so
     * a retry stays possible.
     */
    public final synchronized void start() {
        if (loaded) {
            return;
        }

        long begin = System.currentTimeMillis();
        try {
            runStage();
            loaded = true;
            lastFailure = null;
        } catch (RuntimeException | Error e) {
            loaded = false;
            lastFailure = e;
            throw e;
        } finally {
            loadTimeMillis = System.currentTimeMillis() - begin;
        }
    }

    /**
     * 阶段业务体：由子类实现（原各网关的启动逻辑）。
     * Stage body implemented by subclasses (the former gateway logic).
     */
    protected abstract void runStage();

    /**
     * 是否已加载。
     * Whether this stage is loaded.
     * @return 已加载为 {@code true} / {@code true} if loaded
     */
    public final synchronized boolean isLoaded() {
        return loaded;
    }

    /**
     * 加载耗时毫秒。
     * Load time in milliseconds.
     * @return 耗时毫秒，未启动为 -1 / Elapsed millis, or {@code -1} if not started
     */
    public final synchronized long getLoadTimeMillis() {
        return loadTimeMillis;
    }

    /**
     * 最近失败。
     * Last failure.
     * @return 最近异常，无则为 null / Last throwable, or {@code null}
     */
    public final synchronized Throwable getLastFailure() {
        return lastFailure;
    }
}
