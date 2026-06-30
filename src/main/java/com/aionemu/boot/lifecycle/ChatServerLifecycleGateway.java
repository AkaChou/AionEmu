package com.aionemu.boot.lifecycle;

import com.aionemu.chatserver.ChatServerRuntime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class ChatServerLifecycleGateway {

    private ObjectProvider<ChatServerRuntime> chatServerRuntimeProvider;
    private ObjectProvider<ChatServerRuntimeBridge> runtimeBridgeProvider;
    private ChatServerRuntimeBridge runtimeBridge;

    public ChatServerLifecycleGateway() {
    }

    @Autowired(required = false)
    void setChatServerRuntimeProvider(ObjectProvider<ChatServerRuntime> chatServerRuntimeProvider) {
        this.chatServerRuntimeProvider = chatServerRuntimeProvider;
    }

    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<ChatServerRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

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

    public void stop() {
        runtimeBridge().shutdown(false);
    }

    private ChatServerRuntime chatServerRuntime() {
        if (chatServerRuntimeProvider == null) {
            return null;
        }
        return chatServerRuntimeProvider.getIfAvailable();
    }

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
