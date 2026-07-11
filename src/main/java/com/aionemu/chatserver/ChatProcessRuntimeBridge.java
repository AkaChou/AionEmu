package com.aionemu.chatserver;

import com.aionemu.commons.utils.AionProcessExit;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 聊天服进程级运行时桥接：关停钩子注册、关机与 halt。
 * Process-level runtime bridge for chat server: shutdown-hook registration, shutdown, and halt.
 */
@Component
@Lazy
@ConditionalOnProperty(prefix = "aion.services.chat", name = "enabled", havingValue = "true")
public class ChatProcessRuntimeBridge {

    private ObjectProvider<ShutdownHook> shutdownHookProvider;

    /**
     * 可选注入 {@link ShutdownHook} 的 {@link ObjectProvider}（Spring 环境）。
     * Optionally inject the {@link ObjectProvider} of {@link ShutdownHook} (Spring context).
     *
     * @param shutdownHookProvider 关停钩子提供者 / Shutdown-hook provider
     */
    @Autowired(required = false)
    void setShutdownHookProvider(ObjectProvider<ShutdownHook> shutdownHookProvider) {
        this.shutdownHookProvider = shutdownHookProvider;
    }

    /**
     * 返回可用的关停钩子线程实例。
     * Return a usable shutdown-hook thread instance.
     *
     * @return 关停钩子线程 / Shutdown-hook thread
     */
    public Thread shutdownHook() {
        return shutdownHookInstance();
    }

    /**
     * 将给定关停钩子注册到 JVM {@link Runtime}。
     * Register the given shutdown hook with the JVM {@link Runtime}.
     *
     * @param shutdownHook 待注册的关停钩子 / Shutdown hook to register
     */
    public void registerShutdownHook(Thread shutdownHook) {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * 触发聊天服关停流程。
     * Trigger the chat-server shutdown sequence.
     *
     * @param restart 是否仅重启 / Whether to restart only
     */
    public void shutdown(boolean restart) {
        shutdownHookInstance().shutdown(restart);
    }

    /**
     * 以指定状态码 halt 当前进程。
     * Halt the current process with the given status code.
     *
     * @param status 退出状态码 / Exit status code
     */
    public void halt(int status) {
        AionProcessExit.halt(status);
    }

    /**
     * 解析 {@link ShutdownHook}：优先 Spring 提供，否则回退到 {@link ChatShutdownHooks}。
     * Resolve {@link ShutdownHook}: prefer Spring provider, otherwise fall back to {@link ChatShutdownHooks}.
     *
     * @return 关停钩子实例 / Shutdown-hook instance
     */
    private ShutdownHook shutdownHookInstance() {
        if (shutdownHookProvider == null) {
            return ChatShutdownHooks.shutdownHook(this);
        }
        return shutdownHookProvider.getIfAvailable(() -> ChatShutdownHooks.shutdownHook(this));
    }
}
