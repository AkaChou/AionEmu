package com.aionemu.gameserver.lifecycle;

import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.gameserver.ShutdownHook;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 网络启动运行时桥接：boot-embedded 检测、关闭钩子解析/注册与当前时间。
 * Network-startup runtime bridge: boot-embedded check, shutdown-hook resolve/register, and current time.
 */
@Component
public class GameNetworkStartupRuntimeBridge {

    /**
     * 关闭钩子提供者。
     * Shutdown-hook provider.
     */
    private ObjectProvider<ShutdownHook> shutdownHookProvider;

    /**
     * 可选注入关闭钩子提供者。
     * Optionally inject the shutdown-hook provider.
     *
     * @param shutdownHookProvider 关闭钩子提供者 / Shutdown-hook provider
     */
    @Autowired(required = false)
    void setShutdownHookProvider(ObjectProvider<ShutdownHook> shutdownHookProvider) {
        this.shutdownHookProvider = shutdownHookProvider;
    }

    /**
     * 是否处于 boot-embedded 运行模式。
     * Whether the process is in boot-embedded mode.
     *
     * @return {@code true} if boot-embedded。 / {@code true} if boot-embedded
     */
    public boolean isBootEmbedded() {
        return AionRuntimeMode.isBootEmbedded();
    }

    /**
     * 解析关闭钩子线程：优先 Spring 提供者，否则回退工厂。
     * Resolve the shutdown-hook thread: prefer Spring provider, otherwise fallback factory.
     *
     * @return 关闭钩子线程 / Shutdown-hook thread
     */
    public Thread shutdownHook() {
        if (shutdownHookProvider == null) {
            return GameShutdownHookFallbacks.shutdownHook();
        }
        return shutdownHookProvider.getIfAvailable(GameShutdownHookFallbacks::shutdownHook);
    }

    /**
     * 向 JVM 注册关闭钩子。
     * Register the shutdown hook with the JVM.
     *
     * @param shutdownHook 关闭钩子线程 / Shutdown-hook thread
     */
    public void registerShutdownHook(Thread shutdownHook) {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * 返回当前时间毫秒数。
     * Return the current time in milliseconds.
     *
     * @return 当前时间毫秒 / Current time millis
     */
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
