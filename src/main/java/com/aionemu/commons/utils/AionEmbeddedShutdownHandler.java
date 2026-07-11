package com.aionemu.commons.utils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import lombok.experimental.UtilityClass;

/**
 * 嵌入式运行关闭处理器注册表。
 * Registry for embedded-mode shutdown handlers.
 */
@UtilityClass
public class AionEmbeddedShutdownHandler {

    private static final AtomicReference<Registration> HANDLER = new AtomicReference<>();

    /**
     * 注册无模式参数的关闭回调。
     * Register a shutdown callback without mode argument.
     *
     * @param handler 关闭处理器 / Shutdown handler
     */
    public void register(Runnable handler) {
        Objects.requireNonNull(handler, "handler");
        HANDLER.set(new Registration(handler, ignored -> handler.run()));
    }

    /**
     * 注册带关闭模式的回调。
     * Register a shutdown callback that receives the mode.
     *
     * @param handler 关闭处理器 / Shutdown handler
     */
    public void register(Consumer<AionEmbeddedShutdownMode> handler) {
        Objects.requireNonNull(handler, "handler");
        HANDLER.set(new Registration(handler, handler));
    }

    /**
     * 按 {@link Runnable} 键清除回调。
     * Clear callback by {@link Runnable} key.
     *
     * @param handler 注册时的处理器 / Originally registered handler
     */
    public void clear(Runnable handler) {
        clearByKey(handler);
    }

    /**
     * 按 {@link Consumer} 键清除回调。
     * Clear callback by {@link Consumer} key.
     *
     * @param handler 注册时的处理器 / Originally registered handler
     */
    public void clear(Consumer<AionEmbeddedShutdownMode> handler) {
        clearByKey(handler);
    }

    /**
     * 清除当前关闭回调。
     * Clear the current shutdown callback.
     */
    public void clear() {
        HANDLER.set(null);
    }

    /**
     * 以默认 {@link AionEmbeddedShutdownMode#SHUTDOWN} 请求关闭。
     * Request shutdown with default {@link AionEmbeddedShutdownMode#SHUTDOWN}.
     *
     * @return 是否已成功派发 / Whether a handler was invoked
     */
    public boolean requestShutdown() {
        return requestShutdown(AionEmbeddedShutdownMode.SHUTDOWN);
    }

    /**
     * 以指定模式请求关闭。
     * Request shutdown with the given mode.
     *
     * @param mode 关闭模式 / Shutdown mode
     * @return 是否已成功派发 / Whether a handler was invoked
     */
    public boolean requestShutdown(AionEmbeddedShutdownMode mode) {
        Registration registration = HANDLER.get();
        if (registration == null) {
            return false;
        }
        registration.handler().accept(Objects.requireNonNull(mode, "mode"));
        return true;
    }

    /**
     * 仅当 key 匹配时清除注册。
     * Clear registration only when key matches.
     *
     * Registration key
     */
    private void clearByKey(Object handler) {
        Registration registration = HANDLER.get();
        if (registration != null && registration.key() == handler) {
            HANDLER.compareAndSet(registration, null);
        }
    }

    /**
     * 关闭处理器注册项。
     * Shutdown handler registration entry.
     *
     * Registration key
     * Callback
     */
    private record Registration(Object key, Consumer<AionEmbeddedShutdownMode> handler) {
    }
}
