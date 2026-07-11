package com.aionemu.chatserver;

import lombok.experimental.UtilityClass;

/**
 * 非 Spring 环境下关停钩子的回退工厂。
 * Fallback factory for shutdown hooks outside the Spring context.
 */
@UtilityClass
final class ChatShutdownHooks {

    /**
     * 取得共享回退 {@link ShutdownHook} 并绑定进程桥。
     * Obtain the shared fallback {@link ShutdownHook} and bind the process bridge.
     *
     * @param processBridge 进程运行时桥 / Process runtime bridge
     * @return 已配置的关停钩子 / Configured shutdown hook
     */
    static ShutdownHook shutdownHook(ChatProcessRuntimeBridge processBridge) {
        ShutdownHook shutdownHook = Fallbacks.SHUTDOWN_HOOK;
        shutdownHook.configure(processBridge, null, null);
        return shutdownHook;
    }

    /**
     * 懒加载静态回退实例持有者。
     * Lazy static holder for the fallback instance.
     */
    private static final class Fallbacks {

        private static final ShutdownHook SHUTDOWN_HOOK = new ShutdownHook();
    }
}
