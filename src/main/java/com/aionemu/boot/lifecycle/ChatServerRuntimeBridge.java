package com.aionemu.boot.lifecycle;

import com.aionemu.chatserver.ChatServer;
import com.aionemu.chatserver.ChatProcessRuntimeBridge;
import com.aionemu.chatserver.ChatServerRuntime;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 聊天服运行时桥：对接 ChatServer/ChatServerRuntime 与进程级关闭钩子。
 * Chat-server runtime bridge: wires ChatServer/ChatServerRuntime to process-level shutdown hooks.
 */
@Component
@Lazy
public class ChatServerRuntimeBridge {

    private ObjectProvider<ChatServerRuntime> chatServerRuntimeProvider;
    private ObjectProvider<ChatProcessRuntimeBridge> processBridgeProvider;
    private ChatProcessRuntimeBridge processBridge;

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
     * 注入可选的进程运行时桥提供者。
     * Injects an optional process runtime-bridge provider.
     *
     * @param processBridgeProvider 进程运行时桥提供者 / process-bridge ObjectProvider
     */
    @Autowired(required = false)
    void setProcessBridgeProvider(ObjectProvider<ChatProcessRuntimeBridge> processBridgeProvider) {
        this.processBridgeProvider = processBridgeProvider;
    }

    /**
     * 准备关闭钩子后启动聊天服（优先托管运行时）。
     * Prepares the shutdown hook, then starts chat (preferring managed runtime).
     *
     * @param args 启动参数 / startup arguments
     */
    public void start(String[] args) {
        prepareShutdown();
        ChatServerRuntime chatServerRuntime = chatServerRuntime();
        if (chatServerRuntime != null) {
            chatServerRuntime.start(args);
            return;
        }
        ChatServer.start(args);
    }

    /**
     * 注册/准备进程关闭钩子。
     * Registers/prepares the process shutdown hook.
     */
    public void prepareShutdown() {
        processBridge().shutdownHook();
    }

    /**
     * 关闭聊天进程。
     * Shuts down the chat process.
     *
     * @param restart 是否以重启意图关闭 / whether shutdown is for restart
     */
    public void shutdown(boolean restart) {
        processBridge().shutdown(restart);
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
     * 懒加载并缓存进程运行时桥。
     * Lazily loads and caches the process runtime bridge.
     *
     * @return 进程运行时桥 / process runtime bridge
     */
    private synchronized ChatProcessRuntimeBridge processBridge() {
        if (processBridge == null) {
            if (processBridgeProvider == null) {
                processBridge = new ChatProcessRuntimeBridge();
            } else {
                processBridge = processBridgeProvider.getIfAvailable(ChatProcessRuntimeBridge::new);
            }
        }
        return processBridge;
    }
}
