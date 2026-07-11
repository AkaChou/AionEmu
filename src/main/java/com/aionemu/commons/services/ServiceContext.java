package com.aionemu.commons.services;

import java.util.Locale;

/**
 * 服务上下文工具，用线程本地变量隔离不同服务（如 LS/GS）的运行环境
 * Service context utility that isolates runtime environments of different services (e.g. LS/GS) via thread-local storage
 */
public final class ServiceContext {

    private static final String DEFAULT_CONTEXT = "default";
    private static final InheritableThreadLocal<String> CURRENT = new InheritableThreadLocal<String>() {
        /**
         * 提供默认上下文名称
         * Provide the default context name
         *
         * @return 默认上下文 / Default context
         */
        @Override
        protected String initialValue() {
            return DEFAULT_CONTEXT;
        }
    };

    private ServiceContext() {
    }

    /**
     * 获取当前线程的服务上下文名称
     * Get the current thread's service context name
     *
     * @return 当前上下文名称 / Current context name
     */
    public static String current() {
        return CURRENT.get();
    }

    /**
     * 切换当前线程服务上下文，返回可关闭的作用域
     * Switch the current thread service context and return a closable scope
     *
     * @param name 上下文名称 / Context name
     * @return 作用域，关闭后恢复原上下文 / Scope that restores the previous context when closed
     */
    public static Scope use(String name) {
        String previous = CURRENT.get();
        CURRENT.set(normalize(name));
        return new Scope(previous);
    }

    /**
     * 使用当前上下文包装 Runnable
     * Wrap a Runnable with the current context
     *
     * Original runnable
     *
     * @param runnable @return 带上下文的任务 / Context-aware runnable
     */
    public static Runnable wrap(Runnable runnable) {
        return wrap(runnable, current());
    }

    /**
     * 使用指定上下文包装 Runnable
     * Wrap a Runnable with the given context
     *
     * Original runnable
     *
     * @param context 上下文名称 / Context name
     * @param context @return 带上下文的任务 / Context-aware runnable
     */
    public static Runnable wrap(Runnable runnable, String context) {
        String normalizedContext = normalize(context);
        return new Runnable() {
            /**
             * 在目标上下文中执行任务
             * Execute the task under the target context
             */
            @Override
            public void run() {
                try (Scope ignored = use(normalizedContext)) {
                    runnable.run();
                }
            }
        };
    }

    /**
     * 规范化上下文名称
     * Normalize a context name
     *
     * @param name 原始名称 / Original name
     * @return 规范化后的名称 / Normalized name
     */
    private static String normalize(String name) {
        if (name == null || name.trim().isEmpty()) {
            return DEFAULT_CONTEXT;
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 上下文作用域，关闭时恢复先前上下文
     * Context scope that restores the previous context on close
     */
    public static final class Scope implements AutoCloseable {
        private final String previous;
        private boolean closed;

        private Scope(String previous) {
            this.previous = previous;
        }

        /**
         * 恢复进入作用域前的上下文
         * Restore the context that was active before entering this scope
         */
        @Override
        public void close() {
            if (!closed) {
                CURRENT.set(previous);
                closed = true;
            }
        }
    }
}
