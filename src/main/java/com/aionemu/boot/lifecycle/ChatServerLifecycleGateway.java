package com.aionemu.boot.lifecycle;

import com.aionemu.chatserver.ChatServerRuntime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class ChatServerLifecycleGateway {

    private ObjectProvider<ChatServerRuntime> chatServerRuntimeProvider;
    private ObjectProvider<ChatServerRuntimeBridge> runtimeBridgeProvider;

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
        ChatServerRuntime chatServerRuntime = chatServerRuntime();
        if (chatServerRuntime == null) {
            runtimeBridge().start(args);
            return;
        }
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

    private ChatServerRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new ChatServerRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(ChatServerRuntimeBridge::new);
    }
}
