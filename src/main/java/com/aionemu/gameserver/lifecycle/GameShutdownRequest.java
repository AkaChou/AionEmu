package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.ShutdownHook;
import com.aionemu.gameserver.ShutdownHook.ShutdownMode;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 关停请求静态门面：通过 {@link ShutdownHook} 发起延迟关停、等待玩家离线与完成关停。
 * Static facade for shutdown requests: delay, wait-for-players, and complete via {@link ShutdownHook}.
 */
@Component
public final class GameShutdownRequest implements DisposableBean {

    /**
     * Spring 注入的 {@link ShutdownHook} 提供者（静态持有）。
     * Spring-injected {@link ShutdownHook} provider (held statically).
     */
    private static volatile ObjectProvider<ShutdownHook> shutdownHookProvider;

    /**
     * 构造并缓存 {@link ShutdownHook} 提供者。
     * Construct and cache the {@link ShutdownHook} provider.
     *
     * @param shutdownHookProvider 关停钩子提供者 / Shutdown-hook provider
     */
    @Autowired
    public GameShutdownRequest(ObjectProvider<ShutdownHook> shutdownHookProvider) {
        setShutdownHookProvider(shutdownHookProvider);
    }

    /**
     * 设置静态 {@link ShutdownHook} 提供者。
     * Set the static {@link ShutdownHook} provider.
     *
     * @param shutdownHookProvider 关停钩子提供者 / Shutdown-hook provider
     */
    public static void setShutdownHookProvider(ObjectProvider<ShutdownHook> shutdownHookProvider) {
        GameShutdownRequest.shutdownHookProvider = shutdownHookProvider;
    }

    /**
     * 以指定延迟、公告间隔与模式执行关停。
     * Perform shutdown with the given delay, announce interval, and mode.
     *
     * @param delay 延迟秒数 / Delay in seconds
     * @param announceInterval 公告间隔秒数 / Announce interval in seconds
     * @param mode 关停模式 / Shutdown mode
     */
    public static void doShutdown(int delay, int announceInterval, ShutdownMode mode) {
        shutdownHook().doShutdown(delay, announceInterval, mode);
    }

    /**
     * 等待玩家离线后再进入关停（固定 {@link ShutdownMode#SHUTDOWN}）。
     * Wait for players to leave before shutdown (fixed {@link ShutdownMode#SHUTDOWN}).
     *
     * @param delay 延迟秒数 / Delay in seconds
     * @param announceInterval 公告间隔秒数 / Announce interval in seconds
     */
    public static void waitForPlayersToLeave(int delay, int announceInterval) {
        shutdownHook().waitForPlayersToLeave(delay, announceInterval, ShutdownMode.SHUTDOWN);
    }

    /**
     * 完成关停流程，可选 halt 运行时。
     * Complete the shutdown sequence, optionally halting the runtime.
     *
     * @param mode 关停模式 / Shutdown mode
     * @param haltRuntime 是否 halt 运行时 / Whether to halt the runtime
     */
    public static void completeShutdown(ShutdownMode mode, boolean haltRuntime) {
        shutdownHook().completeShutdown(mode, haltRuntime);
    }

    /**
     * Spring 销毁时清空静态提供者。
     * Clear the static provider on Spring destroy.
     */
    @Override
    public void destroy() {
        shutdownHookProvider = null;
    }

    /**
     * 解析 {@link ShutdownHook}：优先 Spring 提供，否则回退。
     * Resolve {@link ShutdownHook}: prefer Spring, otherwise fallback.
     *
     * Shutdown hook
     */
    private static ShutdownHook shutdownHook() {
        ObjectProvider<ShutdownHook> provider = shutdownHookProvider;
        if (provider == null) {
            return GameShutdownHookFallbacks.shutdownHook();
        }
        return provider.getIfAvailable(GameShutdownHookFallbacks::shutdownHook);
    }
}
