package com.aionemu.commons.utils;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import lombok.experimental.UtilityClass;

/**
 * 嵌入式运行失败处理器注册表。
 * Registry for embedded-mode failure handlers.
 */
@UtilityClass
public class AionEmbeddedFailureHandler {

    private static final AtomicReference<Consumer<RuntimeException>> HANDLER = new AtomicReference<>();

    /**
     * 注册失败回调。
     * Register a failure callback.
     *
     * @param handler 失败处理器 / Failure handler
     */
    public void register(Consumer<RuntimeException> handler) {
        HANDLER.set(handler);
    }

    /**
     * 清除指定失败回调（仅当当前注册对象相同）。
     * Clear the given failure callback (only if it is currently registered).
     *
     * @param handler 要清除的处理器 / Handler to clear
     */
    public void clear(Consumer<RuntimeException> handler) {
        HANDLER.compareAndSet(handler, null);
    }

    /**
     * 清除当前失败回调。
     * Clear the current failure callback.
     */
    public void clear() {
        HANDLER.set(null);
    }

    /**
     * 触发失败处理；若无处理器则直接抛出。
     * Dispatch failure; rethrow when no handler is registered.
     *
     * @param failure 运行时异常 / Runtime exception
     */
    public void fail(RuntimeException failure) {
        Consumer<RuntimeException> handler = HANDLER.get();
        if (handler != null) {
            handler.accept(failure);
            return;
        }
        throw failure;
    }
}
