package com.aionemu.boot.lifecycle;

import com.aionemu.chatserver.ChatServerRuntime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 聊天服生命周期网关：在可选运行时与运行时桥之间协调启动/停止。
 * Chat-server lifecycle gateway: coordinates start/stop between optional runtime and runtime bridge.
 */
@Component
public class ChatServerLifecycleGateway {

    private ObjectProvider<ChatServerRuntime> chatServerRuntimeProvider;
    private ObjectProvider<ChatServerRuntimeBridge> runtimeBridgeProvider;
    private ChatServerRuntimeBridge runtimeBridge;

    /**
     * 默认构造，依赖通过可选 setter 注入。
     * Default constructor; dependencies are injected via optional setters.
     */
    public ChatServerLifecycleGateway() {
    }

    /**
     * 注入可选的聊天服运行时提供者。
     * Injects an optional chat-server runtime provider.
     *
     * @param chatServerRuntimeProvider 聊天服运行时提供者 / runtime ObjectProvider
     */
    @Autowired(required = false)
    void setChatServerRuntimeProvider(ObjectProvider<ChatServerRuntime> chatServerRuntimeProvider) {
        this.chatServerRuntimeProvider = chatServerRuntimeProvider;
    }

    /**
     * 注入可选的聊天运行时桥提供者。
     * Injects an optional chat runtime-bridge provider.
     *
     * @param runtimeBridgeProvider 运行时桥提供者 / runtime-bridge ObjectProvider
     */
    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<ChatServerRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    /**
     * 启动聊天服：优先使用 Spring 管理的运行时，否则走桥接默认启动。
     * Starts the chat server: prefers a Spring-managed runtime, otherwise default bridge start.
     *
     * @param args 启动参数 / startup arguments
     */
    public void start(String[] args) {
        ChatServerRuntimeBridge bridge = runtimeBridge();
        ChatServerRuntime chatServerRuntime = chatServerRuntime();
        if (chatServerRuntime == null) {
            bridge.start(args);
            return;
        }
        bridge.prepareShutdown();
        chatServerRuntime.start(args);
    }

    /**
     * 停止聊天服（非重启）。
     * Stops the chat server without restart.
     */
    public void stop() {
        runtimeBridge().shutdown(false);
    }

    /**
     * 解析可选的聊天服运行时。
     * Resolves the optional chat-server runtime.
     *
     * @return 运行时实例，不可用则为 null / runtime instance, or null if unavailable
     */
    private ChatServerRuntime chatServerRuntime() {
        if (chatServerRuntimeProvider == null) {
            return null;
        }
        return chatServerRuntimeProvider.getIfAvailable();
    }

    /**
     * 懒加载并缓存运行时桥；无 Provider 时新建默认实例。
     * Lazily loads and caches the runtime bridge; creates a default when no provider is present.
     *
     * @return 运行时桥 / runtime bridge
     */
    private synchronized ChatServerRuntimeBridge runtimeBridge() {
        if (runtimeBridge == null) {
            if (runtimeBridgeProvider == null) {
                runtimeBridge = new ChatServerRuntimeBridge();
            } else {
                runtimeBridge = runtimeBridgeProvider.getIfAvailable(ChatServerRuntimeBridge::new);
            }
        }
        return runtimeBridge;
    }
}
