package com.aionemu.commons.utils.concurrent;

import com.aionemu.commons.services.ServiceContext;

/**
 * Runnable 包装器：绑定服务上下文并监控执行耗时。
 * Runnable wrapper that binds service context and monitors runtime.
 */
public class RunnableWrapper implements Runnable {

    /**
     * 被包装的原始任务。
     * Original wrapped runnable.
     */
    private final Runnable runnable;

    /**
     * 无警告最大运行时间（毫秒）。
     * Max runtime in ms without warning.
     */
    private final long maxRuntimeMsWithoutWarning;

    /**
     * 捕获的服务上下文。
     * Captured service context.
     */
    private final String serviceContext;

    /**
     * 使用默认最大运行时间创建包装器。
     * Create wrapper with default max runtime.
     *
     * @param runnable 待包装任务 / Task to wrap
     */
    public RunnableWrapper(Runnable runnable) {
        this(runnable, Long.MAX_VALUE);
    }

    /**
     * 使用指定最大运行时间创建包装器。
     * Create wrapper with the given max runtime.
     *
     * @param runnable                   待包装任务 / Task to wrap
     * @param maxRuntimeMsWithoutWarning 无警告最大毫秒 / Max ms without warning
     */
    public RunnableWrapper(Runnable runnable, long maxRuntimeMsWithoutWarning) {
        this.runnable = runnable;
        this.maxRuntimeMsWithoutWarning = maxRuntimeMsWithoutWarning;
        this.serviceContext = ServiceContext.current();
    }

    /**
     * 在绑定上下文中执行被包装任务。
     * Run the wrapped task under the bound context.
     */
    @Override
    public final void run() {
        try (ServiceContext.Scope ignored = ServiceContext.use(serviceContext)) {
            ExecuteWrapper.execute(this.runnable, this.maxRuntimeMsWithoutWarning);
        }
    }
}
