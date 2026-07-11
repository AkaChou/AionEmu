package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.ShutdownHook;

/**
 * 关停钩子回退工厂：在 Spring 未就绪时提供 {@link ShutdownHook} 单例。
 * Shutdown-hook fallback factory: supplies a {@link ShutdownHook} singleton when Spring is unavailable.
 */
final class GameShutdownHookFallbacks {

    /**
     * 禁止实例化。
     * Prevent instantiation.
     */
    private GameShutdownHookFallbacks() {
    }

    /**
     * 返回回退用的 {@link ShutdownHook} 实例。
     * Return the fallback {@link ShutdownHook} instance.
     *
     * Shutdown hook
     */
    static ShutdownHook shutdownHook() {
        return ShutdownHookFallback.INSTANCE;
    }

    /**
     * 懒加载持有 {@link ShutdownHook} 的回退单例。
     * Lazy holder for the fallback {@link ShutdownHook} singleton.
     */
    private static final class ShutdownHookFallback {
        /**
         * 回退实例。
         * Fallback instance.
         */
        private static final ShutdownHook INSTANCE = new ShutdownHook();
    }
}
